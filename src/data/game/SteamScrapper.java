package data.game;

import org.json.JSONObject;
import org.json.JSONTokener;
import system.os.Terminal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.stream.Stream;

/**
 * Created by LM on 08/08/2016.
 */
public class SteamScrapper {
    private final static String STEAM_PATH_REG = "reg query  /v SteamPath";

    public static ArrayList<GameEntry> getSteamApps() throws IOException {
        ArrayList<GameEntry> steamApps = new ArrayList<>();
        File steamAppsFolder = new File(getSteamAppsPath());
        String idPrefix = "appmanifest_";
        String idSuffix = ".acf";
        String namePrefix = "\t\"name\"\t\t\"";
        String nameSuffix = "\"\n\t";
        for(File file : steamAppsFolder.listFiles()){
            String fileName = file.getName();
            if(fileName.startsWith(idPrefix)){
                int id = Integer.parseInt(fileName.substring(idPrefix.length(),fileName.indexOf(idSuffix)));
                String fileString = new String(Files.readAllBytes(file.toPath()));
                int indexNamePrefix = fileString.indexOf(namePrefix);
                fileString = fileString.substring(indexNamePrefix+namePrefix.length());
                int indexNameSuffix = fileString.indexOf(nameSuffix);
                String name = fileString.substring(0,fileString.indexOf(nameSuffix));

                GameEntry entry= new GameEntry(name);
                entry.setPath("steam://rungameid/"+id);
                entry.setSteam_id(id);
                steamApps.add(entry);
            }
        }
        return steamApps;
    }
    private static String getSteamAppsPath() throws IOException {
        File vdfFile = new File(getSteamPath()+"\\steamapps\\libraryfolders.vdf");
        String returnValue=null;
        String fileString = new String(Files.readAllBytes(vdfFile.toPath()));

        String prefix = "\t\"1\"\t\t";
        if(fileString.contains(prefix)){
            int index = fileString.indexOf(prefix)+prefix.length();
            returnValue = fileString.substring(index)
                    .replace("\"","")
                    .replace("\n","")
                    .replace("}","")
                    .replace("\\\\","\\")
                    +"\\steamapps";
        }
        return returnValue;
    }
    private static String getSteamPath() throws IOException {
        Terminal terminal = new Terminal();
        String[] output = terminal.execute("reg","query","\"HKEY_CURRENT_USER\\SOFTWARE\\Valve\\Steam\"","/v","SteamPath");
        for(String s : output){
            if(s.contains("SteamPath")){
                String prefix = "    SteamPath    REG_SZ    ";
                int index = s.indexOf(prefix)+prefix.length();
                return s.substring(index);
            }
        }
        return null;
    }
    public static void main(String[] args) throws IOException {
        for (GameEntry entry : getSteamApps()){
            System.out.println(entry.getName()+","+entry.getSteam_id()+","+entry.isSteamGame());
        }
    }
}

package data.game.scrapper;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.sun.jna.platform.win32.WinNT;
import data.game.entry.GameEntry;
import data.game.scanner.FolderGameScanner;
import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONObject;
import org.json.JSONTokener;
import system.os.Terminal;
import ui.Main;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;

import static ui.Main.LOGGER;

/**
 * Created by LM on 08/08/2016.
 */
public class SteamLocalScrapper {
    public static String getSteamUserId() throws IOException {
        File vdfFile = new File(getSteamPath()+"\\config\\config.vdf");
        String returnValue=null;
        String fileString = new String(Files.readAllBytes(vdfFile.toPath()));

        String prefix = "\"SteamID\"";
        for(String line : fileString.split("\n")){
            if(line.contains(prefix)){
                int indexPrefix = line.indexOf(prefix)+prefix.length();
                String temp = line.substring(indexPrefix);
                temp = temp.replace(" ","").replace("\"","").trim();
                returnValue = temp;
            }
        }
        if(returnValue == null){
            LOGGER.error("Could not retrieve user's steam id, here is config.vdf : ");
            LOGGER.error(fileString);
        }

        LOGGER.info("Retrieved user steam id : "+returnValue);
        return returnValue;
    }
    public static ArrayList<GameEntry> getSteamAppsInstalledExcludeIgnored() throws IOException {
        ArrayList<GameEntry> steamEntries = getSteamAppsInstalled();
        ArrayList<GameEntry> result = new ArrayList<>();

        SteamPreEntry[] ignoredEntries = Main.GENERAL_SETTINGS.getSteamAppsIgnored();

        for(GameEntry entry : steamEntries){
            boolean ignored = false;
            for(SteamPreEntry pre : ignoredEntries){
                ignored = ignored || pre.getId() == entry.getSteam_id();
            }
            for(String name : FolderGameScanner.EXCLUDED_FILE_NAMES){
                ignored = ignored || name.equals(entry.getName());
            }
            if(!ignored){
                result.add(entry);
            }
        }
        return result;
    }

        private static ArrayList<GameEntry> getSteamAppsInstalled() throws IOException {
        ArrayList<SteamPreEntry> installedPreEntries = getSteamAppsInstalledPreEntries();
        ArrayList<GameEntry> result = new ArrayList<>();
        for(SteamPreEntry s : installedPreEntries){
            GameEntry g = new GameEntry(s.getName());
            g.setSteam_id(s.getId());
            result.add(g);
        }
        return result;
    }
    public static ArrayList<SteamPreEntry> getSteamAppsInstalledPreEntries() throws IOException {
        ArrayList<SteamPreEntry> steamApps = new ArrayList<>();
        File steamAppsFolder = new File(getSteamAppsPath());
        String idPrefix = "appmanifest_";
        String idSuffix = ".acf";
        String namePrefix = "\"name\"";
        for(File file : steamAppsFolder.listFiles()){
            String fileName = file.getName();
            if(fileName.contains(idPrefix)){
                int id = Integer.parseInt(fileName.substring(idPrefix.length(),fileName.indexOf(idSuffix)));

                String fileString = new String(Files.readAllBytes(file.toPath()));
                String name = "";
                for(String line : fileString.split("\n")){
                    if(line.contains(namePrefix)){
                        int indexNamePrefix = line.indexOf(namePrefix);
                        name = line.substring(indexNamePrefix+namePrefix.length());
                        name = name.replace("\"","").trim();
                    }
                }

                steamApps.add(new SteamPreEntry(name,id));
            }
        }
        return steamApps;
    }
    private static String getSteamAppsPath() throws IOException {
        File defaultPath = new File("C:\\Program Files (x86)\\Steam\\steamapps");
        File defaultCommonPath = new File("C:\\Program Files (x86)\\Steam\\steamapps\\common");
        if(defaultCommonPath.exists() && defaultPath.exists()){
            return defaultPath.getAbsolutePath();
        }

        File vdfFile = new File(getSteamPath()+"\\steamapps\\libraryfolders.vdf");
        String returnValue=null;
        String fileString = new String(Files.readAllBytes(vdfFile.toPath()));

        String prefix = "\"1\"";
        for(String line : fileString.split("\n")){
            if(line.contains(prefix)){
                int index = line.indexOf(prefix)+prefix.length();
                returnValue = line.substring(index)
                        .replace("\"","")
                        .replace("\n","")
                        .replace("}","")
                        .replace("\\\\","\\")
                        .trim()
                        +"\\steamapps";
            }
        }
        if(returnValue == null){
            LOGGER.error("Could not retrieve user's steam apps path, here is libraryfolders.vdf : ");
            LOGGER.error(fileString);
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
            LOGGER.error("Could not retrieve user's steam path, here is the reg query result : ");
        for(String s : output){
            LOGGER.error("\t"+s);

        }
        return null;
    }
    public static boolean isSteamGameRunning(int steam_id) throws IOException {
        return getSteamGameStatus(steam_id,"Running");
    }
    public static boolean isSteamGameInstalled(int steam_id) throws IOException {
        return getSteamGameStatus(steam_id,"Installed");
    }
    public static boolean isSteamGameLaunching(int steam_id) throws IOException {
        return getSteamGameStatus(steam_id,"Launching");
    }
    private static boolean getSteamGameStatus(int steam_id, String status) throws IOException {
        Terminal terminal = new Terminal();
        String[] output = terminal.execute("reg","query","\"HKEY_CURRENT_USER\\SOFTWARE\\Valve\\Steam\\Apps\\"+steam_id+"\"","/v",status);
        String result = null;
        for(String s : output){
            if(s.contains(status)){
                String prefix = status+"    REG_DWORD    0x";
                int index = s.indexOf(prefix)+prefix.length();
                result = s.substring(index,index+1);
                break;
            }
        }
        return (result!=null) && (result.equals("1"));
    }
    public static void main(String[] args) throws IOException {
        System.out.println(getSteamUserId());
    }
}

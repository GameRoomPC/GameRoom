package data.game.scrapper;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.sun.jna.platform.win32.WinNT;
import data.game.entry.GameEntry;
import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONObject;
import org.json.JSONTokener;
import system.os.Terminal;
import ui.Main;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;

/**
 * Created by LM on 08/08/2016.
 */
public class SteamLocalScrapper {

    public static String getSteamUserId() throws IOException {
        File vdfFile = new File(getSteamPath()+"\\config\\config.vdf");
        String returnValue=null;
        String fileString = new String(Files.readAllBytes(vdfFile.toPath()));

        String prefix = "\"SteamID\"\t\t\"";
        String suffix = "\"";
        if(fileString.contains(prefix)){
            int indexPrefix = fileString.indexOf(prefix)+prefix.length();
            String temp = fileString.substring(indexPrefix);
            temp = temp.substring(0,temp.indexOf(suffix));
            returnValue = temp;
        }
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

                steamApps.add(new SteamPreEntry(name,id));
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

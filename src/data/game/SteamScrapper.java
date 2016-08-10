package data.game;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;
import system.os.Terminal;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;

/**
 * Created by LM on 08/08/2016.
 */
public class SteamScrapper {

    private static JSONObject getInfoForGame(int steam_id) throws ConnectTimeoutException {
        try {
            HttpResponse<String> response = Unirest.get("http://store.steampowered.com/api/appdetails?appids=" + steam_id)
                    .header("Accept", "application/json")
                    .asString();
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getRawBody(), "UTF-8"));
            String json = reader.readLine();
            reader.close();
            JSONTokener tokener = new JSONTokener(json);
            return new JSONObject(tokener).getJSONObject(""+steam_id).getJSONObject("data");
        } catch (UnirestException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static ArrayList<SteamPreEntry> getSteamAppsInstalled() throws IOException {
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
    public static void main(String[] args) throws IOException {
        JSONObject skyrim_results = getInfoForGame(72850);
        System.out.println(skyrim_results.toString(4));
    }
}

package data;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

/**
 * Created by LM on 03/07/2016.
 */
public class GameScrapper {

    public static void main(String[] args){
        JSONArray bf4_results = searchGame("Battlefield 4");
        System.out.println(bf4_results);

        JSONArray jsonArray = getData(bf4_results.getJSONObject(0).getInt("id"));
        System.out.println(jsonArray);
    }
    public static JSONArray searchGame(String gameName){
        gameName = gameName.replace(' ', '+');
        try {
            HttpResponse<String> response = Unirest.get("https://igdbcom-internet-game-database-v1.p.mashape.com/games/?fields=name&limit=10&offset=0&search="+gameName)
                    .header("X-Mashape-Key", "8nsMgKEZ37mshwMwg2TC3Y3FYJRGp15lZycjsnduYWVMRNN8e5")
                    .header("Accept", "application/json")
                    .asString();
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getRawBody(), "UTF-8"));
            String json = reader.readLine();
            reader.close();
            JSONTokener tokener = new JSONTokener(json);
            return new JSONArray(tokener);
        } catch (UnirestException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static JSONArray getData(int id){
        try {
            HttpResponse<String> response = Unirest.get("https://igdbcom-internet-game-database-v1.p.mashape.com/games/"+id+"?fields=name,release_dates,esrb.synopsis,rating,cover")
                    .header("X-Mashape-Key", "8nsMgKEZ37mshwMwg2TC3Y3FYJRGp15lZycjsnduYWVMRNN8e5")
                    .header("Accept", "application/json")
                    .asString();
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getRawBody(), "UTF-8"));
            String json = reader.readLine();
            reader.close();
            JSONTokener tokener = new JSONTokener(json);
            return new JSONArray(tokener);
        } catch (UnirestException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}

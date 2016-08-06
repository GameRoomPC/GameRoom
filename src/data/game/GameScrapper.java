package data.game;

import ui.Main;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;

/**
 * Created by LM on 03/07/2016.
 */
public class GameScrapper {

    public static void main(String[] args) throws ConnectTimeoutException {
        JSONArray bf4_results = searchGame("Battlefield 4");
        //System.out.println(bf4_results);
        //System.out.println(getYear(1979));

        ArrayList list = new ArrayList();
        list.add(bf4_results.getJSONObject(0).getInt("id"));
        JSONArray bf4_data = getGamesData(list);
        System.out.println(bf4_data);
        //System.out.println(getEntry(bf4_data.getJSONObject(0)).getPublisher());

        /*ArrayList<Integer> list = new ArrayList<>();
        list.add(1);
        JSONArray EA_data = getCompaniesData(list);
        System.out.println(EA_data);*/
    }

    public static String getYear(int id, JSONArray gamesData) {
        ArrayList<String> years = new ArrayList<>();
        for (Object obj : gamesData.getJSONObject(indexOf(id,gamesData)).getJSONArray("release_dates")) {
            //Windows platform is number 6
            //if (((JSONObject) obj).getInt("platform") == 6) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
            try {
                years.add(sdf.format(new Date((long) ((JSONObject) obj).getLong("date"))));
            }catch(JSONException je){
                if(!je.toString().contains("date")){
                    je.printStackTrace();
                }
            }
            //}
        }
        years.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });
        return ((years.size() > 0) ? years.get(0) : "");
    }

    public static int indexOf(int id, JSONArray data) {
        int i = 0;
        for (Object obj : data) {
            if (((JSONObject) obj).getInt("id") == id) {
                return i;
            }
            i++;
        }
        return -1;
    }
    public static String getScreenshotImage(String screenshot_size, JSONObject jsob){
        //TODO implement multiple screenshots downloads?
        String cloudinary_id = jsob.getJSONArray("screenshots").getJSONObject(0).getString("cloudinary_id");
        return "https://res.cloudinary.com/igdb/image/upload/t_" + screenshot_size + "/" + cloudinary_id + ".jpg";
    }
    public static String getCoverImage(String cover_size, JSONObject jsob){
        String cloudinary_id = jsob.getJSONObject("cover").getString("cloudinary_id");
        return "https://res.cloudinary.com/igdb/image/upload/t_" + cover_size + "/" + cloudinary_id + ".jpg";
    }
    public static String getScreenshotImage(int id, String screenshot_size, JSONArray gamesData) {
        return getScreenshotImage(screenshot_size,gamesData.getJSONObject(indexOf(id, gamesData)));
    }
    public static String getCoverImage(int id, String cover_size, JSONArray gamesData) {
        return getCoverImage(cover_size,gamesData.getJSONObject(indexOf(id, gamesData)));
    }
    /*public static GameEntry getEntry(int id){
        JSONArray gamesData = getGamesData(new ArrayList<>(id));
        return getEntry(gamesData.getJSONObject(indexOf(id,gamesData)));
    }*/

    public static GameEntry getEntry(JSONObject game_data) {
        GameEntry entry = new GameEntry(game_data.getString("name"));
        entry.setSavedLocaly(false);

        try {
            entry.setDescription(game_data.getJSONObject("esrb").getString("synopsis"));
        } catch (JSONException je) {
            if(je.toString().contains("not found")){
                Main.LOGGER.warn(entry.getName()+" : no synopsis");
            }else{
                je.printStackTrace();
            }
        }

        try {
            ArrayList<String> years = new ArrayList<>();
            for (Object obj : game_data.getJSONArray("release_dates")) {
                //Windows platform is number 6
                //if (((JSONObject) obj).getInt("platform") == 6) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
                    years.add(sdf.format(new Date((long) ((JSONObject) obj).getLong("date"))));
                //}
            }
            years.sort(new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    return o1.compareTo(o2);
                }
            });
            entry.setYear(years.get(0));
        } catch (JSONException je) {
            if(je.toString().contains("not found")){
                Main.LOGGER.warn(entry.getName()+" : no year");
            }else{
                je.printStackTrace();
            }
        }
        try {
            String publishers = "";
            int publishersNumber = game_data.getJSONArray("publishers").length();
            ArrayList<Integer> companiesIDS = new ArrayList<>();
            for (int i = 0; i < publishersNumber; i++) {
                companiesIDS.add(game_data.getJSONArray("publishers").getInt(i));
                //System.out.println("TEST :"+ game_data.getJSONArray("publishers").getInt(i));
            }
            JSONArray companiesData = getCompaniesData(companiesIDS);

            for (int i = 0; i < publishersNumber; i++) {
                publishers += getCompanyName(game_data.getJSONArray("publishers").getInt(i),companiesData);
                if (i != publishersNumber - 1) {
                    publishers += ", ";
                }
            }
            entry.setPublisher(publishers);
        } catch (JSONException je) {
            if(je.toString().contains("not found")){
                Main.LOGGER.warn(entry.getName()+" : no publishers");
            }else{
                je.printStackTrace();
            }
        }
        try {
            String developers = "";
            int developersNumber = game_data.getJSONArray("developers").length();
            ArrayList<Integer> companiesIDS = new ArrayList<>();
            for (int i = 0; i < developersNumber; i++) {
                companiesIDS.add(game_data.getJSONArray("developers").getInt(i));
            }
            JSONArray companiesData = getCompaniesData(companiesIDS);

            for (int i = 0; i < developersNumber; i++) {
                developers += getCompanyName(game_data.getJSONArray("developers").getInt(i),companiesData);
                if (i != developersNumber - 1) {
                    developers += ", ";
                }
            }
            entry.setDeveloper(developers);
        } catch (JSONException je) {
            if(je.toString().contains("not found")){
                Main.LOGGER.warn(entry.getName()+" : no developers");
            }else{
                je.printStackTrace();
            }
        }
        try {
            entry.setIgdb_imageURL(0, GameScrapper.getCoverImage("cover_big_2x", game_data));
            entry.setIgdb_imageURL(1, GameScrapper.getScreenshotImage("screenshot_big_2x", game_data));
        } catch (JSONException je) {
            if(je.toString().contains("not found")){
                Main.LOGGER.warn(entry.getName()+" : no cover");
            }else{
                je.printStackTrace();
            }
        }
        try {
            entry.setIgdb_ID(game_data.getInt("id"));
        } catch (JSONException je) {
            if(je.toString().contains("not found")){
                Main.LOGGER.warn(entry.getName()+" : no id");
            }else{
                je.printStackTrace();
            }
        }

        return entry;
    }

    public static JSONArray searchGame(String gameName) throws ConnectTimeoutException{
        gameName = gameName.replace(' ', '+');
        try {
            HttpResponse<String> response = Unirest.get("https://igdbcom-internet-game-database-v1.p.mashape.com/games/?fields=name&limit=10&offset=0&search=" + gameName)
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

    public static JSONArray getGamesData(Collection<Integer> ids) {
        try {
            String idsString = "";
            for (Integer id : ids) {
                idsString += id + ",";
            }
            HttpResponse<String> response = Unirest.get("https://igdbcom-internet-game-database-v1.p.mashape.com/games/" + idsString.substring(0, idsString.length() - 1) + "?fields=name,release_dates,esrb.synopsis,rating,cover,developers,publishers,screenshots")
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

    private static String getCompanyName(int id, JSONArray companiesData) {
        try {
            return companiesData.getJSONObject(indexOf(id, companiesData)).getString("name");
        } catch (JSONException je) {
            je.printStackTrace();
            return "";
        }
    }

    private static JSONArray getCompaniesData(Collection<Integer> ids) {
        String idsString = "";
        for (Integer id : ids) {
            idsString += id + ",";
        }
        try {
            HttpResponse<String> response = Unirest.get("https://igdbcom-internet-game-database-v1.p.mashape.com/companies/" + idsString.substring(0, idsString.length() - 1) + /*"?fields=*"+*/ "?fields=name")
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

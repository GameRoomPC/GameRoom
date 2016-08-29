package data.game.scrapper;

import data.game.entry.GameEntry;
import data.game.entry.GameGenre;
import data.game.entry.GameTheme;
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
public class IGDBScrapper {

    public static void main(String[] args) throws ConnectTimeoutException {
        JSONArray bf4_results = searchGame("iwuefbuiw");
        ArrayList list = new ArrayList();
        list.add(bf4_results.getJSONObject(0).getInt("id"));
        JSONArray bf4_data = null;
        try {
            bf4_data = getGamesData(list);
            System.out.println(bf4_data.toString(4));
        } catch (UnirestException e) {
            e.printStackTrace();
        }

        //System.out.println(getSerie(bf4_data.getJSONObject(0)));

        //System.out.println(getThemes(bf4_data.getJSONObject(0)));

        /*ArrayList<Integer> list = new ArrayList();
        list.add(12);
        list.add(31);
        JSONArray genres_data = getGenresData(list);
        System.out.println(genres_data);
        System.out.println(GameGenre.getThemeFromIGDB(getGenreName(12,genres_data)));*/


    }

    public static JSONArray getAllFields(int id) {
        try {
            HttpResponse<String> response = Unirest.get("https://igdbcom-internet-game-database-v1.p.mashape.com/games/" + id + "?fields=*")
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

    public static Date getReleaseDate(JSONObject gameData) {
        ArrayList<Date> releaseDates = new ArrayList<>();
        try {
            for (Object obj : gameData.getJSONArray("release_dates")) {
                //Windows platform is number 6
                //if (((JSONObject) obj).getInt("platform") == 6) {
                try {
                    releaseDates.add(new Date((long) ((JSONObject) obj).getLong("date")));
                } catch (JSONException je) {
                    if (!je.toString().contains("date")) {
                        je.printStackTrace();
                    }
                }
                //}
            }
            releaseDates.sort(new Comparator<Date>() {
                @Override
                public int compare(Date o1, Date o2) {
                    return o1.compareTo(o2);
                }
            });
            return ((releaseDates.size() > 0) ? releaseDates.get(0) : null);
        } catch (JSONException jse) {
            if (jse.toString().contains("not found")) {
                //Main.LOGGER.error("Year not found");
            } else {
                jse.printStackTrace();
            }
        }
        return null;
    }

    public static Date getReleaseDate(int id, JSONArray gamesData) {
        return getReleaseDate(gamesData.getJSONObject(indexOf(id, gamesData)));
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

    public static String[] getScreenshotHash(JSONObject jsob) {
        JSONArray screenshotsArray = jsob.getJSONArray("screenshots");
        String[] result = new String[screenshotsArray.length()];
        for (int i = 0; i < screenshotsArray.length(); i++) {
            result[i] = screenshotsArray.getJSONObject(i).getString("cloudinary_id");
        }
        return result;
    }

    public static String getCoverImageHash(JSONObject jsob) {
        String cloudinary_id = jsob.getJSONObject("cover").getString("cloudinary_id");
        return cloudinary_id;
    }

    public static String[] getScreenshotHash(int id, JSONArray gamesData) {
        return getScreenshotHash(gamesData.getJSONObject(indexOf(id, gamesData)));
    }

    public static String getCoverImageHash(int id, JSONArray gamesData) {
        return getCoverImageHash(gamesData.getJSONObject(indexOf(id, gamesData)));
    }

    /*public static GameEntry getEntry(int id){
        JSONArray gamesData = getGamesData(new ArrayList<>(id));
        return getEntry(gamesData.getJSONObject(indexOf(id,gamesData)));
    }*/
    public static ArrayList<GameEntry> getEntries(JSONArray searchData) {
        ArrayList<GameEntry> entries = new ArrayList<>();
        ArrayList<Integer> companiesIDs = new ArrayList<>();
        ArrayList<Integer> seriesIDs = new ArrayList<>();

        for (int i = 0; i < searchData.length(); i++) {
            try {
                int publishersNumber = searchData.getJSONObject(i).getJSONArray("publishers").length();
                for (int j = 0; j < publishersNumber; j++) {
                    companiesIDs.add(searchData.getJSONObject(i).getJSONArray("publishers").getInt(j));
                }
            } catch (JSONException je) {
                if (je.toString().contains("not found")) {
                    //Main.LOGGER.warn(entry.getName()+" : no publishers");
                } else {
                    je.printStackTrace();
                }
            }

            try {
                int developersNumber = searchData.getJSONObject(i).getJSONArray("developers").length();
                for (int j = 0; j < developersNumber; j++) {
                    companiesIDs.add(searchData.getJSONObject(i).getJSONArray("developers").getInt(j));
                }
            } catch (JSONException je) {
                if (je.toString().contains("not found")) {
                    //Main.LOGGER.warn(entry.getName()+" : no publishers");
                } else {
                    je.printStackTrace();
                }
            }

            try {
                seriesIDs.add(searchData.getJSONObject(i).getInt("collection"));
            } catch (JSONException je) {
                if (je.toString().contains("not found")) {
                    //Main.LOGGER.warn(entry.getName()+" : no publishers");
                } else {
                    je.printStackTrace();
                }
            }
        }
        JSONArray companiesData = getCompaniesData(companiesIDs);
        JSONArray seriesData = getSeriesData(seriesIDs);


        for (int i = 0; i < searchData.length(); i++) {
            GameEntry entry = getEntry(searchData.getJSONObject(i), false);

            try {
                int publishersNumber = searchData.getJSONObject(i).getJSONArray("publishers").length();
                String publishers = "";
                if(companiesData!=null) {
                    for (int j = 0; j < publishersNumber; j++) {
                        publishers += getCompanyName(searchData.getJSONObject(i).getJSONArray("publishers").getInt(j), companiesData);
                        if (j != publishersNumber - 1) {
                            publishers += ", ";
                        }
                    }
                }
                entry.setPublisher(publishers);
            } catch (JSONException je) {
                if (je.toString().contains("not found")) {
                    //Main.LOGGER.warn(entry.getName()+" : no publishers");
                } else {
                    je.printStackTrace();
                }
            }

            try {
                String developers = "";
                if(companiesData!=null) {
                    int developersNumber = searchData.getJSONObject(i).getJSONArray("developers").length();
                    for (int j = 0; j < developersNumber; j++) {
                        developers += getCompanyName(searchData.getJSONObject(i).getJSONArray("developers").getInt(j), companiesData);
                        if (j != developersNumber - 1) {
                            developers += ", ";
                        }
                    }
                }
                entry.setDeveloper(developers);
            } catch (JSONException je) {
                if (je.toString().contains("not found")) {
                    //Main.LOGGER.warn(entry.getName()+" : no devs");
                } else {
                    je.printStackTrace();
                }
            }

            try {
                int serieId = searchData.getJSONObject(i).getInt("collection");
                if(seriesData!=null) {
                    entry.setSerie(getSerieName(serieId, seriesData));
                }
            } catch (JSONException je) {
                if (je.toString().contains("not found")) {
                    //Main.LOGGER.warn(entry.getName()+" : no serie");
                } else {
                    je.printStackTrace();
                }
            }
            entries.add(entry);
        }
        return entries;
    }

    public static GameEntry getEntry(JSONObject game_data) {
        return getEntry(game_data, true);
    }

    public static GameEntry getEntry(JSONObject game_data, boolean allowUseMoreRequest) {
        GameEntry entry = new GameEntry(game_data.getString("name"));
        entry.setSavedLocaly(false);

        try {
            entry.setDescription(game_data.getString("summary"));
        } catch (JSONException je) {
            if (je.toString().contains("not found")) {
                //Main.LOGGER.warn(entry.getName()+" : no synopsis");
            } else {
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
            entry.setReleaseDate(getReleaseDate(game_data));
        } catch (JSONException je) {
            if (je.toString().contains("not found")) {
                //Main.LOGGER.warn(entry.getName()+" : no release date");
            } else {
                je.printStackTrace();
            }
        }
        if (allowUseMoreRequest) {
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
                    publishers += getCompanyName(game_data.getJSONArray("publishers").getInt(i), companiesData);
                    if (i != publishersNumber - 1) {
                        publishers += ", ";
                    }
                }
                entry.setPublisher(publishers);
            } catch (JSONException je) {
                if (je.toString().contains("not found")) {
                    //Main.LOGGER.warn(entry.getName()+" : no publishers");
                } else {
                    je.printStackTrace();
                }
            }

            try {
                entry.setSerie(getSerie(game_data));
            } catch (JSONException je) {
                if (je.toString().contains("not found")) {
                    //Main.LOGGER.warn(entry.getName()+" : no serie");
                } else {
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
                    developers += getCompanyName(game_data.getJSONArray("developers").getInt(i), companiesData);
                    if (i != developersNumber - 1) {
                        developers += ", ";
                    }
                }
                entry.setDeveloper(developers);
            } catch (JSONException je) {
                if (je.toString().contains("not found")) {
                    //Main.LOGGER.warn(entry.getName()+" : no developers");
                } else {
                    je.printStackTrace();
                }
            }
        }
        try {
            entry.setGenres(getGenres(game_data));
        } catch (JSONException je) {
            if (je.toString().contains("not found")) {
                //Main.LOGGER.warn(entry.getName()+" : no developers");
            } else {
                je.printStackTrace();
            }
        }
        try {
            entry.setThemes(getThemes(game_data));
        } catch (JSONException je) {
            if (je.toString().contains("not found")) {
                //Main.LOGGER.warn(entry.getName()+" : no developers");
            } else {
                je.printStackTrace();
            }
        }

        try {
            entry.setIgdb_imageHash(0, IGDBScrapper.getCoverImageHash(game_data));
            String[] screenshotsHashes = IGDBScrapper.getScreenshotHash(game_data);
            for (int i = 0; i < screenshotsHashes.length; i++) {
                entry.setIgdb_imageHash(i + 1, screenshotsHashes[i]);
            }
        } catch (JSONException je) {
            if (je.toString().contains("not found")) {
                //Main.LOGGER.warn(entry.getName()+" : no cover");
            } else {
                je.printStackTrace();
            }
        }
        try {
            entry.setIgdb_id(game_data.getInt("id"));
        } catch (JSONException je) {
            if (je.toString().contains("not found")) {
                //Main.LOGGER.warn(entry.getName()+" : no id");
            } else {
                je.printStackTrace();
            }
        }
        try {
            entry.setAggregated_rating(game_data.getInt("aggregated_rating"));
        } catch (JSONException je) {
            if (je.toString().contains("not found")) {
                //Main.LOGGER.warn(entry.getName()+" : no aggregated_rating");
            } else {
                je.printStackTrace();
            }
        }

        return entry;
    }

    public static JSONArray searchGame(String gameName) throws ConnectTimeoutException {
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
            Main.LOGGER.error("Not connected to internet");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject getGameData(int id) throws UnirestException {
        ArrayList<Integer> list = new ArrayList<>();
        list.add(id);
        return getGamesData(list).getJSONObject(0);
    }

    public static JSONArray getGenresData(Collection<Integer> ids) {
        try {
            String idsString = "";
            for (Integer id : ids) {
                idsString += id + ",";
            }
            HttpResponse<String> response = Unirest.get("https://igdbcom-internet-game-database-v1.p.mashape.com/genres/" + idsString.substring(0, idsString.length() - 1) + "?fields=name")
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

    public static JSONArray getGamesData(Collection<Integer> ids) throws UnirestException {
        try {
            String idsString = "";
            for (Integer id : ids) {
                idsString += id + ",";
            }
            HttpResponse<String> response = Unirest.get("https://igdbcom-internet-game-database-v1.p.mashape.com/games/" + idsString.substring(0, idsString.length() - 1) + "?fields=*")
                    .header("X-Mashape-Key", "8nsMgKEZ37mshwMwg2TC3Y3FYJRGp15lZycjsnduYWVMRNN8e5")
                    .header("Accept", "application/json")
                    .asString();
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getRawBody(), "UTF-8"));
            String json = reader.readLine();
            reader.close();
            JSONTokener tokener = new JSONTokener(json);
            return new JSONArray(tokener);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static GameGenre[] getGenres(JSONObject gameData) {
        try {
            int genresNumber = gameData.getJSONArray("genres").length();
            GameGenre[] gameGenres = new GameGenre[genresNumber];
            for (int i = 0; i < genresNumber; i++) {
                int genreId = gameData.getJSONArray("genres").getInt(i);
                gameGenres[i] = GameGenre.getGenreFromIGDB(genreId);
            }
            return gameGenres;
        } catch (JSONException jse) {
            if (jse.toString().contains("not found")) {
                //Main.LOGGER.error("Genres not found");
            } else {
                jse.printStackTrace();
            }
        }
        return null;
    }

    private static GameTheme[] getThemes(JSONObject gameData) {
        try {
            int themesNumber = gameData.getJSONArray("themes").length();
            GameTheme[] gameThemes = new GameTheme[themesNumber];
            for (int i = 0; i < themesNumber; i++) {
                int genreId = gameData.getJSONArray("themes").getInt(i);
                gameThemes[i] = GameTheme.getThemeFromIGDB(genreId);
            }
            return gameThemes;
        } catch (JSONException jse) {
            if (jse.toString().contains("not found")) {
                //Main.LOGGER.error("Themes not found");
            } else {
                jse.printStackTrace();
            }
        }
        return null;
    }

    private static String getSerie(JSONObject gameData) {
        try {
            int serieId = gameData.getInt("collection");
            HttpResponse<String> response = Unirest.get("https://igdbcom-internet-game-database-v1.p.mashape.com/collections/" + serieId + "?fields=name")
                    .header("X-Mashape-Key", "8nsMgKEZ37mshwMwg2TC3Y3FYJRGp15lZycjsnduYWVMRNN8e5")
                    .header("Accept", "application/json")
                    .asString();
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getRawBody(), "UTF-8"));
            String json = reader.readLine();
            reader.close();
            JSONTokener tokener = new JSONTokener(json);
            return new JSONArray(tokener).getJSONObject(0).getString("name");
        } catch (JSONException jse) {
            if (jse.toString().contains("not found")) {
                //Main.LOGGER.error("Serie not found");
            } else {
                jse.printStackTrace();
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getSerieName(int id, JSONArray serieData) {
        try {
            return serieData.getJSONObject(indexOf(id, serieData)).getString("name");
        } catch (JSONException je) {
            je.printStackTrace();
            return "";
        }
    }

    private static JSONArray getSeriesData(Collection<Integer> ids) {
        if(ids.size()<1){
            return null;
        }
        String idsString = "";
        for (Integer id : ids) {
            idsString += id + ",";
        }
        try {
            HttpResponse<String> response = Unirest.get("https://igdbcom-internet-game-database-v1.p.mashape.com/collections/" + idsString.substring(0, idsString.length() - 1) + /*"?fields=*"+*/ "?fields=name")
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
        if(ids.size()<1){
            return null;
        }
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

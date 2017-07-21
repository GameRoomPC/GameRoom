package data.game.scraper;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import data.game.entry.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Created by LM on 03/07/2016.
 */
public class IGDBScraper {
    public static String IGDB_BASIC_KEY = "ntso6TigR0msheVZZBFQPyOuqu6tp1OdtgFjsnkTZXRLTj9tgb";
    public static String IGDB_PRO_KEY = "ntso6TigR0msheVZZBFQPyOuqu6tp1OdtgFjsnkTZXRLTj9tgb";
    public static String key = IGDB_BASIC_KEY;

    public static void main(String[] args) throws IOException, UnirestException {
        String gameName = "Battlefield 1";
        JSONArray bf4_results = searchGame(gameName);
        if (bf4_results != null) {
            ArrayList list = new ArrayList();
            list.add(bf4_results.getJSONObject(0).getInt("id"));
            JSONArray bf4_data = null;
            try {
                bf4_data = getGamesData(list);
                System.out.println(bf4_data.toString(4));
            } catch (UnirestException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Found no matching game for " + gameName);
        }
    }

    public static JSONArray getAllFields(int id) throws UnirestException {
        HttpResponse<JsonNode> response = Unirest.get("https://igdbcom-internet-game-database-v1.p.mashape.com/games/" + id + "?fields=*")
                .header("X-Mashape-Key", key)
                .header("Accept", "application/json")
                .asJson();

        if (response.getBody().isArray()) {
            return response.getBody().getArray();
        }
        return null;
    }

    private static Date getReleaseDate(JSONObject gameData) {
        ArrayList<Date> releaseDates = new ArrayList<>();
        try {
            for (Object obj : gameData.getJSONArray("release_dates")) {
                //Windows platform is number 6
                //if (((JSONObject) obj).getInt("platform") == 6) {
                try {
                    releaseDates.add(new Date(((JSONObject) obj).getLong("date")));
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

    private static int indexOf(int id, JSONArray data) {
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
        return jsob.getJSONObject("cover").getString("cloudinary_id");
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
    public static ArrayList<GameEntry> getEntries(JSONArray searchData) throws UnirestException {
        ArrayList<GameEntry> entries = new ArrayList<>();

        JSONArray companiesData = getCompaniesData(getUnknownCompaniesIDs(searchData));
        JSONArray seriesData = getSeriesData(getUnknownSeriesIDs(searchData));


        for (int i = 0; i < searchData.length(); i++) {
            JSONObject gameData = searchData.getJSONObject(i);
            GameEntry entry = getEntry(gameData, false);
            setGameCompanies(entry, gameData, companiesData);
            setGameSerie(entry, gameData, seriesData);

            entries.add(entry);
            try {
                Thread.sleep(2 * 100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return entries;
    }

    public static GameEntry getEntry(JSONObject game_data) {
        return getEntry(game_data, true);
    }

    private static GameEntry getEntry(JSONObject game_data, boolean allowUseMoreRequest) {
        GameEntry entry = new GameEntry(game_data.getString("name"));
        entry.setSavedLocally(false);

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
                years.add(sdf.format(new Date(((JSONObject) obj).getLong("date"))));
                //}
            }
            years.sort(new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    return o1.compareTo(o2);
                }
            });
            Date releaseDate = getReleaseDate(game_data);
            LocalDateTime date = releaseDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            entry.setReleaseDate(date);
        } catch (JSONException je) {
            if (je.toString().contains("not found")) {
                //Main.LOGGER.warn(entry.getName()+" : no release date");
            } else {
                je.printStackTrace();
            }
        }
        if (allowUseMoreRequest) {
            JSONArray companiesData = getCompaniesData(getUnknownCompaniesIDs(game_data));
            JSONArray seriesData = getSeriesData(getUnknownSeriesIDs(game_data));
            setGameCompanies(entry, game_data, companiesData);
            setGameSerie(entry, game_data, seriesData);
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
            entry.setIgdb_imageHash(0, IGDBScraper.getCoverImageHash(game_data));
            String[] screenshotsHashes = IGDBScraper.getScreenshotHash(game_data);
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

    public static JSONArray searchGame(String gameName) throws UnirestException {
        gameName = gameName.replace(' ', '+');
        HttpResponse<JsonNode> response = Unirest.get("https://igdbcom-internet-game-database-v1.p.mashape.com/games/?fields=name&limit=10&offset=0&search=" + gameName)
                .header("X-Mashape-Key", key)
                .header("Accept", "application/json")
                .asJson();

        if (response.getBody().isArray()) {
            return response.getBody().getArray();
        }
        return null;
    }

    public static JSONObject getGameData(int id) throws UnirestException {
        ArrayList<Integer> list = new ArrayList<>();
        list.add(id);
        return getGamesData(list).getJSONObject(0);
    }

    public static JSONArray getGenresData(Collection<Integer> ids) throws UnirestException {
        String idsString = "";
        int i = 0;
        for (Integer id : ids) {
            if (id >= 0) {
                idsString += id;
                if (i != ids.size() - 1) {
                    idsString += ",";
                }
            }
            i++;
        }
        HttpResponse<JsonNode> response = Unirest.get("https://igdbcom-internet-game-database-v1.p.mashape.com/genres/" + idsString + "?fields=name")
                .header("X-Mashape-Key", key)
                .header("Accept", "application/json")
                .asJson();
        if (response.getBody().isArray()) {
            return response.getBody().getArray();
        }
        return null;
    }

    public static JSONArray getGamesData(Collection<Integer> ids) throws UnirestException {
        String idsString = "";
        int i = 0;
        for (Integer id : ids) {
            if (id >= 0) {
                idsString += id;
                if (i != ids.size() - 1) {
                    idsString += ",";
                }
            }
            i++;
        }
        /*LOGGER.debug("IDS string: \"" + idsString+"\"");

        HttpResponse<String> responseString = Unirest.get("https://igdbcom-internet-game-database-v1.p.mashape.com/games/" + idsString + "?fields=*")
                .header("X-Mashape-Key", key)
                .header("Accept", "application/json")
                .asString();
        LOGGER.debug("Response getGamesData:" + responseString.getBody());*/

        HttpResponse<JsonNode> response = Unirest.get("https://igdbcom-internet-game-database-v1.p.mashape.com/games/" + idsString + "?fields=*")
                .header("X-Mashape-Key", key)
                .header("Accept", "application/json")
                .asJson();
        if (response.getBody().isArray()) {
            return response.getBody().getArray();
        }
        return null;
    }

    private static ArrayList<GameGenre> getGenres(JSONObject gameData) {
        try {
            int genresNumber = gameData.getJSONArray("genres").length();
            ArrayList<GameGenre> genres = new ArrayList<>();

            for (int i = 0; i < genresNumber; i++) {
                int genreId = gameData.getJSONArray("genres").getInt(i);
                genres.add(GameGenre.getGenreFromID(genreId));
            }
            return genres;
        } catch (JSONException jse) {
            if (jse.toString().contains("not found")) {
                //Main.LOGGER.error("Genres not found");
            } else {
                jse.printStackTrace();
            }
        }
        return null;
    }

    private static ArrayList<GameTheme> getThemes(JSONObject gameData) {
        try {
            int themesNumber = gameData.getJSONArray("themes").length();
            ArrayList<GameTheme> themes = new ArrayList<>();

            for (int i = 0; i < themesNumber; i++) {
                int genreId = gameData.getJSONArray("themes").getInt(i);
                themes.add(GameTheme.getThemeFromId(genreId));
            }
            return themes;
        } catch (JSONException jse) {
            if (jse.toString().contains("not found")) {
                //Main.LOGGER.error("Themes not found");
            } else {
                jse.printStackTrace();
            }
        }
        return null;
    }

    private static String getSerie(JSONObject gameData) throws UnirestException {
        int serieId = gameData.getInt("collection");
        HttpResponse<JsonNode> response = Unirest.get("https://igdbcom-internet-game-database-v1.p.mashape.com/collections/" + serieId + "?fields=name")
                .header("X-Mashape-Key", key)
                .header("Accept", "application/json")
                .asJson();

        if (response.getBody().isArray()) {
            try {
                JSONArray array = response.getBody().getArray();
                return array.getJSONObject(0).getString("name");
            } catch (JSONException jse) {
                if (jse.toString().contains("not found")) {
                    //Main.LOGGER.error("Serie not found");
                }
            }
        }
        return null;
    }

    private static Serie getSerie(int id, JSONArray serieData) {
        Serie s = Serie.getFromIGDBId(id);
        try {
            if (s == null && serieData != null) {
                String name = serieData.getJSONObject(indexOf(id, serieData)).getString("name");
                s = new Serie(id, name);
            }
        } catch (JSONException je) {
            return Serie.NONE;
        }
        return s;
    }

    private static JSONArray getSeriesData(Collection<Integer> ids) {
        if (ids.size() < 1) {
            return null;
        }
        String idsString = "";
        int i = 0;
        for (Integer id : ids) {
            if (id >= 0) {
                idsString += id;
                if (i != ids.size() - 1) {
                    idsString += ",";
                }
            }
            i++;
        }
        try {

            HttpResponse<JsonNode> response = Unirest.get("https://igdbcom-internet-game-database-v1.p.mashape.com/collections/" + idsString + /*"?fields=*"+*/ "?fields=name")
                    .header("X-Mashape-Key", key)
                    .header("Accept", "application/json")
                    .asJson();
            if (response.getBody().isArray()) {
                return response.getBody().getArray();
            }

        } catch (UnirestException e) {
            //there was no serie ?
        }
        return null;
    }

    private static Company extractCompany(int id, JSONArray companiesData) {
        Company c = Company.getFromIGDBId(id);
        try {
            if (c == null && companiesData != null) {
                String name = companiesData.getJSONObject(indexOf(id, companiesData)).getString("name");
                c = new Company(id, name,true);
            }
        } catch (JSONException je) {
            je.printStackTrace();
        }
        return c;
    }

    private static JSONArray getCompaniesData(Collection<Integer> ids) {
        if (ids.size() < 1) {
            return null;
        }
        String idsString = "";
        int i = 0;
        for (Integer id : ids) {
            if (id >= 0) {
                idsString += id;
                if (i != ids.size() - 1) {
                    idsString += ",";
                }
            }
            i++;
        }
        try {
            HttpResponse<JsonNode> response = Unirest.get("https://igdbcom-internet-game-database-v1.p.mashape.com/companies/" + idsString + /*"?fields=*"+*/ "?fields=name")
                    .header("X-Mashape-Key", key)
                    .header("Accept", "application/json")
                    .asJson();

            if (response.getBody().isArray()) {
                return response.getBody().getArray();
            }
            return null;
        } catch (UnirestException e) {
            //there was no company ?
        }
        return null;
    }

    /**
     * Looks up in db if knows a company given its id, or adds it to the result set
     *
     * @param searchData a json object containing the research on a game
     * @return a hashset containing IDs of yet unknown companies
     */
    private static HashSet<Integer> getUnknownCompaniesIDs(JSONObject searchData) {
        HashSet<Integer> companiesIDs = new HashSet<>();
        try {
            int publishersNumber = searchData.getJSONArray("publishers").length();
            for (int j = 0; j < publishersNumber; j++) {
                int igdbId = searchData.getJSONArray("publishers").getInt(j);
                Company publi = Company.getFromIGDBId(igdbId);
                if (publi == null) {
                    companiesIDs.add(igdbId);
                }
            }
        } catch (JSONException je) {
            if (je.toString().contains("not found")) {
                //Main.LOGGER.warn(entry.getName()+" : no publishers");
            } else {
                je.printStackTrace();
            }
        }

        try {
            int developersNumber = searchData.getJSONArray("developers").length();
            for (int j = 0; j < developersNumber; j++) {
                int igdbId = searchData.getJSONArray("developers").getInt(j);
                Company dev = Company.getFromIGDBId(igdbId);
                if (dev == null) {
                    companiesIDs.add(igdbId);
                }
            }
        } catch (JSONException je) {
            if (je.toString().contains("not found")) {
                //Main.LOGGER.warn(entry.getName()+" : no publishers");
            } else {
                je.printStackTrace();
            }
        }
        return companiesIDs;
    }

    /**
     * See @getUnknownCompaniesIDs(JSONObject searchData)
     *
     * @param searchData a JSONArray of search about games
     * @return a hashset containing idsof yet unknown companies
     */
    private static HashSet<Integer> getUnknownCompaniesIDs(JSONArray searchData) {
        HashSet<Integer> companiesIDs = new HashSet<>();
        for (int i = 0; i < searchData.length(); i++) {
            companiesIDs.addAll(getUnknownCompaniesIDs(searchData.getJSONObject(i)));
        }
        return companiesIDs;
    }

    /**
     * Given searchData on a game and companiesData, update it with companies
     *
     * @param searchData    data fetched about a game
     * @param companiesData data fetched about companies
     */
    private static void setGameCompanies(GameEntry entryToSet, JSONObject searchData, JSONArray companiesData) {
        ArrayList<Company> companies = new ArrayList<>();
        try {
            int publishersNumber = searchData.getJSONArray("publishers").length();
            for (int j = 0; j < publishersNumber; j++) {
                int igdbId = searchData.getJSONArray("publishers").getInt(j);
                companies.add(extractCompany(igdbId, companiesData));
            }
        } catch (JSONException je) {
            if (je.toString().contains("not found")) {
                //Main.LOGGER.warn(entry.getName()+" : no publishers");
            } else {
                je.printStackTrace();
            }
        }
        companies.removeIf(Objects::isNull);
        entryToSet.setPublishers(companies);

        companies.clear();
        try {
            int developersNumber = searchData.getJSONArray("developers").length();

            for (int j = 0; j < developersNumber; j++) {
                int igdbId = searchData.getJSONArray("developers").getInt(j);
                companies.add(extractCompany(igdbId, companiesData));
            }
        } catch (JSONException je) {
            if (je.toString().contains("not found")) {
                //Main.LOGGER.warn(entry.getName()+" : no devs");
            } else {
                je.printStackTrace();
            }
        }
        companies.removeIf(Objects::isNull);
        entryToSet.setDevelopers(companies);

    }

    /**
     * Looks up in db if knows a serie given its id, or adds it to the result set
     *
     * @param searchData a json object containing the research on a game
     * @return a hashset containing IDs of yet unknown series
     */
    private static HashSet<Integer> getUnknownSeriesIDs(JSONObject searchData) {
        HashSet<Integer> seriesIDs = new HashSet<>();
        try {
            int serieId = searchData.getInt("collection");
            Serie serie = Serie.getFromIGDBId(serieId);
            if (serie == null) {
                seriesIDs.add(serieId);
            }
        } catch (JSONException je) {
            if (je.toString().contains("not found")) {
                //Main.LOGGER.warn(entry.getName()+" : no publishers");
            } else {
                je.printStackTrace();
            }
        }
        return seriesIDs;
    }

    /**
     * See @getUnknownSeriesIDs(JSONObject searchData)
     *
     * @param searchData a JSONArray of search about games
     * @return a hashset containing ids of yet unknown serie
     */
    private static HashSet<Integer> getUnknownSeriesIDs(JSONArray searchData) {
        HashSet<Integer> seriesIDs = new HashSet<>();
        for (int i = 0; i < searchData.length(); i++) {
            seriesIDs.addAll(getUnknownSeriesIDs(searchData.getJSONObject(i)));
        }
        return seriesIDs;
    }

    /**
     * Given searchData on a game and serieDAta, update it with corresponding serie
     *
     * @param searchData data fetched about a game
     * @param seriesData data fetched about series
     */
    private static void setGameSerie(GameEntry entryToSet, JSONObject searchData, JSONArray seriesData) {

        try {
            int serieId = searchData.getInt("collection");
            entryToSet.setSerie(getSerie(serieId, seriesData));
        } catch (JSONException je) {
            if (je.toString().contains("not found")) {
                //Main.LOGGER.warn(entry.getName()+" : no serie");
            } else {
                je.printStackTrace();
            }
        }
    }
}

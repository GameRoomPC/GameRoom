package data.game.scraper;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import data.game.entry.*;
import data.io.DataBase;
import org.apache.commons.lang.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import ui.Main;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static ui.Main.LOGGER;

/**
 * Created by LM on 03/07/2016.
 */
public class IGDBScraper {
    public static String IGDB_BASIC_KEY = "ntso6TigR0msheVZZBFQPyOuqu6tp1OdtgFjsnkTZXRLTj9tgb";
    public static String IGDB_PRO_KEY = "ntso6TigR0msheVZZBFQPyOuqu6tp1OdtgFjsnkTZXRLTj9tgb";
    public static String key = IGDB_BASIC_KEY;

    public static final String API_URL = "http://62.210.219.110/api/v1";

    public static int REQUEST_COUNTER = 0;

    public static void main(String[] args) throws IOException, UnirestException {
        String appdataFolder = System.getenv("APPDATA");
        String dataPath = appdataFolder + File.separator + "GameRoom_dev";
        System.out.println("Datapath :" + dataPath);
        System.setProperty("data.dir", dataPath);
        Main.LOGGER = LogManager.getLogger(IGDBScraper.class);
        Main.FILES_MAP.put("db", new File("D:\\Documents\\GameRoom\\library.db"));
        DataBase.initDB();

        String gameName = "Battlefield 1";
        JSONArray bf4_results = searchGame(gameName);
        //System.out.println(bf4_results.toString(4));
        if (bf4_results != null) {
            ArrayList list = new ArrayList();
            ArrayList<GameEntry> entries = getEntries(bf4_results);
            for (GameEntry ge : entries) {
                System.out.println(ge);
            }
        } else {
            System.out.println("Found no matching game for " + gameName);
        }
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
            releaseDates.sort(Date::compareTo);
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
        String[] result = new String[0];
        if (jsob != null
                && jsob.has("screenshot_hashs")
                && !jsob.isNull("screenshot_hashs")) {
            JSONArray screenshotsArray = jsob.getJSONArray("screenshot_hashs");
            result = new String[screenshotsArray.length()];
            for (int i = 0; i < screenshotsArray.length(); i++) {
                result[i] = screenshotsArray.getString(i);
            }
        }
        return result;
    }

    public static String getCoverImageHash(JSONObject jsob) {
        return jsob.optString("cover_hash",null);
    }

    public static int[] getPlatformIds(JSONObject jsob) {
        try {
            JSONArray releaseDates = jsob.getJSONArray("release_dates");
            int[] platformIds = new int[releaseDates.length()];
            for (int i = 0; i < releaseDates.length(); i++) {
                int id = releaseDates.getJSONObject(i).optInt("platform", -1);
                platformIds[i] = ArrayUtils.contains(platformIds, id) ? -1 : id;
            }
            return platformIds;
        } catch (JSONException e) {
            return new int[]{};
        }
    }

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

        entry.setDescription(game_data.optString("description"));

        if (!game_data.has("release_date") && !game_data.isNull("release_date")) {
            Date release_date = new Date(game_data.getLong("release_date"));
            LocalDateTime date = release_date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            entry.setReleaseDate(date);
        } else {
            entry.setReleaseDate(null);
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
        entry.setAggregated_rating(game_data.optInt("aggregated_rating", 0));

        return entry;
    }

    public static JSONArray searchGame(String gameName) throws UnirestException {
        gameName = gameName.replace(' ', '+');
        incrementRequestCounter();
        HttpResponse<JsonNode> response = Unirest.get(API_URL + "/Games/SearchGame/" + gameName)
                .header("Accept", "application/json")
                .asJson();

        return extractData(response, "games");
    }

    public static JSONObject getGameData(int id) throws UnirestException {
        ArrayList<Integer> list = new ArrayList<>();
        list.add(id);
        return getGamesData(list).getJSONObject(0);
    }

    public static JSONArray getGamesData(Collection<Integer> ids) throws UnirestException {
        String idsString = ids.stream().map(String::valueOf).collect(Collectors.joining(","));

        incrementRequestCounter();
        HttpResponse<JsonNode> response = Unirest.get(API_URL + "/Games/GetGames/" + idsString)
                .header("Accept", "application/json")
                .asJson();
        return extractData(response, "games");
    }

    private static ArrayList<GameGenre> getGenres(JSONObject gameData) {
        try {
            if (gameData.has("genres") && !gameData.isNull("genres")) {
                int genresNumber = gameData.getJSONArray("genres").length();
                ArrayList<GameGenre> genres = new ArrayList<>();

                for (int i = 0; i < genresNumber; i++) {
                    int genreId = gameData.getJSONArray("genres").getInt(i);
                    genres.add(GameGenre.getGenreFromID(genreId));
                }
                return genres;
            }
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
            if (gameData.has("themes") && !gameData.isNull("themes")) {

                int themesNumber = gameData.getJSONArray("themes").length();
                ArrayList<GameTheme> themes = new ArrayList<>();

                for (int i = 0; i < themesNumber; i++) {
                    int genreId = gameData.getJSONArray("themes").getInt(i);
                    themes.add(GameTheme.getThemeFromId(genreId));
                }
                return themes;
            }
        } catch (JSONException jse) {
            if (jse.toString().contains("not found")) {
                //Main.LOGGER.error("Themes not found");
            } else {
                jse.printStackTrace();
            }
        }
        return null;
    }

    private static Serie extractSerie(int id, JSONArray serieData) {
        Serie s = Serie.getFromIGDBId(id);
        try {
            if (s == null && serieData != null) {
                String name = serieData.getJSONObject(indexOf(id, serieData)).getString("name");
                s = new Serie(id, name,true);
            }
        } catch (JSONException je) {
            return Serie.NONE;
        }
        return s;
    }

    private static JSONArray getSeriesData(Collection<Integer> ids) {
        String idsString = ids.stream().map(String::valueOf).collect(Collectors.joining(","));

        try {

            incrementRequestCounter();
            HttpResponse<JsonNode> response = Unirest.get(API_URL + "/Series/GetSeries/" + idsString)
                    .header("Accept", "application/json")
                    .asJson();
            return extractData(response, "series");
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
                c = new Company(id, name, true);
            }
        } catch (JSONException je) {
            je.printStackTrace();
        }
        return c;
    }

    private static JSONArray getCompaniesData(Collection<Integer> ids) {
        String idsString = ids.stream().map(String::valueOf).collect(Collectors.joining(","));

        try {
            incrementRequestCounter();
            HttpResponse<JsonNode> response = Unirest.get(API_URL + "/Companies/GetCompanies/" + idsString)
                    .header("Accept", "application/json")
                    .asJson();

            return extractData(response, "companies");

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
            if (searchData.has("publishers") && !searchData.isNull("publishers")) {
                int publishersNumber = searchData.getJSONArray("publishers").length();
                for (int j = 0; j < publishersNumber; j++) {
                    int igdbId = searchData.getJSONArray("publishers").getInt(j);
                    Company publi = Company.getFromIGDBId(igdbId);
                    if (publi == null) {
                        companiesIDs.add(igdbId);
                    }
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
            if (searchData.has("developers") && !searchData.isNull("developers")) {
                int developersNumber = searchData.getJSONArray("developers").length();
                for (int j = 0; j < developersNumber; j++) {
                    int igdbId = searchData.getJSONArray("developers").getInt(j);
                    Company dev = Company.getFromIGDBId(igdbId);
                    if (dev == null) {
                        companiesIDs.add(igdbId);
                    }
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
            if (searchData.has("publishers") && !searchData.isNull("publishers")) {
                int publishersNumber = searchData.getJSONArray("publishers").length();
                for (int j = 0; j < publishersNumber; j++) {
                    int igdbId = searchData.getJSONArray("publishers").getInt(j);
                    companies.add(extractCompany(igdbId, companiesData));
                }
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
            if (searchData.has("developers") && !searchData.isNull("developers")) {
                int developersNumber = searchData.getJSONArray("developers").length();

                for (int j = 0; j < developersNumber; j++) {
                    int igdbId = searchData.getJSONArray("developers").getInt(j);
                    companies.add(extractCompany(igdbId, companiesData));
                }
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
            if (searchData.has("collection") && !searchData.isNull("collection")) {
                int serieId = searchData.getInt("collection");
                Serie serie = Serie.getFromIGDBId(serieId);
                if (serie == null) {
                    seriesIDs.add(serieId);
                }
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
            if (searchData.has("collection") && !searchData.isNull("collection")) {
                int serieId = searchData.getInt("collection");
                entryToSet.setSerie(extractSerie(serieId, seriesData));
            }
        } catch (JSONException je) {
            if (je.toString().contains("not found")) {
                //Main.LOGGER.warn(entry.getName()+" : no serie");
            } else {
                je.printStackTrace();
            }
        }
    }

    private static void incrementRequestCounter() {
        REQUEST_COUNTER++;
        if (LOGGER != null) {
            LOGGER.debug("IGDBScraper : added req, total=" + REQUEST_COUNTER);
        } else {
            System.out.println("IGDBScraper : added req, total=" + REQUEST_COUNTER);
        }
    }

    private static JSONArray extractData(HttpResponse<JsonNode> response, String key) {
        if (response.getBody() != null
                && response.getBody().getObject() != null
                && response.getBody().getObject().optJSONObject("data") != null) {
            return response.getBody().getObject().optJSONObject("data").optJSONArray(key);
        }
        return null;
    }
}

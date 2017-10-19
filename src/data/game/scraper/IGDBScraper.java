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
 * This class is responsible for querying the API server containing all information provided by IGDB, as well as extracting
 * data from JSON response and building {@link GameEntry}s, {@link Company}s or {@link Serie}s out of it!
 * <p>
 * There is a naming convention for methods :
 * - methods starting with "extract" will just parse JSON data and will not cause more queries
 * - methods starting with "get" will usually query the API server ({@link IGDBScraper#getEntry(JSONObject, boolean)}
 * extracts data and queries, thus is named with a starting "get"
 *
 * @author LM. Garret (admin@gameroom.me)
 * @date 03/07/2016.
 */

public class IGDBScraper {
    private static final boolean DEV_MODE = Main.DEV_MODE;
    private static final String API_URL = "http://62.210.219.110/api/v1" + (DEV_MODE ? "/dev" : "");

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
        JSONArray bf4_results = searchGame(gameName, true,Platform.PC.getId());
        //System.out.println(bf4_results.toString(4));
        if (bf4_results != null) {
            ArrayList list = new ArrayList();
            ArrayList<GameEntry> entries = getGameEntries(bf4_results);
            for (GameEntry ge : entries) {
                System.out.println(ge);
            }
        } else {
            System.out.println("Found no matching game for " + gameName);
        }
    }

    /**
     * Inits all whats needed for the IGDBScraper
     */
    public static void init(){
        Unirest.setTimeouts(8000,15000);
    }

    /**
     * Helper method which finds the index of the object with th given id inside the given {@link JSONArray}
     *
     * @param id   the id of the object we want the index of
     * @param data the {@link JSONArray} on which we iterate
     * @return -1 if the array has no such object with the given id, the index of the object in the array otherwise
     */
    private static int indexOf(int id, JSONArray data) {
        int i = 0;
        if (data == null || data.length() == 0) {
            return -1;
        }
        for (Object obj : data) {
            if (obj instanceof JSONObject
                    && ((JSONObject) obj).has("id")
                    && !((JSONObject) obj).isNull("id")
                    && ((JSONObject) obj).getInt("id") == id) {
                return i;
            }
            i++;
        }
        return -1;
    }

    /**
     * Extracts the screenshots hashs (i.e. cloudinary ids) from the given {@link JSONObject}
     *
     * @param jsob the {@link JSONObject} to extract hashs from
     * @return a {@link String} array containing hashs if found
     */
    public static String[] extractScreenshotHash(JSONObject jsob) {
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

    /**
     * Extracts the cover hash (i.e. cloudinary id) from the given {@link JSONObject}
     *
     * @param jsob the {@link JSONObject} to extract hash from
     * @return a {@link String} containing hash if found, null otherwise
     */
    public static String extractCoverImageHash(JSONObject jsob) {
        return jsob.optString("cover_hash", null);
    }

    /**
     * Extracts the (IGDB) platform ids from the given {@link JSONObject}
     *
     * @param jsob the {@link JSONObject} to extract ids from
     * @return a {@link Integer} array containing ids of platforms if found. There are no valid duplicates of ids in this
     * array, as any already present id in this array is replaced by -1
     */
    public static int[] extractPlatformIds(JSONObject jsob) {
        try {
            if (jsob.has("platforms") && !jsob.isNull("platforms")) {
                JSONArray platforms = jsob.getJSONArray("platforms");
                int[] platformIds = new int[platforms.length()];
                for (int i = 0; i < platforms.length(); i++) {
                    int id = platforms.getInt(i);
                    platformIds[i] = ArrayUtils.contains(platformIds, id) ? -1 : id;
                }
                return platformIds;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new int[]{};
    }

    /**
     * Typically to be used when we have just searched for a game, and we want to build all possible {@link GameEntry}s
     * from this search
     * <p>
     * This will first query the API server to get data about unknown {@link Company} and {@link Serie} for all those games,
     * looking into the given {@link JSONArray} for ids to compare against the one stored in the {@link DataBase} (see
     * {@link IGDBScraper#extractUnknownCompaniesIDs(JSONArray)} and {@link IGDBScraper#extractUnknownSeriesIDs(JSONArray)}.
     * <p>
     * Then it will extract {@link GameEntry}s out of the given {@link JSONArray}, set companies and series from the
     * data that the API server returned for each of them, and return those entries into an {@link ArrayList}.
     *
     * @param gamesData a {@link JSONArray} containing data about some {@link GameEntry}s
     * @return an {@link ArrayList} of ready to use {@link GameEntry}s, with {@link Company}s and {@link Serie} queried if
     * needed and set for each of them.
     * @throws UnirestException in case an error occurred while contacting the API server
     */
    public static ArrayList<GameEntry> getGameEntries(JSONArray gamesData) throws UnirestException {
        ArrayList<GameEntry> entries = new ArrayList<>();

        JSONArray companiesData = getCompaniesData(extractUnknownCompaniesIDs(gamesData));
        JSONArray seriesData = getSeriesData(extractUnknownSeriesIDs(gamesData));


        for (int i = 0; i < gamesData.length(); i++) {
            JSONObject gameData = gamesData.getJSONObject(i);
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

    /**
     * See {@link IGDBScraper#getEntry(JSONObject, boolean)}
     */
    public static GameEntry getEntry(JSONObject game_data) {
        return getEntry(game_data, true);
    }


    /**
     * Builds a {@link GameEntry} instance out of the given {@link JSONObject}. Will contact server (@param allowUseMoreRequest)
     * if allowed to fetch data about {@link Company}s and {@link Serie} in order to complete the {@link GameEntry}
     *
     * @param game_data           the {@link JSONObject} containing data to build the {@link GameEntry}
     * @param allowUseMoreRequest true if the method is allowed to contact the API server about {@link Company}s and
     *                            {@link Serie} in order to complete the {@link GameEntry}, false otherwise. This false
     *                            case is typically when we have already queried the API server about those {@link Company}s
     *                            and {@link Serie}s and we are sure they are available in the {@link DataBase} !
     * @return a {@link GameEntry} based on the {@link JSONObject} given
     */
    private static GameEntry getEntry(JSONObject game_data, boolean allowUseMoreRequest) {
        GameEntry entry = new GameEntry(game_data.getString("name"));
        entry.setSavedLocally(false);

        entry.setDescription(game_data.optString("description"));

        if (game_data.has("release_date") && !game_data.isNull("release_date")) {
            Date release_date = new Date(game_data.getLong("release_date"));
            LocalDateTime date = release_date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            entry.setReleaseDate(date);
        } else {
            entry.setReleaseDate(null);
        }

        if (allowUseMoreRequest) {
            JSONArray companiesData = getCompaniesData(extractUnknownCompaniesIDs(game_data));
            JSONArray seriesData = getSeriesData(extractUnknownSeriesIDs(game_data));
            setGameCompanies(entry, game_data, companiesData);
            setGameSerie(entry, game_data, seriesData);
        }
        try {
            entry.setGenres(extractGenres(game_data));
        } catch (JSONException je) {
            if (je.toString().contains("not found")) {
                //Main.LOGGER.warn(entry.getName()+" : no developers");
            } else {
                je.printStackTrace();
            }
        }
        try {
            entry.setThemes(extractThemes(game_data));
        } catch (JSONException je) {
            if (je.toString().contains("not found")) {
                //Main.LOGGER.warn(entry.getName()+" : no developers");
            } else {
                je.printStackTrace();
            }
        }

        try {
            entry.setIgdb_imageHash(0, IGDBScraper.extractCoverImageHash(game_data));
            String[] screenshotsHashes = IGDBScraper.extractScreenshotHash(game_data);
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

    /**
     * Queries the APi server to search for a game.
     *
     * @param gameName  the name of the game we are looking for
     * @param allowDLCs if we want DLCs to appear in our search results
     * @param platformId IGDB id of a {@link Platform}. Will restrict the search for games only on this platform, or search for
     *                   games on all {@link Platform}s if is -1.
     * @return a {@link JSONArray} containing data about games matching the given name, or null if there was a parsing issue
     * @throws UnirestException in case an error occurred while querying the API server
     */
    public static JSONArray searchGame(String gameName, boolean allowDLCs, int platformId) throws UnirestException {
        gameName = gameName.replace(' ', '+');
        String args = "?dlc=" + (allowDLCs ? "1" : "0");
        if(platformId != -1){
            args += ",platform_id="+platformId;
        }
        incrementRequestCounter();
        HttpResponse<JsonNode> response = Unirest.get(API_URL + "/Games/SearchGame/" + gameName + args)
                .header("Accept", "application/json")
                .asJson();

        return extractData(response, "games");
    }

    /**
     * Queries the API server and gets data about a single game
     *
     * @param id IGDB id of the game we're interested in
     * @return a {@link JSONObject} containing data about our game, null if no data was found
     * @throws UnirestException if there was an error while querying the API server
     */
    public static JSONObject getGameData(int id) throws UnirestException {
        ArrayList<Integer> list = new ArrayList<>();
        list.add(id);
        JSONArray array = getGamesData(list);
        if (array != null && array.length() > 0) {
            return array.getJSONObject(0);
        }
        return null;
    }

    /**
     * Queries the API server to get data about a collection of games
     *
     * @param ids a {@link Collection} of IGDB ids of the games we're interested in
     * @return a {@link JSONArray} containing data about our games, null if no data was found
     * @throws UnirestException if there was an error while querying the API server
     */
    public static JSONArray getGamesData(Collection<Integer> ids) throws UnirestException {
        String idsString = ids.stream().map(String::valueOf).collect(Collectors.joining(","));

        incrementRequestCounter();
        HttpResponse<JsonNode> response = Unirest.get(API_URL + "/Games/GetGames/" + idsString)
                .header("Accept", "application/json")
                .asJson();
        return extractData(response, "games");
    }

    /**
     * Extracts {@link GameGenre}s from the given {@link JSONObject}
     *
     * @param gameData the {@link JSONObject} to extract {@link GameGenre}s from
     * @return an {@link ArrayList} of {@link GameGenre}s if some are found, null otherwise
     */
    private static ArrayList<GameGenre> extractGenres(JSONObject gameData) {
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

    /**
     * Extracts {@link GameTheme}s from the given {@link JSONObject}
     *
     * @param gameData the {@link JSONObject} to extract {@link GameTheme}s from
     * @return an {@link ArrayList} of {@link GameTheme}s if some are found, null otherwise
     */
    private static ArrayList<GameTheme> extractThemes(JSONObject gameData) {
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

    /**
     * Extracts {@link Serie} from the given {@link JSONObject}
     *
     * @param serieData the {@link JSONObject} to extract the {@link Serie} from
     * @return a {@link Serie} if found, {@link Serie#NONE} otherwise
     */
    private static Serie extractSerie(int id, JSONArray serieData) {
        Serie s = Serie.getFromIGDBId(id);
        try {
            if (s == null && serieData != null) {
                String name = serieData.getJSONObject(indexOf(id, serieData)).getString("name");
                s = new Serie(id, name, true);
            }
        } catch (JSONException je) {
            return Serie.NONE;
        }
        return s;
    }

    /**
     * Queries the API server to get data about a collection of {@link Serie}
     *
     * @param ids a {@link Collection} of IGDB ids of the series we're interested in
     * @return a {@link JSONArray} containing data about our series, null if no data was found
     */
    private static JSONArray getSeriesData(Collection<Integer> ids) {
        if(ids.isEmpty()){
            return null;
        }
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

    /**
     * Extracts {@link Company} from the given {@link JSONArray}
     *
     * @param id            IGDB id of the {@link Company} to extract
     * @param companiesData the {@link JSONArray} to extract the {@link Company} from
     * @return a {@link Serie} if found, null otherwise
     */
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

    /**
     * Queries the API server to get data about a collection of {@link Company}
     *
     * @param ids a {@link Collection} of IGDB ids of the companies we're interested in
     * @return a {@link JSONArray} containing data about our companies, null if no data was found
     */
    private static JSONArray getCompaniesData(Collection<Integer> ids) {
        if (ids.isEmpty()){
            return null;
        }
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
    private static HashSet<Integer> extractUnknownCompaniesIDs(JSONObject searchData) {
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
     * See {@link IGDBScraper#extractUnknownCompaniesIDs(JSONObject searchData))
     *
     * @param searchData a JSONArray of search about games
     * @return a hashset containing idsof yet unknown companies
     */
    private static HashSet<Integer> extractUnknownCompaniesIDs(JSONArray searchData) {
        HashSet<Integer> companiesIDs = new HashSet<>();
        for (int i = 0; i < searchData.length(); i++) {
            companiesIDs.addAll(extractUnknownCompaniesIDs(searchData.getJSONObject(i)));
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
    private static HashSet<Integer> extractUnknownSeriesIDs(JSONObject searchData) {
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
     * See {@link IGDBScraper#extractUnknownSeriesIDs(JSONObject searchData)}
     *
     * @param searchData a JSONArray of search about games
     * @return a hashset containing ids of yet unknown serie
     */
    private static HashSet<Integer> extractUnknownSeriesIDs(JSONArray searchData) {
        HashSet<Integer> seriesIDs = new HashSet<>();
        for (int i = 0; i < searchData.length(); i++) {
            seriesIDs.addAll(extractUnknownSeriesIDs(searchData.getJSONObject(i)));
        }
        return seriesIDs;
    }

    /**
     * Given searchData on a game and serieData, update it with corresponding serie
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

    /**
     * Basically increments the request counter for this GameRoom's execution.
     */
    private static void incrementRequestCounter() {
        REQUEST_COUNTER++;
        if (LOGGER != null) {
            LOGGER.debug("IGDBScraper : added req, total=" + REQUEST_COUNTER);
        } else {
            System.out.println("IGDBScraper : added req, total=" + REQUEST_COUNTER);
        }
    }

    /**
     * Assuming the response from the API server follows the following convention :
     * {
     * "status": {
     * "message": "success",
     * "code": 200,
     * "process_time": 257.42197036743
     * },
     * "data": {
     * <key> : [..]
     * }
     * }
     * This method will extract the data given by the server at the key position.
     *
     * @param response the response from the server
     * @param key      the key at which the wanted data is located in the response
     * @return a JSONArray if the response follows the convention and if there is data, null otherwise
     */
    private static JSONArray extractData(HttpResponse<JsonNode> response, String key) {
        if (response != null
                && response.getBody() != null
                && response.getBody().getObject() != null
                && response.getBody().getObject().optJSONObject("data") != null) {
            return response.getBody().getObject().optJSONObject("data").optJSONArray(key);
        }
        return null;
    }
}

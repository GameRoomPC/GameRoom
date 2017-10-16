package data.game.scraper;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import data.game.GameWatcher;
import data.game.entry.GameEntry;
import data.game.entry.GameEntryUtils;
import data.game.entry.Platform;
import data.game.scanner.GameScanner;
import data.game.scanner.OnGameFound;
import data.game.scanner.ScanTask;
import org.apache.http.conn.ConnectTimeoutException;
import org.json.*;
import org.jsoup.Jsoup;
import system.application.settings.PredefinedSetting;
import ui.Main;
import ui.dialog.GameRoomAlert;
import ui.dialog.SteamProfileSelector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static system.application.settings.GeneralSettings.settings;
import static ui.Main.LOGGER;

/**
 * Created by LM on 14/08/2016.
 */
public class SteamOnlineScraper {
    private final static SimpleDateFormat STEAM_DATE_FORMAT = new SimpleDateFormat("dd MMM, yyyy", Locale.US);

    static void scanSteamOnlineGames(GameScanner scanner) {
        scanNonInstalledSteamGames(entry -> {
            ScanTask task = new ScanTask(scanner,() -> {
                if (!GameEntryUtils.isGameIgnored(entry) && !SteamLocalScraper.isSteamGameInstalled(entry.getPlatformGameID())) {
                    scanner.checkAndAdd(entry);
                }
                return null;
            });
            GameWatcher.getInstance().submitTask(task);
        });
    }

    private static void scanNonInstalledSteamGames(OnGameFound handler) {
        try {
            SteamProfile profile = settings().getSteamProfileToScan();
            if (profile != null) {
                JSONArray ownedArray = askGamesOwned(profile.getAccountId());
                for (int i = 0; i < ownedArray.length(); i++) {
                    SteamPreEntry preEntry = new SteamPreEntry(ownedArray.getJSONObject(i).getString("name"), ownedArray.getJSONObject(i).getInt("appID"));
                    GameEntry entry = preEntry.toGameEntry();
                    GameEntry scrapedEntry = SteamOnlineScraper.getEntryForSteamId(entry.getPlatformGameID());
                    if (scrapedEntry != null) {
                        entry = scrapedEntry;
                        try {
                            double playTimeHours = ownedArray.getJSONObject(i).getDouble("hoursOnRecord");
                            entry.setPlayTimeSeconds((long) (playTimeHours * 3600));
                        } catch (JSONException jse) {
                            if (jse.toString().contains("not found")) {
                                //System.out.println(entry.getName() + " was never played");
                            } else {
                                jse.printStackTrace();
                            }
                        }
                        handler.handle(entry);
                    }

                }
            }
        } catch (UnirestException | IOException e) {
            Main.LOGGER.error("Error connectiong to steamcommunity.com");
            Main.LOGGER.error(e.toString().split("\n")[0]);
        }
    }

    public static void checkIfCanScanSteam(boolean forceModify) {
        SteamProfile selectedProfile = settings().getSteamProfileToScan();
        if (selectedProfile != null && !forceModify) {
            return;
        }

        List<SteamProfile> profiles = SteamLocalScraper.getSteamProfiles();
        if (profiles.isEmpty()) {
            GameRoomAlert.error(Main.getString("no_steam_profile_error"));
            return;
        }

        for(SteamProfile p : profiles){
            LOGGER.info("Found a Steam profile : \""+p.getAccountName()+"\", with ID : \""+p.getAccountId()+"\"");
        }

        if (!forceModify && profiles.size() == 1) {
            settings().setSettingValue(PredefinedSetting.STEAM_PROFILE, profiles.get(0));
        } else {
            Main.runAndWait(() -> {
                SteamProfileSelector selector = new SteamProfileSelector(profiles);
                selector.showAndWait();
            });
        }
    }

    public static ArrayList<GameEntry> getOwnedSteamGames() throws IOException {
        ArrayList<GameEntry> entries = new ArrayList<>();
        scanNonInstalledSteamGames(entry -> {
            try {
                long playTime = entry.getPlayTimeSeconds();
                entry = SteamOnlineScraper.getEntryForSteamId(entry.getPlatformGameID());
                if (entry != null) {
                    entry.setPlayTimeSeconds(playTime);
                    LOGGER.debug("Play time of " + entry.getName() + " : " + entry.getPlayTimeFormatted(GameEntry.TIME_FORMAT_FULL_DOUBLEDOTS));
                    entries.add(entry);
                }
            } catch (ConnectTimeoutException | UnirestException ignored) {
                LOGGER.error("scanSteamGames, Error connecting to steam");
            }
        });
        return entries;
    }

    public static GameEntry getEntryForSteamId(int steamId) throws ConnectTimeoutException, UnirestException {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
        JSONObject gameInfoJson = askGameInfos(steamId);
        if (gameInfoJson != null && (gameInfoJson.getString("type").equals("game") || gameInfoJson.getString("type").equals("demo"))) {

            GameEntry entry = new GameEntry(gameInfoJson.getString("name"));
            entry.setDescription(Jsoup.parse(gameInfoJson.getString("about_the_game")).text());
            try {
                Date input = STEAM_DATE_FORMAT.parse(gameInfoJson.getJSONObject("release_date").getString("date"));
                entry.setReleaseDate(input.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
            } catch (ParseException | NumberFormatException e) {
                Main.LOGGER.error("Invalid release date format");
            }
            boolean installed = SteamLocalScraper.isSteamGameInstalled(steamId);
            entry.setPlatformGameId(steamId);
            entry.setPlatform(Platform.getFromId(installed ? Platform.STEAM_ID : Platform.STEAM_ONLINE_ID));
            entry.setInstalled(installed);

            if (entry.getName() == null || entry.getName().isEmpty()) {
                return null;
            }
            return entry;
        }
        return null;
    }

    private static JSONArray askGamesOwned(String steam_profile_id) throws ConnectTimeoutException, UnirestException {
        try {
            HttpResponse<String> response = Unirest.get("http://steamcommunity.com/profiles/" + steam_profile_id + "/games/?tab=all&xml=1")
                    .header("Accept", "application/json")
                    .asString();
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getRawBody(), "UTF-8"));
            String xmlString = "";
            String line = null;
            while ((line = reader.readLine()) != null) {
                xmlString += line + "\n";
            }
            reader.close();
            return XML.toJSONObject(xmlString).getJSONObject("gamesList").getJSONObject("games").getJSONArray("game");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static JSONObject askGameInfos(long steam_id) throws ConnectTimeoutException {
        String json = null;
        try {
            HttpResponse<String> response = Unirest.get("http://store.steampowered.com/api/appdetails?appids=" + steam_id)
                    .header("Accept", "application/json")
                    .asString();
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getRawBody(), "UTF-8"));
            json = reader.readLine();
            reader.close();
            //Main.LOGGER.debug("Recevied : "+json);

            if (json == null || json.equals("null")) {
                return null;
            }
            JSONTokener tokener = new JSONTokener(json);

            JSONObject idObject = new JSONObject(tokener).getJSONObject("" + steam_id);
            boolean success = idObject.getBoolean("success");

            return success ? idObject.getJSONObject("data") : null;
        } catch (JSONException e) {
            if (e.toString().contains("not found")) {
                System.err.println("Data not found");
            } else if (e.toString().contains("Connect to store.steampowered.com:80")) {
                Main.LOGGER.info("Could not join store.steampowered, timeout. (SteamOnlineScraper.askGameInfos)");
            } else if (e.toString().contains("A JSONObject text must begin with '{' at ")) {
                Main.LOGGER.error("Receveid invalid json from steam : " + json);
            } else {
                e.printStackTrace();
            }
        } catch (UnirestException e) {
            if (e.toString().contains("java.net.UnknownHostException:")) {
                Main.LOGGER.info("Could not join store.steampowered, returning no entry");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}

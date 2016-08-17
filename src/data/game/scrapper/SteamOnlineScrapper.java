package data.game.scrapper;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import data.game.entry.GameEntry;
import org.apache.http.conn.ConnectTimeoutException;
import org.json.*;
import org.jsoup.Jsoup;
import ui.Main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by LM on 14/08/2016.
 */
public class SteamOnlineScrapper {
    private final static SimpleDateFormat STEAM_DATE_FORMAT = new SimpleDateFormat("dd MMM, yyyy", Locale.US);

    public static ArrayList<SteamPreEntry> getOwnedSteamGamesPreEntry() throws IOException {
        try {
            ArrayList<SteamPreEntry> entries = new ArrayList<>();
            JSONArray ownedArray = getGamesOwned(SteamLocalScrapper.getSteamUserId());
            for (int i = 0; i < ownedArray.length(); i++) {
                SteamPreEntry preEntry = new SteamPreEntry(ownedArray.getJSONObject(i).getString("name"),ownedArray.getJSONObject(i).getInt("appID"));
                JSONObject info = getInfoForGame(preEntry.getId());
                if(info!=null && (info.getString("type").equals("game") || info.getString("type").equals("demo")))
                    entries.add(preEntry);
            }
            entries.sort(new Comparator<SteamPreEntry>() {
                @Override
                public int compare(SteamPreEntry o1, SteamPreEntry o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            return entries;
        } catch (UnirestException e) {
            Main.LOGGER.error("Error connectiong to steamcommunity.com, giving installed games instead of owned ones");
            Main.LOGGER.error(e.toString().split("\n")[0]);
            return SteamLocalScrapper.getSteamAppsInstalledPreEntries();
        }
    }

    public static ArrayList<GameEntry> getOwnedSteamGames() throws IOException {
        ArrayList<GameEntry> entries = new ArrayList<>();
        try {
            JSONArray ownedArray = getGamesOwned(SteamLocalScrapper.getSteamUserId());
            for (int i = 0; i < ownedArray.length(); i++) {
                GameEntry entry = getEntryForSteamId(ownedArray.getJSONObject(i).getInt("appID"), SteamLocalScrapper.getSteamAppsInstalledPreEntries());
                if (entry != null) {
                    try {
                        double playTimeHours = ownedArray.getJSONObject(i).getDouble("hoursOnRecord");
                        entry.setPlayTimeSeconds((long) (playTimeHours * 3600));
                    } catch (JSONException jse) {
                        if (jse.toString().contains("not found")) {
                            System.out.println(entry.getName() + " was never played");
                        } else {
                            jse.printStackTrace();
                        }
                    }
                    entries.add(entry);
                }
            }
            entries.sort(new Comparator<GameEntry>() {
                @Override
                public int compare(GameEntry o1, GameEntry o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            return entries;
        } catch (UnirestException e) {
            Main.LOGGER.error("Error connectiong to steamcommunity.com, giving installed games instead of owned ones");
            Main.LOGGER.error(e.toString().split("\n")[0]);
        }
        entries.clear();
        for (SteamPreEntry preEntry : SteamLocalScrapper.getSteamAppsInstalledPreEntries()) {
            GameEntry entry = new GameEntry(preEntry.getName());
            entry.setSteam_id(preEntry.getId());
            entries.add(entry);
        }
        entries.sort(new Comparator<GameEntry>() {
            @Override
            public int compare(GameEntry o1, GameEntry o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return entries;
    }

    public static GameEntry getEntryForSteamId(int steamId, ArrayList<SteamPreEntry> installedSteamApps) throws ConnectTimeoutException {
        JSONObject gameInfoJson = getInfoForGame(steamId);
        if (gameInfoJson != null) {
            GameEntry entry = new GameEntry(gameInfoJson.getString("name"));
            entry.setDescription(Jsoup.parse(gameInfoJson.getString("about_the_game")).text());
            try {
                entry.setReleaseDate(STEAM_DATE_FORMAT.parse(gameInfoJson.getJSONObject("release_date").getString("date")));
            } catch (ParseException e) {
                System.err.println("Invalid release date format");
            } catch (NumberFormatException e) {
                System.err.println("Invalid release date format");
            }
            entry.setSteam_id(steamId);
            if (installedSteamApps != null) {
                boolean installed = false;
                for (SteamPreEntry installedApp : installedSteamApps) {
                    installed = entry.getSteam_id() == installedApp.getId();
                    if (installed) {
                        break;
                    }
                }
                entry.setNotInstalled(!installed);
            }
            return entry;
        }
        return null;
    }

    private static JSONArray getGamesOwned(String steam_profile_id) throws ConnectTimeoutException, UnirestException {
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
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static JSONObject getInfoForGame(long steam_id) throws ConnectTimeoutException {
        try {
            HttpResponse<String> response = Unirest.get("http://store.steampowered.com/api/appdetails?appids=" + steam_id)
                    .header("Accept", "application/json")
                    .asString();
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getRawBody(), "UTF-8"));
            String json = reader.readLine();
            reader.close();
            JSONTokener tokener = new JSONTokener(json);

            JSONObject idObject = new JSONObject(tokener).getJSONObject("" + steam_id);
            boolean success = idObject.getBoolean("success");

            return success ? idObject.getJSONObject("data") : null;
        } catch (JSONException e) {
            if (e.toString().contains("not found")) {
                System.err.println("Data not found");
            } else if (e.toString().contains("Connect to store.steampowered.com:80")) {
                Main.LOGGER.info("Could not join store.steampowered, timeout. (SteamOnlineScrapper.getInfoForGame)");
            } else {
                e.printStackTrace();
            }
        } catch (UnirestException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
        //JSONObject skyrim_results = getInfoForGame(72850);
        //System.out.println(skyrim_results.toString(4));

        for (GameEntry entry : getOwnedSteamGames()) {
            System.out.println(entry);
        }
    }
}

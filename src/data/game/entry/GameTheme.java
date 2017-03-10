package data.game.entry;

import com.google.gson.Gson;
import data.io.DataBase;
import ui.Main;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * Created by LM on 13/08/2016.
 */
public enum GameTheme {
    ACTION("action"),
    FANTASY("fantasy"),
    SCIENCE_FICTION("science_fiction"),
    HORROR("horror"),
    THRILLER("thriller"),
    SURVIVAL("survival"),
    HISTORICAL("historical"),
    STEALTH("stealth"),
    COMEDY("comedy"),
    BUSINESS("business"),
    DRAMA("drama"),
    NON_FICTION("non_fiction"),
    SANDBOX("sandbox"),
    EDUCATIONAL("educational"),
    KIDS("kids"),
    OPEN_WORLD("open_world"),
    WARFARE("warfare"),
    PARTY("party"),
    EXPLORE_EXPAND_EXPLOIT_EXTERMINATE("4x"),
    EROTIC("erotic"),
    MYSTERY("mystery");

    private final static HashMap<Integer, GameTheme> IGDB_THEME_MAP = new HashMap<>();
    private String key;
    private boolean hasPayload;

    GameTheme(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    public static String toJson(data.game.entry.GameTheme[] genres) {
        Gson gson = new Gson();
        return gson.toJson(genres, GameTheme[].class);
    }

    public static GameTheme[] fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, GameTheme[].class);
    }

    public String getDisplayName() {
        return Main.GAME_THEMES_BUNDLE.getString(key);
    }

    public static GameTheme getThemeFromIGDB(int igdbThemeId) {
        if (IGDB_THEME_MAP.size() == 0) {
            fillIGDBThemeMap();
        }
        return IGDB_THEME_MAP.get(igdbThemeId);
    }

    private static void fillIGDBThemeMap() {
        IGDB_THEME_MAP.put(1, ACTION);
        IGDB_THEME_MAP.put(17, FANTASY);
        IGDB_THEME_MAP.put(18, SCIENCE_FICTION);
        IGDB_THEME_MAP.put(19, HORROR);
        IGDB_THEME_MAP.put(20, THRILLER);
        IGDB_THEME_MAP.put(21, SURVIVAL);
        IGDB_THEME_MAP.put(22, HISTORICAL);
        IGDB_THEME_MAP.put(23, STEALTH);
        IGDB_THEME_MAP.put(27, COMEDY);
        IGDB_THEME_MAP.put(28, BUSINESS);
        IGDB_THEME_MAP.put(31, DRAMA);
        IGDB_THEME_MAP.put(32, NON_FICTION);
        IGDB_THEME_MAP.put(33, SANDBOX);
        IGDB_THEME_MAP.put(34, EDUCATIONAL);
        IGDB_THEME_MAP.put(35, KIDS);
        IGDB_THEME_MAP.put(38, OPEN_WORLD);
        IGDB_THEME_MAP.put(39, WARFARE);
        IGDB_THEME_MAP.put(40, PARTY);
        IGDB_THEME_MAP.put(41, EXPLORE_EXPAND_EXPLOIT_EXTERMINATE);
        IGDB_THEME_MAP.put(42, EROTIC);
        IGDB_THEME_MAP.put(43, MYSTERY);
    }

    public static HashMap<Integer, GameTheme> getIgdbThemeMap() {
        return IGDB_THEME_MAP;
    }

    public String getKey() {
        return key;
    }

    public static int getIGDBId(String nameKey) {
        if (nameKey == null || nameKey.isEmpty()) {
            return -1;
        }

        try {
            Connection connection = DataBase.getUserConnection();
            PreparedStatement getIdQuery = connection.prepareStatement("SELECT igdb_id FROM GameTheme WHERE name_key = ?");
            getIdQuery.setString(1, nameKey);
            ResultSet result = getIdQuery.executeQuery();

            if (result.next()) {
                int id = result.getInt(1);
                result.close();
                return id;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;

    }
}

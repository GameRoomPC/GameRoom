package data.game.entry;

import com.google.gson.Gson;
import ui.Main;

import java.util.HashMap;

/**
 * Created by LM on 11/02/2017.
 */
public enum Platform {
    WINDOWS("windows"),
    WII("wii"),
    GAMECUBE("gamecube");

    private final static HashMap<Integer, Platform> PLATFORM_MAP = new HashMap<>();
    private String key;
    private boolean hasPayload;

    Platform(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    public static String toJson(data.game.entry.GameGenre[] genres) {
        Gson gson = new Gson();
        return gson.toJson(genres, data.game.entry.GameGenre[].class);
    }

    public static data.game.entry.GameGenre[] fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, data.game.entry.GameGenre[].class);
    }

    public String getDisplayName() {
        return Main.PLATFORM_BUNDLE.getString(key);
    }

    public static Platform getGenreFromIGDB(int IGDBPlatformId) {
        if (PLATFORM_MAP.size() == 0) {
            fillIGDBGenreMap();
        }
        return PLATFORM_MAP.get(IGDBPlatformId);
    }

    private static void fillIGDBGenreMap() {
        PLATFORM_MAP.put(0, WINDOWS);
        PLATFORM_MAP.put(1, WII);
        PLATFORM_MAP.put(2, GAMECUBE);
    }

    public String getKey() {
        return key;
    }

    public static Platform fromString(String s){
        for(Platform p : Platform.values()){
            if(p.getKey().equals(s)){
                return p;
            }
        }
        return WINDOWS;
    }
}

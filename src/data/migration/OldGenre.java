package data.migration;

import com.google.gson.Gson;
import ui.Main;

import java.util.HashMap;

/**
 * Created by LM on 01/03/2017.
 */
public enum OldGenre {
    POINT_AND_CLICK("point_and_click"),
    FIGHTING("fighting"),
    SHOOTER("fps"),
    MUSIC("music"),
    PLATFORM("platform"),
    PUZZLE("puzzle"),
    RACING("racing"),
    REAL_TIME_STRATEGY("rts"),
    ROLE_PLAYING("rpg"),
    SIMULATOR("simulator"),
    SPORT("sport"),
    STRATEGY("strategy"),
    TURN_BASED_STRATEGY("tbs"),
    TACTICAL("tactical"),
    HACK_AND_SLASH_BEAT_EM_UP("hack_and_slash"),
    QUIZ_TRIVIA("quiz"),
    PINBALL("pinball"),
    ADVENTURE("adventure"),
    INDIE("indie"),
    ARCADE("arcade");

    private String key;

    OldGenre(String key) {
        this.key = key;
    }

    public static String toJson(OldGenre[] genres) {
        Gson gson = new Gson();
        return gson.toJson(genres, OldGenre[].class);
    }

    public static OldGenre[] fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, OldGenre[].class);
    }

    public String getKey() {
        return key;
    }
}

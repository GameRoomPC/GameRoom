package data.game;

import com.google.gson.Gson;
import ui.Main;

import java.util.HashMap;

/**
 * Created by LM on 12/08/2016.
 */
public enum GameGenre {
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

    private final static HashMap<String,GameGenre> IGDB_GENRE_MAP = new HashMap<>();
    private String key;
    private boolean hasPayload;

    GameGenre(String key){
        this.key = key;
    }

    @Override
    public String toString(){
        return getDisplayName();
    }

    public static String toJson(GameGenre[] genres){
        Gson gson = new Gson();
        return gson.toJson(genres,GameGenre[].class);
    }
    public static GameGenre[] fromJson(String json){
        Gson gson = new Gson();
        return gson.fromJson(json, GameGenre[].class);
    }

    public String getDisplayName(){
        return Main.GAMES_GENRE_BUNDLE.getString(key);
    }

    public static GameGenre getGenreFromIGDB(String igdbGenre){
        if(IGDB_GENRE_MAP.size() == 0){
            fillIGDBGenreMap();
        }
        return IGDB_GENRE_MAP.get(igdbGenre);
    }
    private static void fillIGDBGenreMap(){
        IGDB_GENRE_MAP.put("Point-and-click",POINT_AND_CLICK);
        IGDB_GENRE_MAP.put("Fighting",FIGHTING);
        IGDB_GENRE_MAP.put("Shooter",SHOOTER);
        IGDB_GENRE_MAP.put("Music",MUSIC);
        IGDB_GENRE_MAP.put("Platform",PLATFORM);
        IGDB_GENRE_MAP.put("Puzzle",PUZZLE);
        IGDB_GENRE_MAP.put("Racing",RACING);
        IGDB_GENRE_MAP.put("Real Time Strategy (RTS)",REAL_TIME_STRATEGY);
        IGDB_GENRE_MAP.put("Role-playing (RPG)", ROLE_PLAYING);
        IGDB_GENRE_MAP.put("Simulator",SIMULATOR);
        IGDB_GENRE_MAP.put("Sport",SPORT);
        IGDB_GENRE_MAP.put("Strategy",STRATEGY);
        IGDB_GENRE_MAP.put("Turn-based strategy (TBS)",TURN_BASED_STRATEGY);
        IGDB_GENRE_MAP.put("Hack and slash/Beat 'em up", HACK_AND_SLASH_BEAT_EM_UP);
        IGDB_GENRE_MAP.put("Quiz/Trivia", QUIZ_TRIVIA);
        IGDB_GENRE_MAP.put("Pinball",PINBALL);
        IGDB_GENRE_MAP.put("Tactical",TACTICAL);
        IGDB_GENRE_MAP.put("Adventure",ADVENTURE);
        IGDB_GENRE_MAP.put("Indie",INDIE);
        IGDB_GENRE_MAP.put("Arcade",ARCADE);
    }
}

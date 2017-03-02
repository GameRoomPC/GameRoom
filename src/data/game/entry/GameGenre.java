package data.game.entry;

import com.google.gson.Gson;
import data.io.DataBase;
import ui.Main;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import static ui.Main.LOGGER;

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

    private final static HashMap<Integer,GameGenre> IGDB_GENRE_MAP = new HashMap<>();
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
        return Main.GAME_GENRES_BUNDLE.getString(key);
    }

    public static GameGenre getGenreFromIGDB(int igdbGenreId){
        if(IGDB_GENRE_MAP.size() == 0){
            fillIGDBGenreMap();
        }
        return IGDB_GENRE_MAP.get(igdbGenreId);
    }
    private static void fillIGDBGenreMap(){
        IGDB_GENRE_MAP.put(2,POINT_AND_CLICK);
        IGDB_GENRE_MAP.put(4,FIGHTING);
        IGDB_GENRE_MAP.put(5,SHOOTER);
        IGDB_GENRE_MAP.put(7,MUSIC);
        IGDB_GENRE_MAP.put(8,PLATFORM);
        IGDB_GENRE_MAP.put(9,PUZZLE);
        IGDB_GENRE_MAP.put(10,RACING);
        IGDB_GENRE_MAP.put(11,REAL_TIME_STRATEGY);
        IGDB_GENRE_MAP.put(12, ROLE_PLAYING);
        IGDB_GENRE_MAP.put(13,SIMULATOR);
        IGDB_GENRE_MAP.put(14,SPORT);
        IGDB_GENRE_MAP.put(15,STRATEGY);
        IGDB_GENRE_MAP.put(16,TURN_BASED_STRATEGY);
        IGDB_GENRE_MAP.put(24, HACK_AND_SLASH_BEAT_EM_UP);
        IGDB_GENRE_MAP.put(25, QUIZ_TRIVIA);
        IGDB_GENRE_MAP.put(26,PINBALL);
        IGDB_GENRE_MAP.put(30,TACTICAL);
        IGDB_GENRE_MAP.put(31,ADVENTURE);
        IGDB_GENRE_MAP.put(32,INDIE);
        IGDB_GENRE_MAP.put(33,ARCADE);
    }

    public static HashMap<Integer, GameGenre> getIgdbGenreMap() {
        return IGDB_GENRE_MAP;
    }

    public String getKey() {
        return key;
    }

    public static int getIGDBId(String nameKey){
        if(nameKey == null || nameKey.isEmpty()){
            return -1;
        }


        try {
            Connection connection = DataBase.getConnection();
            PreparedStatement getIdQuery = connection.prepareStatement("SELECT igdb_id FROM GameGenre WHERE name_key = ?");
            getIdQuery.setString(1,nameKey);
            ResultSet result = getIdQuery.executeQuery();

            if(result.next()){
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

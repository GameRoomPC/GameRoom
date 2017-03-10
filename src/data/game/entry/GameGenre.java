package data.game.entry;

import data.io.DataBase;
import ui.Main;

import java.sql.*;
import java.util.Collection;
import java.util.HashMap;

/**
 * Created by LM on 12/08/2016.
 */
public class GameGenre {
    private final static HashMap<Integer,GameGenre> ID_MAP = new HashMap<>();
    private String key;
    private int id;

    private GameGenre(int id, String key){
        this.key = key;
        this.id = id;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    public String getDisplayName(){
        return Main.GAME_GENRES_BUNDLE.getString(key);
    }

    public static GameGenre getGenreFromID(int id){
        if(ID_MAP.isEmpty()){
            try {
                initWithDb();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return ID_MAP.get(id);
    }

    private static void initWithDb() throws SQLException {
        Connection connection = DataBase.getUserConnection();
        Statement statement = connection.createStatement();
        ResultSet set = statement.executeQuery("select * from GameGenre");
        while(set.next()){
            int id = set.getInt("igdb_id");
            String key = set.getString("name_key");
            ID_MAP.put(id, new GameGenre(id,key));
        }
        statement.close();
    }

    public static HashMap<Integer, GameGenre> getIdMap() {
        return ID_MAP;
    }

    public String getKey() {
        return key;
    }

    public static int getIGDBId(String nameKey){
        if(nameKey == null || nameKey.isEmpty()){
            return -1;
        }


        try {
            Connection connection = DataBase.getUserConnection();
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

    public static Collection<GameGenre> values(){
        return ID_MAP.values();
    }
}

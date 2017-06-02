package data.game.entry;

import data.io.DataBase;
import ui.Main;

import java.sql.*;
import java.util.Collection;
import java.util.HashMap;

import static ui.Main.LOGGER;

/**
 * Created by LM on 13/08/2016.
 */
public class GameTheme{
    private final static HashMap<Integer, GameTheme> ID_MAP = new HashMap<>();
    private String key;
    private int id;

    private GameTheme(int id, String key) {
        this.key = key;
        this.id = id;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    public String getDisplayName() {
        return Main.GAME_THEMES_BUNDLE.getString(key);
    }


    public static GameTheme getThemeFromId(int id) {
        if (ID_MAP.size() == 0) {
            try {
                initWithDb();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        GameTheme theme = ID_MAP.get(id);
        if (theme == null) {
            //try to see if it exists in db
            try {
                Connection connection = DataBase.getUserConnection();
                PreparedStatement statement = connection.prepareStatement("select * from GameTheme where igdb_id = ?");
                statement.setInt(1, id);
                ResultSet set = statement.executeQuery();
                if (set.next()) {
                    int genreId = set.getInt("igdb_id");
                    String key = set.getString("name_key");
                    GameTheme newTheme = new GameTheme(genreId, key);
                    ID_MAP.put(genreId, newTheme);

                    return newTheme;
                }
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }
        return theme;
    }

    private static void initWithDb() throws SQLException {
        Connection connection = DataBase.getUserConnection();
        Statement statement = connection.createStatement();
        ResultSet set = statement.executeQuery("select * from GameTheme");
        while (set.next()) {
            int id = set.getInt("igdb_id");
            String key = set.getString("name_key");
            ID_MAP.put(id, new GameTheme(id, key));
        }
        statement.close();
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

    public static Collection<GameTheme> values() {
        return ID_MAP.values();
    }

    public static String getDisplayString(Collection<GameTheme> themes){
        if(themes == null || themes.isEmpty()){
            return "-";
        }
        String temp = "";
        int i = 0;
        for(GameTheme theme : themes){
            if(theme!=null) {
                temp += theme.getDisplayName();
                if (i != themes.size() - 1) {
                    temp += ", ";
                }
            }
            i++;
        }
        return temp;
    }
}

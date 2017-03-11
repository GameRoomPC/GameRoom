package data.game.entry;

import data.io.DataBase;

import java.sql.*;
import java.util.HashMap;

import static ui.Main.LOGGER;

/**
 * Created by LM on 02/03/2017.
 */
public class Developer {
    private final static HashMap<Integer, Developer> ID_MAP = new HashMap<>();
    private int igdb_id = -1;
    private int id;
    private String name;
    private int IGDBId;


    public Developer(int igdb_id, String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Developer's name was either null or empty : \"" + name + "\"");
        }
        this.igdb_id = igdb_id;
        this.name = name;

        insertInDB();
    }

    public Developer(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Developer's name was either null or empty : \"" + name + "\"");
        }
        this.name = name;
    }

    public int insertInDB() {
        try {
            String sql = "INSERT OR IGNORE INTO Developer(name_key," + (igdb_id < 0 ? "id_needs_update) VALUES (?,?)" : "igdb_id) VALUES (?,?)");
            PreparedStatement devStatement = DataBase.getUserConnection().prepareStatement(sql);
            devStatement.setString(1, name);
            if (igdb_id >= 0) {
                devStatement.setInt(2, igdb_id);
            } else {
                devStatement.setInt(2, 1);
            }
            devStatement.execute();
            devStatement.close();
            id = getIdInDb();

            ID_MAP.put(id, this);
            return id;
            //DataBase.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private int getIdInDb() {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Developer's name was either null or empty : \"" + name + "\"");
        }
        int id = -1;
        try {
            Connection connection = DataBase.getUserConnection();
            PreparedStatement getIdQuery = connection.prepareStatement("SELECT id FROM Developer WHERE name_key = ?");
            getIdQuery.setString(1, name);
            ResultSet result = getIdQuery.executeQuery();

            if (result.next()) {
                id = result.getInt(1);
                result.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return id;
    }

    public static Developer getFromIGDBId(int igdb_id) {
        if (ID_MAP.isEmpty()) {
            try {
                initWithDb();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        for (Developer dev : ID_MAP.values()) {
            if (dev.getIGDBId() == igdb_id) {
                return dev;
            }
        }
        //try to see if it exists in db
        try {
            Connection connection = DataBase.getUserConnection();
            PreparedStatement statement = connection.prepareStatement("select * from Developer where igdb_id = ?");
            statement.setInt(1, igdb_id);
            ResultSet set = statement.executeQuery();
            if (set.next()) {
                int devId = set.getInt("id");
                String key = set.getString("name_key");
                Developer newDev = new Developer(devId, key);
                newDev.setIGDBId(igdb_id);
                ID_MAP.put(devId, newDev);

                return newDev;
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Developer getFromId(int id) {
        if (ID_MAP.isEmpty()) {
            try {
                initWithDb();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        Developer dev = ID_MAP.get(id);

        if(dev == null){
            //try to see if it exists in db
            try {
                Connection connection = DataBase.getUserConnection();
                PreparedStatement statement = connection.prepareStatement("select * from Developer where id = ?");
                statement.setInt(1, id);
                ResultSet set = statement.executeQuery();
                if (set.next()) {
                    int devId = set.getInt("id");
                    String key = set.getString("name_key");
                    Developer newDev = new Developer(devId, key);
                    newDev.setIGDBId(set.getInt("igdb_id"));
                    ID_MAP.put(devId, newDev);

                    return newDev;
                }
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return dev;
    }

    private static void initWithDb() throws SQLException {
        Connection connection = DataBase.getUserConnection();
        Statement statement = connection.createStatement();
        ResultSet set = statement.executeQuery("select * from Developer");
        while (set.next()) {
            int id = set.getInt("id");
            String key = set.getString("name_key");
            ID_MAP.put(id, new Developer(id, key));
        }
        statement.close();
    }

    public int getIGDBId() {
        return IGDBId;
    }

    public void setIGDBId(int IGDBId) {
        this.IGDBId = IGDBId;
    }

    public String getName() {
        return name;
    }
}

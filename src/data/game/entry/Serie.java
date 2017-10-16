package data.game.entry;

import data.io.DataBase;

import java.sql.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;

import static ui.Main.LOGGER;

/**
 * Created by LM on 02/03/2017.
 */
public class Serie {
    private final static HashMap<Integer, Serie> ID_MAP = new HashMap<>();

    private final static int NONE_ID = -2;
    public final static int DEFAULT_ID = -1;
    public final static Serie NONE = new Serie(NONE_ID, NONE_ID, "-", false);


    private int igdb_id = DEFAULT_ID;
    private int id = DEFAULT_ID;
    private boolean id_needs_update = true;
    private String name;
    private boolean idNeedsUpdate;


    private Serie(int id, int igdb_id, String name, boolean updateIfExists) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Serie's name was either null or empty : \"" + name + "\"");
        }
        this.igdb_id = igdb_id;
        this.name = name;
        this.id = id;
        this.id_needs_update = false;

        if (id != NONE_ID) {
            insertInDB(updateIfExists);
        } else {
            ID_MAP.put(id, this);
        }
    }

    public Serie(int igdb_id, String name, boolean updateIfExists) {
        this(DEFAULT_ID, igdb_id, name, updateIfExists);
    }

    public Serie(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Company's name was either null or empty : \"" + name + "\"");
        }
        this.name = name;
    }

    public int insertInDB(boolean updateIfExists) {
        try {
            if (isInDB()) {
                id = getIdInDb();
                if (!updateIfExists) {
                    return id;
                }
                String sql = "UPDATE Serie set name_key=?" + (igdb_id < 0 ? "" : ", igdb_id=?,id_needs_update=?") + " where id=?" + id;
                PreparedStatement serieStatement = DataBase.getUserConnection().prepareStatement(sql);
                serieStatement.setString(1, name);
                if (igdb_id >= 0) {
                    serieStatement.setInt(2, igdb_id);
                    serieStatement.setInt(3, 0);
                }
                serieStatement.execute();
                serieStatement.close();
            } else {
                String sql = "INSERT OR IGNORE INTO Serie(name_key," + (igdb_id < 0 ? "id_needs_update) VALUES (?,?)" : "igdb_id) VALUES (?,?)");
                PreparedStatement serieStatement = DataBase.getUserConnection().prepareStatement(sql);
                serieStatement.setString(1, name);
                if (igdb_id >= 0) {
                    serieStatement.setInt(2, igdb_id);
                } else {
                    serieStatement.setInt(2, 1);
                }
                serieStatement.execute();
                serieStatement.close();

                id = getIdInDb();

            }
            ID_MAP.put(id, this);
            return id;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private boolean isInDB() {
        return getIdInDb() != DEFAULT_ID;
    }

    private int getIdInDb() {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Serie's name was either null or empty : \"" + name + "\"");
        }
        int id = DEFAULT_ID;
        try {
            Connection connection = DataBase.getUserConnection();
            PreparedStatement getIdQuery = connection.prepareStatement("SELECT id FROM Serie WHERE name_key = ?");
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

    public static Serie getFromIGDBId(int igdb_id) {
        if (ID_MAP.isEmpty()) {
            try {
                initWithDb();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        for (Serie serie : ID_MAP.values()) {
            if (serie.getIGDBId() == igdb_id) {
                return serie;
            }
        }
        //try to see if it exists in db
        try {
            Connection connection = DataBase.getUserConnection();
            PreparedStatement statement = connection.prepareStatement("select * from Serie where igdb_id = ? AND id_needs_update = 0");
            statement.setInt(1, igdb_id);
            ResultSet set = statement.executeQuery();
            if (set.next()) {
                int serieId = set.getInt("id");
                String key = set.getString("name_key");
                Serie newSerie = new Serie(serieId, igdb_id, key, false);
                ID_MAP.put(serieId, newSerie);

                return newSerie;
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Serie getFromId(int id) {
        if (ID_MAP.isEmpty()) {
            try {
                initWithDb();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        Serie serie = ID_MAP.get(id);

        if (serie == null) {
            //try to see if it exists in db
            try {
                Connection connection = DataBase.getUserConnection();
                PreparedStatement statement = connection.prepareStatement("select * from Serie where id = ?");
                statement.setInt(1, id);
                ResultSet set = statement.executeQuery();
                if (set.next()) {
                    int serieId = set.getInt("id");
                    int igdbId = set.getInt("igdb_id");
                    boolean idNeedsUpdate = set.getBoolean("id_needs_update");
                    String key = set.getString("name_key");
                    Serie newSerie = new Serie(serieId, igdbId, key, false);
                    newSerie.setIdNeedsUpdate(idNeedsUpdate);

                    ID_MAP.put(serieId, newSerie);

                    return newSerie;
                }
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return serie;
    }

    private static void initWithDb() throws SQLException {
        Connection connection = DataBase.getUserConnection();
        Statement statement = connection.createStatement();
        ResultSet set = statement.executeQuery("select * from Serie where id_needs_update = 0");
        while (set.next()) {
            int id = set.getInt("id");
            int igdbId = set.getInt("igdb_id");
            String key = set.getString("name_key");
            ID_MAP.put(id, new Serie(id, igdbId, key,false));
        }
        statement.close();
    }

    public String getName() {
        return name;
    }

    public static Collection<Serie> values() {
        return ID_MAP.values();
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        if (name == null) {
            return "-";
        }
        return name;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setIdNeedsUpdate(boolean idNeedsUpdate) {
        this.idNeedsUpdate = idNeedsUpdate;
    }

    public int getIGDBId() {
        return igdb_id;
    }
}

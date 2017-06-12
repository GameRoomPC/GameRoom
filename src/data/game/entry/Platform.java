package data.game.entry;

import data.io.DataBase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import ui.Main;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;

/**
 * Created by LM on 11/02/2017.
 */
public class Platform {
    public final static int STEAM_ID = 1;
    public final static int STEAM_ONLINE_ID = 2;
    public final static int ORIGIN_ID = 3;
    public final static int UPLAY_ID = 4;
    public final static int BATTLENET_ID = 5;
    public final static int GOG_ID = 6;

    private final static HashMap<Integer, Platform> ID_MAP = new HashMap<>();

    public final static int DEFAULT_ID = -1;
    public final static int NONE_ID = -2;

    public final static Platform NONE = new Platform(NONE_ID, NONE_ID, "default", true, "");


    private int igdb_id = DEFAULT_ID;
    private int id = DEFAULT_ID;

    private String nameKey;
    private boolean isPC;
    private String defaultSupportedExtensions;
    private String supportedExtensions;
    private String ROMFolder="";

    private Platform(ResultSet set) throws SQLException {
        id = set.getInt("id");
        nameKey = set.getString("name_key");
        isPC = set.getBoolean("is_pc");
        defaultSupportedExtensions =  set.getString("default_supported_extensions");
        supportedExtensions = set.getString("supported_extensions");
        if (supportedExtensions == null) {
            supportedExtensions = defaultSupportedExtensions;
        }
        ROMFolder = set.getString("path");
    }

    //should only be used to build the NONE platform
    private Platform(int id, int igdb_id, String nameKey, boolean isPC, String supportedExtensions) {
        if (nameKey == null || nameKey.isEmpty()) {
            throw new IllegalArgumentException("Platform's nameKey was either null or empty : \"" + nameKey + "\"");
        }
        this.igdb_id = igdb_id;
        this.nameKey = nameKey;
        this.id = id;
        this.isPC = isPC;
        this.supportedExtensions = supportedExtensions;
    }

    public static Platform getFromId(int id) {
        if (ID_MAP.isEmpty()) {
            try {
                initWithDb();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        Platform platform = ID_MAP.get(id);

        if (platform == null) {
            //try to see if it exists in db
            try {
                Connection connection = DataBase.getUserConnection();
                PreparedStatement statement = connection.prepareStatement("SELECT * \n" +
                        "FROM Platform\n" +
                        "LEFT JOIN GameFolder ON GameFolder.platform_id = Platform.id\n" +
                        "WHERE Platform.id=?");
                statement.setInt(1, id);
                ResultSet set = statement.executeQuery();
                if (set.next()) {
                    Platform newPlatform = new Platform(set);
                    ID_MAP.put(newPlatform.getId(), newPlatform);

                    return newPlatform;
                }
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return platform;
    }

    public static void initWithDb() throws SQLException {
        Connection connection = DataBase.getUserConnection();
        Statement statement = connection.createStatement();
        ResultSet set = statement.executeQuery("SELECT * \n" +
                "FROM Platform\n" +
                "LEFT JOIN GameFolder ON GameFolder.platform_id = Platform.id\n");
        while (set.next()) {
            Platform platform = new Platform(set);
            ID_MAP.put(platform.getId(), platform);
        }
        statement.close();
    }

    public int getIGDBId() {
        return igdb_id;
    }

    public void setIGDBId(int IGDBId) {
        this.igdb_id = IGDBId;
    }

    public String getName() {
        if (nameKey == null) {
            return "-";
        }
        String s = Main.getString(nameKey);
        return s.equals(Main.NO_STRING) ? nameKey : s;
    }

    public String getIconCSSId() {
        if (id == STEAM_ONLINE_ID) {
            //TODO implement a cleaner way to have icons for this
            return "steam-icon";
        }
        return nameKey + "-icon";
    }

    public static Collection<Platform> values() {
        return ID_MAP.values();
    }

    public static Collection<Platform> getEmulablePlatforms() {
        ArrayList<Platform> items = new ArrayList<>(Platform.values());
        items.removeIf(Platform::isPC);
        items.removeIf(platform -> platform.equals(Platform.NONE));
        items.removeIf(platform -> Emulator.getPossibleEmulators(platform).isEmpty());
        items.sort(Comparator.comparing(Platform::getName));
        return items;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return getName();
    }

    public boolean isPC() {
        return isPC;
    }

    public void setChosenEmulator(Emulator chosenEmulator) {
        try {
            Connection connection = DataBase.getUserConnection();
            Statement statement = connection.createStatement();
            statement.addBatch("UPDATE emulates SET user_choice = 0 WHERE platform_id = " + id);
            if (chosenEmulator != null) {
                statement.addBatch("UPDATE emulates SET user_choice = 1 WHERE platform_id = " + id + " AND emu_id=" + chosenEmulator.getSQLId());
            }
            statement.executeBatch();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Emulator getChosenEmulator() {
        try {
            Connection connection = DataBase.getUserConnection();
            Statement statement = connection.createStatement();
            ResultSet set = statement.executeQuery("SELECT emu_id from emulates WHERE user_choice = 1 AND platform_id = " + id);
            if (set.next()) {
                int emuId = set.getInt("emu_id");
                return Emulator.getFromId(emuId);
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!Platform.class.isAssignableFrom(obj.getClass())) {
            return false;
        }
        return this.hashCode() == obj.hashCode();
    }

    public String[] getSupportedExtensions() {
        if (supportedExtensions == null) {
            return new String[0];
        }
        String[] cpy = supportedExtensions.split(",");
        for (int i = 0; i < cpy.length; i++) {
            cpy[i] = "*." + cpy[i];
        }
        return cpy;
    }

    public String getSupportedExtensionsString() {
        return supportedExtensions == null ? "" : supportedExtensions;
    }

    public void setSupportedExtensions(String supportedExtensions) {
        setSupportedExtensions(supportedExtensions.split(","));
    }

    public void setSupportedExtensions(String[] supportedExtensions) {
        StringBuilder newValue = new StringBuilder("");
        if (supportedExtensions != null) {
            for (String s : supportedExtensions) {
                newValue.append(s).append(",");
            }
            newValue = newValue.deleteCharAt(newValue.length() - 1);
        }
        this.supportedExtensions = newValue.toString();
        try {
            Connection connection = DataBase.getUserConnection();
            PreparedStatement statement = connection.prepareStatement("UPDATE Platform SET supported_extensions=? WHERE id =" + id);
            statement.setString(1, this.supportedExtensions);
            statement.execute();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setROMFolder(String path) {
        this.ROMFolder = path;
        try {
            Connection connection = DataBase.getUserConnection();
            Statement statement = connection.createStatement();
            ResultSet set = statement.executeQuery("SELECT id from GameFolder WHERE platform_id = " + id);
            int folderId = -666;
            if (set.next()) {
                folderId = set.getInt("id");
            }
            statement.close();
            if (folderId == -666) {
                PreparedStatement insertStatement = connection
                        .prepareStatement("INSERT INTO GameFolder(path,platform_id) VALUES (?,?)");
                insertStatement.setString(1, path);
                insertStatement.setInt(2, id);
                insertStatement.execute();
                insertStatement.close();
            } else {
                PreparedStatement updateStatement = connection.prepareStatement("UPDATE GameFolder SET path=? WHERE id=" + folderId);
                updateStatement.setString(1, path);
                updateStatement.execute();
                updateStatement.close();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getDefaultSupportedExtensionsString() {
        return defaultSupportedExtensions;
    }

    public String getROMFolder() {
        return ROMFolder;
    }
}

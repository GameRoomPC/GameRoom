package data.game.entry;

import data.io.DataBase;
import ui.Main;

import java.io.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by LM on 11/02/2017.
 */
public class Emulator {

    private final static String PATH_MARKER = "%p";

    private int sqlId;
    private String name;
    private File defaultPath; //should never be modified
    private File path;
    private String defaultArgSchema; // should never be modified
    private String argSchema;

    private final static HashMap<Integer, Emulator> EMULATOR_MAPPING = new HashMap<>();

    public static void loadEmulators() {
        try {
            PreparedStatement statement = DataBase.getUserConnection().prepareStatement("SELECT * FROM Emulator");
            ResultSet set = statement.executeQuery();
            EMULATOR_MAPPING.clear();
            while (set.next()) {
                Emulator emulator = new Emulator(set);
                EMULATOR_MAPPING.put(emulator.getSQLId(), emulator);
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    /**
     * Given a {@link Platform}, returns an {@link ArrayList} containing {@link Emulator} that can emulate this game.
     *
     * @param platform the platform to emulate
     * @return a list of {@link Emulator} that can emulate this platform
     */
    public static ArrayList<Emulator> getPossibleEmulators(Platform platform) {
        ArrayList<Emulator> emulators = new ArrayList<>();
        try {
            PreparedStatement statement = DataBase.getUserConnection().prepareStatement("SELECT * FROM emulates WHERE platform_id=?");
            statement.setInt(1, platform.getId());
            ResultSet set = statement.executeQuery();

            while (set.next()) {
                int emuId = set.getInt("emu_id");
                emulators.add(EMULATOR_MAPPING.get(emuId));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return emulators;
    }

    public static Emulator getChosenEmulator(Platform platform) {
        try {
            PreparedStatement statement = DataBase.getUserConnection().prepareStatement("SELECT * FROM emulates WHERE platform_id=? AND user_choice=1");
            statement.setInt(1, platform.getId());
            ResultSet set = statement.executeQuery();

            if (set.next()) {
                loadEmulators();
                int emuId = set.getInt("emu_id");
                return EMULATOR_MAPPING.get(emuId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Emulator(ResultSet set) throws SQLException {
        sqlId = set.getInt("id");
        name = set.getString("name");
        defaultPath = new File(set.getString("default_path"));
        String pathString = set.getString("path");
        if (pathString == null) {
            path = defaultPath;
        } else {
            path = new File(pathString);
        }
        defaultArgSchema = set.getString("default_args_schema");
        argSchema = set.getString("args_schema");
        if (argSchema == null) {
            argSchema = defaultArgSchema;
        }

        save();
    }

    public Collection<Platform> getSupportedPlatforms() {
        ArrayList<Platform> platforms = new ArrayList<>();
        try {
            PreparedStatement statement = DataBase.getUserConnection().prepareStatement("SELECT * FROM emulates WHERE emu_id=?");
            statement.setInt(1, sqlId);
            ResultSet set = statement.executeQuery();

            while (set.next()) {
                int platformId = set.getInt("platform_id");
                platforms.add(Platform.getFromId(platformId));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return platforms;
    }

    private void save() {
        try {
            PreparedStatement statement = DataBase.getUserConnection()
                    .prepareStatement("UPDATE Emulator SET path=?,args_schema =?" +
                            "WHERE id = ?");
            statement.setString(1, path.getAbsolutePath());
            statement.setString(2, argSchema);
            statement.setInt(3, sqlId);
            statement.execute();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return name;
    }

    public File getPath() {
        return path;
    }

    public void setPath(File path) {
        this.path = path;
        save();
    }


    public List<String> getCommandsToExecute(GameEntry entry) {
        String[] args = argSchema.split("\\s");

        ArrayList<String> cmds = new ArrayList<>();
        cmds.add("\"" + path.getAbsolutePath() + "\"");

        for (String arg : args) {
            cmds.add(arg.replace(PATH_MARKER, "\"" + entry.getPath() + "\""));
        }
        return cmds;
    }

    public String getProcessName() {
        return path.getName();
    }

    public Integer getSQLId() {
        return sqlId;
    }

    public String getArgSchema() {
        return argSchema;
    }

    public void setArgSchema(String argSchema) {
        this.argSchema = argSchema;
    }

    @Override
    public int hashCode(){
        return sqlId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!Emulator.class.isAssignableFrom(obj.getClass())) {
            return false;
        }
        return this.hashCode() == obj.hashCode();
    }

}

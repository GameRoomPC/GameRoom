package data.game.entry;

import data.io.DataBase;
import system.application.GameStarter;
import system.os.Terminal;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * This modelizes an Emulator, which simply is a program to start a game. As not all Emulators work the same way, we have a
 * {@link #defaultArgSchema} defined for each of them, which some kind of pattern used to determine which arguments to use
 * and where to place the ROM's path.
 * Starting the emulator with the game pre-loaded os mostly done in {@link GameStarter#getStartGameCMD()}.
 *
 * @author LM. Garret (admin@gameroom.me)
 * @date 11/02/2017.
 */
public class Emulator {

    private final static String PATH_MARKER = "%p";
    private final static String ENTRY_ARGS_MARKER = "%a";

    private int sqlId;
    private String name;
    private File defaultPath; //should never be modified
    private File path;
    private String defaultArgSchema; // should never be modified

    private final static HashMap<Integer, Emulator> EMULATOR_MAPPING = new HashMap<>();

    /**
     * Loads all {@link Emulator} and puts them into the {@link #EMULATOR_MAPPING} to be retrieved easily afterwards
     */
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

    /**
     * Given a {@link Platform}, returns the {@link Emulator} that can has be chosen to emulate this game, or null
     *
     * @param platform the platform to emulate
     * @return the {@link Emulator} that can emulate this platform, or null if there is none configured
     */
    public static Emulator getChosenEmulator(Platform platform) {
        try {
            PreparedStatement statement = DataBase.getUserConnection().prepareStatement("SELECT * FROM emulates WHERE platform_id=? AND user_choice=1");
            statement.setInt(1, platform.getId());
            ResultSet set = statement.executeQuery();

            if (set.next()) {
                if (EMULATOR_MAPPING.isEmpty()) {
                    loadEmulators();
                }
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
                    .prepareStatement("UPDATE Emulator SET path=? WHERE id = ?");
            statement.setString(1, path.getAbsolutePath());
            statement.setInt(2, sqlId);
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

    /**
     * Gets the command line arguments associated to this emulator in order to emulate the given {@param entry}. It does
     * replace the {@link #ENTRY_ARGS_MARKER} with the {@link GameEntry#getArgs() associated, and then replaces the
     * {@link #PATH_MARKER} with the {@link GameEntry#getPath()}
     *
     * @param entry the entry to emulate
     * @return a {@link List<String>} containing the different arguments used to emulate this game.
     */
    public List<String> getCommandArguments(GameEntry entry) {
        ArrayList<String> cmds = new ArrayList<>();
        String emuArgs = getArgSchema(entry.getPlatform());
        String entryArgs = entry.getArgs();
        if (emuArgs != null) {
            emuArgs = emuArgs.replace(ENTRY_ARGS_MARKER, entryArgs != null ? entryArgs : "");
            emuArgs = emuArgs.replace(PATH_MARKER, "\"" + entry.getPath() + "\"");
            cmds = Terminal.splitCMDLine(emuArgs);
        }
        return cmds;
    }

    /**
     * Returns the same as {@link #getCommandArguments(GameEntry)} but with the path of the Emulator added at position 0
     *
     * @param entry the entry to emulate
     * @return a {@link List<String>} containing the different arguments used to emulate this game, preceded by the emulator's path
     */
    public List<String> getCommandsToExecute(GameEntry entry) {
        ArrayList<String> cmds = new ArrayList<>();
        cmds.add(path.getAbsolutePath());
        cmds.addAll(getCommandArguments(entry));
        return cmds;
    }

    public String getProcessName() {
        return path.getName();
    }

    public Integer getSQLId() {
        return sqlId;
    }

    public String getArgSchema(Platform platform) {
        if (platform == null || platform.isPCLauncher()) {
            return defaultArgSchema;
        }

        try {
            PreparedStatement statement = DataBase.getUserConnection().prepareStatement("SELECT args_schema FROM emulates WHERE emu_id=? AND platform_id=?");
            statement.setInt(1, sqlId);
            statement.setInt(2, platform.getId());
            ResultSet set = statement.executeQuery();

            if (set.next()) {
                String argSchema = set.getString("args_schema");
                return argSchema == null || argSchema.isEmpty() ? defaultArgSchema : argSchema;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return defaultArgSchema;
    }

    public void setArgSchema(String argSchema, Platform platform) {
        try {
            PreparedStatement statement = DataBase.getUserConnection().prepareStatement("UPDATE emulates SET args_schema=? WHERE emu_id=? AND platform_id=?");
            statement.setString(1, argSchema);
            statement.setInt(2, sqlId);
            statement.setInt(3, platform.getId());
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int hashCode() {
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

    public static Emulator getFromId(int emuId) {
        if (EMULATOR_MAPPING.isEmpty()) {
            loadEmulators();
        }
        return EMULATOR_MAPPING.get(emuId);
    }

    public String getDefaultArgSchema() {
        return defaultArgSchema;
    }
}

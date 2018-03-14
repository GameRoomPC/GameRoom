package com.gameroom.data.game.entry;

import com.gameroom.data.game.GameFolderManager;
import com.gameroom.data.game.GameWatcher;
import com.gameroom.data.game.scanner.FolderGameScanner;
import com.gameroom.data.io.DataBase;
import com.gameroom.ui.Main;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringJoiner;

import static com.gameroom.ui.Main.FILES_MAP;
import static com.gameroom.ui.Main.LOGGER;

/**
 * Created by LM on 03/07/2016.
 */
public class GameEntryUtils {
    public static final ArrayList<GameEntry> ENTRIES_LIST = new ArrayList<>();
    public static final ArrayList<GameEntry> IGNORED_ENTRIES = new ArrayList<>();

    public static ArrayList<GameEntry> loadToAddGames() {
        ArrayList<GameEntry> toAddGames = new ArrayList<>();

        try {
            Connection connection = DataBase.getUserConnection();
            Statement statement = connection.createStatement();

            ResultSet set = statement.executeQuery("select * from GameEntry where toAdd = 1 AND ignored = 0");
            while (set.next()) {
                toAddGames.add(GameEntry.loadFromDB(set));
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return toAddGames;
    }

    /**
     * Checks if a given entry is ignored (folder or steam)
     *
     * @param entry the entry to check
     * @return true if this entry is ignored, false otherwise
     */
    public static boolean isGameIgnored(GameEntry entry) {
        if (entry.isIgnored()) {
            return true;
        }
        boolean ignored = false;
        for (GameEntry ignoredEntry : IGNORED_ENTRIES) {
            ignored = entriesPathsEqual(entry, ignoredEntry);
            if (ignored) {
                return true;
            }
        }
        return ignored;
    }

    public static void loadIgnoredGames() {
        IGNORED_ENTRIES.clear();
        try {
            Connection connection = DataBase.getUserConnection();
            Statement statement = connection.createStatement();

            ResultSet set = statement.executeQuery("select * from GameEntry where ignored = 1");
            while (set.next()) {
                IGNORED_ENTRIES.add(GameEntry.loadFromDB(set));
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static int indexOf(GameEntry entry) {
        int index = -1;
        int i = 0;
        for (GameEntry gameEntry : ENTRIES_LIST) {
            if (entry.getId() == gameEntry.getId()) {
                index = i;
                break;
            }
            i++;
        }
        return index;
    }

    public static void updateGame(GameEntry entry) {
        int index = indexOf(entry);
        if (index != -1) {
            ENTRIES_LIST.set(index, entry);
            Main.LOGGER.info("Updated game : " + entry.getName());
        }
    }

    public static void loadGames() {
        try {
            Connection connection = DataBase.getUserConnection();
            Statement statement = connection.createStatement();

            ResultSet set = statement.executeQuery("select * from GameEntry where toAdd = 0 AND ignored = 0");
            while (set.next()) {
                addGame(GameEntry.loadFromDB(set));
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void addGame(GameEntry entry) {
        boolean validToAdd = true;
        for (GameEntry entryInList : ENTRIES_LIST) {
            validToAdd = entryInList.getId() != entry.getId();
            if (!validToAdd) {
                break;
            }
        }
        if (validToAdd) {
            ENTRIES_LIST.add(entry);
            Main.LOGGER.info("Added game : " + entry.getName());
        }
    }

    public static void removeGame(GameEntry entry) {
        ENTRIES_LIST.remove(entry);
        Main.LOGGER.info("Removed game : " + entry.getName());
    }

    public static String coverPath(GameEntry entry) {
        File coverFolder = FILES_MAP.get("cover");
        return coverFolder.getAbsolutePath() + File.separator + entry.getId();
    }

    public static String screenshotPath(GameEntry entry) {
        File screenshotFolder = FILES_MAP.get("screenshot");
        return screenshotFolder.getAbsolutePath() + File.separator + entry.getId();
    }

    /**
     * Compares entry with paths, and not with UUID.
     * This is helpful to compare entries in toAdd and !toAdd states, as they may point to the same game but not have the same UUID
     *
     * @param e1 the first entry to compare
     * @param e2 the other entry to compare
     * @return true if a path includes an other, false otherwise
     */
    public static boolean entriesPathsEqual(GameEntry e1, GameEntry e2) {
        if (e1 == null && e2 == null) {
            return true;
        } else if (e1 == null || e2 == null) {
            return false;
        }
        return entriesPathsEqual(e1.getPath(), e2);
    }

    /**
     * Compares a path against an entry's path
     * This is helpful to compare entries in toAdd and !toAdd states, as they may point to the same game but not have the same UUID
     *
     * @param path  the first entry to compare
     * @param entry the other entry to compare
     * @return true if a path includes an other, false otherwise
     */
    public static boolean entriesPathsEqual(String path, GameEntry entry) {
        if (path == null && entry == null) {
            return true;
        } else if (path == null || entry == null) {
            return false;
        }
        boolean e1IncludesE2 = path.trim().toLowerCase().contains(entry.getPath().trim().toLowerCase());
        boolean e2IncludesE1 = entry.getPath().trim().toLowerCase().contains(path.trim().toLowerCase());

        return e1IncludesE2 || e2IncludesE1;
    }

    public static boolean parentFolderIsUserGameFolder(GameEntry entry) {
        if (entry == null) {
            return false;
        }
        return parentFolderIsUserGameFolder(entry.getPath());
    }

    public static boolean parentFolderIsUserGameFolder(String absPath) {
        if (absPath == null) {
            return false;
        }
        File parent = new File(absPath);
        if (!parent.exists() || parent.getParentFile() == null || !parent.getParentFile().exists()) {
            return false;
        }

        for (File folder : GameFolderManager.getPCFolders()) {
            if (folder != null && folder.getAbsolutePath().trim().toLowerCase().equals(parent.getParentFile().getAbsolutePath().trim().toLowerCase())) {
                return true;
            }
        }
        return false;

    }

    public static boolean pathFromSameParentFolder(String path1, String path2) {
        if (path1 == null && path2 == null) {
            return true;
        } else if (path1 == null || path2 == null) {
            return false;
        }
        boolean e1IncludesE2 = path1.trim().toLowerCase().contains(path2.trim().toLowerCase());
        boolean e2IncludesE1 = path2.trim().toLowerCase().contains(path1.trim().toLowerCase());
        if (e1IncludesE2 || e2IncludesE1) {
            return true;
        }
        File f1 = new File(path1);
        File f2 = new File(path2);

        if (f1.exists() && f2.exists()) {
            File p1 = f1.getParentFile();
            File p2 = f2.getParentFile();

            boolean p1IncludesP2 = p1.getAbsolutePath().trim().toLowerCase().contains(p2.getAbsolutePath().trim().toLowerCase());
            boolean p2IncludesP1 = p2.getAbsolutePath().trim().toLowerCase().contains(p1.getAbsolutePath().trim().toLowerCase());

            return p1IncludesP2 || p2IncludesP1;
        }
        return false;
    }

    /**
     * Checks if Game is already in GameRoom's library
     * Compareason is done on the path or the name, as UUID may be different at this time
     *
     * @param foundEntry the entry to check
     * @return true if already in the library, false otherwise
     */
    public static boolean gameAlreadyInLibrary(GameEntry foundEntry) {
        return gameAlreadyIn(foundEntry, GameEntryUtils.ENTRIES_LIST);
    }

    /**
     * Checks if Game's parent folder is already in GameRoom's library
     * Compareason is done on the path or the name, as UUID may be different at this time
     *
     * @param foundEntry the entry to check
     * @return true if already in the library, false otherwise
     */
    public static boolean parentFolderAlreadyInLibrary(GameEntry foundEntry) {
        return parentFolderAlreadyIn(foundEntry, GameEntryUtils.ENTRIES_LIST);
    }

    /**
     * Checks if Game is already in GameRoom's library
     * Compareason is done on the path or the name, as UUID may be different at this time
     *
     * @param foundEntry the entry to check
     * @return true if already in the library, false otherwise
     */
    public static boolean gameAlreadyIn(GameEntry foundEntry, Collection<GameEntry> library) {
        if (foundEntry == null) {
            return true;
        }
        return pathAlreadyIn(foundEntry.getPath(), library);
    }

    /**
     * Checks if Game has its parent path is already in GameRoom's library
     * Compareason is done on the path
     *
     * @param foundEntry the entry to check
     * @return true if already in the library, false otherwise
     */
    private static boolean parentFolderAlreadyIn(GameEntry foundEntry, Collection<GameEntry> library) {
        if (foundEntry == null || parentFolderIsUserGameFolder(foundEntry.getPath())) {
            return true;
        }
        for (GameEntry entry : library) {
            if (pathFromSameParentFolder(entry.getPath(), foundEntry.getPath())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if Game is already in GameRoom's library
     * Compareason is done on the path or the name, as UUID may be different at this time
     *
     * @param path the entry to check
     * @return true if already in the library, false otherwise
     */
    public static boolean pathAlreadyIn(String path, Collection<GameEntry> library) {
        boolean alreadyAddedToLibrary = false;
        for (GameEntry entry : library) {
            alreadyAddedToLibrary = entriesPathsEqual(path, entry);
            if (alreadyAddedToLibrary) {
                break;
            }
        }
        return alreadyAddedToLibrary;
    }

    /**
     * Update the {@link DataBase} and sets in one SQL request games given as no toAdd anymore, and also updates the added
     * date.
     *
     * @param entries entries to consider as not to add anymore.
     */
    public static boolean updateAsNotToAdd(@NonNull Collection<GameEntry> entries) {
        StringJoiner joiner = new StringJoiner(",", "(", ")");
        for (GameEntry entry : entries) {
            if (entry != null) {
                joiner.add("?");

            }
        }
        int paramIndex = 1;
        try {
            PreparedStatement statement = DataBase.getUserConnection().prepareStatement(
                    "UPDATE GameEntry SET toAdd = ?, added_date = ? WHERE id in " + joiner.toString());
            statement.setInt(paramIndex++, 0);
            statement.setTimestamp(paramIndex++, Timestamp.valueOf(LocalDateTime.now()));
            for (GameEntry entry : entries) {
                if (entry != null) {
                    statement.setInt(paramIndex++, entry.getId());
                }
            }

            statement.execute();
            statement.close();

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}

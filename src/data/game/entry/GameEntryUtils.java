package data.game.entry;

import data.io.DataBase;
import data.io.FileUtils;
import data.migration.OldGameEntry;
import ui.Main;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;

import static ui.Main.FILES_MAP;

/**
 * Created by LM on 03/07/2016.
 */
public class GameEntryUtils {
    public static final ArrayList<GameEntry> ENTRIES_LIST = new ArrayList<>();

    public static ArrayList<GameEntry> loadToAddGames(){
        ArrayList<GameEntry> toAddGames = new ArrayList<>();

        try {
            Connection connection = DataBase.getUserConnection();
            Statement statement = connection.createStatement();

            ResultSet set = statement.executeQuery("select * from GameEntry where toAdd = 1");
            while (set.next()) {
                GameEntry nextEntry = GameEntry.loadFromDB(set);
                if (nextEntry != null) {
                    toAddGames.add(nextEntry);
                }
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
    public static boolean isGameIgnored(GameEntry entry){
        ArrayList<GameEntry> ignoredGames = loadIgnoredGames();
        boolean ignored = false;
        for (GameEntry ignoredEntry : ignoredGames) {
            ignored = ignoredEntry.getPath().toLowerCase().contains(entry.getPath().toLowerCase())
                    || entry.getPath().toLowerCase().contains(ignoredEntry.getPath().toLowerCase());
            if (ignored) {
                return true;
            }
        }
        return ignored;
    }

    public static ArrayList<GameEntry> loadIgnoredGames(){
        ArrayList<GameEntry> toAddGames = new ArrayList<>();

        try {
            Connection connection = DataBase.getUserConnection();
            Statement statement = connection.createStatement();

            ResultSet set = statement.executeQuery("select * from GameEntry where ignored = 1");
            while (set.next()) {
                GameEntry nextEntry = GameEntry.loadFromDB(set);
                if (nextEntry != null) {
                    toAddGames.add(nextEntry);
                }
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return toAddGames;
    }

    private static int indexOf(GameEntry entry){
        int index = -1;
        int i = 0;
        for(GameEntry gameEntry : ENTRIES_LIST){
            if(entry.getId() == gameEntry.getId()){
                index = i;
                break;
            }
            i++;
        }
        return index;
    }
    public static void updateGame(GameEntry entry){
        int index = indexOf(entry);
        if(index!=-1) {
            ENTRIES_LIST.set(index, entry);
            Main.LOGGER.info("Updated game : " + entry.getName());
        }
    }
    public static void loadGames(){
        //TODO detect in following method if exists old games
        loadGames(FILES_MAP.get("games"));

        DataBase.initDB();
        OldGameEntry.transferOldGameEntries();

        try {
            Connection connection = DataBase.getUserConnection();
            Statement statement = connection.createStatement();

            ResultSet set = statement.executeQuery("select * from GameEntry where toAdd = 0");
            while (set.next()){
                GameEntry nextEntry = GameEntry.loadFromDB(set);
                if(nextEntry != null){
                    addGame(nextEntry);
                    //LOGGER.debug("Loaded game \""+nextEntry.getName()+"\"");
                }
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void loadGames(File entryFolder){
        /*ArrayList<UUID> uuids = GameEntryUtils.readUUIDS(entryFolder);
        for (UUID uuid : uuids) {
            GameEntry entry = new GameEntry(uuid);
            addGame(entry);
        }*/
    }
    public static void addGame(GameEntry entry){
        addGame(entry,true);
    }
    private static void addGame(GameEntry entry, boolean filterByUUID){
        boolean validToAdd = true;
        if(filterByUUID){
            for(GameEntry entryInList : ENTRIES_LIST){
                validToAdd = entryInList.getId() != entry.getId();
                if(!validToAdd){
                    Main.LOGGER.debug("Matching uuids for games : "+entryInList.getName());
                    break;
                }
            }
        }
        if(validToAdd){
            ENTRIES_LIST.add(entry);
            Main.LOGGER.info("Added game : " + entry.getName());
        }
    }

    public static void removeGame(GameEntry entry){
        ENTRIES_LIST.remove(entry);
        Main.LOGGER.info("Removed game : " + entry.getName());
    }

    public static String coverPath(GameEntry entry){
        File coverFolder = FILES_MAP.get("cover");
        return coverFolder.getAbsolutePath()+ File.separator + entry.getId() ;
    }

    public static String screenshotPath(GameEntry entry){
        File screenshotFolder = FILES_MAP.get("screenshot");
        return screenshotFolder.getAbsolutePath()+ File.separator + entry.getId();

    }
}

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
import static ui.Main.LOGGER;

/**
 * Created by LM on 03/07/2016.
 */
public class GameEntryUtils {
    public static final ArrayList<GameEntry> ENTRIES_LIST = new ArrayList<>();

    public static ArrayList<GameEntry> loadToAddGames(){
        //TODO return toAdd games from db
        return new ArrayList<>();
    }

    public static ArrayList<UUID> readUUIDS(File entriesFolder){
        ArrayList<UUID> uuids = new ArrayList<>();
        entriesFolder = FileUtils.initOrCreateFolder(entriesFolder);

        for(File gameFolder : entriesFolder.listFiles()){
            String name = gameFolder.getName();
            try{
                if(gameFolder.isDirectory()){
                    uuids.add(UUID.fromString(name));
                }
            }catch (IllegalArgumentException iae){
                Main.LOGGER.warn("Folder "+name+" is not a valid UUID, ignoring");
            }
        }
        Main.LOGGER.info("Loaded " + uuids.size()+" uuids from folder "+entriesFolder.getName());
        return uuids;
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

        //TODO remove this deletion, this is just for dev
        File dbFile = Main.FILES_MAP.get("db");
        //dbFile.delete();

        DataBase.initDB();
        OldGameEntry.transferOldGameEntries();

        try {
            Connection connection = DataBase.getConnection();
            Statement statement = connection.createStatement();
            ResultSet set = statement.executeQuery("select * from GameEntry");
            while (set.next()){
                addGame(GameEntry.loadFromDB(set));
                LOGGER.debug("Loaded game \""+set.getString("name")+"\"");
            }
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
}

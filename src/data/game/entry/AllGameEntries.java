package data.game.entry;

import data.FileUtils;
import ui.Main;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;

import static ui.Main.FILES_MAP;

/**
 * Created by LM on 03/07/2016.
 */
public class AllGameEntries {
    public static final ArrayList<GameEntry> ENTRIES_LIST = new ArrayList<>();


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
            if(entry.getUuid().equals(gameEntry.getUuid())){
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
        loadGames(FILES_MAP.get("games"));
    }

    private static void loadGames(File entryFolder){
        ArrayList<UUID> uuids = AllGameEntries.readUUIDS(entryFolder);
        for (UUID uuid : uuids) {
            GameEntry entry = new GameEntry(uuid);
            addGame(entry);
        }
    }
    public static void addGame(GameEntry entry){
        addGame(entry,true);
    }
    private static void addGame(GameEntry entry, boolean filterByUUID){
        boolean validToAdd = true;
        if(filterByUUID){
            for(GameEntry entryInList : ENTRIES_LIST){
                validToAdd = !entryInList.getUuid().equals(entry.getUuid());
                if(!validToAdd){
                    Main.LOGGER.debug("MAtching uuids for games : "+entryInList.getName()+", "+entry.getName());
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

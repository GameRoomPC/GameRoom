package data.game;

import data.game.entry.GameEntry;
import ui.Main;

import java.io.*;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by LM on 03/07/2016.
 */
public class AllGameEntries {
    public static final ArrayList<GameEntry> ENTRIES_LIST = new ArrayList<>();

    public static ArrayList<UUID> readUUIDS(){
        ArrayList<UUID> uuids = new ArrayList<>();
        File entriesFolder = null;
        try {
            entriesFolder = entriesFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        Main.LOGGER.info("Loaded " + uuids.size()+" uuids.");
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
        ENTRIES_LIST.set(indexOf(entry),entry);
        Main.LOGGER.info("Updated game : " + entry.getName());
    }
    public static void addGame(GameEntry entry){
        ENTRIES_LIST.add(entry);
        Main.LOGGER.info("Added game : " + entry.getName());
    }
    public static void removeGame(GameEntry entry){
        ENTRIES_LIST.remove(entry);
        Main.LOGGER.info("Removed game : " + entry.getName());
    }

    private static File entriesFile() throws IOException {
        File file = new File("games");
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }
}

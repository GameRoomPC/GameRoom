package data.game;

import ui.Main;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.UUID;

/**
 * Created by LM on 03/07/2016.
 */
public class AllGameEntries {
    public static final ArrayList<GameEntry> ENTRIES_LIST = new ArrayList<>();

    public ArrayList<UUID> readUUIDS(){
        ArrayList<UUID> uuids = new ArrayList<>();
        File entriesFolder = null;
        try {
            entriesFolder = entriesFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*Scanner scanner = null;
        try {
            scanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        //now read the file line by line...
        int lineNum = 0;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            uuids.add(UUID.fromString(line));
            lineNum++;
        }*/
        for(File gameFolder : entriesFolder.listFiles()){
            String name = gameFolder.getName();
            try{
                if(gameFolder.isDirectory()){
                    uuids.add(UUID.fromString(name));
                }
            }catch (IllegalArgumentException iae){
                Main.logger.warn("Folder "+name+" is not a valid UUID, ignoring");
            }
        }
        Main.logger.info("Loaded " + uuids.size()+" uuids.");
        return uuids;
    }


    private File entriesFile() throws IOException {
        File file = new File("games");
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }
}

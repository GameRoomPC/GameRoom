package data.game.scanner;

import data.game.GameWatcher;
import data.game.entry.AllGameEntries;
import data.game.entry.GameEntry;
import javafx.concurrent.Task;
import system.application.settings.PredefinedSetting;
import ui.Main;

import java.io.File;
import java.util.ArrayList;

import static data.game.GameWatcher.cleanNameForCompareason;

/**
 * Created by LM on 19/08/2016.
 */
public class FolderGameScanner extends GameScanner{
    private final static String[] EXCLUDED_FILE_NAMES = new String[]{"Steam Library"};
    private final static String[] VALID_EXECUTABLE_EXTENSION = new String[]{".exe",".ink",".jar"};


    public FolderGameScanner(GameWatcher parentLooker) {
        super(parentLooker);
        isLocalScanner = true;
    }

    @Override
    public void scanForGames() {
        scanDone = false;
        Task steamTask = new Task() {
            @Override
            protected Object call() throws Exception {
                foundGames.clear();

                ArrayList<GameEntry> potentialEntries = getPotentialEntries();

                for (GameEntry potentialEntry : potentialEntries) {
                    boolean gameAlreadyInLibrary = gameAlreadyInLibrary(potentialEntry);
                    boolean folderGameIgnored = folderGameIgnored(potentialEntry);
                    boolean alreadyWaitingToBeAdded = parentLooker.alreadyWaitingToBeAdded(potentialEntry);
                    if (!gameAlreadyInLibrary
                            && !folderGameIgnored
                            && !alreadyWaitingToBeAdded) {
                        addGameEntryFound(potentialEntry);
                    }
                }
                return null;
            }
        };
        steamTask.setOnSucceeded(event -> {
            scanDone = true;
            if(foundGames.size() > 0) {
                Main.LOGGER.info(this.getClass().getName() + " : found = " + foundGames.size());
            }
        });
        Thread th = new Thread(steamTask);
        th.setDaemon(false);
        th.start();
    }
    protected boolean folderGameIgnored(GameEntry entry){
        boolean ignored = false;
        for(File ignoredFile : Main.GENERAL_SETTINGS.getFiles(PredefinedSetting.IGNORED_GAME_FOLDERS)){
            ignored = ignoredFile.toPath().toAbsolutePath().toString().toLowerCase().contains(entry.getPath().toLowerCase());
            if(ignored){
                return true;
            }
        }
        return false;
    }
    protected ArrayList<GameEntry> getPotentialEntries(){
        File gamesFolder = Main.GENERAL_SETTINGS.getFile(PredefinedSetting.GAMES_FOLDER);
        ArrayList<GameEntry> entriesFound = new ArrayList<>();
        if(!gamesFolder.exists() || !gamesFolder.isDirectory()){
            return entriesFound;
        }
        for(File file : gamesFolder.listFiles()){
            if(isPotentiallyAGame(file)){
                GameEntry potentialEntry = new GameEntry(file.getName());
                potentialEntry.setPath(file.getAbsolutePath());
                potentialEntry.setNotInstalled(false);
                entriesFound.add(potentialEntry);
            }
        }
        return entriesFound;
    }
    private boolean isPotentiallyAGame(File file){
        for(String excludedName : EXCLUDED_FILE_NAMES){
            if(file.getName().equals(excludedName)){
                return false;
            }
        }
        if(file.isDirectory()){
            boolean potentialGame = false;
            for(File subFile : file.listFiles()){
                potentialGame = potentialGame ||isPotentiallyAGame(subFile);
            }
            return potentialGame;
        }else{
            return fileHasValidExtension(file);
        }
    }
    private boolean fileHasValidExtension(File file){
        boolean hasAValidExtension = false;
        for(String validExtension : VALID_EXECUTABLE_EXTENSION){
            hasAValidExtension = hasAValidExtension || file.getAbsolutePath().endsWith(validExtension.toLowerCase());
        }
        return hasAValidExtension;
    }
    protected boolean gameAlreadyInLibrary(GameEntry foundEntry){
        boolean alreadyAddedToLibrary = false;
        for (GameEntry entry : AllGameEntries.ENTRIES_LIST) {
            alreadyAddedToLibrary = entry.getPath().toLowerCase().trim().contains(foundEntry.getPath().trim().toLowerCase())
                || foundEntry.getPath().trim().toLowerCase().contains(entry.getPath().trim().toLowerCase())
                || cleanNameForCompareason(foundEntry.getName()).equals(cleanNameForCompareason(entry.getName())); //cannot use UUID as they are different at this pre-add-time
            if (alreadyAddedToLibrary) {
                break;
            }
        }
        return alreadyAddedToLibrary;
    }
}

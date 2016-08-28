package data.game;

import com.mashape.unirest.http.exceptions.UnirestException;
import data.ImageUtils;
import data.game.entry.AllGameEntries;
import data.game.entry.GameEntry;
import data.game.scanner.FolderGameScanner;
import data.game.scanner.GameScanner;
import data.game.scanner.OnGameFoundHandler;
import data.game.scanner.SteamGameScanner;
import data.game.scrapper.*;
import javafx.application.Platform;
import org.json.JSONArray;
import ui.Main;
import ui.control.button.gamebutton.GameButton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by LM on 17/08/2016.
 */
public class GameWatcher {
    private final static int SCAN_DELAY_MINUTES = 5;
    protected OnGameFoundHandler onGameFoundHandler;

    private ArrayList<GameEntry> entriesToAdd = new ArrayList<>();

    private ArrayList<GameScanner> gameScanners = new ArrayList<>();

    public GameWatcher(OnGameFoundHandler onGameFoundHandler) {
        this.onGameFoundHandler = onGameFoundHandler;
        gameScanners.add(new SteamGameScanner(this));
        gameScanners.add(new FolderGameScanner(this));
    }

    public void startService() {
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                while (Main.KEEP_THREADS_RUNNING) {
                    scanNewGamesRoutine();
                    tryScrapToAddEntries();
                    scanSteamGamesTime();

                    try {
                        Thread.sleep(SCAN_DELAY_MINUTES * 60 * 100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        th.setDaemon(true);
        th.start();
    }
    private void tryScrapToAddEntries(){
        ArrayList<Integer> searchIGDBIDs = new ArrayList<>();
        ArrayList<GameEntry> toScrapEntries = new ArrayList<>();
        if (Main.SUPPORTER_MODE) {
            for (GameEntry entry : entriesToAdd) {
                if (entry.isWaitingToBeScrapped()) {
                    try {
                        JSONArray search_results = IGDBScrapper.searchGame(entry.getName());
                        searchIGDBIDs.add(search_results.getJSONObject(0).getInt("id"));
                        toScrapEntries.add(entry);

                    } catch (Exception e) {
                        Main.LOGGER.error(entry.getName() + " not found on igdb first guess");
                    }
                }
            }
        }
        if(searchIGDBIDs.size()>0){
            try {
                JSONArray search_data = IGDBScrapper.getGamesData(searchIGDBIDs);
                for (int i = 0; i < search_data.length(); i++) {
                    GameEntry scrappedEntry = IGDBScrapper.getEntry(search_data.getJSONObject(i));
                    scrappedEntry.setUuid(toScrapEntries.get(i).getUuid());
                    scrappedEntry.setWaitingToBeScrapped(false);
                    scrappedEntry.setName(toScrapEntries.get(i).getName());
                    scrappedEntry.setPath(toScrapEntries.get(i).getPath());
                    if(toScrapEntries.get(i).getPlayTimeSeconds()!=0) {
                        scrappedEntry.setPlayTimeSeconds(toScrapEntries.get(i).getPlayTimeSeconds());
                    }
                    if(toScrapEntries.get(i).getSteam_id()!=-1) {
                        scrappedEntry.setSteam_id(toScrapEntries.get(i).getSteam_id());
                    }
                    scrappedEntry.setNotInstalled(toScrapEntries.get(i).isNotInstalled());
                    if(toScrapEntries.get(i).getDescription()!= null && !toScrapEntries.get(i).getDescription().equals("")){
                        scrappedEntry.setDescription(toScrapEntries.get(i).getDescription());
                    }
                    if (scrappedEntry.getReleaseDate() == null) {
                        scrappedEntry.setReleaseDate(toScrapEntries.get(i).getReleaseDate());
                    }
                    ImageUtils.downloadIGDBImageToCache(scrappedEntry.getIgdb_id()
                            , scrappedEntry.getIgdb_imageHash(0)
                            , ImageUtils.IGDB_TYPE_COVER
                            , ImageUtils.IGDB_SIZE_BIG_2X
                            , new OnDLDoneHandler() {
                                @Override
                                public void run(File outputfile) {
                                    scrappedEntry.setImagePath(0, outputfile);
                                    ImageUtils.downloadIGDBImageToCache(scrappedEntry.getIgdb_id()
                                            , scrappedEntry.getIgdb_imageHash(1)
                                            , ImageUtils.IGDB_TYPE_SCREENSHOT
                                            , ImageUtils.IGDB_SIZE_BIG_2X
                                            , new OnDLDoneHandler() {
                                                @Override
                                                public void run(File outputfile) {
                                                    scrappedEntry.setImagePath(1, outputfile);
                                                    Platform.runLater(() -> {
                                                        Main.MAIN_SCENE.updateGame(scrappedEntry);
                                                    });
                                                }
                                            });

                                }
                            });
                }
            } catch (UnirestException e) {
                e.printStackTrace();
            }

        }
    }
    private void scanSteamGamesTime(){
        try {
            ArrayList<GameEntry> ownedSteamApps = SteamOnlineScrapper.getOwnedSteamGames();
            for(GameEntry ownedEntry : ownedSteamApps){
                if(ownedEntry.getPlayTimeSeconds()!=0){
                    for(GameEntry storedEntry : AllGameEntries.ENTRIES_LIST){
                        if(ownedEntry.getSteam_id() == storedEntry.getSteam_id() && ownedEntry.getPlayTimeSeconds()!=storedEntry.getPlayTimeSeconds()){
                            storedEntry.setPlayTimeSeconds(ownedEntry.getPlayTimeSeconds());
                            Platform.runLater(() -> {
                                Main.MAIN_SCENE.updateGame(storedEntry);
                            });
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void scanNewGamesRoutine(){
        final int originalGameFoundNumber = entriesToAdd.size();
        for(GameScanner scanner : gameScanners){
            scanner.scanForGames();
        }
        //now we wait for the scanners to have all finished
        boolean allScannersDone = false;
        while(!allScannersDone){
            int i = 0;
            for(GameScanner scanner : gameScanners){
                if(i==0){
                    allScannersDone = scanner.isScanDone();
                }else{
                    allScannersDone = allScannersDone && scanner.isScanDone();
                }
                i++;
            }
            try {
                Thread.sleep(2 * 100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if(entriesToAdd.size()>originalGameFoundNumber){
            Main.LOGGER.info("GameWatcher : found "+entriesToAdd.size()+" new games!");
            onGameFoundHandler.onAllGamesFound();
        }
    }

    public boolean alreadyWaitingToBeAdded(GameEntry entry) {
        boolean already = false;
        for (GameEntry gameEntry : entriesToAdd) {
            already = gameEntry.getPath().equals(entry.getPath());
            if (already) {
                return already;
            }
        }
        return already;
    }

    public boolean alreadyWaitingToBeAdded(SteamPreEntry entry) {
        boolean already = false;
        for (GameEntry gameEntry : entriesToAdd) {
            already = gameEntry.getSteam_id() == entry.getId();
            if (already) {
                return already;
            }
        }
        return already;
    }
    public GameButton onGameFound(GameEntry foundEntry) {
        if(!alreadyWaitingToBeAdded(foundEntry)){
            Main.LOGGER.debug(GameWatcher.class.getName()+" : found new game, "+foundEntry.getName());
            entriesToAdd.add(foundEntry);
            return onGameFoundHandler.gameToAddFound(foundEntry);
        }
        return null;
    }
}

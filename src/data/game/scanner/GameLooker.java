package data.game.scanner;

import data.game.entry.GameEntry;
import data.game.scrapper.*;
import org.json.JSONArray;
import ui.Main;
import ui.control.button.gamebutton.GameButton;

import java.util.ArrayList;

/**
 * Created by LM on 17/08/2016.
 */
public class GameLooker {
    private final static int SCAN_DELAY_MINUTES = 5;
    protected OnGameFoundHandler onGameFoundHandler;

    private ArrayList<GameEntry> entriesToAdd = new ArrayList<>();

    private ArrayList<GameScanner> gameScanners = new ArrayList<>();

    public GameLooker(OnGameFoundHandler onGameFoundHandler) {
        this.onGameFoundHandler = onGameFoundHandler;
        gameScanners.add(new SteamGameScanner(this));
        gameScanners.add(new FolderGameScanner(this));
    }

    public void startService() {
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                while (Main.KEEP_THREADS_RUNNING) {
                    Main.LOGGER.info("Starting game watch routine");

                    int originalGameFoundNumber = entriesToAdd.size();
                    for(GameScanner scanner : gameScanners){
                        scanner.scanForGames();
                    }
                    //now we wait for the scanners to have all finished
                    boolean allScannersDone = false;
                    while(!allScannersDone){
                        int i = 0;
                        for(GameScanner scanner : gameScanners){
                            if(i==0){
                                allScannersDone = scanner.scanDone;
                            }else{
                                allScannersDone = allScannersDone && scanner.scanDone;
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
                        Main.LOGGER.info(GameLooker.class.toGenericString()+" : total games found = "+entriesToAdd.size());
                        onGameFoundHandler.onAllGamesFound();
                    }

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

    protected boolean alreadyWaitingToBeAdded(GameEntry entry) {
        boolean already = false;
        for (GameEntry gameEntry : entriesToAdd) {
            already = gameEntry.getPath().equals(entry.getPath());
            if (already) {
                break;
            }
        }
        return already;
    }

    protected boolean alreadyWaitingToBeAdded(SteamPreEntry entry) {
        boolean already = false;
        for (GameEntry gameEntry : entriesToAdd) {
            already = gameEntry.getSteam_id() == entry.getId();
            if (already) {
                break;
            }
        }
        return already;
    }

    protected GameEntry tryGetFirstIGDBResult(String name) {
        try {
            JSONArray search_results = IGDBScrapper.searchGame(name);
            ArrayList list = new ArrayList();
            list.add(search_results.getJSONObject(0).getInt("id"));
            JSONArray search_data = IGDBScrapper.getGamesData(list);
            return IGDBScrapper.getEntry(search_data.getJSONObject(0));

        } catch (Exception e) {
            Main.LOGGER.error(name + " not found on igdb first guess");
            return null;
        }

    }
    public GameButton onGameFound(GameEntry foundEntry) {
        if(!alreadyWaitingToBeAdded(foundEntry)){
            Main.LOGGER.debug(GameLooker.class.getName()+" : found new game, "+foundEntry.getName());
            entriesToAdd.add(foundEntry);
            return onGameFoundHandler.gameToAddFound(foundEntry);
        }
        return null;
    }
}

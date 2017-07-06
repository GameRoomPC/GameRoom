package data.game.scanner;

import data.game.GameWatcher;
import data.game.entry.GameEntry;
import data.game.entry.Platform;
import ui.Main;
import ui.control.button.gamebutton.GameButton;
import ui.GeneralToast;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ui.Main.MAIN_SCENE;

/**
 * Created by LM on 19/08/2016.
 */
public abstract class GameScanner {
    ScannerProfile profile = null;
    private volatile boolean scanDone = false;
    GameWatcher parentLooker;


    GameScanner(GameWatcher parentLooker) {
        this.parentLooker = parentLooker;
    }

    public final void startScanning() {
        scanDone = false;
        if (profile == null || profile.isEnabled()) {
            displayStartToast();
            scanAndAddGames();
        }
        scanDone = true;
    }

    protected abstract void scanAndAddGames();

    public boolean isScanDone() {
        return scanDone;
    }

    private GameButton onGameFound(GameEntry foundEntry) {
        return parentLooker.onGameFound(foundEntry);
    }

    void addGameEntryFound(GameEntry entryFound) {
        entryFound.setWaitingToBeScrapped(true);

        Main.runAndWait(() -> {
            onGameFound(entryFound);
        });
    }

    protected void displayStartToast() {
        if (MAIN_SCENE != null) {
            if (profile != null) {
                GeneralToast.displayToast(Main.getString("scanning") + " " + profile.toString(), MAIN_SCENE.getParentStage(), GeneralToast.DURATION_SHORT, true);
            } else {
                GeneralToast.displayToast(Main.getString("scanning"), MAIN_SCENE.getParentStage(), GeneralToast.DURATION_SHORT, true);
            }
        }

    }

    public abstract void checkAndAdd(GameEntry entry);

    public ScannerProfile getScannerProfile() {
        return profile;
    }

    public Platform getPlatform(){
        if(profile == null){
            return Platform.NONE;
        }
        int platformID = profile.getPlatformId();
        return Platform.getFromId(platformID);
    }
}

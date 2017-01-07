package data.game.scanner;

import data.game.GameWatcher;
import data.game.entry.GameEntry;
import ui.Main;
import ui.control.button.gamebutton.GameButton;
import ui.control.specific.GeneralToast;

import java.util.ArrayList;

import static ui.Main.MAIN_SCENE;

/**
 * Created by LM on 19/08/2016.
 */
public abstract class GameScanner {
    protected ScannerProfile profile = null;
    private volatile boolean scanDone = false;
    ArrayList<GameEntry> foundGames = new ArrayList<>();
    GameWatcher parentLooker;

    GameScanner(GameWatcher parentLooker) {
        this.parentLooker = parentLooker;
    }

    public void startScanning() {
        scanDone = false;
        scanForGames();
        scanDone = true;
    }

    protected abstract void scanForGames();

    public boolean isScanDone() {
        return scanDone;
    }

    private GameButton onGameFound(GameEntry foundEntry) {
        return parentLooker.onGameFound(foundEntry);
    }

    void addGameEntryFound(GameEntry entryFound) {
        entryFound.setWaitingToBeScrapped(true);
        foundGames.add(entryFound);

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
}

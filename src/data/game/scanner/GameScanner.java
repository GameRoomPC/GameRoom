package data.game.scanner;

import data.game.GameWatcher;
import data.game.entry.GameEntry;
import data.game.entry.Platform;
import ui.Main;
import ui.control.button.gamebutton.GameButton;
import ui.GeneralToast;

import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static ui.Main.LOGGER;
import static ui.Main.MAIN_SCENE;

/**
 * Created by LM on 19/08/2016.
 */
public abstract class GameScanner {
    private final static int MAX_LATCH_AWAIT_SECONDS = 20;

    ScannerProfile profile = null;
    private volatile boolean scanDone = false;
    private volatile HashSet<CountDownLatch> tasksLatchs = new HashSet<>();
    GameWatcher parentLooker;


    GameScanner(GameWatcher parentLooker) {
        this.parentLooker = parentLooker;
    }

    public final void startScanning() {
        scanDone = false;
        if (profile == null || profile.isEnabled()) {
            LOGGER.info(getScannerName() + " started");
            displayStartToast();
            scanAndAddGames();

            final int[] size = {tasksLatchs.size()};
            LOGGER.debug(getScannerName() + ": "+ size[0] +" latchs");
            tasksLatchs.forEach(latch -> {
                try {
                    int sec = 20;
                    boolean completed = latch.await(MAX_LATCH_AWAIT_SECONDS, TimeUnit.SECONDS);
                    if(!completed){
                        LOGGER.debug(getScannerName() + ": skeeping latch"+ (size[0]) +" after "+MAX_LATCH_AWAIT_SECONDS+"s");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                LOGGER.debug(getScannerName() + " : "+ (--size[0]) +" latchs remaining");
            });
            if (profile != null)
                LOGGER.info(getScannerName() + " finished");
        }
        scanDone = true;
    }

    protected void addLatch(CountDownLatch latch) {
        tasksLatchs.add(latch);
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

    public Platform getPlatform() {
        if (profile == null) {
            return Platform.PC;
        }
        int platformID = profile.getPlatformId();
        return Platform.getFromId(platformID);
    }

    public String getScannerName() {
        if (getScannerProfile() != null) {
            return getScannerProfile().name()+" scanner";
        } else {
            return "(unnamed scanner)";
        }
    }
}

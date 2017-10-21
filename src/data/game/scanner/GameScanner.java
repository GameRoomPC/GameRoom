package data.game.scanner;

import data.game.GameWatcher;
import data.game.entry.GameEntry;
import data.game.entry.Platform;
import ui.Main;
import ui.control.button.gamebutton.GameButton;
import ui.GeneralToast;

import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static ui.Main.LOGGER;
import static ui.Main.MAIN_SCENE;

/**
 * Represents a scanner of a specific types of games, e.g. there can be a GOG scanner, a Steam scanner or GamesFolders
 * scanner. It should only be used by the {@link GameWatcher} instance, that will instantiate them and use them to look
 * for new games.
 *
 * @author LM. Garret (admin@gameroom.me)
 * @date 19/08/2016.
 */
public abstract class GameScanner {
    /**
     * Number of seconds to wait on a {@link CountDownLatch} before stopping await
     */
    private final static int MAX_LATCH_AWAIT_SECONDS = 20;

    /**
     * {@link ScannerProfile} which determines the type of scanner we use. May be null, there are values only for launcher
     * scanners
     */
    protected ScannerProfile profile = null;

    /**
     * {@link HashSet} containing all {@link CountDownLatch}s associated to {@link ScanTask}s that this {@link GameScanner}
     * has created (in its {@link GameScanner#scanAndAddGames()} method). We will await on those {@link CountDownLatch}s
     * so that we know when all {@link ScanTask} have been created
     */
    private volatile HashSet<CountDownLatch> tasksLatchs = new HashSet<>();

    /**
     * {@link GameWatcher} instance that created this {@link GameScanner} instance. Is used as a callback on
     * {@link GameWatcher#onGameFound(GameEntry)}
     */
    protected GameWatcher parentLooker;


    GameScanner(GameWatcher parentLooker) {
        this.parentLooker = parentLooker;
    }

    /**
     * Starts scanning. First, builds and submits all tasks with {@link GameScanner#scanAndAddGames()}; then await on them
     * using {@link GameScanner#tasksLatchs}, then returns.
     */
    public final void startScanning() {
        if (profile == null || profile.isEnabled()) {
            LOGGER.info(getScannerName() + " started");
            displayStartToast();
            scanAndAddGames();

            final int[] size = {tasksLatchs.size()};
            LOGGER.debug(getScannerName() + ": " + size[0] + " latchs");
            tasksLatchs.forEach(latch -> {
                try {
                    boolean completed = latch.await(MAX_LATCH_AWAIT_SECONDS, TimeUnit.SECONDS);
                    if (!completed) {
                        LOGGER.debug(getScannerName() + ": skeeping latch" + (size[0]) + " after " + MAX_LATCH_AWAIT_SECONDS + "s");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //LOGGER.debug(getScannerName() + " : " + (--size[0]) + " latchs remaining");
            });
            LOGGER.info(getScannerName() + " finished");
        }
    }

    /**
     * Adds a latch to the collection of {@link CountDownLatch} of this {@link GameScanner}
     *
     * @param latch a {@link CountDownLatch}, usually created by {@link ScanTask#ScanTask(GameScanner, Callable)}
     */
    protected void addLatch(CountDownLatch latch) {
        tasksLatchs.add(latch);
    }

    /**
     * Performs here the true "scanning" part. This overridden method should chop the work into small {@link ScanTask}s,
     * and submit them directly to {@link Main#getExecutorService()}.
     */
    protected abstract void scanAndAddGames();

    /**
     * Callback method when a game is found
     *
     * @param entryFound the found game
     */
    void addGameEntryFound(GameEntry entryFound) {
        entryFound.setWaitingToBeScrapped(true);

        Main.runAndWait(() -> {
            parentLooker.onGameFound(entryFound);
        });
    }

    protected void displayStartToast() {
        if (MAIN_SCENE != null) {
            if (profile != null) {
                GeneralToast.displayToast(Main.getString("scanning") + " " + profile.toString(), MAIN_SCENE.getParentStage(), GeneralToast.DURATION_SHORT);
            } else {
                GeneralToast.displayToast(Main.getString("scanning"), MAIN_SCENE.getParentStage(), GeneralToast.DURATION_SHORT);
            }
        }

    }

    /**
     * Checks if a given {@link GameEntry} should be considered as valid (for the current {@link GameScanner} instance, all
     * may have different criteria), and if it's the case then adds it.
     * @param entry the {@link GameEntry} to check and add.
     */
    public abstract void checkAndAdd(GameEntry entry);

    public ScannerProfile getScannerProfile() {
        return profile;
    }

    /**
     *
     * @return the {@link Platform} associated to this {@link GameScanner}
     */
    public Platform getPlatform() {
        if (profile == null) {
            return Platform.PC;
        }
        int platformID = profile.getPlatformId();
        return Platform.getFromId(platformID);
    }

    /**
     * @return a console printable name of this scanner
     */
    public String getScannerName() {
        if (getScannerProfile() != null) {
            return getScannerProfile().name() + " scanner";
        } else {
            return "(unnamed scanner)";
        }
    }
}

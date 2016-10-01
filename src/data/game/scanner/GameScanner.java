package data.game.scanner;

import data.game.GameWatcher;
import data.game.entry.GameEntry;
import javafx.concurrent.Task;
import ui.Main;
import ui.control.button.gamebutton.GameButton;

import java.util.ArrayList;

/**
 * Created by LM on 19/08/2016.
 */
public abstract class GameScanner {
    private volatile boolean scanDone = false;
    protected ArrayList<GameEntry> foundGames = new ArrayList<>();
    protected GameWatcher parentLooker;

    public GameScanner(GameWatcher parentLooker){
        this.parentLooker = parentLooker;
    }

    public void startScanning(){
        scanDone = false;
        Task scanningTask = new Task() {
            @Override
            protected Object call() throws Exception {
                scanForGames();
                scanDone = true;
                return null;
            }
        };
        Thread scanThread = new Thread(scanningTask);
        scanThread.setDaemon(true);
        scanThread.start();
    }
    protected abstract void scanForGames();

    public boolean isScanDone() {
        return scanDone;
    }

    private GameButton onGameFound(GameEntry foundEntry){
        return parentLooker.onGameFound(foundEntry);
    }

    protected void addGameEntryFound(GameEntry entryFound) {
        entryFound.setWaitingToBeScrapped(true);
        foundGames.add(entryFound);

        Main.runAndWait(() -> {
            onGameFound(entryFound);
        });
    }

}

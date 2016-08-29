package data.game.scanner;

import data.game.GameWatcher;
import data.game.entry.GameEntry;
import javafx.application.Platform;
import ui.Main;
import ui.control.button.gamebutton.GameButton;

import java.util.ArrayList;

/**
 * Created by LM on 19/08/2016.
 */
public abstract class GameScanner {
    protected volatile boolean scanDone = false;
    protected ArrayList<GameEntry> foundGames = new ArrayList<>();
    protected GameWatcher parentLooker;
    protected boolean isLocalScanner = true;

    public GameScanner(GameWatcher parentLooker){
        this.parentLooker = parentLooker;
    }

    public abstract void scanForGames();

    public boolean isScanDone() {
        return scanDone;
    }

    public synchronized ArrayList<GameEntry> getFoundGames() {
        return foundGames;
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

    public boolean isLocalScanner() {
        return isLocalScanner;
    }
}

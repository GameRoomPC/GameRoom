package data.game.scanner;

import data.ImageUtils;
import data.game.entry.GameEntry;
import data.game.scrapper.OnDLDoneHandler;
import javafx.application.Platform;
import ui.Main;
import ui.control.button.gamebutton.GameButton;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by LM on 19/08/2016.
 */
public abstract class GameScanner {
    protected volatile boolean scanDone = false;
    protected ArrayList<GameEntry> foundGames = new ArrayList<>();
    protected GameLooker parentLooker;

    public GameScanner(GameLooker parentLooker){
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
    public void automaticScrapAndAdd(GameEntry entryFound){
        if (Main.SUPPORTER_MODE) {
            onlineScrapAndAdd(entryFound);
        }else{
            offlineScrapAndAdd(entryFound);
        }
    }
    private void offlineScrapAndAdd(GameEntry entryFound) {
        foundGames.add(entryFound);

        Platform.runLater(() -> {
            onGameFound(entryFound);
        });
    }

    private void onlineScrapAndAdd(GameEntry entryFound){
        GameEntry guessedEntry = parentLooker.tryGetFirstIGDBResult(entryFound.getName());
        if (guessedEntry != null) {
            guessedEntry.setName(entryFound.getName());
            if(entryFound.getPlayTimeSeconds()!=0) {
                guessedEntry.setPlayTimeSeconds(entryFound.getPlayTimeSeconds());
            }
            guessedEntry.setSteam_id(entryFound.getSteam_id());
            guessedEntry.setNotInstalled(entryFound.isNotInstalled());
            if(entryFound.getDescription()!= null && !entryFound.getDescription().equals("")){
                guessedEntry.setDescription(entryFound.getDescription());
            }
            if (guessedEntry.getIgdb_imageHashs().length < 2 || guessedEntry.getIgdb_imageHash(1) == null) {
                guessedEntry.setIgdb_imageHash(1, guessedEntry.getIgdb_imageHash(0));
            }
            if (guessedEntry.getReleaseDate() == null) {
                guessedEntry.setReleaseDate(entryFound.getReleaseDate());
            }
            foundGames.add(guessedEntry);

            final GameButton[] createdGameButton = {null};
            Platform.runLater(() -> {
                createdGameButton[0] = onGameFound(guessedEntry);
            });

            ImageUtils.downloadIGDBImageToCache(guessedEntry.getIgdb_id()
                    , guessedEntry.getIgdb_imageHash(0)
                    , ImageUtils.IGDB_TYPE_COVER
                    , ImageUtils.IGDB_SIZE_BIG_2X
                    , new OnDLDoneHandler() {
                        @Override
                        public void run(File outputfile) {
                            guessedEntry.setImagePath(0, outputfile);
                            ImageUtils.downloadIGDBImageToCache(guessedEntry.getIgdb_id()
                                    , guessedEntry.getIgdb_imageHash(1)
                                    , ImageUtils.IGDB_TYPE_SCREENSHOT
                                    , ImageUtils.IGDB_SIZE_BIG_2X
                                    , new OnDLDoneHandler() {
                                        @Override
                                        public void run(File outputfile) {
                                            guessedEntry.setImagePath(1, outputfile);
                                            if (createdGameButton[0] != null) {
                                                createdGameButton[0].reloadWith(guessedEntry);
                                            }
                                        }
                                    });

                        }
                    });
        } else {
            offlineScrapAndAdd(entryFound);
        }
    }

}

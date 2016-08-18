package data.game;

import data.game.entry.GameEntry;
import data.game.scrapper.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONArray;
import system.application.settings.PredefinedSetting;
import ui.Main;
import ui.control.button.gamebutton.GameButton;
import ui.dialog.GameRoomAlert;
import ui.dialog.SteamIgnoredSelector;
import ui.scene.GameEditScene;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static ui.Main.GENERAL_SETTINGS;
import static ui.Main.MAIN_SCENE;
import static ui.Main.RESSOURCE_BUNDLE;

/**
 * Created by LM on 17/08/2016.
 */
public class GameLooker {
    private OnGameFoundHandler onGameFoundHandler;
    private ArrayList<SteamPreEntry> ownedSteamApps = new ArrayList<>();
    private ArrayList<SteamPreEntry> installedSteamApps = new ArrayList<>();

    private ArrayList<GameEntry> entriesToAdd = new ArrayList<>();
    private AtomicInteger foundGames = new AtomicInteger(0);

    public GameLooker(OnGameFoundHandler onGameFoundHandler) {
        this.onGameFoundHandler = onGameFoundHandler;
    }

    public void startService() {
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                while (Main.KEEP_THREADS_RUNNING) {
                    Main.LOGGER.info("Starting game watch routine");
                    foundGames = new AtomicInteger(0);

                    initFolderWatchTask()/*.run()*/;
                    initSteamWatchTask().run();
                    initSteamUpdateStatusTask();

                    if (foundGames.doubleValue()>0) {
                        onGameFoundHandler.onAllGamesFound();
                    }

                    try {
                        Thread.sleep(5 * 60 * 100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        th.setDaemon(true);
        th.start();
    }

    private Task initSteamUpdateStatusTask() {
        //TODO implement steam update watcher here
        /*
        Status flag :
        4 : fully installed
        6,1030 : downloading update



         */
        return null;
    }

    private Task initFolderWatchTask() {
        //TODO implement folder watch task here
        return null;
    }

    private Task initSteamWatchTask() {
        Task<ArrayList<SteamPreEntry>> steamTask = new Task() {
            @Override
            protected Object call() throws Exception {
                ArrayList<SteamPreEntry> steamEntriesToAdd = new ArrayList<SteamPreEntry>();
                ownedSteamApps.clear();
                ownedSteamApps.addAll(SteamOnlineScrapper.getOwnedSteamGamesPreEntry());
                installedSteamApps.clear();
                installedSteamApps.addAll(SteamLocalScrapper.getSteamAppsInstalledPreEntries());

                for (SteamPreEntry steamEntry : ownedSteamApps) {
                    boolean alreadyAddedToLibrary = false;
                    for (GameEntry entry : AllGameEntries.ENTRIES_LIST) {
                        alreadyAddedToLibrary = steamEntry.getId() == entry.getSteam_id();
                        if (alreadyAddedToLibrary) {
                            for (SteamPreEntry installedEntry : installedSteamApps) {
                                if (installedEntry.getId() == entry.getSteam_id()) {
                                    //games is installed!
                                    if (entry.isNotInstalled()) {
                                        entry.setNotInstalled(false);
                                        Platform.runLater(() -> {
                                            MAIN_SCENE.updateGame(entry);
                                        });
                                        break;
                                    }
                                }
                            }
                            break;
                        }
                    }
                    if (!alreadyAddedToLibrary) {
                        SteamPreEntry[] ignoredSteamApps = GENERAL_SETTINGS.getSteamAppsIgnored();
                        for (SteamPreEntry ignoredEntry : ignoredSteamApps) {
                            alreadyAddedToLibrary = steamEntry.getId() == ignoredEntry.getId();
                            if (alreadyAddedToLibrary) {
                                break;
                            }
                        }
                    }
                    if (!alreadyAddedToLibrary && !alreadyWaitingToBeAdded(steamEntry)) {
                        Main.LOGGER.debug("To add : " + steamEntry.getName());
                        steamEntriesToAdd.add(steamEntry);
                    }
                }
                foundGames.addAndGet(steamEntriesToAdd.size());

                return steamEntriesToAdd;
            }
        };
        steamTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                ArrayList<SteamPreEntry> steamEntriesToAdd = steamTask.getValue();

                Main.LOGGER.info(steamEntriesToAdd.size() + " steam games to add");
                if (steamEntriesToAdd.size() != 0) {
                    for (SteamPreEntry preEntryToAdd : steamEntriesToAdd) {
                        Task addGameEntryTask = new Task<GameEntry>() {
                            @Override
                            protected GameEntry call() throws Exception {
                                GameEntry convertedEntry = new GameEntry(preEntryToAdd.getName());
                                convertedEntry.setSteam_id(preEntryToAdd.getId());

                                GameEntry fetchedEntry = null;
                                try {
                                    fetchedEntry = SteamOnlineScrapper.getEntryForSteamId(preEntryToAdd.getId(), installedSteamApps);
                                } catch (Exception e) {
                                    if(e.toString().contains("UnknownHostException")){
                                        Main.LOGGER.error("Could not connect to steam, toAdd entry will not be scrapped");
                                    }
                                }
                                GameEntry entryToAdd = fetchedEntry != null ? fetchedEntry : convertedEntry;
                                if (!alreadyWaitingToBeAdded(entryToAdd)) {
                                    GameEntry guessedEntry = tryGetFirstIGDBResult(entryToAdd.getName());
                                    if (guessedEntry != null) {
                                        guessedEntry.setName(entryToAdd.getName());
                                        guessedEntry.setSteam_id(entryToAdd.getSteam_id());
                                        guessedEntry.setPlayTimeSeconds(entryToAdd.getPlayTimeSeconds());
                                        guessedEntry.setNotInstalled(entryToAdd.isNotInstalled());
                                        guessedEntry.setDescription(entryToAdd.getDescription());
                                        guessedEntry.setIgdb_imageHash(1,guessedEntry.getIgdb_imageHash(0));
                                        if (guessedEntry.getReleaseDate() == null) {
                                            guessedEntry.setReleaseDate(entryToAdd.getReleaseDate());
                                        }
                                        ImageUtils.downloadIGDBImageToCache(guessedEntry.getIgdb_id()
                                                , guessedEntry.getIgdb_imageHash(0)
                                                , ImageUtils.IGDB_TYPE_COVER
                                                , ImageUtils.IGDB_SIZE_MED
                                                , new OnDLDoneHandler() {
                                                    @Override
                                                    public void run(File outputfile) {
                                                       // guessedEntry.setSavedLocaly(true);
                                                        //File localCoverFile = new File(GameEntry.ENTRIES_FOLDER + File.separator + guessedEntry.getUuid().toString() + File.separator + ImageUtils.IGDB_TYPE_COVER + "." + GameEditScene.getExtension(outputfile));
                                                        //try {
                                                        //    Files.copy(outputfile.toPath().toAbsolutePath(),localCoverFile.toPath().toAbsolutePath(), StandardCopyOption.REPLACE_EXISTING);
                                                        //    guessedEntry.setImagePath(0, localCoverFile);

                                                      //  } catch (IOException e) {
                                                        //    e.printStackTrace();
                                                            guessedEntry.setImagePath(0,outputfile);
                                                        //}
                                                        ImageUtils.downloadIGDBImageToCache(guessedEntry.getIgdb_id()
                                                                , guessedEntry.getIgdb_imageHash(1)
                                                                , ImageUtils.IGDB_TYPE_SCREENSHOT
                                                                , ImageUtils.IGDB_SIZE_MED
                                                                , new OnDLDoneHandler() {
                                                                    @Override
                                                                    public void run(File outputfile) {
                                                                       // File localScreenshotFile = new File(GameEntry.ENTRIES_FOLDER + File.separator + guessedEntry.getUuid().toString() + File.separator + ImageUtils.IGDB_TYPE_SCREENSHOT + "." + GameEditScene.getExtension(outputfile));
                                                                       // try {
                                                                         //   Files.copy(outputfile.toPath().toAbsolutePath(),localScreenshotFile.toPath().toAbsolutePath(), StandardCopyOption.REPLACE_EXISTING);
                                                                           // guessedEntry.setImagePath(1, localScreenshotFile);

                                                                      //  } catch (IOException e) {
                                                                        //    e.printStackTrace();
                                                                            guessedEntry.setImagePath(1,outputfile);
                                                                       // }
                                                                        entriesToAdd.add(guessedEntry);

                                                                        Platform.runLater(() -> {
                                                                            onGameFoundHandler.gameToAddFound(guessedEntry);
                                                                        });
                                                                    }
                                                                });

                                                    }
                                                });
                                    } else {
                                        entriesToAdd.add(entryToAdd);

                                        Platform.runLater(() -> {
                                            onGameFoundHandler.gameToAddFound(entryToAdd);
                                        });
                                    }
                                }
                                return null;
                            }
                        };
                        Thread th = new Thread(addGameEntryTask);
                        th.setDaemon(true);
                        th.start();
                    }
                }
            }
        });
        return steamTask;
    }

    private boolean alreadyWaitingToBeAdded(GameEntry entry) {
        return entriesToAdd.contains(entry);
    }

    private boolean alreadyWaitingToBeAdded(SteamPreEntry entry) {
        boolean already = false;
        for (GameEntry gameEntry : entriesToAdd) {
            already = gameEntry.getSteam_id() == entry.getId();
            if (already) {
                break;
            }
        }
        return already;
    }

    private GameEntry tryGetFirstIGDBResult(String name) {
        try {
            JSONArray bf4_results = null;
            bf4_results = IGDBScrapper.searchGame(name);
            ArrayList list = new ArrayList();
            list.add(bf4_results.getJSONObject(0).getInt("id"));
            JSONArray bf4_data = IGDBScrapper.getGamesData(list);
            return IGDBScrapper.getEntry(bf4_data.getJSONObject(0));

        } catch (Exception e) {
            Main.LOGGER.error(name + " not found on igdb first guess");
            return null;
        }

    }
}

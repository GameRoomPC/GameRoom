package data.game;

import com.mashape.unirest.http.exceptions.UnirestException;
import data.ImageUtils;
import data.game.entry.AllGameEntries;
import data.game.entry.GameEntry;
import data.game.scanner.*;
import data.game.scrapper.*;
import data.http.key.KeyChecker;
import javafx.application.Platform;
import javafx.scene.Node;
import org.json.JSONArray;
import ui.Main;
import ui.control.button.gamebutton.GameButton;
import ui.scene.GameEditScene;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.UUID;

import static system.application.settings.PredefinedSetting.SUPPORTER_KEY;

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
        gameScanners.add(new FolderGameScanner(this));
        gameScanners.add(new OtherLaunchersScanner(this) {
            @Override
            public ArrayList<GameEntry> getEntriesInstalled() {
                return InstalledGameScrapper.getGOGGames();
            }
        });
        gameScanners.add(new OtherLaunchersScanner(this) {
            @Override
            public ArrayList<GameEntry> getEntriesInstalled() {
                return InstalledGameScrapper.getOriginGames();
            }
        });
        gameScanners.add(new OtherLaunchersScanner(this) {
            @Override
            public ArrayList<GameEntry> getEntriesInstalled() {
                return InstalledGameScrapper.getUplayGames();
            }
        });
        gameScanners.add(new OtherLaunchersScanner(this) {
            @Override
            public ArrayList<GameEntry> getEntriesInstalled() {
                return InstalledGameScrapper.getBattleNetGames();
            }
        });
        gameScanners.add(new SteamGameScanner(this));

    }

    public void startService() {
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<UUID> uuids = AllGameEntries.readUUIDS(GameEntry.TOADD_FOLDER);
                for (UUID uuid : uuids) {
                    GameEntry entry = new GameEntry(uuid, true);
                    entry.setSavedLocaly(true);
                    Main.runAndWait(() -> {
                        onGameFound(entry);
                    });
                }
                while (Main.KEEP_THREADS_RUNNING) {
                    validateKey();
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

    private void validateKey() {
        if (!Main.SUPPORTER_MODE) {
            Main.SUPPORTER_MODE = !Main.GENERAL_SETTINGS.getString(SUPPORTER_KEY).equals("") && KeyChecker.isKeyValid(Main.GENERAL_SETTINGS.getString(SUPPORTER_KEY));
        }
    }

    private void tryScrapToAddEntries() {
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
                        entry.setWaitingToBeScrapped(false);
                    }
                }
            }
        }
        if (searchIGDBIDs.size() > 0) {
            try {
                ArrayList<GameEntry> scrappedEntries = IGDBScrapper.getEntries(IGDBScrapper.getGamesData(searchIGDBIDs));

                int i = 0;
                for (GameEntry scrappedEntry : scrappedEntries) {
                    scrappedEntry.setUuid(toScrapEntries.get(i).getUuid());
                    scrappedEntry.setName(toScrapEntries.get(i).getName());
                    scrappedEntry.setPath(toScrapEntries.get(i).getPath());
                    if (toScrapEntries.get(i).getPlayTimeSeconds() != 0) {
                        scrappedEntry.setPlayTimeSeconds(toScrapEntries.get(i).getPlayTimeSeconds());
                    }
                    if (toScrapEntries.get(i).getSteam_id() != -1) {
                        scrappedEntry.setSteam_id(toScrapEntries.get(i).getSteam_id());
                    }
                    scrappedEntry.setNotInstalled(toScrapEntries.get(i).isNotInstalled());
                    if (toScrapEntries.get(i).getDescription() != null && !toScrapEntries.get(i).getDescription().equals("")) {
                        scrappedEntry.setDescription(toScrapEntries.get(i).getDescription());
                    }
                    if (scrappedEntry.getReleaseDate() == null) {
                        scrappedEntry.setReleaseDate(toScrapEntries.get(i).getReleaseDate());
                    }
                    int finalI = i;
                    ImageUtils.downloadIGDBImageToCache(scrappedEntry.getIgdb_id()
                            , scrappedEntry.getIgdb_imageHash(0)
                            , ImageUtils.IGDB_TYPE_COVER
                            , ImageUtils.IGDB_SIZE_BIG_2X
                            , new OnDLDoneHandler() {
                                @Override
                                public void run(File outputfile) {
                                    try {
                                        File localCoverFile = new File(GameEntry.TOADD_FOLDER + File.separator + toScrapEntries.get(finalI).getUuid().toString() + File.separator + ImageUtils.IGDB_TYPE_COVER + "." + GameEditScene.getExtension(outputfile));
                                        Files.copy(outputfile.getAbsoluteFile().toPath()
                                                , localCoverFile.getAbsoluteFile().toPath()
                                                , StandardCopyOption.REPLACE_EXISTING);
                                        toScrapEntries.get(finalI).setImagePath(0, localCoverFile);
                                    } catch (Exception e) {
                                        toScrapEntries.get(finalI).setImagePath(0, outputfile);
                                    }


                                    Main.runAndWait(() -> {
                                        Main.MAIN_SCENE.updateGame(scrappedEntry);
                                    });
                                    ImageUtils.downloadIGDBImageToCache(scrappedEntry.getIgdb_id()
                                            , scrappedEntry.getIgdb_imageHash(1)
                                            , ImageUtils.IGDB_TYPE_SCREENSHOT
                                            , ImageUtils.IGDB_SIZE_BIG_2X
                                            , new OnDLDoneHandler() {
                                                @Override
                                                public void run(File outputfile) {
                                                    try {
                                                        File localCoverFile = new File(GameEntry.TOADD_FOLDER + File.separator + toScrapEntries.get(finalI).getUuid().toString() + File.separator + ImageUtils.IGDB_TYPE_SCREENSHOT + "." + GameEditScene.getExtension(outputfile));
                                                        Files.copy(outputfile.getAbsoluteFile().toPath()
                                                                , localCoverFile.getAbsoluteFile().toPath()
                                                                , StandardCopyOption.REPLACE_EXISTING);
                                                        toScrapEntries.get(finalI).setImagePath(1, localCoverFile);
                                                    } catch (Exception e) {
                                                        toScrapEntries.get(finalI).setImagePath(1, outputfile);
                                                    }
                                                    toScrapEntries.get(finalI).setWaitingToBeScrapped(false);
                                                    Main.runAndWait(() -> {
                                                        Main.MAIN_SCENE.updateGame(toScrapEntries.get(finalI));
                                                    });
                                                }
                                            });

                                }
                            });
                    i++;
                }
            } catch (UnirestException e) {
                e.printStackTrace();
            }

        }
    }

    private void scanSteamGamesTime() {
        try {
            ArrayList<GameEntry> ownedSteamApps = SteamOnlineScrapper.getOwnedSteamGames();
            for (GameEntry ownedEntry : ownedSteamApps) {
                if (ownedEntry.getPlayTimeSeconds() != 0) {
                    for (GameEntry storedEntry : AllGameEntries.ENTRIES_LIST) {
                        if (ownedEntry.getSteam_id() == storedEntry.getSteam_id() && ownedEntry.getPlayTimeSeconds() != storedEntry.getPlayTimeSeconds()) {
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

    private void scanNewGamesRoutine() {
        final int originalGameFoundNumber = entriesToAdd.size();
        for (GameScanner scanner : gameScanners) {
            scanner.scanForGames();
        }
        //now we wait for the scanners to have all finished
        boolean allScannersDone = true;
        boolean allLocalScannersDone;
        while (!allScannersDone) {
            allLocalScannersDone = true;
            allScannersDone = true;

            for (GameScanner scanner : gameScanners) {
                allScannersDone = allScannersDone && scanner.isScanDone();
                if (scanner.isLocalScanner()) {
                    allLocalScannersDone = allLocalScannersDone && scanner.isScanDone();
                }
            }
            if (allLocalScannersDone) {
                tryScrapToAddEntries();
                Main.LOGGER.debug("All local scanners done, trying to scrap now");
            }
            try {
                Thread.sleep(2 * 100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (entriesToAdd.size() > originalGameFoundNumber) {
            Main.LOGGER.info("GameWatcher : found " + (entriesToAdd.size() - originalGameFoundNumber) + " new games!");
            onGameFoundHandler.onAllGamesFound();
        }
    }

    public boolean alreadyWaitingToBeAdded(GameEntry entry) {
        boolean already = false;
        for (GameEntry gameEntry : entriesToAdd) {
            already = entry.getPath().toLowerCase().trim().contains(gameEntry.getPath().trim().toLowerCase())
                    || gameEntry.getPath().trim().toLowerCase().contains(entry.getPath().trim().toLowerCase())
                    || cleanNameForCompareason(entry.getName()).equals(cleanNameForCompareason(gameEntry.getName())); //cannot use UUID as they are different at this pre-add-time
            if (already) {
                break;
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
        if (!alreadyWaitingToBeAdded(foundEntry)) {
            foundEntry.setToAdd(true);
            foundEntry.setSavedLocaly(true);

            Main.LOGGER.debug(GameWatcher.class.getName() + " : found new game, " + foundEntry.getName() + ", path:" + foundEntry.getPath());
            entriesToAdd.add(foundEntry);
            return onGameFoundHandler.gameToAddFound(foundEntry);
        }
        return null;
    }

    public static String cleanNameForCompareason(String name) {
        return name.toLowerCase().trim()
                .replace(":", "")
                .replace("-", "")
                .replace("_", "")
                .replace(".", "");
    }

    public void removeGame(GameEntry entry) {
        ArrayList<GameEntry> toRemoveEntries = new ArrayList<>();
        for (GameEntry n : entriesToAdd) {
            if (n.getUuid().equals(entry.getUuid())) {
                toRemoveEntries.add(n);
                if(n.isToAdd()) //check if not added to Games folder
                    n.deleteFiles();
            } else {
                if (n.getPath().trim().toLowerCase().equals(entry.getPath().trim().toLowerCase())) {
                    toRemoveEntries.add(n);
                    if(n.isToAdd()) //check if not added to Games folder
                        n.deleteFiles();
                }
            }
        }
        entriesToAdd.removeAll(toRemoveEntries);
    }
}

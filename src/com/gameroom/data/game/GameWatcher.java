package com.gameroom.data.game;

import com.mashape.unirest.http.exceptions.UnirestException;
import com.gameroom.data.LevenshteinDistance;
import com.gameroom.data.game.entry.GameEntry;
import com.gameroom.data.game.entry.GameEntryUtils;
import com.gameroom.data.game.scanner.*;
import com.gameroom.data.game.scraper.IGDBScraper;
import com.gameroom.data.http.images.ImageUtils;
import com.gameroom.data.io.FileUtils;
import javafx.application.Platform;
import org.json.JSONArray;
import com.gameroom.system.application.settings.PredefinedSetting;
import com.gameroom.ui.GeneralToast;
import com.gameroom.ui.Main;
import com.gameroom.ui.control.button.gamebutton.GameButton;
import com.gameroom.ui.dialog.GameRoomAlert;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.gameroom.system.application.settings.GeneralSettings.settings;
import static com.gameroom.ui.Main.LOGGER;
import static com.gameroom.ui.Main.MAIN_SCENE;

/**
 * Created by LM on 17/08/2016.
 */
public class GameWatcher {
    private ScanPeriod scanPeriod = ScanPeriod.HALF_HOUR;
    private static GameWatcher WATCHER;

    private OnScannerResultHandler onGameFoundHandler;

    private final CopyOnWriteArrayList<GameEntry> entriesToAdd = new CopyOnWriteArrayList<>();

    private ArrayList<GameScanner> localGameScanners = new ArrayList<>();
    private ArrayList<GameScanner> onlineGameScanners = new ArrayList<>();
    private int originalGameFoundNumber = entriesToAdd.size();

    private ArrayList<Runnable> onSearchStartedListeners = new ArrayList<>();
    private ArrayList<Runnable> onSearchDoneListeners = new ArrayList<>();

    private Runnable scanningTask;
    private Future scanningFuture;

    private volatile boolean alreadyDisplayedIGDBError = false;



    public static GameWatcher getInstance() {
        if (WATCHER == null) {
            WATCHER = new GameWatcher();
        }
        return WATCHER;
    }

    public void setOnGameFoundHandler(OnScannerResultHandler onGameFoundHandler) {
        this.onGameFoundHandler = onGameFoundHandler;
    }

    private GameWatcher() {
        initService();

        localGameScanners.add(new LauncherScanner(this, ScannerProfile.BATTLE_NET));
        localGameScanners.add(new LauncherScanner(this, ScannerProfile.GOG));
        localGameScanners.add(new LauncherScanner(this, ScannerProfile.ORIGIN));
        localGameScanners.add(new LauncherScanner(this, ScannerProfile.UPLAY));
        localGameScanners.add(new LauncherScanner(this, ScannerProfile.STEAM));
        localGameScanners.add(new FolderGameScanner(this));
        localGameScanners.add(new ROMScanner(this));
        onlineGameScanners.add(new LauncherScanner(this, ScannerProfile.STEAM_ONLINE));
        onlineGameScanners.add(new LauncherScanner(this, ScannerProfile.MICROSOFT_STORE));

        setScanPeriod(settings().getScanPeriod());
        if (scanPeriod == null) {
            setScanPeriod((ScanPeriod) PredefinedSetting.SCAN_PERIOD.getDefaultValue().getSettingValue());
        }
    }

    private void initService() {
        scanningTask = () -> {
            loadToAddEntries();
            long start = System.currentTimeMillis();
            routine();
            long elapsedTime = System.currentTimeMillis() - start;
        };


    }

    private void routine() {
        for (Runnable onSearchStarted : onSearchStartedListeners) {
            if (onSearchStarted != null) {
                onSearchStarted.run();
            }
        }

        LOGGER.info("GameWatcher started");
        if (MAIN_SCENE != null) {
            GeneralToast.displayToast(Main.getString("search_started"), MAIN_SCENE.getParentStage(), GeneralToast.DURATION_SHORT);
        }
        originalGameFoundNumber = entriesToAdd.size();

        scanNewGamesRoutine();
        scanNewOnlineGamesRoutine();

        if (MAIN_SCENE != null) {
            GeneralToast.displayToast(Main.getString("search_done"), MAIN_SCENE.getParentStage(), GeneralToast.DURATION_SHORT);
        }

        if (entriesToAdd.size() > originalGameFoundNumber) {
            int numberFound = entriesToAdd.size() - originalGameFoundNumber;
            Main.LOGGER.info("GameWatcher : found " + numberFound + " new games!");
            if (MAIN_SCENE != null) {
                String end = numberFound > 1 ? Main.getString("new_games") : Main.getString("new_game");
                GeneralToast.displayToast(Main.getString("gameroom_has_found") + " " + numberFound + " " + end, MAIN_SCENE.getParentStage(), GeneralToast.DURATION_LONG);
            }
            onGameFoundHandler.onAllGamesFound(numberFound);
        }


        tryScrapToAddEntries();

        LOGGER.info("GameWatcher ended");
        LOGGER.info("IGDB requests made : " + IGDBScraper.REQUEST_COUNTER);
        for (Runnable onSeachDone : onSearchDoneListeners) {
            if (onSeachDone != null) {
                onSeachDone.run();
            }
        }
    }

    public void start(boolean manualStart) {
        loadToAddEntries();
        if (manualStart) {
            scanningFuture = Main.getScheduledExecutor().submit(scanningTask);
        } else {
            if (scanPeriod != null && scanPeriod.toMillis() > 0) {
                if (scanningFuture != null) {
                    scanningFuture.cancel(false);
                }
                scanningFuture = Main.getScheduledExecutor().scheduleAtFixedRate(scanningTask, scanPeriod.toMillis(), scanPeriod.toMillis(), TimeUnit.MILLISECONDS);
            }
        }
    }

    public void setScanPeriod(ScanPeriod period) {
        if (period == null) {
            return;
        }
        scanPeriod = period;
    }

    public void loadToAddEntries() {

        ArrayList<GameEntry> savedEntries = new ArrayList<>();
        GameEntryUtils.loadIgnoredGames();
        GameEntryUtils.loadToAddGames().forEach(entry -> {
            if (!GameEntryUtils.isGameIgnored(entry)) {
                entry.setSavedLocally(true);
                savedEntries.add(entry);
            }
        });
        savedEntries.sort((o1, o2) -> {
            int result = 0;
            LocalDateTime date1 = o1.getAddedDate();
            LocalDateTime date2 = o2.getAddedDate();

            if (date1 == null && date2 != null) {
                return 1;
            } else if (date2 == null && date1 != null) {
                return -1;
            } else if (date1 == null && date2 == null) {
                result = 0;
            } else {
                result = date1.compareTo(date2);
            }
            if (result == 0) {
                String name1 = o1.getName();
                String name2 = o2.getName();
                result = name1.compareToIgnoreCase(name2);
            }

            return result;
        });
        for (GameEntry savedEntry : savedEntries) {
            Main.runAndWait(() -> {
                onGameFound(savedEntry);
            });
        }
    }


    private void tryScrapToAddEntries() {
        CopyOnWriteArrayList<Integer> searchIGDBIDs = new CopyOnWriteArrayList<>();
        CopyOnWriteArrayList<GameEntry> toScrapEntries = new CopyOnWriteArrayList<>();
        HashSet<Callable<Object>> tasks = new HashSet<>();

        if (MAIN_SCENE != null) {
            GeneralToast.displayToast(Main.getString("fetching_data_igdb"), MAIN_SCENE.getParentStage(), GeneralToast.DURATION_SHORT);
        }
        LOGGER.info("Now scraping found games");

        alreadyDisplayedIGDBError = false;
        for (GameEntry entry : entriesToAdd) {
            if (entry.isWaitingToBeScrapped() && !entry.isBeingScraped() && !GameEntryUtils.isGameIgnored(entry)) {
                Callable task = () -> {
                    try {
                        entry.setSavedLocally(true);
                        entry.setBeingScraped(true);
                        entry.setSavedLocally(false);
                        Platform.runLater(() -> MAIN_SCENE.updateGame(entry));

                        int platformId = entry.getPlatform().getIGDBId();
                        if(platformId == -1 && entry.getPlatform().isPCLauncher()){
                            platformId = com.gameroom.data.game.entry.Platform.PC.getIGDBId();
                        }
                        JSONArray search_results = IGDBScraper.searchGame(entry.getName(),
                                false,
                                platformId
                        );
                        GameEntry scrapedEntry = LevenshteinDistance.getClosestEntry(entry.getName(),search_results,10);
                        if(scrapedEntry != null){
                            scrapedEntry.setIgdb_id(scrapedEntry.getIgdb_id());
                            LOGGER.debug("Added scrapped info to game \"" + scrapedEntry.getName() + "\"");
                            entry.setSavedLocally(true);
                            if (entry.getDescription() == null || scrapedEntry.getDescription().equals("")) {
                                entry.setDescription(scrapedEntry.getDescription());
                            }
                            if (entry.getReleaseDate() == null) {
                                entry.setReleaseDate(scrapedEntry.getReleaseDate());
                            }
                            entry.setThemes(scrapedEntry.getThemes());
                            entry.setGenres(scrapedEntry.getGenres());
                            entry.setSerie(scrapedEntry.getSerie());
                            entry.setDevelopers(scrapedEntry.getDevelopers());
                            entry.setPublishers(scrapedEntry.getPublishers());
                            entry.setIgdb_id(scrapedEntry.getIgdb_id());
                            entry.setSavedLocally(false);

                            ImageUtils.downloadIGDBImageToCache(scrapedEntry.getIgdb_id()
                                    , scrapedEntry.getIgdb_imageHash(0)
                                    , ImageUtils.IGDB_TYPE_COVER
                                    , ImageUtils.IGDB_SIZE_BIG_2X
                                    , outputFile -> {
                                        try {
                                            entry.setSavedLocally(true);
                                            entry.updateImage(0, outputFile);
                                            entry.setSavedLocally(false);
                                        } catch (Exception e) {
                                            Main.LOGGER.error("GameWatcher : could not move image for game " + entry.getName());
                                            e.printStackTrace();
                                        }

                                        ImageUtils.downloadIGDBImageToCache(entry.getIgdb_id()
                                                , scrapedEntry.getIgdb_imageHash(1)
                                                , ImageUtils.IGDB_TYPE_SCREENSHOT
                                                , ImageUtils.IGDB_SIZE_BIG_2X
                                                , outputfile1 -> {
                                                    try {
                                                        entry.setSavedLocally(true);
                                                        entry.updateImage(1, outputfile1);
                                                        entry.setSavedLocally(false);
                                                    } catch (Exception e) {
                                                        Main.LOGGER.error("GameWatcher : could not move image for game " + entry.getName());
                                                        e.printStackTrace();
                                                    }
                                                    entry.setSavedLocally(true);
                                                    entry.setWaitingToBeScrapped(false);
                                                    entry.setBeingScraped(false);
                                                    entry.setSavedLocally(false);
                                                    Platform.runLater(() -> MAIN_SCENE.updateGame(entry));
                                                });
                                    });
                        } else {
                            entry.setSavedLocally(true);
                            entry.setBeingScraped(false);
                            entry.setSavedLocally(false);
                            Platform.runLater(() -> MAIN_SCENE.updateGame(entry));
                        }

                    } catch (Exception e) {
                        entry.setSavedLocally(true);
                        if (e instanceof IOException) {
                            Main.LOGGER.error(entry.getName() + " not found on igdb first guess");
                            entry.setWaitingToBeScrapped(false);
                        } else if (e instanceof UnirestException) {
                            entry.setWaitingToBeScrapped(true);
                            if (!alreadyDisplayedIGDBError) {
                                alreadyDisplayedIGDBError = true;
                                GameRoomAlert.errorGameRoomAPI();
                            }
                        } else{
                            LOGGER.error("Error for game \"" + entry.getName()+ "\": " + e.getMessage());
                            e.printStackTrace();
                        }
                        entry.setBeingScraped(false);
                        entry.setSavedLocally(false);
                        Platform.runLater(() -> MAIN_SCENE.updateGame(entry));
                    }
                    return null;
                };
                tasks.add(task);
            }
        }

        try {
            Main.getExecutorService().invokeAll(tasks);
        } catch (InterruptedException e) {
            LOGGER.error("GameWatcher : error starting tasks");
            e.printStackTrace();
        }


    }

    private void scanNewOnlineGamesRoutine() {
        for (GameScanner scanner : onlineGameScanners) {
            scanner.startScanning();
        }
    }

    private void scanNewGamesRoutine() {
        for (GameScanner scanner : localGameScanners) {
            scanner.startScanning();
        }
    }

    public void submitTask(Callable task) {
        Main.getExecutorService().submit(task);
    }

    public CopyOnWriteArrayList<GameEntry> getEntriesToAdd() {
        return entriesToAdd;
    }

    public GameButton onGameFound(GameEntry foundEntry) {
        if (!GameEntryUtils.gameAlreadyIn(foundEntry, entriesToAdd) && !GameEntryUtils.isGameIgnored(foundEntry)) {
            if (!foundEntry.isInDb()) {
                foundEntry.setAddedDate(LocalDateTime.now());
                foundEntry.setToAdd(true);
                foundEntry.setSavedLocally(true);
                foundEntry.saveEntry();
                foundEntry.setName(cleanNameForDisplay(
                        foundEntry.getName(),
                        com.gameroom.data.game.entry.Platform.PC.getSupportedExtensions())
                );

                Main.LOGGER.debug(GameWatcher.class.getName() + " : found new game, " + foundEntry.getName() + ", path:" + foundEntry.getPath());
            }
            entriesToAdd.add(foundEntry);
            return onGameFoundHandler.gameToAddFound(foundEntry);
        }
        return null;
    }

    public static String formatNameForComparison(String name) {
        return cleanName(name).toLowerCase()
                .replace(":", "")
                .replace("-", "")
                .replace("_", "")
                .replace(".", "")
                .replace("!", "")
                .replace("?", "")
                .replace(" ", "");//remove spaces for a cleaner comparison;
    }

    public static String cleanName(String name) {
        return name.replace(".", " ")
                .replace("\u00AE", "")//registered symbol
                .replace("\u00A9", "")//copyright symbol
                .replace("\u2122", "")//TM symbol
                .replace("32bit", "")
                .replace("32 bit", "")
                .replace("64bit", "")
                .replace("64 bit", "")
                .replace("x86", "")
                .replace("x64", "")
                .replace("()", "")
                .replaceAll("\\(.*\\)", "")
                .replaceAll("\\[.*\\]", "")
                .replaceAll("\\{.*\\}", "")
                .replaceAll("for Windows 10","")
                .replaceAll("for Windows","")
                .trim();
    }

    public static String cleanNameForDisplay(String name, String[] possibleExtensions) {
        return cleanName(FileUtils.getNameNoExtension(name, possibleExtensions));
    }

    public void removeGame(GameEntry entry) {
        ArrayList<GameEntry> toRemoveEntries = new ArrayList<>();
        for (GameEntry n : entriesToAdd) {
            boolean delete = n.getId() == entry.getId()
                    || GameEntryUtils.entriesPathsEqual(n, entry);
            if (delete) {
                toRemoveEntries.add(n);
                break;
            }
        }

        entriesToAdd.removeAll(toRemoveEntries);
    }

    public void addOnSearchStartedListener(Runnable onSearchStarted) {
        if (onSearchStarted != null) {
            onSearchStartedListeners.add(onSearchStarted);
        }
    }

    public void addOnSearchDoneListener(Runnable onSearchDone) {
        if (onSearchDone != null) {
            onSearchDoneListeners.add(onSearchDone);
        }
    }

    private static GameEntry getGameWithIGDBId(int id, Collection<GameEntry> collection) {
        if (collection == null || collection.isEmpty()) {
            return null;
        }
        for (GameEntry ge : collection) {
            if (ge.getIgdb_id() == id) {
                return ge;
            }
        }
        return null;
    }
}

package data.game;

import com.mashape.unirest.http.exceptions.UnirestException;
import data.LevenshteinDistance;
import data.game.entry.GameEntry;
import data.game.entry.GameEntryUtils;
import data.game.scanner.*;
import data.game.scraper.IGDBScraper;
import data.http.images.ImageUtils;
import data.io.FileUtils;
import javafx.application.Platform;
import org.json.JSONArray;
import system.application.settings.PredefinedSetting;
import ui.GeneralToast;
import ui.Main;
import ui.control.button.gamebutton.GameButton;
import ui.dialog.GameRoomAlert;

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

import static system.application.settings.GeneralSettings.settings;
import static ui.Main.LOGGER;
import static ui.Main.MAIN_SCENE;

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
        //validateKey();
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
        LOGGER.info("IGDB requests made : "+IGDBScraper.REQUEST_COUNTER);
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
        savedEntries.sort(new Comparator<GameEntry>() {
            @Override
            public int compare(GameEntry o1, GameEntry o2) {
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
            }
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
            GeneralToast.displayToast(Main.getString("fetching_data_igdb"), MAIN_SCENE.getParentStage(), GeneralToast.DURATION_SHORT, true);
        }
        LOGGER.info("Now scraping found games");

        final boolean[] alreadyDisplayedIGDBError = {false};
        for (GameEntry entry : entriesToAdd) {
            if (entry.isWaitingToBeScrapped() && !entry.isBeingScraped() && !GameEntryUtils.isGameIgnored(entry)) {
                Callable task = new Callable() {
                    @Override
                    public Object call() throws Exception {
                        try {
                            entry.setSavedLocally(true);
                            entry.setBeingScraped(true);
                            entry.setSavedLocally(false);
                            JSONArray search_results = IGDBScraper.searchGame(entry.getName());
                            if (search_results != null) {
                                int igdbId = LevenshteinDistance.closestName(entry.getName(), search_results);
                                searchIGDBIDs.add(igdbId);
                                entry.setIgdb_id(igdbId);
                                toScrapEntries.add(entry);
                                Platform.runLater(() -> MAIN_SCENE.updateGame(entry));
                            }

                        } catch (Exception e) {
                            if (e instanceof IOException) {
                                Main.LOGGER.error(entry.getName() + " not found on igdb first guess");
                            } else if (e instanceof UnirestException) {
                                if (!alreadyDisplayedIGDBError[0]) {
                                    GameRoomAlert.errorIGDB();
                                    alreadyDisplayedIGDBError[0] = true;
                                }
                            }
                            entry.setSavedLocally(true);
                            entry.setWaitingToBeScrapped(false);
                            entry.setBeingScraped(false);
                            entry.setSavedLocally(false);
                            Platform.runLater(() -> MAIN_SCENE.updateGame(entry));
                        }
                        return null;
                    }
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

        CopyOnWriteArrayList<CopyOnWriteArrayList<Integer>> searchIDsCollection = new CopyOnWriteArrayList<>();

        int listCounter = -1;
        for (int i = 0; i < searchIGDBIDs.size(); i++) {
            if (i % 200 == 0) {
                listCounter++;
                searchIDsCollection.add(new CopyOnWriteArrayList<>());
            }
            searchIDsCollection.get(listCounter).add(searchIGDBIDs.get(i));
        }

        boolean anErrorOccured = false;

        if (searchIDsCollection.size() > 0) {
            for (CopyOnWriteArrayList<Integer> ids : searchIDsCollection) {
                if (!ids.isEmpty()) {
                    try {
                        JSONArray gamesDataArray = IGDBScraper.getGamesData(ids);
                        if (gamesDataArray != null) {
                            ArrayList<GameEntry> scrappedEntries = IGDBScraper.getEntries(gamesDataArray);

                            int i = 0;

                            if (MAIN_SCENE != null) {
                                GeneralToast.displayToast(Main.getString("downloading_images"), MAIN_SCENE.getParentStage(), GeneralToast.DURATION_SHORT, true);
                            }
                            for (GameEntry scrappedEntry : scrappedEntries) {
                                GameEntry toScrapEntry = getGameWithIGDBId(scrappedEntry.getIgdb_id(), toScrapEntries);
                                if (toScrapEntry != null && !GameEntryUtils.isGameIgnored(toScrapEntry)) {
                                    toScrapEntry.setSavedLocally(true);
                                    if (toScrapEntry.getDescription() == null || toScrapEntry.getDescription().equals("")) {
                                        toScrapEntry.setDescription(scrappedEntry.getDescription());
                                    }
                                    if (toScrapEntry.getReleaseDate() == null) {
                                        toScrapEntry.setReleaseDate(scrappedEntry.getReleaseDate());
                                    }
                                    toScrapEntry.setThemes(scrappedEntry.getThemes());
                                    toScrapEntry.setGenres(scrappedEntry.getGenres());
                                    toScrapEntry.setSerie(scrappedEntry.getSerie());
                                    toScrapEntry.setDevelopers(scrappedEntry.getDevelopers());
                                    toScrapEntry.setPublishers(scrappedEntry.getPublishers());
                                    toScrapEntry.setIgdb_id(scrappedEntry.getIgdb_id());
                                    toScrapEntry.setSavedLocally(false);

                                    ImageUtils.downloadIGDBImageToCache(scrappedEntry.getIgdb_id()
                                            , scrappedEntry.getIgdb_imageHash(0)
                                            , ImageUtils.IGDB_TYPE_COVER
                                            , ImageUtils.IGDB_SIZE_BIG_2X
                                            , outputFile -> {
                                                try {
                                                    toScrapEntry.setSavedLocally(true);
                                                    toScrapEntry.updateImage(0, outputFile);
                                                    toScrapEntry.setSavedLocally(false);
                                                } catch (Exception e) {
                                                    Main.LOGGER.error("GameWatcher : could not move image for game " + toScrapEntry.getName());
                                                    e.printStackTrace();
                                                }

                                                Main.runAndWait(() -> {
                                                    Main.MAIN_SCENE.updateGame(scrappedEntry);
                                                });

                                                ImageUtils.downloadIGDBImageToCache(scrappedEntry.getIgdb_id()
                                                        , scrappedEntry.getIgdb_imageHash(1)
                                                        , ImageUtils.IGDB_TYPE_SCREENSHOT
                                                        , ImageUtils.IGDB_SIZE_BIG_2X
                                                        , outputfile1 -> {
                                                            try {
                                                                toScrapEntry.setSavedLocally(true);
                                                                toScrapEntry.updateImage(1, outputfile1);
                                                                toScrapEntry.setSavedLocally(false);
                                                            } catch (Exception e) {
                                                                Main.LOGGER.error("GameWatcher : could not move image for game " + toScrapEntry.getName());
                                                                e.printStackTrace();
                                                            }
                                                            toScrapEntry.setSavedLocally(true);
                                                            toScrapEntry.setWaitingToBeScrapped(false);
                                                            toScrapEntry.setBeingScraped(false);
                                                            toScrapEntry.setSavedLocally(false);
                                                            Main.runAndWait(() -> {
                                                                Main.MAIN_SCENE.updateGame(toScrapEntry);
                                                            });
                                                        });
                                            });
                                }

                                i++;

                                try {
                                    Thread.sleep(2 * 100);
                                } catch (InterruptedException ignored) {
                                }
                            }
                        }
                    } catch (UnirestException e) {
                        LOGGER.error(e.getMessage());
                        GameRoomAlert.errorIGDB();
                        anErrorOccured = true;
                    }
                }
            }
            for (GameEntry ge : toScrapEntries) {
                if (ge.isBeingScraped()) { //this means that there was no id on igdb for this game
                    //here we set it to false as IGDB was not able to find our game
                    ge.setSavedLocally(true);
                    ge.setBeingScraped(false);
                    ge.setWaitingToBeScrapped(ge.isWaitingToBeScrapped() && anErrorOccured);
                    ge.setSavedLocally(false);
                    Main.runAndWait(() -> {
                        Main.MAIN_SCENE.updateGame(ge);
                    });
                }
            }
        }
    }

    private void scanNewOnlineGamesRoutine() {
        for (GameScanner scanner : onlineGameScanners) {
            scanner.startScanning();
        }
        //now we wait for the scanners to have all finished
        boolean allScannersDone = true;
        while (!allScannersDone) {
            allScannersDone = true;

            for (GameScanner scanner : onlineGameScanners) {
                allScannersDone = allScannersDone && scanner.isScanDone();
            }
            try {
                Thread.sleep(2 * 100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void scanNewGamesRoutine() {
        originalGameFoundNumber = entriesToAdd.size();

        for (GameScanner scanner : localGameScanners) {
            scanner.startScanning();
        }
        //now we wait for the scanners to have all finished
        boolean allScannersDone = true;
        while (!allScannersDone) {
            allScannersDone = true;

            for (GameScanner scanner : localGameScanners) {
                allScannersDone = allScannersDone && scanner.isScanDone();
            }
            try {
                Thread.sleep(2 * 100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void submitTask(Callable task) {
        Main.getExecutorService().submit(task);
    }

    public CopyOnWriteArrayList<GameEntry> getEntriesToAdd() {
        return entriesToAdd;
    }

    public GameButton onGameFound(GameEntry foundEntry) {
        if (!FolderGameScanner.gameAlreadyIn(foundEntry, entriesToAdd) && !GameEntryUtils.isGameIgnored(foundEntry)) {
            if (!foundEntry.isInDb()) {
                foundEntry.setAddedDate(LocalDateTime.now());
                foundEntry.setToAdd(true);
                foundEntry.setSavedLocally(true);
                foundEntry.saveEntry();
                foundEntry.setName(cleanNameForDisplay(
                        foundEntry.getName(),
                        data.game.entry.Platform.NONE.getSupportedExtensions())
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
                .replaceAll("\\(.*\\)","")
                .replaceAll("\\[.*\\]","")
                .replaceAll("\\{.*\\}","");
    }

    public static String cleanNameForDisplay(String name, String[] possibleExtensions) {
        return cleanName(FileUtils.getNameNoExtension(name, possibleExtensions));
    }

    public void removeGame(GameEntry entry) {
        ArrayList<GameEntry> toRemoveEntries = new ArrayList<>();
        for (GameEntry n : entriesToAdd) {
            boolean delete = n.getId() == entry.getId()
                    || FolderGameScanner.entryNameOrPathEquals(n, entry);
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

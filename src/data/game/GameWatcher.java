package data.game;

import com.mashape.unirest.http.exceptions.UnirestException;
import data.game.entry.AllGameEntries;
import data.game.entry.GameEntry;
import data.game.scanner.*;
import data.game.scraper.IGDBScraper;
import data.game.scraper.OnDLDoneHandler;
import data.http.images.ImageUtils;
import data.http.key.KeyChecker;
import org.json.JSONArray;
import ui.GeneralToast;
import ui.Main;
import ui.control.button.gamebutton.GameButton;
import ui.dialog.GameRoomAlert;
import ui.scene.GameEditScene;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.*;

import static system.application.settings.PredefinedSetting.SUPPORTER_KEY;
import static ui.Main.*;

/**
 * Created by LM on 17/08/2016.
 */
public class GameWatcher {
    private static ScanPeriod SCAN_PERIOD = ScanPeriod.HALF_HOUR;
    private static GameWatcher WATCHER;

    private OnScannerResultHandler onGameFoundHandler;

    private final ArrayList<GameEntry> entriesToAdd = new ArrayList<>();

    private ArrayList<GameScanner> localGameScanners = new ArrayList<>();
    private ArrayList<GameScanner> onlineGameScanners = new ArrayList<>();
    private int originalGameFoundNumber = entriesToAdd.size();

    private ArrayList<Runnable> onSearchStartedListeners = new ArrayList<>();
    private ArrayList<Runnable> onSearchDoneListeners = new ArrayList<>();

    private Thread serviceThread;
    private volatile static boolean KEEP_LOOPING = true;
    private volatile static boolean WAIT_FULL_PERIOD = false;

    private static ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

    private volatile boolean awaitingStart = false;

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
        localGameScanners.add(new LauncherScanner(this, ScannerProfile.BATTLE_NET));
        localGameScanners.add(new LauncherScanner(this, ScannerProfile.GOG));
        localGameScanners.add(new LauncherScanner(this, ScannerProfile.ORIGIN));
        localGameScanners.add(new LauncherScanner(this, ScannerProfile.UPLAY));
        localGameScanners.add(new LauncherScanner(this, ScannerProfile.STEAM));
        localGameScanners.add(new FolderGameScanner(this));
        onlineGameScanners.add(new LauncherScanner(this, ScannerProfile.STEAM_ONLINE));

        SCAN_PERIOD = GENERAL_SETTINGS.getScanPeriod();
    }

    private void initService() {
        serviceThread = new Thread(new Runnable() {
            @Override
            public void run() {
                initToAddEntries();
                do {
                    long start = System.currentTimeMillis();

                    if (WAIT_FULL_PERIOD) {
                        WAIT_FULL_PERIOD = false;
                        try {
                            Thread.sleep(SCAN_PERIOD.toMillis());
                            awaitingStart = false;
                        } catch (InterruptedException e) {
                            awaitingStart = false;
                            LOGGER.info("Forced start of GameWatcher");
                        }
                    }

                    routine();

                    long elapsedTime = System.currentTimeMillis() - start;
                    if (!awaitingStart) {
                        if (elapsedTime < SCAN_PERIOD.toMillis()) {
                            try {
                                Thread.sleep(SCAN_PERIOD.toMillis() - elapsedTime);
                                awaitingStart = false;
                            } catch (InterruptedException e) {
                                awaitingStart = false;
                                LOGGER.info("Forced start of GameWatcher");
                            }
                        }
                    } else {
                        awaitingStart = false;
                    }

                } while (Main.KEEP_THREADS_RUNNING && KEEP_LOOPING);
            }
        });
        serviceThread.setPriority(Thread.MIN_PRIORITY);
        serviceThread.setDaemon(true);
    }

    private void routine() {
        for (Runnable onSearchStarted : onSearchStartedListeners) {
            if (onSearchStarted != null) {
                onSearchStarted.run();
            }
        }

        if (EXECUTOR_SERVICE.isShutdown()) {
            EXECUTOR_SERVICE = Executors.newCachedThreadPool();
        }

        LOGGER.info("GameWatcher started");
        if (MAIN_SCENE != null) {
            GeneralToast.displayToast(Main.getString("search_started"), MAIN_SCENE.getParentStage(), GeneralToast.DURATION_SHORT);
        }
        //validateKey();
        scanNewGamesRoutine();
        scanNewOnlineGamesRoutine();

        EXECUTOR_SERVICE.shutdown();
        try {
            EXECUTOR_SERVICE.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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
        for (Runnable onSeachDone : onSearchDoneListeners) {
            if (onSeachDone != null) {
                onSeachDone.run();
            }
        }
    }

    public void start() {
        if (serviceThread == null) {
            initService();
        }
        if(serviceThread.getState().equals(Thread.State.TIMED_WAITING)){
            serviceThread.interrupt();
        }
        if (serviceThread.getState().equals(Thread.State.NEW)) {
            serviceThread.start();
        }
    }

    private void initToAddEntries() {
        ArrayList<UUID> uuids = AllGameEntries.readUUIDS(FILES_MAP.get("to_add"));

        ArrayList<GameEntry> savedEntries = new ArrayList<>();
        for (UUID uuid : uuids) {
            GameEntry entry = new GameEntry(uuid, true);
            if (!FolderGameScanner.isGameIgnored(entry)) {
                entry.setSavedLocaly(true);
                savedEntries.add(entry);
            }
        }
        savedEntries.sort(new Comparator<GameEntry>() {
            @Override
            public int compare(GameEntry o1, GameEntry o2) {
                int result = 0;
                Date date1 = o1.getAddedDate();
                Date date2 = o2.getAddedDate();

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

    private void validateKey() {
        if (!Main.SUPPORTER_MODE) {
            Main.SUPPORTER_MODE = !Main.GENERAL_SETTINGS.getString(SUPPORTER_KEY).equals("") && KeyChecker.isKeyValid(Main.GENERAL_SETTINGS.getString(SUPPORTER_KEY));
            if (Main.SUPPORTER_MODE) {
                IGDBScraper.key = IGDBScraper.IGDB_PRO_KEY;
            }
        }
    }

    private void tryScrapToAddEntries() {
        ArrayList<Integer> searchIGDBIDs = new ArrayList<>();
        ArrayList<GameEntry> toScrapEntries = new ArrayList<>();
        synchronized (entriesToAdd) {
            if (MAIN_SCENE != null) {
                GeneralToast.displayToast(Main.getString("fetching_data_igdb"), MAIN_SCENE.getParentStage(), GeneralToast.DURATION_SHORT, true);
            }
            LOGGER.info("Now scraping found games");

            boolean alreadyDisplayedIGDBError = false;
            for (GameEntry entry : entriesToAdd) {
                if (entry.isWaitingToBeScrapped() && !entry.isBeingScrapped() && !FolderGameScanner.isGameIgnored(entry)) {
                    try {
                        entry.setSavedLocaly(true);
                        entry.setBeingScrapped(true);
                        entry.setSavedLocaly(false);
                        JSONArray search_results = IGDBScraper.searchGame(entry.getName());
                        searchIGDBIDs.add(search_results.getJSONObject(0).getInt("id"));
                        toScrapEntries.add(entry);
                        MAIN_SCENE.updateGame(entry);

                    } catch (Exception e) {
                        if (e instanceof IOException) {
                            Main.LOGGER.error(entry.getName() + " not found on igdb first guess");
                        } else if (e instanceof UnirestException) {
                            if (!alreadyDisplayedIGDBError) {
                                GameRoomAlert.errorIGDB();
                                alreadyDisplayedIGDBError = true;
                            }
                        }
                        entry.setSavedLocaly(true);
                        entry.setWaitingToBeScrapped(false);
                        entry.setBeingScrapped(false);
                        entry.setSavedLocaly(false);
                        MAIN_SCENE.updateGame(entry);
                    }
                    try {
                        Thread.sleep(2 * 100);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
        if (searchIGDBIDs.size() > 0) {
            try {
                JSONArray gamesDataArray = IGDBScraper.getGamesData(searchIGDBIDs);
                ArrayList<GameEntry> scrappedEntries = IGDBScraper.getEntries(gamesDataArray);

                int i = 0;

                if (MAIN_SCENE != null) {
                    GeneralToast.displayToast(Main.getString("downloading_images"), MAIN_SCENE.getParentStage(), GeneralToast.DURATION_SHORT, true);
                }
                for (GameEntry scrappedEntry : scrappedEntries) {
                    GameEntry toScrapEntry = toScrapEntries.get(i);
                    if (!FolderGameScanner.isGameIgnored(toScrapEntry)) {
                        toScrapEntry.setSavedLocaly(true);
                        if (toScrapEntry.getDescription() == null || toScrapEntry.getDescription().equals("")) {
                            toScrapEntry.setDescription(scrappedEntry.getDescription());
                        }
                        if (toScrapEntry.getReleaseDate() == null) {
                            toScrapEntry.setReleaseDate(scrappedEntry.getReleaseDate());
                        }
                        toScrapEntry.setThemes(scrappedEntry.getThemes());
                        toScrapEntry.setGenres(scrappedEntry.getGenres());
                        toScrapEntry.setSerie(scrappedEntry.getSerie());
                        toScrapEntry.setDeveloper(scrappedEntry.getDeveloper());
                        toScrapEntry.setPublisher(scrappedEntry.getPublisher());
                        toScrapEntry.setIgdb_id(scrappedEntry.getIgdb_id());
                        toScrapEntry.setSavedLocaly(false);

                        ImageUtils.downloadIGDBImageToCache(scrappedEntry.getIgdb_id()
                                , scrappedEntry.getIgdb_imageHash(0)
                                , ImageUtils.IGDB_TYPE_COVER
                                , ImageUtils.IGDB_SIZE_BIG_2X
                                , new OnDLDoneHandler() {
                                    @Override
                                    public void run(File outputfile) {
                                        try {
                                            File localCoverFile = new File(FILES_MAP.get("to_add") + File.separator + toScrapEntry.getUuid().toString() + File.separator + ImageUtils.IGDB_TYPE_COVER + "." + GameEditScene.getExtension(outputfile));
                                            Files.copy(outputfile.getAbsoluteFile().toPath()
                                                    , localCoverFile.getAbsoluteFile().toPath()
                                                    , StandardCopyOption.REPLACE_EXISTING);
                                            toScrapEntry.setSavedLocaly(true);
                                            toScrapEntry.setImagePath(0, localCoverFile);
                                            toScrapEntry.setSavedLocaly(false);
                                        } catch (Exception e) {
                                            toScrapEntry.setSavedLocaly(true);
                                            toScrapEntry.setImagePath(0, outputfile);
                                            toScrapEntry.setSavedLocaly(false);
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
                                                            File localCoverFile = new File(FILES_MAP.get("to_add") + File.separator + toScrapEntry.getUuid().toString() + File.separator + ImageUtils.IGDB_TYPE_SCREENSHOT + "." + GameEditScene.getExtension(outputfile));
                                                            Files.copy(outputfile.getAbsoluteFile().toPath()
                                                                    , localCoverFile.getAbsoluteFile().toPath()
                                                                    , StandardCopyOption.REPLACE_EXISTING);
                                                            toScrapEntry.setSavedLocaly(true);
                                                            toScrapEntry.setImagePath(1, localCoverFile);
                                                            toScrapEntry.setSavedLocaly(false);
                                                        } catch (Exception e) {
                                                            toScrapEntry.setSavedLocaly(true);
                                                            toScrapEntry.setImagePath(1, outputfile);
                                                            toScrapEntry.setSavedLocaly(false);
                                                        }
                                                        toScrapEntry.setSavedLocaly(true);
                                                        toScrapEntry.setWaitingToBeScrapped(false);
                                                        toScrapEntry.setBeingScrapped(false);
                                                        toScrapEntry.setSavedLocaly(false);
                                                        Main.runAndWait(() -> {
                                                            Main.MAIN_SCENE.updateGame(toScrapEntry);
                                                        });
                                                    }
                                                });
                                    }
                                });

                    }

                    i++;

                    try {
                        Thread.sleep(2 * 100);
                    } catch (InterruptedException ignored) {
                    }
                }
            } catch (UnirestException | IOException e) {
                LOGGER.error(e.getMessage());
                GameRoomAlert.errorIGDB();
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
        EXECUTOR_SERVICE.submit(task);
    }

    public ArrayList<GameEntry> getEntriesToAdd() {
        return entriesToAdd;
    }

    public GameButton onGameFound(GameEntry foundEntry) {
        synchronized (entriesToAdd) {
            if (!FolderGameScanner.gameAlreadyIn(foundEntry, entriesToAdd)) {
                foundEntry.setAddedDate(new Date());
                foundEntry.setToAdd(true);
                foundEntry.setSavedLocaly(true);

                Main.LOGGER.debug(GameWatcher.class.getName() + " : found new game, " + foundEntry.getName() + ", path:" + foundEntry.getPath());
                entriesToAdd.add(foundEntry);
                return onGameFoundHandler.gameToAddFound(foundEntry);
            }
        }
        return null;
    }

    public static String formatNameForCompareason(String name) {
        return name.toLowerCase().trim()
                .replace(":", "")
                .replace("-", "")
                .replace("_", "")
                .replace(".", "")
                .replace(" ", "")
                .replace("\\u00AE", "")//registered symbol
                .replace("\\u00A9", "")//copyright symbol
                .replace("\\u2122", ""); //TM symbol
    }

    public void removeGame(GameEntry entry) {
        ArrayList<GameEntry> toRemoveEntries = new ArrayList<>();
        for (GameEntry n : entriesToAdd) {
            boolean delete = n.getUuid().equals(entry.getUuid())
                    || FolderGameScanner.entryNameOrPathEquals(n, entry);
            if (delete) {
                toRemoveEntries.add(n);
                if (n.isToAdd()) { //check if not added to Games folder
                    n.deleteFiles();
                }
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

    public static void setScanPeriod(ScanPeriod period, boolean waitFullPeriod) {
        boolean oldValue = KEEP_LOOPING;
        if (period.equals(ScanPeriod.START_ONLY)) {
            KEEP_LOOPING = false;
        } else {
            SCAN_PERIOD = period;
            KEEP_LOOPING = true;
            if (!oldValue) {
                WAIT_FULL_PERIOD = waitFullPeriod;
            }
        }
    }
}

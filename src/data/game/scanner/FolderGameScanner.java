package data.game.scanner;

import data.game.GameWatcher;
import data.game.entry.GameEntryUtils;
import data.game.entry.GameEntry;
import data.game.entry.Platform;
import data.game.scraper.SteamLocalScraper;
import system.application.settings.PredefinedSetting;
import ui.Main;
import ui.GeneralToast;

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;

import static data.game.GameWatcher.formatNameForComparison;
import static system.application.settings.GeneralSettings.settings;
import static ui.Main.MAIN_SCENE;

/**
 * Created by LM on 19/08/2016.
 */
public class FolderGameScanner extends GameScanner {
    public final static String[] EXCLUDED_FILE_NAMES = new String[]{"Steam Library", "SteamLibrary", "SteamVR"
            , "!Downloads", "VCRedist", "vcredist_x86.exe", "vcredist_x64.exe", "Redist", "__Installer", "Data"
            , "GameData", "data_win32", "DXSETUP.exe", "unins000.exe", "uninstall", "Uninstall.exe", "Updater.exe"
            , "Installers", "_CommonRedist", "directx", "DotNetFX", "DirectX8", "DirectX9", "DirectX10", "DirectX11"
            , "DirectX12", "UPlayBrowser.exe", "UbisoftGameLauncherInstaller.exe", "FirewallInstall.exe"
            , "GDFInstall.exe", "pbsvc.exe"};
    private final static String[] PREFERRED_FOLDER = new String[]{"Bin", "Binary", "Binaries", "win32", "win64", "x64"};
    private final static String[] VALID_EXECUTABLE_EXTENSION = new String[]{".exe", ".lnk", ".jar"};

    public final static Comparator<File> APP_FINDER_COMPARATOR = new Comparator<File>() {
        @Override
        public int compare(File o1, File o2) {
            if (o1.isDirectory() && !o2.isDirectory()) {
                return 1;
            } else if (!o1.isDirectory() && o2.isDirectory()) {
                return -1;
            } else if (o1.isDirectory() && o2.isDirectory()) {
                boolean o1p = false;
                boolean o2p = false;
                for (String pref : PREFERRED_FOLDER) {
                    o1p = o1p || o1.getName().toLowerCase().equals(pref.toLowerCase());
                    o2p = o2p || o2.getName().toLowerCase().equals(pref.toLowerCase());
                    if (o1p && o2p) {
                        break;
                    }
                }
                if (o1p && !o2p) {
                    return -1;
                } else if (!o1p && o2p) {
                    return 1;
                }

                return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
            } else {
                return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
            }
        }
    };

    public FolderGameScanner(GameWatcher parentLooker) {
        super(parentLooker);
    }

    public void scanAndAddGames() {
        settings().getGameFolders().forEach(gamesFolder -> {
            if (!gamesFolder.exists() || !gamesFolder.isDirectory()) {
                return;
            }
            File[] children = gamesFolder.listFiles();
            if (children == null) {
                return;
            }
            for (File file : children) {
                Callable<Object> task = new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        GameEntry potentialEntry = new GameEntry(file.getName());
                        potentialEntry.setPath(file.getAbsolutePath());
                        if (checkValidToAdd(potentialEntry)) {
                            if (isPotentiallyAGame(file)) {
                                potentialEntry.setInstalled(true);
                                addGameEntryFound(potentialEntry);
                            }
                        }
                        return null;
                    }
                };
                GameWatcher.getInstance().submitTask(task);
            }
        });
    }

    /**
     * Checks if it should add the entry (i.e. not already in the library, not ignored and not in toAdd list)
     * And then adds it
     *
     * @param potentialEntry the potential entry to check and add
     */
    public void checkAndAdd(GameEntry potentialEntry) {
        if (checkValidToAdd(potentialEntry)) {
            addGameEntryFound(potentialEntry);
        } else if (!GameEntryUtils.isGameIgnored(potentialEntry)) {
            compareAndSetLauncherId(potentialEntry);
        }
    }

    /**
     * Checks if it should add the entry (i.e. not already in the library, not ignored and not in toAdd list)
     *
     * @param potentialEntry the potential entry to check
     * @return true if must be added to the toAdd row, false otherwise
     */
    protected boolean checkValidToAdd(GameEntry potentialEntry) {
        boolean gameAlreadyInLibrary = gameAlreadyInLibrary(potentialEntry);
        boolean folderGameIgnored = GameEntryUtils.isGameIgnored(potentialEntry);
        boolean alreadyWaitingToBeAdded = gameAlreadyIn(potentialEntry, parentLooker.getEntriesToAdd());
        boolean pathExists = new File(potentialEntry.getPath()).exists() || potentialEntry.getPath().startsWith("steam");
        return !gameAlreadyInLibrary && !folderGameIgnored && !alreadyWaitingToBeAdded && pathExists;
    }

    /**
     * Checks if the given file is a valid application or if the folder contains a valid application
     *
     * @param file the file/folder to check
     * @return true if is/contains a valid application supported by GameRoom, false otherwise
     */
    public static boolean isPotentiallyAGame(File file) {
        try {
            Thread.sleep(50);
        } catch (InterruptedException ignored) {
        }
        if (!file.exists()) {
            return false;
        }
        for (String excludedName : EXCLUDED_FILE_NAMES) {
            if (file.getName().toLowerCase().equals(excludedName.toLowerCase())) {
                return false;
            }
        }
        if (file.isDirectory()) {
            File[] subfiles = file.listFiles();
            if (subfiles == null || subfiles.length == 0) {
                return false;
            }

            ArrayList<File> sortedFiles = new ArrayList<File>();
            Collections.addAll(sortedFiles, subfiles);

            sortedFiles.sort(APP_FINDER_COMPARATOR);

            boolean potentialGame = false;
            for (File subFile : sortedFiles) {
                potentialGame = potentialGame || isPotentiallyAGame(subFile);
                if (potentialGame) {
                    return potentialGame;
                }
            }
            return potentialGame;
        } else {
            return fileHasValidExtension(file);
        }
    }

    @Override
    protected void displayStartToast() {
        if (MAIN_SCENE != null) {
            if (profile != null) {
                GeneralToast.displayToast(Main.getString("scanning") + " " + profile.toString(), MAIN_SCENE.getParentStage(), GeneralToast.DURATION_SHORT, true);
            } else {
                GeneralToast.displayToast(Main.getString("scanning") + " " + Main.getString("games_folder"), MAIN_SCENE.getParentStage(), GeneralToast.DURATION_SHORT, true);
            }
        }
    }


    /**
     * Checks the entry against other entries in toAdd and library, and if they are the same but launcher is not set, sets it.
     *
     * @param foundEntry the entry to check against other existing entries
     */
    protected void compareAndSetLauncherId(GameEntry foundEntry) {
        List<GameEntry> toAddAndLibEntries = new ArrayList<>();
        toAddAndLibEntries.addAll(GameEntryUtils.ENTRIES_LIST);
        toAddAndLibEntries.addAll(parentLooker.getEntriesToAdd());

        if (GameEntryUtils.isGameIgnored(foundEntry)) {
            return;
        }
        for (GameEntry entry : toAddAndLibEntries) {
            if (entryNameOrPathEquals(entry, foundEntry)) {
                entry.setSavedLocally(true);
                boolean needRefresh = false;

                for(Platform platform : Platform.values()){
                    if(foundEntry.getPlatform().equals(platform) && !entry.getPlatform().equals(platform)){
                        entry.setPlatform(platform);
                        needRefresh = true;
                    }
                }
                if(entry.isInstalled() != foundEntry.isInstalled()){
                    entry.setInstalled(foundEntry.isInstalled());
                    needRefresh = true;
                }
                entry.setSavedLocally(false);
                if (needRefresh) {
                    if (MAIN_SCENE != null) {
                        MAIN_SCENE.updateGame(entry);
                    }
                }
                break;
            }
        }
    }

    /**
     * Compares entry with paths or name, and not with UUID.
     * This is helpful to compare entries in toAdd and !toAdd states, as they may point to the same game but not have the same UUID
     *
     * @param e1 the first entry to compare
     * @param e2 the other entry to compare
     * @return true if a path includes an other or if they have the same name, false otherwise
     */
    public static boolean entryNameOrPathEquals(GameEntry e1, GameEntry e2) {
        if (e1 == null && e2 == null) {
            return true;
        } else if (e1 == null || e2 == null) {
            return false;
        }
        boolean e1IncludesE2 = e1.getPath().trim().toLowerCase().contains(e2.getPath().trim().toLowerCase());
        boolean e2IncludesE1 = e2.getPath().trim().toLowerCase().contains(e1.getPath().trim().toLowerCase());

        if (e1IncludesE2) {
            if (e2.isToAdd()) {
                e2.setPath(e1.getPath());
            }
        }
        if (e2IncludesE1) {
            if (e1.isToAdd()) {
                e1.setPath(e2.getPath());
            }
        }
        //cannot use UUID as they are different at this pre-add-time
        return e1IncludesE2 || e2IncludesE1 || formatNameForComparison(e2.getName()).equals(formatNameForComparison(e1.getName()));
    }

    /**
     * Checks if Game is already in GameRoom's library
     * Compareason is done on the path or the name, as UUID may be different at this time
     *
     * @param foundEntry the entry to check
     * @return true if already in the library, false otherwise
     */
    public static boolean gameAlreadyInLibrary(GameEntry foundEntry) {
        return gameAlreadyIn(foundEntry, GameEntryUtils.ENTRIES_LIST);
    }

    /**
     * Checks if Game is already in GameRoom's library
     * Compareason is done on the path or the name, as UUID may be different at this time
     *
     * @param foundEntry the entry to check
     * @return true if already in the library, false otherwise
     */
    public static boolean gameAlreadyIn(GameEntry foundEntry, Collection<GameEntry> library) {
        boolean alreadyAddedToLibrary = false;
        for (GameEntry entry : library) {
            alreadyAddedToLibrary = entryNameOrPathEquals(entry, foundEntry);
            if (alreadyAddedToLibrary) {
                break;
            }
        }
        return alreadyAddedToLibrary;
    }

    /**
     * Checks if a file can be a supported application by GameRoom
     *
     * @param file the file to check
     * @return true if the file has a valid extension supported by GameRoom, false otherwise
     */
    public static boolean fileHasValidExtension(File file) {
        boolean hasAValidExtension = false;
        for (String validExtension : VALID_EXECUTABLE_EXTENSION) {
            hasAValidExtension = hasAValidExtension || file.getAbsolutePath().toLowerCase().endsWith(validExtension.toLowerCase());
            if (hasAValidExtension) {
                return hasAValidExtension;
            }
        }
        return hasAValidExtension;
    }
}

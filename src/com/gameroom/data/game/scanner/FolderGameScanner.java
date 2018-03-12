package com.gameroom.data.game.scanner;

import com.gameroom.data.game.GameFolderManager;
import com.gameroom.data.game.GameWatcher;
import com.gameroom.data.game.entry.GameEntry;
import com.gameroom.data.game.entry.GameEntryUtils;
import com.gameroom.data.game.entry.Platform;
import com.gameroom.data.io.FileUtils;
import com.gameroom.ui.GeneralToast;
import com.gameroom.ui.Main;

import java.io.File;
import java.util.*;

import static com.gameroom.data.game.GameWatcher.cleanNameForDisplay;
import static com.gameroom.ui.Main.LOGGER;
import static com.gameroom.ui.Main.MAIN_SCENE;

/**
 * Created by LM on 19/08/2016.
 */
public class FolderGameScanner extends GameScanner {
    public final static String[] EXCLUDED_FILE_NAMES = new String[]{"Steam Library", "SteamLibrary", "SteamVR"
            , "!Downloads", "VCRedist", "vcredist_x86.exe", "vcredist_x64.exe", "Redist", "__Installer", "Data"
            , "GameData", "data_win32", "DXSETUP.exe", "unins000.exe", "uninstall", "Uninstall.exe", "Updater.exe"
            , "Installers", "_CommonRedist", "directx", "DotNetFX", "DirectX8", "DirectX9", "DirectX10", "DirectX11"
            , "DirectX12", "UPlayBrowser.exe", "UbisoftGameLauncherInstaller.exe", "FirewallInstall.exe"
            , "GDFInstall.exe", "pbsvc.exe", "ActivationUI.exe", "vcredist_x64_2012.exe", "vcredist_x86_2012.exe"
            , "EasyAntiCheat_Setup.exe", "DirectX", "videos", "sounddata", "support", "_support", "__support"
            , "Resources", "maindata", "Licenses", "localisation", "music", "sound", "config", "cache", "video", "EngineData"
            , "Engine", "gamesave", "gamesaves", "Log", "Logs", "mods", "Profiles", "EULA", "Locale", "Resource", "Assets", ""
    };
    private final static String[] PREFERRED_FOLDER = new String[]{"Bin", "Binary", "Binaries", "win32", "win64", "x64"};

    public final static Comparator<File> APP_FINDER_COMPARATOR = (o1, o2) -> {
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
    };

    public FolderGameScanner(GameWatcher parentLooker) {
        super(parentLooker);
    }

    public void scanAndAddGames() {
        GameFolderManager.getPCFolders().forEach(gamesFolder -> {
            if (!gamesFolder.exists() || !gamesFolder.isDirectory()) {
                return;
            }
            File[] children = gamesFolder.listFiles();
            if (children == null) {
                return;
            }
            for (File f : children) {
                ScanTask task = new ScanTask(this, () -> {
                    File file = FileUtils.tryResolveLnk(f);
                    GameEntry potentialEntry = new GameEntry(cleanNameForDisplay(
                            f.getName(),
                            Platform.PC.getSupportedExtensions()
                    )); //f because we prefer to use the .lnk name if its the case !
                    potentialEntry.setPath(file.getAbsolutePath());
                    if (checkValidToAdd(potentialEntry, true)) {
                        if (isPotentiallyAGame(file)) {
                            potentialEntry.setInstalled(true);
                            addGameEntryFound(potentialEntry);
                        }
                    }
                    return null;
                });
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
        if (checkValidToAdd(potentialEntry, true)) {
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
    protected boolean checkValidToAdd(GameEntry potentialEntry, boolean checkParentFolder) {
        boolean parentFolderInLib = GameEntryUtils.parentFolderAlreadyInLibrary(potentialEntry);
        boolean parentFolderIsUserGameFolder = GameEntryUtils.parentFolderIsUserGameFolder(potentialEntry);
        boolean gameAlreadyInLibrary = GameEntryUtils.gameAlreadyInLibrary(potentialEntry);
        boolean folderGameIgnored = GameEntryUtils.isGameIgnored(potentialEntry);
        boolean alreadyWaitingToBeAdded = GameEntryUtils.gameAlreadyIn(potentialEntry, parentLooker.getEntriesToAdd());
        boolean pathExists = new File(potentialEntry.getPath()).exists()
                || potentialEntry.getPath().startsWith("steam")
                || potentialEntry.getPath().startsWith("shell:AppsFolder");
        /*LOGGER.debug(
                "FolderGameScanner: " + potentialEntry.getName()
                        + ": parentFolderInLib=" + parentFolderInLib
                        + ", checkParentFolder=" + checkParentFolder
                        + ", parentFolderIsUserGameFolder=" + parentFolderIsUserGameFolder
                        + ", gameAlreadyInLibrary=" + gameAlreadyInLibrary
                        + ", folderGameIgnored=" + folderGameIgnored
                        + ", alreadyWaitingToBeAdded=" + alreadyWaitingToBeAdded
                        + ", pathExists=" + pathExists
        );*/
        return !gameAlreadyInLibrary
                && (!checkParentFolder || !parentFolderInLib || parentFolderIsUserGameFolder)
                && !folderGameIgnored
                && !alreadyWaitingToBeAdded
                && pathExists;
    }

    /**
     * Checks if the given file is a valid application or if the folder contains a valid application
     *
     * @param file           the file/folder to check
     * @param fileExtensions extensions of files considered valid for this kind of games
     * @return true if is/contains a valid application supported by GameRoom, false otherwise
     */
    public static boolean isPotentiallyAGame(File file, String[] fileExtensions) {
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
                potentialGame = potentialGame || isPotentiallyAGame(subFile, fileExtensions);
                if (potentialGame) {
                    return potentialGame;
                }
            }
            return potentialGame;
        } else {
            return fileHasValidExtension(file, fileExtensions);
        }
    }

    public static boolean isPotentiallyAGame(File file) {
        return isPotentiallyAGame(file, Platform.PC.getSupportedExtensions());
    }

    @Override
    protected void displayStartToast() {
        if (MAIN_SCENE != null) {
            if (profile != null) {
                GeneralToast.displayToast(Main.getString("scanning") + " " + profile.toString(), MAIN_SCENE.getParentStage(), GeneralToast.DURATION_SHORT);
            } else {
                GeneralToast.displayToast(Main.getString("scanning") + " " + Main.getString("games_folders"), MAIN_SCENE.getParentStage(), GeneralToast.DURATION_SHORT);
            }
        }
    }

    @Override
    public String getScannerName() {
        return "GameFolders scanner";
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
            if (GameEntryUtils.entriesPathsEqual(entry, foundEntry)) {
                entry.setSavedLocally(true);
                boolean needRefresh = false;

                for (Platform platform : Platform.values()) {
                    if (foundEntry.getPlatform().equals(platform) && !entry.getPlatform().equals(platform)) {
                        entry.setPlatform(platform);
                        needRefresh = true;
                    }
                }
                if (entry.isInstalled() != foundEntry.isInstalled()) {
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
     * Checks if a file can be a supported application by GameRoom. Extensions can be like '.exa','*.exa' or 'exa'
     *
     * @param file       the file to check
     * @param extensions extensions of files that are considered valid
     * @return true if the file has a valid extension supported by GameRoom, false otherwise
     */
    public static boolean fileHasValidExtension(File file, String[] extensions) {
        boolean hasAValidExtension = false;
        if (extensions == null) {
            return false;
        }
        for (String validExtension : extensions) {
            String ext = validExtension.replace("*", "").trim().toLowerCase();
            hasAValidExtension = hasAValidExtension || file.getAbsolutePath().toLowerCase().endsWith(ext);
            if (hasAValidExtension) {
                return hasAValidExtension;
            }
        }
        return hasAValidExtension;
    }

    public static boolean fileHasValidExtension(File file) {
        return fileHasValidExtension(file, Platform.PC.getSupportedExtensions());
    }
}

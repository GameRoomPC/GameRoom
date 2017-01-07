package data.game.scraper;

import data.game.entry.GameEntry;
import data.game.scanner.GameScanner;
import system.os.Terminal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by LM on 29/08/2016.
 */
public class LauncherGameScraper {

    public static void scanInstalledGames(GameScanner scanner) {
        if (scanner.getScannerProfile() == null) {
            return;
        }
        switch (scanner.getScannerProfile()) {
            case GOG:
                scanGOGGames(scanner);
                break;
            case UPLAY:
                scanUplayGames(scanner);
                break;
            case ORIGIN:
                scanOriginGames(scanner);
                break;
            case BATTLE_NET:
                scanBattleNetGames(scanner);
                break;
            case STEAM:
                scanSteamGames(scanner);
                break;
            case STEAM_ONLINE:
                scanSteamOnlineGames(scanner);
                break;
            default:
                break;

        }
    }

    private static void scanSteamGames(GameScanner scanner) {
        SteamLocalScraper.scanSteamGames(scanner);
    }

    private static void scanSteamOnlineGames(GameScanner scanner) {
        SteamOnlineScraper.scanSteamOnlineGames(scanner);
    }

    private static void scanInstalledGames(String regFolder, String installDirPrefix, String namePrefix, GameScanner scanner) {
        Terminal terminal = new Terminal(false);
        String[] output = new String[0];
        try {
            output = terminal.execute("reg", "query", '"' + regFolder + '"');
            for (String s : output) {
                if (s.contains(regFolder)) {
                    int index = s.indexOf(regFolder) + regFolder.length() + 1;
                    String subFolder = s.substring(index);

                    String[] subOutPut = terminal.execute("reg", "query", '"' + regFolder + "\\" + subFolder + '"');
                    String installDir = null;
                    String name = null;

                    boolean notAGame = false; //this is to detect GOG non games
                    for (String s2 : subOutPut) {
                        if (s2.contains(installDirPrefix)) {
                            int index2 = s2.indexOf(installDirPrefix) + installDirPrefix.length();
                            installDir = s2.substring(index2).replace("/", "\\");
                        } else if (namePrefix != null && s2.contains(namePrefix)) {
                            int index2 = s2.indexOf(namePrefix) + namePrefix.length();
                            name = s2.substring(index2).replace("®", "").replace("™", "");
                        } else if (s2.contains("DEPENDSON    REG_SZ    ")) {
                            notAGame = !s2.equals("    DEPENDSON    REG_SZ    ");
                        }
                    }
                    if (installDir != null && !notAGame) {
                        File file = new File(installDir);
                        if (file.exists()) {
                            if (name == null) {
                                name = file.isDirectory() ? file.getName() : new File(file.getParent()).getName();
                            }
                            GameEntry potentialEntry = new GameEntry(name);
                            potentialEntry.setPath(file.getAbsolutePath());
                            potentialEntry.setNotInstalled(false);

                            int id = 0;
                            try {
                                id = Integer.parseInt(subFolder);
                                if (id == -1) {
                                    id = 0;
                                }
                            } catch (NumberFormatException nfe) {
                                //no id to scrap!
                            }
                            switch (scanner.getScannerProfile()) {
                                case GOG:
                                    potentialEntry.setGog_id(id);
                                    break;
                                case UPLAY:
                                    potentialEntry.setUplay_id(id);
                                    break;
                                case BATTLE_NET:
                                    potentialEntry.setBattlenet_id(id);
                                    break;
                                case ORIGIN:
                                    potentialEntry.setOrigin_id(id);
                                    break;
                                default:
                                    break;
                            }
                            scanner.checkAndAdd(potentialEntry);
                        }
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void scanUplayGames(GameScanner scanner) {
        scanInstalledGames("HKEY_LOCAL_MACHINE\\SOFTWARE\\WOW6432Node\\Ubisoft\\Launcher\\Installs", "InstallDir    REG_SZ    ", null, scanner);
    }

    public static void scanGOGGames(GameScanner scanner) {
        scanInstalledGames("HKEY_LOCAL_MACHINE\\SOFTWARE\\WOW6432Node\\GOG.com\\Games", "EXE    REG_SZ    ", "GAMENAME    REG_SZ    ", scanner);
    }

    public static void scanOriginGames(GameScanner scanner) {
        scanInstalledGames("HKEY_LOCAL_MACHINE\\SOFTWARE\\WOW6432Node\\EA Games", "Install Dir    REG_SZ    ", "DisplayName    REG_SZ    ", scanner);
        scanInstalledGames("HKEY_LOCAL_MACHINE\\SOFTWARE\\WOW6432Node\\Electronic Arts", "Install Dir    REG_SZ    ", "DisplayName    REG_SZ    ", scanner);
        scanUXGamesForOrigin(scanner);
    }

    public static void scanUXGamesForOrigin(GameScanner scanner) {
        String regFolder = "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\GameUX\\Games";
        String pathPrefix = "AppExePath    REG_SZ";
        String namePrefix = "Title    REG_SZ";
        String rootPrefix = "ConfigApplicationPath    REG_SZ";

        try {
            Terminal terminal = new Terminal(false);
            String[] output = terminal.execute("reg", "query", regFolder);
            for (String line : output) {
                if (line.startsWith(regFolder)) {
                    String[] gameRegOutput = terminal.execute("reg", "query", line);

                    String name = null;
                    String path = null;
                    String root = null;
                    for (String propLine : gameRegOutput) {
                        if (propLine.contains(pathPrefix)) {
                            path = propLine.substring(propLine.indexOf(pathPrefix) + pathPrefix.length()).trim();
                        }
                        if (propLine.contains(namePrefix)) {
                            name = propLine.substring(propLine.indexOf(namePrefix) + namePrefix.length()).trim();
                        }
                        if (propLine.contains(rootPrefix)) {
                            root = propLine.substring(propLine.indexOf(rootPrefix) + rootPrefix.length()).trim();
                        }
                    }
                    if (name != null && path != null) {
                        GameEntry entry = new GameEntry(name);
                        entry.setPath(path);
                        if(gameIsOrigin(root)){
                            entry.setOrigin_id(0);
                        }
                        scanner.checkAndAdd(entry);
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Checks the existence of the Origin's typical folder "Support\EA Help"
     *
     * @param gameRoot
     * @return
     */
    private static boolean gameIsOrigin(String gameRoot) {
        if (gameRoot == null) {
            return false;
        }

        File root = new File(gameRoot);
        if(!root.exists()){
            return false;
        }
        File eaHelp = new File(root.getAbsolutePath()+File.separator+"Support"+File.separator+"EA Help");
        return eaHelp.exists();
    }

    public static void scanBattleNetGames(GameScanner scanner) {
        String regFolder = "HKEY_LOCAL_MACHINE\\SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall";
        String keywordSeach = "Battle.net\\Agent\\Blizzard Uninstaller.exe";
        String[] excludedNames = new String[]{"Battle.net"};

        Terminal terminal = new Terminal(false);

        try {
            String[] output = terminal.execute("reg", "query", '"' + regFolder + '"', "/s", "/f", '"' + keywordSeach + '"');
            for (String line : output) {
                if (line.startsWith(regFolder)) {
                    String name = line.substring(regFolder.length() + 1); //+1 for the \

                    boolean excluded = false;
                    for (String excludedName : excludedNames) {
                        excluded = name.equals(excludedName);
                        if (excluded) {
                            break;
                        }
                    }
                    if (!excluded) {
                        String[] gameRegOutput = terminal.execute("reg", "query", '"' + regFolder + '\\' + name + '"', "/v", "DisplayIcon");
                        String pathPrefix = "DisplayIcon    REG_SZ";
                        String path = null;
                        for (String s : gameRegOutput) {
                            if (s.contains(pathPrefix)) {
                                path = s.substring(s.indexOf(pathPrefix) + pathPrefix.length()).trim();
                                break;
                            }
                        }
                        if (path != null) {
                            GameEntry entry = new GameEntry(name);
                            entry.setPath(path);
                            entry.setBattlenet_id(0);
                            entry.setNotInstalled(false);
                            scanner.checkAndAdd(entry);
                        }
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

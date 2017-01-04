package data.game.scrapper;

import data.game.entry.GameEntry;
import system.os.Terminal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by LM on 29/08/2016.
 */
public class InstalledGameScrapper {
    private final static int SCRAPPING_GOG = 0;
    private final static int SCRAPPING_UPLAY = 1;
    private final static int SCRAPPING_ORIGIN = 2;
    private final static int SCRAPPING_BATTLE_NET = 3;

    private static ArrayList<GameEntry> getInstalledGames(String regFolder, String installDirPrefix, String namePrefix, int scrappingCode) {
        ArrayList<GameEntry> entries = new ArrayList<>();
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
                    for (String s2 : subOutPut) {
                        if (s2.contains(installDirPrefix)) {
                            int index2 = s2.indexOf(installDirPrefix) + installDirPrefix.length();
                            installDir = s2.substring(index2).replace("/", "\\");
                        } else if (namePrefix != null && s2.contains(namePrefix)) {
                            int index2 = s2.indexOf(namePrefix) + namePrefix.length();
                            name = s2.substring(index2).replace("®", "").replace("™", "");
                        }
                    }
                    if (installDir != null) {
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
                            switch (scrappingCode) {
                                case SCRAPPING_GOG:
                                    potentialEntry.setGog_id(id);
                                    break;
                                case SCRAPPING_UPLAY:
                                    potentialEntry.setUplay_id(id);
                                    break;
                                case SCRAPPING_BATTLE_NET:
                                    potentialEntry.setBattlenet_id(id);
                                    break;
                                case SCRAPPING_ORIGIN:
                                    potentialEntry.setOrigin_id(id);
                                    break;
                                default:
                                    break;
                            }
                            entries.add(potentialEntry);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return entries;
    }

    public static ArrayList<GameEntry> getUplayGames() {
        return getInstalledGames("HKEY_LOCAL_MACHINE\\SOFTWARE\\WOW6432Node\\Ubisoft\\Launcher\\Installs", "InstallDir    REG_SZ    ", null, SCRAPPING_UPLAY);
    }

    public static ArrayList<GameEntry> getGOGGames() {
        return getInstalledGames("HKEY_LOCAL_MACHINE\\SOFTWARE\\WOW6432Node\\GOG.com\\Games", "EXE    REG_SZ    ", "GAMENAME    REG_SZ    ", SCRAPPING_GOG);
    }

    /**
     * @return Games that do have a shortcut in Windows Game folder (introduced in windows 7, not used so much)
     */
    public static ArrayList<GameEntry> getGameUXGames() {
        //TODO scan reg with HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\GameUX\Games
        return null;
    }

    public static ArrayList<GameEntry> getOriginGames() {
        ArrayList<GameEntry> entries = new ArrayList<>();
        entries.addAll(getInstalledGames("HKEY_LOCAL_MACHINE\\SOFTWARE\\WOW6432Node\\EA Games", "Install Dir    REG_SZ    ", "DisplayName    REG_SZ    ", SCRAPPING_ORIGIN));
        entries.addAll(getInstalledGames("HKEY_LOCAL_MACHINE\\SOFTWARE\\WOW6432Node\\Electronic Arts", "Install Dir    REG_SZ    ", "DisplayName    REG_SZ    ", SCRAPPING_ORIGIN));
        return entries;
    }

    public static ArrayList<GameEntry> getBattleNetGames() {
        String regFolder = "HKEY_LOCAL_MACHINE\\SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall";
        String keywordSeach = "Battle.net\\Agent\\Blizzard Uninstaller.exe";
        String[] excludedNames = new String[]{"Battle.net"};

        ArrayList<GameEntry> entries = new ArrayList<>();
        Terminal terminal = new Terminal(false);

        try {
            String[] output = terminal.execute("reg", "query", '"' + regFolder + '"',"/s","/f",'"'+keywordSeach+'"');
            for (String line : output) {
                if (line.startsWith(regFolder)) {
                    String name = line.substring(regFolder.length()+1); //+1 for the \

                    boolean excluded = false;
                    for (String excludedName : excludedNames) {
                        excluded = name.equals(excludedName);
                        if (excluded) {
                            break;
                        }
                    }
                    if(!excluded){
                        String[] gameRegOutput = terminal.execute("reg","query",'"' + regFolder + '\\' + name + '"',"/v","DisplayIcon");
                        String pathPrefix = "DisplayIcon    REG_SZ";
                        String path = null;
                        for(String s : gameRegOutput){
                            if(s.contains(pathPrefix)){
                                path = s.substring(s.indexOf(pathPrefix)+pathPrefix.length()).trim();
                                break;
                            }
                        }
                        if(path!=null){
                            GameEntry entry = new GameEntry(name);
                            entry.setPath(path);
                            entry.setBattlenet_id(0);
                            entry.setNotInstalled(false);
                            entries.add(entry);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return entries;

    }
}

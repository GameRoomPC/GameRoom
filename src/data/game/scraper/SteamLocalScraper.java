package data.game.scraper;

import com.mashape.unirest.http.exceptions.UnirestException;
import data.game.GameWatcher;
import data.game.entry.GameEntry;
import data.game.scanner.FolderGameScanner;
import data.game.scanner.GameScanner;
import org.apache.http.conn.ConnectTimeoutException;
import system.os.Terminal;
import ui.Main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ui.Main.LOGGER;

/**
 * Created by LM on 08/08/2016.
 */
public class SteamLocalScraper {
    private static boolean STEAM_ID_ALREADY_DISPLAYED = false;
    private static boolean STEAMAPPS_DEFAULTPATH1_ALREADY_DISPLAYED = false;
    private static boolean STEAMAPPS_DEFAULTPATH2_ALREADY_DISPLAYED = false;
    private static boolean STEAMAPPS_DEFAULTPATH3_ALREADY_DISPLAYED = false;
    private static boolean STEAM_PATH_ALREADY_DISPLAYED = false;
    private static boolean STEAM_DRIVE_LETTER_ALREADY_DISPLAYED = false;

    private final static Pattern STEAM_ACCOUNT_PATTERN = Pattern.compile("(?:\\s*\\\"(.*)\\\"\\s*\\{\\s*\\\"SteamID\\\"\\s*\\\"(\\d*)\\\"\\s*\\})");

    static List<SteamProfile> getSteamProfiles(){
        ArrayList<SteamProfile> profiles = new ArrayList<>();
        try {
            File vdfFile = new File(getSteamPath() + "\\config\\config.vdf");

            String fileString = new String(Files.readAllBytes(vdfFile.toPath()));
            String accountsLines = "";

            int bracketCount = 1;
            boolean firstBracketEncountered = false;
            boolean capturingAccounts = false;

            for (String line : fileString.split("\n")) {
                if (line.contains("\"Accounts\"")) {
                    capturingAccounts = true;
                }
                if(capturingAccounts){
                    if(line.contains("{")){
                        if(firstBracketEncountered) {
                            bracketCount++;
                        }else{
                            firstBracketEncountered = true; //we start bracketCount at 1, thus including the first bracket
                        }
                    }
                    if(line.contains("}")){
                        bracketCount--;
                    }
                    accountsLines += line +"\n";
                }

                if(bracketCount == 0 && capturingAccounts){
                    break;
                }
            }

            Matcher matcher = STEAM_ACCOUNT_PATTERN.matcher(accountsLines);
            while (matcher.find()){
                SteamProfile profile = new SteamProfile(matcher.group(1),matcher.group(2));
                profiles.add(profile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return profiles;
    }

    static void scanSteamGames(GameScanner scanner) {
        try {
            scanSteamApps(scanner);
        } catch (IOException e) {
            e.printStackTrace();
        }
        scanSteamAppsByReg(scanner);
    }

    private static void scanSteamApps(GameScanner scanner) throws IOException {
        String[] steamAppsPaths = getSteamAppsPath();

        boolean allNull = true;
        for (String s : steamAppsPaths) {
            allNull = allNull && (s == null);
        }
        if (allNull) {
            return;
        }

        for (String path : steamAppsPaths) {
            if (path != null) {
                Callable task = new Callable() {
                    @Override
                    public Object call() throws Exception {
                        File steamAppsFolder = new File(path);
                        String idPrefix = "appmanifest_";
                        String idSuffix = ".acf";
                        String namePrefix = "\"name\"";
                        for (File file : steamAppsFolder.listFiles()) {
                            String fileName = file.getName();
                            if (fileName.contains(idPrefix)) {
                                int id = Integer.parseInt(fileName.substring(idPrefix.length(), fileName.indexOf(idSuffix)));

                                String fileString = new String(Files.readAllBytes(file.toPath()));
                                String name = "";
                                for (String line : fileString.split("\n")) {
                                    if (line.contains(namePrefix)) {
                                        int indexNamePrefix = line.indexOf(namePrefix);
                                        name = line.substring(indexNamePrefix + namePrefix.length());
                                        name = name.replace("\"", "").trim();
                                    }
                                }
                                scanner.checkAndAdd(new SteamPreEntry(name, id).toGameEntry());
                            }
                        }
                        return null;
                    }
                };

                GameWatcher.getInstance().submitTask(task);
            }
        }
    }

    private static void scanSteamAppsByReg(GameScanner scanner) {
        String installedPrefix = "Installed    REG_DWORD";
        String regFolder = "HKEY_CURRENT_USER\\SOFTWARE\\Valve\\Steam\\Apps\\";
        ArrayList<String> steamIds = new ArrayList<>();
        Terminal terminal = new Terminal(false);
        String[] output = null;
        try {
            output = terminal.execute("reg", "query", regFolder, "/s");
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (output == null || output.length == 0) {
            return;
        }

        String id = null;
        for (String line : output) {
            if (line.contains(regFolder)) {
                id = LauncherGameScraper.getValue(regFolder, line);
            }
            if (id != null && line.contains(installedPrefix)) {
                String installed = LauncherGameScraper.getValue(installedPrefix, line);
                if (installed.equals("0x1")) {
                    steamIds.add(id);
                }
                id = null;
            }
        }
        for (String steamId : steamIds) {
            Callable task = new Callable() {
                @Override
                public Object call() throws Exception {
                    try {
                        GameEntry entry = SteamOnlineScraper.getEntryForSteamId(Integer.parseInt(steamId));
                        if (entry != null) {
                            entry.setInstalled(true);
                            scanner.checkAndAdd(entry);
                        }
                    } catch (ConnectTimeoutException | UnirestException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };
            GameWatcher.getInstance().submitTask(task);
        }
    }

    private static String[] getSteamAppsPath() throws IOException {
        int dirCounter = 0;
        String[] pathsToReturn = new String[5];

        String steamPath = getSteamPath();
        char driveLetter = 'C';
        if (steamPath != null && (new File(steamPath)).exists()) {
            if (Character.isLetter(steamPath.charAt(0))) {
                driveLetter = steamPath.charAt(0);
                if (!STEAM_DRIVE_LETTER_ALREADY_DISPLAYED) {
                    LOGGER.info("SteamLocalScraper : Steam path on drive '" + driveLetter + "'");
                    STEAM_DRIVE_LETTER_ALREADY_DISPLAYED = true;
                }
            }
        }
        String defaultSteamAppsPath = "C:\\Program Files (x86)\\Steam\\steamapps";
        String defaultCommonPath = "C:\\Program Files (x86)\\Steam\\steamapps\\common";
        if (new File(defaultCommonPath).exists()) {
            if (!STEAMAPPS_DEFAULTPATH1_ALREADY_DISPLAYED) {
                LOGGER.info("Using default Steam's common path at : " + defaultSteamAppsPath);
                STEAMAPPS_DEFAULTPATH1_ALREADY_DISPLAYED = true;
            }
            pathsToReturn[dirCounter++] = defaultSteamAppsPath;
        }

        defaultSteamAppsPath = defaultSteamAppsPath.replaceFirst("C", driveLetter + "");
        defaultCommonPath = defaultCommonPath.replaceFirst("C", driveLetter + "");
        if (new File(defaultCommonPath).exists()) {
            if (!STEAMAPPS_DEFAULTPATH2_ALREADY_DISPLAYED) {
                LOGGER.info("Using default Steam's common path at : " + defaultSteamAppsPath);
                STEAMAPPS_DEFAULTPATH2_ALREADY_DISPLAYED = true;
            }
            pathsToReturn[dirCounter++] = defaultSteamAppsPath;
        }
        defaultSteamAppsPath = steamPath + "\\steamapps";
        defaultCommonPath = steamPath + "\\steamapps\\common";
        if (new File(defaultCommonPath).exists()) {
            if (!STEAMAPPS_DEFAULTPATH3_ALREADY_DISPLAYED) {
                LOGGER.info("Using default Steam's common path at : " + defaultSteamAppsPath);
                STEAMAPPS_DEFAULTPATH3_ALREADY_DISPLAYED = true;
            }
            pathsToReturn[dirCounter++] = defaultSteamAppsPath;
        }

        File vdfFile = new File(getSteamPath() + "\\steamapps\\libraryfolders.vdf");
        String returnValue = null;
        String fileString = new String(Files.readAllBytes(vdfFile.toPath()));

        String prefix = "\"1\"";
        for (String line : fileString.split("\n")) {
            if (line.contains(prefix)) {
                int index = line.indexOf(prefix) + prefix.length();
                returnValue = line.substring(index)
                        .replace("\"", "")
                        .replace("\n", "")
                        .replace("}", "")
                        .replace("\\\\", "\\")
                        .trim()
                        + "\\steamapps";
            }
        }
        if (returnValue == null) {
            LOGGER.error("Steam's path is : " + getSteamPath());
            LOGGER.error("Could not retrieve user's steam apps path, here is libraryfolders.vdf : ");
            LOGGER.error(fileString);
        }
        pathsToReturn[dirCounter++] = returnValue;
        return pathsToReturn;
    }

    private static String getSteamPath() throws IOException {
        Terminal terminal = new Terminal();
        String[] output = terminal.execute("reg", "query", "\"HKEY_CURRENT_USER\\SOFTWARE\\Valve\\Steam\"", "/v", "SteamPath");
        for (String s : output) {
            if (s.contains("SteamPath")) {
                String prefix = "    SteamPath    REG_SZ    ";
                int index = s.indexOf(prefix) + prefix.length();
                return s.substring(index);
            }
        }
        LOGGER.error("Could not retrieve user's steam path, here is the reg query result : ");
        for (String s : output) {
            LOGGER.error("\t" + s);

        }
        return null;
    }

    public static boolean isSteamGameRunning(int steam_id) throws IOException {
        return getSteamGameStatus(steam_id, "Running");
    }

    public static boolean isSteamGameInstalled(int steam_id) {
        try {
            return getSteamGameStatus(steam_id, "Installed");
        } catch (IOException ignored) {

        }
        return false;
    }

    public static boolean isSteamGameLaunching(int steam_id) throws IOException {
        return getSteamGameStatus(steam_id, "Launching");
    }

    private static boolean getSteamGameStatus(int steam_id, String status) throws IOException {
        Terminal terminal = new Terminal(false);
        String[] output = terminal.execute("reg", "query", "\"HKEY_CURRENT_USER\\SOFTWARE\\Valve\\Steam\\Apps\\" + steam_id + "\"", "/v", status);
        String result = null;
        for (String s : output) {
            if (s.contains(status)) {
                String prefix = status + "    REG_DWORD    0x";
                int index = s.indexOf(prefix) + prefix.length();
                result = s.substring(index, index + 1);
                break;
            }
        }
        return (result != null) && (result.equals("1"));
    }

    public static boolean isSteamGameIgnored(GameEntry entry) {
        SteamPreEntry[] ignoredEntries = Main.GENERAL_SETTINGS.getSteamAppsIgnored();

        if (ignoredEntries == null || ignoredEntries.length == 0) {
            return false;
        }
        if (!entry.isSteamGame()) {
            return false;
        }
        boolean ignored = false;

        for (SteamPreEntry pre : ignoredEntries) {
            ignored = ignored || pre.getId() == entry.getSteam_id();
        }
        for (String name : FolderGameScanner.EXCLUDED_FILE_NAMES) {
            ignored = ignored || name.toLowerCase().equals(entry.getName().toLowerCase());
        }

        return ignored;
    }
}

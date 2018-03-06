package com.gameroom.data.game.scraper;

import com.gameroom.data.LevenshteinDistance;
import com.gameroom.data.game.entry.GameEntry;
import com.gameroom.system.os.Terminal;
import com.mashape.unirest.http.exceptions.UnirestException;
import edu.umd.cs.findbugs.annotations.NonNull;
import javafx.application.Platform;
import org.json.JSONArray;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.gameroom.ui.Main.LOGGER;
import static com.gameroom.ui.Main.MAIN_SCENE;

/**
 * @author LM. Garret (admin@gameroom.me)
 * @date 22/02/2018.
 */
public class MSStoreScraper {
    private final static String TAG = "MSStoreScraper";

    private final static Pattern PATH_PATTERN = Pattern.compile("(.*)([a-z|A-Z]\\:\\\\.*)");
    private final static Pattern DISPLAY_NAME_PATTERN = Pattern.compile("<DisplayName>(.*)<\\/DisplayName>");
    private final static Pattern LOGO_PATTERN = Pattern.compile("<Logo>(.*)<\\/Logo>");
    private final static Pattern APPLICATION_ID_PATTERN = Pattern.compile("<Application Id=\\\"([a-z|A-Z|0-9]*)\\\"");

    private final static int MAX_LEVENSHTEIN_DISTANCE = 2;

    private final static String[] EXCLUDED_PACKAGE_PREFIX = new String[]{
            "Microsoft.NET",
            "Microsoft.Windows",
            "Microsoft.VCLibs",
            "Microsoft.Services",
            "Microsoft.Media",
            "Microsoft.Office",
            "Microsoft.Advertising",
            "Microsoft.Xbox",
            "Microsoft.3DBuilder",
            "Microsoft.Microsoft",
            "Microsoft.HEVCVideoExtension",
            "Microsoft.Print3D",
            "Microsoft.Reader",
            "Microsoft.Wallet",
            "InputApp_",
            "AMZNMobileLLC",
            "Facebook",
    };

    private final static String[] EXCLUDED_DISPLAY_NAME_PREFIX = new String[]{
            "ms-resource"
    };


    /**
     * @return list of installed {@link MSStoreEntry} on the computer, excluding well known ones that are not games.
     */
    public static List<MSStoreEntry> getApps() {
        List<MSStoreEntry> entries = new ArrayList<>();
        Terminal terminal = new Terminal(false);
        try {
            String[] result = terminal.executePowerShell("Get-AppxPackage | Select PackageFamilyName, InstallLocation");
            for (String s : result) {
                Matcher matcher = PATH_PATTERN.matcher(s);
                if (matcher.find()) {
                    MSStoreEntry entry = new MSStoreEntry(matcher.group(1).trim(), matcher.group(2).trim());

                    try {
                        entry.readAppManifest();

                        if (!isPackageFamilyNameExcluded(entry.packageFamilyName)
                                && !isDisplayNameExcluded(entry.displayName)) {
                            entry.findRealIconPath();
                            entry.findExecutableFilePath();
                            entries.add(entry);
                        }
                    } catch (IOException e) {
                        LOGGER.error(e);
                    }
                }
            }

        } catch (IOException e) {
            LOGGER.error(e);
        }
        return entries;
    }

    /**
     * Contacts GameRoom's API to determine whether this {@link MSStoreEntry} should be considered as a game or not,
     * as it would be necessary for scanning for example.
     *
     * @param msStoreEntry the Microsoft Store application to check
     * @return a filled {@link GameEntry} if this should be considered as a game, or null if not
     */
    public static GameEntry shouldConsiderGame(MSStoreEntry msStoreEntry) {
        List<GameEntry> gameEntries = new ArrayList<>();

        try {
            JSONArray searchResults = IGDBScraper.searchGame(msStoreEntry.getName(),
                    false,
                    com.gameroom.data.game.entry.Platform.PC.getIGDBId()
            );
            if (searchResults != null) {
                int minDistance = -1;
                int jsonIndex = 0;
                for (int i = 0; i < searchResults.length(); i++) {
                    String name = searchResults.getJSONObject(i).getString("name");

                    int distance = LevenshteinDistance.distance(msStoreEntry.getName(), name);
                    if (minDistance == -1 || distance < minDistance) {
                        minDistance = distance;
                        jsonIndex = i;
                    }
                    if (minDistance == 0) {
                        break;
                    }
                }
                if (minDistance >= 0 && minDistance < MAX_LEVENSHTEIN_DISTANCE) {
                    return IGDBScraper.getGameEntries(searchResults).get(jsonIndex);
                }
            }
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Checks if the given PackageFamilyName is amongst the excluded ones
     *
     * @param packageFamilyName the PackageFamilyName of the app to test
     * @return true if it is excluded, false otherwise
     */
    private static boolean isPackageFamilyNameExcluded(String packageFamilyName) {
        if (packageFamilyName == null || packageFamilyName.isEmpty()) {
            return true;
        }
        boolean toFilter = false;
        for (int i = 0; i < EXCLUDED_PACKAGE_PREFIX.length && !toFilter; i++) {
            toFilter = packageFamilyName.startsWith(EXCLUDED_PACKAGE_PREFIX[i]);
        }
        return toFilter;
    }

    /**
     * Checks if the given DisplayName is amongst the excluded ones
     *
     * @param displayName the DisplayName of the app to test
     * @return true if it is excluded, false otherwise
     */
    private static boolean isDisplayNameExcluded(String displayName) {
        if (displayName == null || displayName.isEmpty()) {
            return true;
        }
        boolean toFilter = false;
        for (int i = 0; i < EXCLUDED_DISPLAY_NAME_PREFIX.length && !toFilter; i++) {
            toFilter = displayName.startsWith(EXCLUDED_DISPLAY_NAME_PREFIX[i]);
        }
        return toFilter;
    }


    public static void main(String[] args) {
        for (MSStoreEntry ws : getApps()) {
            System.out.println(ws);
        }
    }

    /**
     * Helper class representing a Microsoft Store application that is installed on the system. Offers convenient methods
     * to fill itself reading the app's manifest.
     */
    public static class MSStoreEntry {
        private final static String MANIFEST_FILE_NAME = "AppxManifest.xml";
        //path to the folder of the app
        private String path;
        //package family name, defining the application
        private String packageFamilyName;
        //the advertised app icon (often does not exist directly but under scaled variants)
        private String virtualIconPath;
        //true app icon, file exists
        private String realIconPath;
        //name displayed in the Microsoft Store
        private String displayName;

        //application id, used to start app
        private String applicationId = "App";

        //command to execute to start the app
        private String startCommand;

        //path to the executable in the file path
        private String executableFilePath = "";

        MSStoreEntry(String packageFamilyName, String path) {
            this.packageFamilyName = packageFamilyName != null ? packageFamilyName : "";
            this.path = path != null ? path : "";
        }


        /**
         * Attempts to find a matching icon in the folder indicated by {@link #virtualIconPath}, as it does not always point
         * to a true image file.
         */
        private void findRealIconPath() {

            String noExtName = virtualIconPath.substring(virtualIconPath.lastIndexOf(File.separator) + 1, virtualIconPath.length() - 4);

            String iconsPath = virtualIconPath.substring(0, virtualIconPath.lastIndexOf(File.separator));
            File[] iconFiles = new File(iconsPath).listFiles();
            if (iconFiles == null) {
                LOGGER.warn(TAG + ": Empty icons folder for " + displayName);
                return;
            }

            Pattern p = Pattern.compile(noExtName.toLowerCase() + "(?:.scale[0-9]*)?\\.[a-z|A-Z|0-9]{3}");

            for (File f : iconFiles) {
                Matcher m = p.matcher(f.getName().toLowerCase().trim());
                if (m.find()) {
                    realIconPath = iconsPath + File.separator + f.getName();
                }

            }
        }

        private void findExecutableFilePath() {
            File[] subFiles = new File(path).listFiles();
            if (subFiles == null) {
                LOGGER.warn(TAG + ": Empty folder for " + displayName);
                return;
            }

            for (File f : subFiles) {
                if (f.getName().endsWith(".exe")) {
                    executableFilePath = f.getAbsolutePath();
                    //LOGGER.debug(TAG + ": Found .exe for " + displayName + ", " + executableFilePath);
                }

            }
        }

        /**
         * Reads the app manifest file and fill {@link #displayName} and {@link #virtualIconPath} from it.
         *
         * @throws IOException in case there was an error reading the file
         */
        private void readAppManifest() throws IOException {
            File f = new File(path + File.separator + MSStoreEntry.MANIFEST_FILE_NAME);
            if (!f.exists()) {
                LOGGER.warn(TAG + ": File \"" + f + "\" does not exist");
                return;
            }
            InputStream stream = new FileInputStream(f);

            BufferedReader r = new BufferedReader(new InputStreamReader(stream));

            String line;
            while ((line = r.readLine()) != null) {
                Matcher nameMatcher = DISPLAY_NAME_PATTERN.matcher(line);
                if (nameMatcher.find()) {
                    displayName = nameMatcher.group(1);
                }

                Matcher logoMatcher = LOGO_PATTERN.matcher(line);
                if (logoMatcher.find()) {
                    virtualIconPath = path + File.separator + logoMatcher.group(1);
                }

                Matcher appIdMatcher = APPLICATION_ID_PATTERN.matcher(line);
                if (appIdMatcher.find()) {
                    applicationId = appIdMatcher.group(1);
                }

            }
            r.close();
            stream.close();

            startCommand = "shell:AppsFolder\\" + packageFamilyName + "!" + applicationId;
        }

        @Override
        public String toString() {
            return "DisplayName: " + displayName
                    + "\n\tPackageFamilyName: " + packageFamilyName
                    + "\n\tVirtualIconPath: " + virtualIconPath
                    + "\n\tRealIconPath: " + realIconPath;
        }

        public String getIconPath() {
            return realIconPath;
        }

        public String getName() {
            return displayName;
        }

        public String getStartCommand() {
            return startCommand;
        }

        public String getExecutableFilePath() {
            return executableFilePath;
        }
    }
}

package ui;

import data.game.entry.GameEntry;
import data.http.URLTools;
import data.http.key.KeyChecker;
import system.application.InternalAppNetworkManager;
import system.application.MessageListener;
import system.application.MessageTag;
import system.application.settings.PredefinedSetting;
import system.application.settings.SettingValue;
import system.device.XboxController;
import ui.scene.MainScene;
import system.application.settings.GeneralSettings;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;

import static system.application.settings.PredefinedSetting.SUPPORTER_KEY;

public class Main {
    private final static String TEMP_UPDATER_JAR_NAME=  "Updater.jar.temp";
    private final static String UPDATER_JAR_NAME=  "Updater.jar";

    private final static String HTTPS_HOST = "gameroom.me";
    private final static String HTTP_HOST = "s639232867.onlinehome.fr";
    private final static String URL_VERSION_XML_SUFFIX = "/software/version.xml";
    private final static String URL_CHANGELOG_MD_SUFFIX = "/software/changelog.md";


    public final static String ARGS_FLAG_DEV = "-dev";
    public final static String ARGS_FLAG_IGDB_KEY = "-igdb_key";
    public final static String ARGS_FLAG_SHOW = "-show";

    public static boolean DEV_MODE = false;
    public static boolean SUPPORTER_MODE = false;
    public static double SCREEN_WIDTH;
    public static double SCREEN_HEIGHT;

    public static MainScene MAIN_SCENE;

    public final static String DEFAULT_IMAGES_PATH = "res/defaultImages/";

    public static ResourceBundle RESSOURCE_BUNDLE;
    public static ResourceBundle SETTINGS_BUNDLE;
    public static ResourceBundle GAME_GENRES_BUNDLE;
    public static ResourceBundle GAME_THEMES_BUNDLE;

    public static GeneralSettings GENERAL_SETTINGS;

    public static final Logger LOGGER = LogManager.getLogger(Main.class);
    public static final File CACHE_FOLDER = new File("cache");

    public static Menu START_TRAY_MENU = new Menu();

    public static XboxController xboxController;

    public static TrayIcon TRAY_ICON;

    public static InternalAppNetworkManager NETWORK_MANAGER;

    public static volatile boolean KEEP_THREADS_RUNNING = true;

    public static void main(String[] args) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Main.SCREEN_WIDTH = (int) screenSize.getWidth();
        Main.SCREEN_HEIGHT = (int) screenSize.getHeight();
        PredefinedSetting.WINDOW_WIDTH.setDefaultValue(new SettingValue((int)SCREEN_WIDTH,Integer.class,PredefinedSetting.WINDOW_WIDTH.getDefaultValue().getCategory()));
        PredefinedSetting.WINDOW_HEIGHT.setDefaultValue(new SettingValue((int)SCREEN_HEIGHT,Integer.class,PredefinedSetting.WINDOW_HEIGHT.getDefaultValue().getCategory()));

        LOGGER.info("Started app with resolution : " + (int) SCREEN_WIDTH + "x" + (int) SCREEN_HEIGHT);

        GENERAL_SETTINGS = new GeneralSettings();

        SUPPORTER_MODE = !GENERAL_SETTINGS.getString(SUPPORTER_KEY).equals("") && KeyChecker.isKeyValid(GENERAL_SETTINGS.getString(SUPPORTER_KEY));
        LOGGER.info("Supporter mode : "+ SUPPORTER_MODE);
        RESSOURCE_BUNDLE = ResourceBundle.getBundle("strings", GENERAL_SETTINGS.getLocale(PredefinedSetting.LOCALE));
        SETTINGS_BUNDLE = ResourceBundle.getBundle("settings", GENERAL_SETTINGS.getLocale(PredefinedSetting.LOCALE));
        GAME_GENRES_BUNDLE = ResourceBundle.getBundle("gamegenres", GENERAL_SETTINGS.getLocale(PredefinedSetting.LOCALE));
        GAME_THEMES_BUNDLE = ResourceBundle.getBundle("gamethemes", GENERAL_SETTINGS.getLocale(PredefinedSetting.LOCALE));
        initNetworkManager();
        //if(!DEV_MODE){
        startUpdater();
        //}

        CACHE_FOLDER.mkdirs();
        GameEntry.TOADD_FOLDER.mkdirs();
        GameEntry.ENTRIES_FOLDER.mkdirs();

    }
    public static String getArg(String flag,String[] args, boolean hasOption){
        boolean argsHere = false;
        int index = 0;
        for(String arg : args){
            argsHere = argsHere || arg.compareToIgnoreCase(flag) == 0;
            if(!argsHere){
                index++;
            }else{
                break;
            }
        }
        if(argsHere && args.length > index + 1 && hasOption){
            String option = args[index+1];
            if(!option.startsWith("-")){
                return option;
            }
            return null;
        }
        if(argsHere){
            return "";
        }
        return null;
    }

    public static void forceStop(Stage stage) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                KEEP_THREADS_RUNNING = false;
                Platform.setImplicitExit(true);
                NETWORK_MANAGER.disconnect();
                stage.close();
                Platform.exit();
                //
            }
        });
    }

    public static void open(Stage stage) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                stage.show();
            }
        });
    }

    public static void startUpdater() {
        try {
            String currentDir = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            File currentDirFile = new File(currentDir);

            if(!currentDirFile.isDirectory()){
                currentDir = currentDirFile.getParent()+File.separator;
            }else{
                currentDir = currentDir.substring(1); //Remove first '/' symbol
            }
            File currentUpdater = new File(currentDir+UPDATER_JAR_NAME);
            File tempUpdater = new File(currentDir+TEMP_UPDATER_JAR_NAME);
            if(tempUpdater.exists()){
                currentUpdater.delete();
                tempUpdater.renameTo(currentUpdater);
            }
            LOGGER.info("Starting updater");

            boolean httpsOnline = URLTools.pingHttps(HTTPS_HOST,2000);
            String urlPrefix = httpsOnline? URLTools.HTTPS_PREFIX + HTTPS_HOST : URLTools.HTTP_PREFIX +HTTP_HOST;
            Main.LOGGER.info("Using URL "+urlPrefix+" for updater.");

            ProcessBuilder builder = new ProcessBuilder("java"
                    ,"-jar"
                    ,currentUpdater.getAbsolutePath()
                    , getVersion()
                    , urlPrefix + URL_VERSION_XML_SUFFIX
                    , urlPrefix + URL_CHANGELOG_MD_SUFFIX
                    , GENERAL_SETTINGS.getLocale(PredefinedSetting.LOCALE).toLanguageTag()
            ).inheritIO();

            //Main.LOGGER.debug(dir);
            //builder.directory(new File(dir).to);

            builder.redirectError(ProcessBuilder.Redirect.PIPE);
            Process process = builder.start();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void initNetworkManager() {
        NETWORK_MANAGER = new InternalAppNetworkManager();
        NETWORK_MANAGER.addMessageListener(new MessageListener() {
            @Override
            public void onMessageReceived(MessageTag tag, String payload) {
                if (tag.equals(MessageTag.CLOSE_APP)) {
                    forceStop(MAIN_SCENE.getParentStage());
                }
            }
        });
        NETWORK_MANAGER.connect();
        NETWORK_MANAGER.sendMessage(MessageTag.CLOSE_APP);
        //TODO reduce start time by thread with this

    }

    public static String getVersion() {
        String version = "unknown";
        if(RESSOURCE_BUNDLE != null){
            version = RESSOURCE_BUNDLE.getString(version);
        }
        if (Main.class.getPackage().getImplementationVersion() != null) {
            version = Main.class.getPackage().getImplementationVersion();
        }
        Main.LOGGER.info("App version : " + version);
        return version;
    }

    /**
     * Runs the specified {@link Runnable} on the
     * JavaFX application thread and waits for completion.
     *
     * @param action the {@link Runnable} to run
     * @throws NullPointerException if {@code action} is {@code null}
     */
    public static void runAndWait(Runnable action) {
        if (action == null)
            throw new NullPointerException("action");

        // run synchronously on JavaFX thread
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        final CountDownLatch doneLatch = new CountDownLatch(1);

        // queue on JavaFX thread and wait for completion
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                doneLatch.countDown();
            }
        });

        try {
            doneLatch.await();
        } catch (InterruptedException e) {
            // ignore exception
        }
    }
}

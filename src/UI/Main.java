package ui;

import data.game.GameEntry;
import data.http.URLTools;
import system.application.InternalAppNetworkManager;
import system.application.MessageListener;
import system.application.MessageTag;
import system.device.XboxController;
import ui.scene.MainScene;
import data.game.AllGameEntries;
import system.application.GeneralSettings;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Locale;
import java.util.ResourceBundle;

public class Main {
    private final static String TEMP_UPDATER_JAR_NAME=  "Updater.jar.temp";
    private final static String UPDATER_JAR_NAME=  "Updater.jar";

    private final static String HTTPS_HOST = "gameroom.me";
    private final static String HTTP_HOST = "s639232867.onlinehome.fr";
    private final static String URL_VERSION_XML_SUFFIX = "/software/version.xml";
    private final static String URL_CHANGELOG_MD_SUFFIX = "/software/changelog.md";

    public static boolean DEV_MODE = false;
    public static double SCREEN_WIDTH;
    public static double SCREEN_HEIGHT;

    public static MainScene MAIN_SCENE;

    public final static String DEFAULT_IMAGES_PATH = "res/defaultImages/";

    public static ResourceBundle RESSOURCE_BUNDLE;
    public static AllGameEntries ALL_GAMES_ENTRIES = new AllGameEntries();
    public static GeneralSettings GENERAL_SETTINGS;

    public static final Logger LOGGER = LogManager.getLogger(Main.class);
    public static final File CACHE_FOLDER = new File("cache");

    public static Menu START_TRAY_MENU = new Menu();

    public static XboxController xboxController;

    public static TrayIcon TRAY_ICON;

    public static InternalAppNetworkManager NETWORK_MANAGER;

    public static void main(String[] args) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Main.SCREEN_WIDTH = (int) screenSize.getWidth();
        Main.SCREEN_HEIGHT = (int) screenSize.getHeight();

        LOGGER.info("Started app with resolution : " + (int) SCREEN_WIDTH + "x" + (int) SCREEN_HEIGHT);
        GENERAL_SETTINGS = new GeneralSettings();
        RESSOURCE_BUNDLE = ResourceBundle.getBundle("strings", Locale.forLanguageTag(GENERAL_SETTINGS.getLocale()));

        initNetworkManager();
        //if(!DEV_MODE){
        startUpdater();
        //}

        CACHE_FOLDER.mkdirs();
        GameEntry.ENTRIES_FOLDER.mkdirs();

    }

    public static void forceStop(Stage stage) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
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
                    , GENERAL_SETTINGS.getLocale()
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
        NETWORK_MANAGER.connect();

        //close other possible instances of GameRoom
        NETWORK_MANAGER.sendMessage(MessageTag.CLOSE_APP);


        NETWORK_MANAGER.addMessageListener(new MessageListener() {
            @Override
            public void onMessageReceived(MessageTag tag, String payload) {
                if (tag.equals(MessageTag.CLOSE_APP)) {
                    forceStop(MAIN_SCENE.getParentStage());
                }
            }
        });
    }

    public static String getVersion() {
        String version = "unknown";
        if (Main.class.getPackage().getImplementationVersion() != null) {
            version = Main.class.getPackage().getImplementationVersion();
        }
        Main.LOGGER.info("App version : " + version);
        return version;
    }
}

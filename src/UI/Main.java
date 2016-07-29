package ui;

import data.game.GameEntry;
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
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.jar.Manifest;

public class Main {
    private final static String VERSION_XML_URL = "http://s639232867.onlinehome.fr/software/version.xml";
    private final static String CHANGELOG_MD_URL = "http://s639232867.onlinehome.fr/software/changelog.md";

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
            LOGGER.info("Starting updater");
            ProcessBuilder builder = new ProcessBuilder("java",
                    "-jar"
                    , "\"" + currentDir + "Updater.jar\""
                    , getVersion()
                    , VERSION_XML_URL
                    , CHANGELOG_MD_URL
                    , GENERAL_SETTINGS.getLocale()
            ).inheritIO();

            //Main.LOGGER.debug(dir);
            //builder.directory(new File(dir).to);

            builder.redirectError(ProcessBuilder.Redirect.PIPE);
            Process process = builder.start();
            Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //TODO there may be a better way read Updater.jar logs than in a new thread
                        final BufferedReader reader = new BufferedReader(
                                new InputStreamReader(process.getInputStream()));
                        String line = null;
                        while ((line = reader.readLine()) != null) {
                            Main.LOGGER.debug("Updater.jar : "+ line);
                        }
                        reader.close();

                        final BufferedReader errorReader = new BufferedReader(
                                new InputStreamReader(process.getErrorStream()));
                        while ((line = errorReader.readLine()) != null) {
                            Main.LOGGER.error("Updater.jar : "+ line);
                        }
                        errorReader.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            th.setDaemon(true);
            th.start();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void initNetworkManager() {
        NETWORK_MANAGER = new InternalAppNetworkManager();
        NETWORK_MANAGER.connect();

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

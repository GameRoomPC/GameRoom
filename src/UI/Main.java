package ui;

import data.http.URLTools;
import data.http.key.KeyChecker;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import system.application.GameRoomUpdater;
import system.application.InternalAppNetworkManager;
import system.application.MessageListener;
import system.application.MessageTag;
import system.application.settings.GeneralSettings;
import system.application.settings.PredefinedSetting;
import system.application.settings.SettingValue;
import system.device.XboxController;
import system.os.Terminal;
import ui.scene.MainScene;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;

import static system.application.settings.PredefinedSetting.SUPPORTER_KEY;

public class Main {
    private final static String TEMP_UPDATER_JAR_NAME=  "Updater.jar.temp";
    private final static String UPDATER_JAR_NAME=  "Updater.jar";

    public final static String ARGS_FLAG_DEV = "-dev";
    public final static String ARGS_FLAG_IGDB_KEY = "-igdb_key";
    public final static String ARGS_FLAG_SHOW = "-show";
    public final static String ARGS_START_GAME = "-start-game";

    public static boolean DEV_MODE = false;
    public static boolean SUPPORTER_MODE = false;
    private static double TRUE_SCREEN_WIDTH;
    private static double TRUE_SCREEN_HEIGHT;

    public static double SCREEN_WIDTH;
    public static double SCREEN_HEIGHT;

    public static MainScene MAIN_SCENE;

    public static ResourceBundle RESSOURCE_BUNDLE;
    public static ResourceBundle SETTINGS_BUNDLE;
    public static ResourceBundle GAME_GENRES_BUNDLE;
    public static ResourceBundle GAME_THEMES_BUNDLE;

    public static GeneralSettings GENERAL_SETTINGS;

    public static final Logger LOGGER = LogManager.getLogger(Main.class);

    public static final HashMap<String, File> FILES_MAP = new HashMap<>();

    public static Menu START_TRAY_MENU = new Menu();

    public static XboxController xboxController;

    public static TrayIcon TRAY_ICON;

    public static InternalAppNetworkManager NETWORK_MANAGER;

    public static volatile boolean KEEP_THREADS_RUNNING = true;

    private static String[] calling_args;

    public static void main(String[] args) {
        calling_args = args;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        Main.TRUE_SCREEN_WIDTH = (int) screenSize.getWidth();
        Main.TRUE_SCREEN_HEIGHT = (int) screenSize.getHeight();
        Main.SCREEN_WIDTH = TRUE_SCREEN_WIDTH <= 1920? TRUE_SCREEN_WIDTH : 1920;
        Main.SCREEN_HEIGHT = TRUE_SCREEN_HEIGHT<= 1080? TRUE_SCREEN_HEIGHT : 1080;


        PredefinedSetting.WINDOW_WIDTH.setDefaultValue(new SettingValue((int)TRUE_SCREEN_WIDTH,Integer.class,PredefinedSetting.WINDOW_WIDTH.getDefaultValue().getCategory()));
        PredefinedSetting.WINDOW_HEIGHT.setDefaultValue(new SettingValue((int)TRUE_SCREEN_HEIGHT,Integer.class,PredefinedSetting.WINDOW_HEIGHT.getDefaultValue().getCategory()));

        LOGGER.info("Started app with screen true resolution : " + (int) TRUE_SCREEN_WIDTH + "x" + (int) TRUE_SCREEN_HEIGHT);

        GENERAL_SETTINGS = new GeneralSettings();

        SUPPORTER_MODE = !GENERAL_SETTINGS.getString(SUPPORTER_KEY).equals("") && KeyChecker.isKeyValid(GENERAL_SETTINGS.getString(SUPPORTER_KEY));
        LOGGER.info("Supporter mode : "+ SUPPORTER_MODE);
        RESSOURCE_BUNDLE = ResourceBundle.getBundle("strings", GENERAL_SETTINGS.getLocale(PredefinedSetting.LOCALE));
        SETTINGS_BUNDLE = ResourceBundle.getBundle("settings", GENERAL_SETTINGS.getLocale(PredefinedSetting.LOCALE));
        GAME_GENRES_BUNDLE = ResourceBundle.getBundle("gamegenres", GENERAL_SETTINGS.getLocale(PredefinedSetting.LOCALE));
        GAME_THEMES_BUNDLE = ResourceBundle.getBundle("gamethemes", GENERAL_SETTINGS.getLocale(PredefinedSetting.LOCALE));
        initNetworkManager();
        //if(!DEV_MODE){
        //startUpdater();
        //}
    }

    public static String getArg(String flag,String[] args, boolean hasOption){
        boolean argsHere = false;
        int index = 0;
        if(args!=null) {
            for (String arg : args) {
                argsHere = argsHere || arg.compareToIgnoreCase(flag) == 0;
                if (!argsHere) {
                    index++;
                } else {
                    break;
                }
            }
            if (argsHere && args.length > index + 1 && hasOption) {
                String option = args[index + 1];
                if (!option.startsWith("-")) {
                    return option;
                }
                return null;
            }
            if (argsHere) {
                return "";
            }
        }
        return null;
    }

    public static void forceStop(Stage stage, String reason) {
        LOGGER.info("Stopping GameRoom because : "+reason);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                MAIN_SCENE.saveScrollBarVValue();
                KEEP_THREADS_RUNNING = false;
                Platform.setImplicitExit(true);
                NETWORK_MANAGER.disconnect();
                stage.close();
                Platform.exit();
                //
            }
        });
    }

    public static void restart(Stage stage, String reason){
        LOGGER.info("Restarting GameRoom because : "+reason);
        try {
            Process process = new ProcessBuilder()
                    .command("GameRoom.exe")
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        forceStop(stage,reason);
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
    }

    private static void initNetworkManager() {
        NETWORK_MANAGER = new InternalAppNetworkManager();
        NETWORK_MANAGER.addMessageListener(new MessageListener() {
            @Override
            public void onMessageReceived(MessageTag tag, String payload) {
                if (tag.equals(MessageTag.CLOSE_APP)) {
                    forceStop(MAIN_SCENE.getParentStage(),"Network manager, received close app message");
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

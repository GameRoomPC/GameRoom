package com.gameroom;

import com.gameroom.data.game.GameFolderManager;
import com.gameroom.data.game.entry.GameEntryUtils;
import com.gameroom.data.game.scraper.IGDBScraper;
import com.gameroom.data.io.DataBase;
import com.gameroom.data.io.FileUtils;
import com.gameroom.data.migration.OldGameEntry;
import com.gameroom.data.migration.OldSettings;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.apache.logging.log4j.LogManager;
import org.boris.winrun4j.DDE;
import org.boris.winrun4j.SplashScreen;
import com.gameroom.system.application.Monitor;
import com.gameroom.system.application.settings.PredefinedSetting;
import com.gameroom.system.device.ControllerButtonListener;
import com.gameroom.system.device.GameController;
import com.gameroom.system.os.WinReg;
import com.gameroom.ui.GeneralToast;
import com.gameroom.ui.Main;
import com.gameroom.ui.dialog.ConsoleOutputDialog;
import com.gameroom.ui.dialog.WindowFocusManager;
import com.gameroom.ui.scene.BaseScene;
import com.gameroom.ui.scene.MainScene;
import com.gameroom.ui.scene.SettingsScene;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.sql.SQLException;

import static com.gameroom.system.application.settings.GeneralSettings.settings;
import static com.gameroom.ui.Main.*;

/**
 * Created by LM on 15/07/2016.
 */
public class Launcher extends Application {
    private final static double MIN_WINDOW_WIDTH = 640;
    private final static double MIN_WINDOW_HEIGHT = 480;

    private final static double MIN_WINDOW_X = 0;
    private final static double MIN_WINDOW_Y = 0;

    private int trayMessageCount = 0;
    private static ConsoleOutputDialog[] console = new ConsoleOutputDialog[1];
    private static boolean START_MINIMIZED = false;
    private static ChangeListener<Boolean> focusListener;
    private static ChangeListener<Boolean> maximizedListener;
    private static ChangeListener<Boolean> fullScreenListener;

    private static double TRUE_SCREEN_WIDTH;
    private static double TRUE_SCREEN_HEIGHT;

    private volatile boolean monitoringStageProps = false;

    private static String DATA_PATH;

    public static void main(String[] args) throws URISyntaxException {

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        TRUE_SCREEN_WIDTH = (int) screenSize.getWidth();
        TRUE_SCREEN_HEIGHT = (int) screenSize.getHeight();

        Main.DEV_MODE = getArg(ARGS_FLAG_DEV, args, false) != null;

        if (DEV_MODE) {
            String appdataFolder = System.getenv("APPDATA");
            DATA_PATH = appdataFolder + File.separator + System.getProperty("working.dir");
        } else {
            DATA_PATH = WinReg.readDataPath();
        }

        if (!DEV_MODE) {
            SplashScreen.setTextColor(149, 156, 161);
            SplashScreen.setTextFont("Arial", 8);
            setSplashscreenText("Opening GameRoom");
        }

        System.setProperty("data.dir", DATA_PATH);
        Main.LOGGER = LogManager.getLogger(Main.class);

        System.setErr(new PrintStream(System.err) {
            public void print(final String string) {
                LOGGER.error(string);
                if (DEV_MODE || settings().getBoolean(PredefinedSetting.DEBUG_MODE)) {
                    Platform.runLater(() -> {
                        try {
                            if (console[0] == null) {
                                console[0] = new ConsoleOutputDialog();
                            }
                            console[0].appendLine(string);
                            console[0].showConsole();
                        } catch (Exception e) {
                            LOGGER.error("Console crashed ! ");
                            LOGGER.error(e.getMessage());
                        }
                    });
                }
            }
        });
        System.setOut(new PrintStream(System.out) {
            public void print(final String string) {
                //System.out.print(string);
                LOGGER.debug(string);
            }
        });

        System.out.println("\n\n==========================================NEW START============================================");

        Main.LOGGER.debug("Received args : ");
        for (String arg : args) {
            Main.LOGGER.debug("\t\"" + arg + "\"");
        }

        if (!DEV_MODE) {
            DDE.addActivationListener(s -> open(MAIN_SCENE.getParentStage()));
            DDE.ready();
        }
        IGDBScraper.init();

        setSplashscreenText("Checking files...");
        initFiles();
        setSplashscreenText("Initializing DB connection...");
        DataBase.initDB();
        setSplashscreenText("Migrating settings...");
        OldSettings.transferOldSettings();
        setSplashscreenText("Loading settings...");
        Main.main(args);
        setSplashscreenText("Migrating games...");
        OldGameEntry.transferOldGameEntries();
        setSplashscreenText("Loading games...");
        GameEntryUtils.loadGames();

        String gameToStartID = getArg(ARGS_START_GAME, args, true);
        if (gameToStartID != null) {
            boolean gameRoomAlreadyStarted = Monitor.isProcessRunning("GameRoom.exe");
            if (gameRoomAlreadyStarted) {
                //TODO implement network init and sendung the game to start (with a repeat functionnality if no ack after 2 sec)

            } else {
                //TODO implement starting the game that has the given id
            }
        }
        String showMode = getArg(ARGS_FLAG_SHOW, args, true);
        if (showMode != null) {
            switch (showMode) {
                case "0":
                    START_MINIMIZED = true;
                    break;
                default:
                    START_MINIMIZED = false;
                    break;
            }
        }
        launch(args);
    }

    private static void initFiles() {

        System.out.println("datapath : " + DATA_PATH);
        File gameRoomFolder = FileUtils.initOrCreateFolder(DATA_PATH);

        if (!DEV_MODE) {
            String appdataFolder = System.getenv("APPDATA");
            String oldDataPath = appdataFolder + File.separator + "GameRoom" + File.separator;

            File configProperties = new File(oldDataPath + "config.properties");
            File logFolder = new File(oldDataPath + "log");
            //File libsFolder = new File("libs");
            File gamesFolder = new File(oldDataPath + "Games");
            File toAddFolder = new File(oldDataPath + "ToAdd");
            File cacheFolder = new File(oldDataPath + "cache");

            /*****************MOVE FILES/FOLDERS IF NEEDED***********************/
            FileUtils.moveToFolder(gamesFolder, gameRoomFolder);
            FileUtils.moveToFolder(configProperties, gameRoomFolder);
            FileUtils.moveToFolder(logFolder, gameRoomFolder);
            if (logFolder.exists()) {
                FileUtils.clearFolder(logFolder);
                logFolder.delete();
            }
            //FileUtils.moveToFolder(libsFolder,gameRoomFolder);
            FileUtils.moveToFolder(toAddFolder, gameRoomFolder);
            FileUtils.moveToFolder(cacheFolder, gameRoomFolder);
        }

        /*****************INIT FILES AND FOLDER***********************/
        Main.FILES_MAP.put("working_dir", gameRoomFolder);
        Main.FILES_MAP.put("cache", FileUtils.initOrCreateFolder(gameRoomFolder + File.separator + "cache"));
        Main.FILES_MAP.put("temp", FileUtils.initOrCreateFolder(gameRoomFolder + File.separator + "temp"));
        Main.FILES_MAP.put("to_add", new File(gameRoomFolder + File.separator + "ToAdd"));
        //Main.FILES_MAP.put("libs",FileUtils.initOrCreateFolder(gameRoomFolder+File.separator+"libs"));
        Main.FILES_MAP.put("games", new File(gameRoomFolder + File.separator + "Games"));
        Main.FILES_MAP.put("log", FileUtils.initOrCreateFolder(gameRoomFolder + File.separator + "log"));
        Main.FILES_MAP.put("config.properties", new File(gameRoomFolder + File.separator + "config.properties"));
        Main.FILES_MAP.put("GameRoom.log", FileUtils.initOrCreateFile(Main.FILES_MAP.get("log").getAbsolutePath() + File.separator + "GameRoom.log"));
        Main.FILES_MAP.put("themes", FileUtils.initOrCreateFolder(gameRoomFolder + File.separator + "themes"));
        Main.FILES_MAP.put("current_theme", FileUtils.initOrCreateFolder(Main.FILES_MAP.get("themes").getAbsolutePath() + File.separator + "current"));
        Main.FILES_MAP.put("theme_css", new File(Main.FILES_MAP.get("current_theme").getAbsolutePath() + File.separator + "theme.css"));
        Main.FILES_MAP.put("db", new File(gameRoomFolder + File.separator + DataBase.DB_NAME));
        Main.FILES_MAP.put("pictures", FileUtils.initOrCreateFolder(gameRoomFolder + File.separator + "pictures"));
        Main.FILES_MAP.put("cover", FileUtils.initOrCreateFolder(Main.FILES_MAP.get("pictures").getAbsolutePath() + File.separator + "cover"));
        Main.FILES_MAP.put("screenshot", FileUtils.initOrCreateFolder(Main.FILES_MAP.get("pictures").getAbsolutePath() + File.separator + "screenshot"));
        Main.FILES_MAP.put("games_log", FileUtils.initOrCreateFolder(Main.FILES_MAP.get("log").getAbsolutePath() + File.separator + "games"));

    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        setSplashscreenText("Configuring window...");

        primaryStage.setWidth(settings().getWindowWidth());
        primaryStage.setHeight(settings().getWindowHeight());
        primaryStage.setX(settings().getDouble(PredefinedSetting.WINDOW_X));
        primaryStage.setY(settings().getDouble(PredefinedSetting.WINDOW_Y));

        MAIN_SCENE = new MainScene(primaryStage);
        initPrimaryStage(primaryStage, MAIN_SCENE);
        initTrayIcon();
        initXboxController(primaryStage);
        setFullScreen(primaryStage, settings().getBoolean(PredefinedSetting.FULL_SCREEN));

        if (!DEV_MODE) {
            SplashScreen.close();
        }
        openStage(primaryStage, true);

        if (!DEV_MODE) {
            startUpdater();
        }
    }

    private void openStage(Stage primaryStage, boolean appStart) {
        /*strangely the two following lines will fix some scaling issues that occurs when not in maximized mode and switching
        between to scenes, the main scene would scale in height too much otherwise.
         */
        primaryStage.setWidth(primaryStage.getWidth());
        primaryStage.setHeight(primaryStage.getHeight());
        primaryStage.setMaximized(settings().getBoolean(PredefinedSetting.WINDOW_MAXIMIZED));

        if (START_MINIMIZED && appStart) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            primaryStage.setIconified(true);
        } else {
            primaryStage.show();
            primaryStage.toFront();
        }
    }

    private void initPrimaryStage(Stage primaryStage, Scene initScene) {
        initIcons(primaryStage);
        primaryStage.setTitle("GameRoom");
        primaryStage.initStyle(StageStyle.DECORATED);

        focusListener = (observable, oldValue, newValue) -> {
            MAIN_SCENE.setChangeBackgroundNextTime(true);
            primaryStage.getScene().getRoot().setMouseTransparent(!newValue);
            GeneralToast.enableToasts(newValue);
            WindowFocusManager.stageFocusChanged(newValue);
        };

        primaryStage.focusedProperty().addListener(focusListener);

        primaryStage.setScene(initScene);
        primaryStage.setFullScreenExitHint("");
        primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

        fullScreenListener = (observable, oldValue, newValue) -> {
            setFullScreen(primaryStage, newValue);
        };
        settings().getBooleanProperty(PredefinedSetting.FULL_SCREEN).addListener((fullScreenListener));

        MAIN_SCENE.setParentStage(primaryStage);

        if (initScene instanceof BaseScene) {
            ((BaseScene) initScene).setParentStage(primaryStage);
        }
        primaryStage.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F11) {
                boolean wasFullScreen = settings().getBoolean(PredefinedSetting.FULL_SCREEN);
                settings().setSettingValue(PredefinedSetting.FULL_SCREEN, !wasFullScreen);
            }
            if (event.getCode() == KeyCode.F10) {
                //TODO toggle drawerMenu of MainScene
            }
            if (event.getCode() == KeyCode.F && event.isControlDown()) {
                MAIN_SCENE.showSearchField();
            }
        });
        maximizedListener = (observable, oldValue, newValue) -> {
            if (!settings().getBoolean(PredefinedSetting.FULL_SCREEN) && !primaryStage.isIconified()) {
                settings().setSettingValue(PredefinedSetting.WINDOW_MAXIMIZED, newValue);
            }
        };
        primaryStage.maximizedProperty().addListener(maximizedListener);

        primaryStage.xProperty().addListener((observable, oldValue, newValue) -> monitorStageProperties(primaryStage));
        primaryStage.yProperty().addListener((observable, oldValue, newValue) -> monitorStageProperties(primaryStage));
        primaryStage.widthProperty().addListener((observableValue, oldSceneWidth, newSceneWidth) -> monitorStageProperties(primaryStage));
        primaryStage.heightProperty().addListener((observableValue, oldSceneHeight, newSceneHeight) -> monitorStageProperties(primaryStage));
    }

    /**
     * Starts a task on the {@link Main#getExecutorService()} instance to monitor and save properties (dimension and coordinates)
     * of the stage to the database.
     * @param stage stage to monitor properties from
     */
    private void monitorStageProperties(Stage stage){
        if (!monitoringStageProps && isStagePropertiesSavable(stage)) {
            monitoringStageProps = true;
            Main.getExecutorService().submit(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
                if (isStagePropertiesSavable(stage)) {
                    settings().setSettingValue(PredefinedSetting.WINDOW_WIDTH, (int) stage.getWidth());
                    settings().setSettingValue(PredefinedSetting.WINDOW_HEIGHT, (int) stage.getHeight());
                    settings().setSettingValue(PredefinedSetting.WINDOW_X, stage.getX());
                    settings().setSettingValue(PredefinedSetting.WINDOW_Y, stage.getY());
                    monitoringStageProps = false;
                }
            });
        }
    }

    /**
     * Computes whether the properties (coordinates and dimensions) of the stage displayed should be saved. They should
     * not be saved when the stage is either iconified, maximized or in fullscreen mode.
     * It also checks that coordinates and dimensions are within plausible ranges, checking against
     *
     * @param stage the stage displayed which properties are monitored
     * @return true if the program should save the stage's dimensions and location, false otherwise
     */
    private boolean isStagePropertiesSavable(Stage stage){
        boolean stateCorrect = !stage.isIconified() && !stage.isMaximized() && !stage.isFullScreen();

        boolean correctCoordinates = stage.getX() >= MIN_WINDOW_X
                && stage.getX() < TRUE_SCREEN_WIDTH
                && stage.getY() >= MIN_WINDOW_Y
                && stage.getY() < TRUE_SCREEN_HEIGHT;

        boolean correctDimension = stage.getWidth() >= MIN_WINDOW_WIDTH
                && stage.getWidth() <= TRUE_SCREEN_WIDTH
                && stage.getHeight() >= MIN_WINDOW_HEIGHT
                && stage.getHeight() <= TRUE_SCREEN_HEIGHT;
        LOGGER.debug("Window event detected, monitoring...");
        LOGGER.debug("True dims: width="+TRUE_SCREEN_WIDTH+", height="+TRUE_SCREEN_HEIGHT);
        LOGGER.debug("stateCorrect=" + stateCorrect +", correctCoord="+correctCoordinates+", correctDims="+correctDimension);
        LOGGER.debug("Coord: x="+stage.getX()+",y="+stage.getY()+"; Dim: width="+stage.getWidth()+", height="+stage.getHeight());
        return stateCorrect && correctCoordinates && correctDimension;
    }

    private void setFullScreen(Stage primaryStage, boolean fullScreen) {
        settings().setSettingValue(PredefinedSetting.FULL_SCREEN, fullScreen);
        primaryStage.setFullScreen(fullScreen);

        if (MAIN_SCENE != null) {
            MAIN_SCENE.toggleScrollBar(fullScreen);
        }
    }

    private void initXboxController(Stage primaryStage) {
        try {
            Robot r = new Robot();
            gameController = new GameController(new ControllerButtonListener() {
                @Override
                public void onButtonPressed(String buttonId) {
                    switch (buttonId) {
                        case GameController.BUTTON_A:
                            r.keyPress(java.awt.event.KeyEvent.VK_ENTER);
                            break;
                        case GameController.BUTTON_B:
                            r.keyPress(java.awt.event.KeyEvent.VK_ESCAPE);
                            break;
                        case GameController.BUTTON_X:
                            r.keyPress(KeyEvent.VK_SPACE);
                            break;
                        case GameController.BUTTON_Y:
                            r.keyPress(java.awt.event.KeyEvent.VK_I);
                            break;
                        case GameController.BUTTON_DPAD_UP:
                            r.keyPress(java.awt.event.KeyEvent.VK_UP);
                            break;
                        case GameController.BUTTON_DPAD_LEFT:
                            r.keyPress(java.awt.event.KeyEvent.VK_LEFT);
                            break;
                        case GameController.BUTTON_DPAD_DOWN:
                            r.keyPress(java.awt.event.KeyEvent.VK_DOWN);
                            break;
                        case GameController.BUTTON_DPAD_RIGHT:
                            r.keyPress(java.awt.event.KeyEvent.VK_RIGHT);
                            break;
                        case GameController.BUTTON_SELECT:
                            r.keyPress(java.awt.event.KeyEvent.VK_F11);
                            break;
                        default:
                            break;
                    }

                }

                @Override
                public void onButtonReleased(String buttonId) {

                }
            });
            if (settings().getBoolean(PredefinedSetting.ENABLE_GAME_CONTROLLER_SUPPORT) && WindowFocusManager.isWindowFocused()) {
                gameController.resume();
            }

        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        Main.LOGGER.info("Closing app, saving settings.");
        if (MAIN_SCENE != null) {
            Main.runAndWait(() -> {
                MAIN_SCENE.saveScrollBarVValue();
            });
        }
        Main.getExecutorService().shutdownNow();
        Main.getScheduledExecutor().shutdownNow();
        WindowFocusManager.shutdown();
        gameController.shutdown();

        FileUtils.clearFolder(Main.FILES_MAP.get("cache"));
        FileUtils.clearFolder(Main.FILES_MAP.get("temp"));

        try {
            settings().save();
            DataBase.getUserConnection().close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.exit(0);
    }

    private static void setSplashscreenText(String text) {
        if (!DEV_MODE) {
            SplashScreen.setText(text, 5, 120);
        } else {
            LOGGER.info(text);
        }
    }

    private void initTrayIcon() {
        //Check the SystemTray is supported
        if (!SystemTray.isSupported()) {
            Main.LOGGER.error("SystemTray not supported");
            return;
        }

        final PopupMenu popup = new PopupMenu();
        Image fxImage = new Image("com/gameroom/ui/icon/icon16.png");
        TRAY_ICON = new TrayIcon(SwingFXUtils.fromFXImage(fxImage, null));
        TRAY_ICON.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    open(MAIN_SCENE.getParentStage());
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });
        TRAY_ICON.setImageAutoSize(true);
        Platform.setImplicitExit(DEV_MODE && false);

        MAIN_SCENE.getParentStage().setOnCloseRequest(event -> {
            if (event.getEventType().equals(WindowEvent.WINDOW_CLOSE_REQUEST)) {
                if (!DEV_MODE) {
                    MAIN_SCENE.getParentStage().setIconified(true);
                    if (trayMessageCount < 2 && !settings().getBoolean(PredefinedSetting.NO_MORE_ICON_TRAY_WARNING) && !settings().getBoolean(PredefinedSetting.NO_NOTIFICATIONS)) {
                        TRAY_ICON.displayMessage("GameRoom"
                                , Main.getString("tray_icon_still_running"), TrayIcon.MessageType.NONE);
                        trayMessageCount++;
                    } else {
                        if (!settings().getBoolean(PredefinedSetting.NO_MORE_ICON_TRAY_WARNING)) {
                            settings().setSettingValue(PredefinedSetting.NO_MORE_ICON_TRAY_WARNING, true);
                        }
                    }
                }
            }
        });
        final SystemTray tray = SystemTray.getSystemTray();

        // Create a pop-up menu components
        MenuItem openItem = new MenuItem(Main.getString("open"));
        openItem.addActionListener(e -> open(MAIN_SCENE.getParentStage()));

        MenuItem gameRoomFolderItem = new MenuItem(Main.getString("gameroom_folder"));
        gameRoomFolderItem.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(new File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile());
            } catch (IOException | URISyntaxException e1) {
                e1.printStackTrace();
            }
        });

        Menu gamesFoldersMenu = new Menu(Main.getString("games_folders"));
        for (File f : GameFolderManager.getPCFolders()) {
            if (f != null && f.exists() && f.isDirectory()) {
                MenuItem gamesFolderItem = new MenuItem(f.getName());
                gamesFolderItem.addActionListener(e -> {
                    try {
                        Desktop.getDesktop().open(f);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                });
                gamesFoldersMenu.add(gamesFolderItem);
            }
        }

        MenuItem settingsItem = new MenuItem(Main.getString("Settings"));
        settingsItem.addActionListener(e -> {
            MAIN_SCENE.fadeTransitionTo(new SettingsScene(new StackPane(), MAIN_SCENE.getParentStage(), MAIN_SCENE), MAIN_SCENE.getParentStage());
            open(MAIN_SCENE.getParentStage());
        });
        //CheckboxMenuItem cb1 = new CheckboxMenuItem("Set auto size");
        //CheckboxMenuItem cb2 = new CheckboxMenuItem("Set tooltip");
        START_TRAY_MENU.setLabel(Main.getString("start"));
        MenuItem exitItem = new MenuItem(Main.getString("exit"));

        exitItem.addActionListener(e -> forceStop(MAIN_SCENE.getParentStage(), "tray icon exitItem called"));

        //Add components to pop-up menu
        popup.add(openItem);
        popup.add(START_TRAY_MENU);
        popup.addSeparator();
        popup.add(gameRoomFolderItem);

        if (gamesFoldersMenu.getItemCount() > 0) {
            popup.add(gamesFoldersMenu);
        }
        popup.add(settingsItem);
        popup.addSeparator();
        popup.add(exitItem);

        TRAY_ICON.setPopupMenu(popup);

        try {
            tray.add(TRAY_ICON);
        } catch (AWTException e) {
            System.out.println("TrayIcon could not be added.");
        }
    }

    private void initIcons(Stage stage) {

        for (int i = 32; i < 513; i *= 2) {
            stage.getIcons().add(new Image("com/gameroom/ui/icon/icon" + i + ".png"));
        }
    }

}
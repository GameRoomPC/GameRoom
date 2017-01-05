import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.WString;
import data.FileUtils;
import data.game.entry.AllGameEntries;
import data.game.entry.GameEntry;
import data.game.scrapper.IGDBScrapper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import system.application.Monitor;
import system.application.settings.PredefinedSetting;
import system.device.ControllerButtonListener;
import system.device.XboxController;
import ui.Main;
import ui.control.specific.GeneralToast;
import ui.dialog.ConsoleOutputDialog;
import ui.scene.BaseScene;
import ui.scene.MainScene;
import ui.scene.SettingsScene;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.UUID;

import static ui.Main.*;

/**
 * Created by LM on 15/07/2016.
 */
public class Launcher extends Application {
    private int trayMessageCount = 0;
    private static ConsoleOutputDialog[] console = new ConsoleOutputDialog[1];
    private double widthBeforeFullScreen = -1;
    private double heightBeforeFullScreen = -1;
    private static boolean START_MINIMIZED = false;
    private static boolean WAS_MAXIMISED = false;
    private static ChangeListener<Boolean> focusListener;
    private static ChangeListener<Boolean> maximizedListener;

    public static void main(String[] args) throws URISyntaxException {
        setCurrentProcessExplicitAppUserModelID("GameRoom");

        System.setErr(new PrintStream(System.err) {
            public void print(final String string) {
                LOGGER.error(string);
                if (DEV_MODE || GENERAL_SETTINGS.getBoolean(PredefinedSetting.DEBUG_MODE)) {
                    Platform.runLater(() -> {
                        if (console[0] == null) {
                            console[0] = new ConsoleOutputDialog();
                        }
                        console[0].appendLine(string);
                        console[0].showConsole();
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

        Main.DEV_MODE = getArg(ARGS_FLAG_DEV, args, false) != null;
        initFiles();
        AllGameEntries.loadGames();

        String gameToStartUUID = getArg(ARGS_START_GAME, args, true);
        if (gameToStartUUID != null) {
            boolean gameRoomAlreadyStarted = Monitor.isProcessRunning("GameRoom.exe");
            if (gameRoomAlreadyStarted) {
                //TODO implement network init and sendung the game to start (with a repeat functionnality if no ack after 2 sec)

            } else {
                //can start the game here, then let GameRoom do as usual
                ArrayList<UUID> uuids = AllGameEntries.readUUIDS(FILES_MAP.get("games"));
                int i = 0;
                for (GameEntry entry : AllGameEntries.ENTRIES_LIST) {
                    if (entry.getUuid().equals(gameToStartUUID)) {
                        //found the game to start!
                        entry.startGame();
                    }
                }
            }
        }

        String igdbKey = getArg(ARGS_FLAG_IGDB_KEY, args, true);
        if (igdbKey != null) {
            IGDBScrapper.IGDB_BASIC_KEY = igdbKey;
            IGDBScrapper.IGDB_PRO_KEY = igdbKey;
            IGDBScrapper.key = igdbKey;
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
        Main.main(args);
        launch(args);
    }

    private static void initFiles() {
        String appdataFolder = System.getenv("APPDATA");
        String gameRoomPath = appdataFolder + File.separator + System.getProperty("working.dir");
        System.out.println("afinfwoeifbnw " + gameRoomPath);
        File gameRoomFolder = FileUtils.initOrCreateFolder(gameRoomPath);

        File configProperties = new File("config.properties");
        File logFolder = new File("log");
        //File libsFolder = new File("libs");
        File gamesFolder = new File("Games");
        File toAddFolder = new File("ToAdd");
        File cacheFolder = new File("cache");

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

        /*****************INIT FILES AND FOLDER***********************/
        Main.FILES_MAP.put("working_dir", gameRoomFolder);
        Main.FILES_MAP.put("cache", FileUtils.initOrCreateFolder(gameRoomFolder + File.separator + "cache"));
        Main.FILES_MAP.put("to_add", FileUtils.initOrCreateFolder(gameRoomFolder + File.separator + "ToAdd"));
        //Main.FILES_MAP.put("libs",FileUtils.initOrCreateFolder(gameRoomFolder+File.separator+"libs"));
        Main.FILES_MAP.put("games", FileUtils.initOrCreateFolder(gameRoomFolder + File.separator + "Games"));
        Main.FILES_MAP.put("log", FileUtils.initOrCreateFolder(gameRoomFolder + File.separator + "log"));
        Main.FILES_MAP.put("config.properties", FileUtils.initOrCreateFile(gameRoomFolder + File.separator + "config.properties"));
        Main.FILES_MAP.put("GameRoom.log", FileUtils.initOrCreateFile(Main.FILES_MAP.get("log").getAbsolutePath() + File.separator + "GameRoom.log"));
        Main.FILES_MAP.put("themes", FileUtils.initOrCreateFolder(gameRoomFolder + File.separator + "themes"));
        Main.FILES_MAP.put("current_theme", FileUtils.initOrCreateFolder(Main.FILES_MAP.get("themes").getAbsolutePath() + File.separator + "current"));
        Main.FILES_MAP.put("theme_css", new File(Main.FILES_MAP.get("current_theme").getAbsolutePath() + File.separator + "theme.css"));
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        MAIN_SCENE = new MainScene(primaryStage);
        initPrimaryStage(primaryStage, MAIN_SCENE, true);
        initTrayIcon();
        initXboxController(primaryStage);
        setFullScreen(primaryStage, GENERAL_SETTINGS.getBoolean(PredefinedSetting.FULL_SCREEN), true);
        if (!DEV_MODE) {
            startUpdater();
        }
    }

    private void openStage(Stage primaryStage, boolean appStart) {
        if (START_MINIMIZED && appStart) {
            primaryStage.setOpacity(0);
        }
        primaryStage.show();
        if (START_MINIMIZED && appStart) {
            primaryStage.hide();
            primaryStage.setOpacity(1);
        }
        Platform.runLater(() -> {
            primaryStage.setWidth(primaryStage.getWidth());
            primaryStage.setHeight(primaryStage.getHeight());
            primaryStage.setMaximized(GENERAL_SETTINGS.getBoolean(PredefinedSetting.WINDOW_MAXIMIZED));
        });
    }

    private void initPrimaryStage(Stage primaryStage, Scene initScene, boolean appStart) {
        initIcons(primaryStage);
        primaryStage.setTitle("GameRoom");
        focusListener = (observable, oldValue, newValue) -> {
            MAIN_SCENE.setChangeBackgroundNextTime(true);
            primaryStage.getScene().getRoot().setMouseTransparent(!newValue);
            GeneralToast.enableToasts(newValue);

            if (newValue && Main.GENERAL_SETTINGS.getBoolean(PredefinedSetting.ENABLE_XBOX_CONTROLLER_SUPPORT)) {
                xboxController.startThreads();
            } else if (!newValue && Main.GENERAL_SETTINGS.getBoolean(PredefinedSetting.ENABLE_XBOX_CONTROLLER_SUPPORT)) {
                xboxController.stopThreads();
            }
        };
        primaryStage.focusedProperty().addListener(focusListener);

        primaryStage.setScene(initScene);
        primaryStage.setFullScreenExitHint("");
        primaryStage.setFullScreenExitKeyCombination(null);
        MAIN_SCENE.setParentStage(primaryStage);
        if (initScene instanceof BaseScene) {
            ((BaseScene) initScene).setParentStage(primaryStage);
        }
        primaryStage.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, new EventHandler<javafx.scene.input.KeyEvent>() {
            @Override
            public void handle(javafx.scene.input.KeyEvent event) {
                if (event.getCode() == KeyCode.F11) {
                    setFullScreen(primaryStage, !GENERAL_SETTINGS.getBoolean(PredefinedSetting.FULL_SCREEN), false);
                }
                if (event.getCode() == KeyCode.F10) {
                    if (Main.MAIN_SCENE != null) {
                        Main.MAIN_SCENE.toggleTopBar();
                    }
                }
            }
        });
        maximizedListener = (observable, oldValue, newValue) -> {
            if (!GENERAL_SETTINGS.getBoolean(PredefinedSetting.FULL_SCREEN)) {
                GENERAL_SETTINGS.setSettingValue(PredefinedSetting.WINDOW_MAXIMIZED, newValue);
            }
        };
        primaryStage.maximizedProperty().addListener(maximizedListener);
    }

    private void clearStage(Stage stage) {
        if (maximizedListener != null) {
            stage.maximizedProperty().removeListener(maximizedListener);
        }
        if (focusListener != null) {
            stage.focusedProperty().removeListener(focusListener);
        }
    }

    private void setFullScreen(Stage primaryStage, boolean fullScreen, boolean appStart) {
        if (fullScreen) {
            WAS_MAXIMISED = primaryStage.isMaximized();
        }
        GENERAL_SETTINGS.setSettingValue(PredefinedSetting.FULL_SCREEN, fullScreen);
        if (!appStart) {
            clearStage(primaryStage);
            primaryStage.close();
        }
        Stage newStage = new Stage();
        if (appStart) {
            newStage = primaryStage;
        }
        newStage.setFullScreen(fullScreen);
        if (fullScreen) {
            widthBeforeFullScreen = GENERAL_SETTINGS.getWindowWidth();
            heightBeforeFullScreen = GENERAL_SETTINGS.getWindowHeight();
            newStage.setWidth(Main.SCREEN_WIDTH);
            newStage.setHeight(Main.SCREEN_HEIGHT);
            newStage.initStyle(StageStyle.UNDECORATED);
        } else {
            if (widthBeforeFullScreen != -1) {
                newStage.setWidth(widthBeforeFullScreen);
            }
            if (heightBeforeFullScreen != -1) {
                newStage.setHeight(heightBeforeFullScreen);
            }
            newStage.initStyle(StageStyle.DECORATED);
            newStage.setMaximized(WAS_MAXIMISED);
        }
        if (!appStart) {
            clearStage(primaryStage);
            initPrimaryStage(newStage, primaryStage.getScene(), appStart);
        }
        openStage(newStage, appStart);
    }

    private void initXboxController(Stage primaryStage) {
        try {
            Robot r = new Robot();
            xboxController = new XboxController(new ControllerButtonListener() {
                @Override
                public void onButtonPressed(String buttonId) {
                    switch (buttonId) {
                        case XboxController.BUTTON_A:
                            r.keyPress(java.awt.event.KeyEvent.VK_ENTER);
                            break;
                        case XboxController.BUTTON_B:
                            r.keyPress(java.awt.event.KeyEvent.VK_ESCAPE);
                            break;
                        case XboxController.BUTTON_X:
                            r.keyPress(KeyEvent.VK_SPACE);
                            break;
                        case XboxController.BUTTON_Y:
                            r.keyPress(java.awt.event.KeyEvent.VK_I);
                            break;
                        case XboxController.BUTTON_DPAD_UP:
                            r.keyPress(java.awt.event.KeyEvent.VK_UP);
                            break;
                        case XboxController.BUTTON_DPAD_LEFt:
                            r.keyPress(java.awt.event.KeyEvent.VK_LEFT);
                            break;
                        case XboxController.BUTTON_DPAD_DOWN:
                            r.keyPress(java.awt.event.KeyEvent.VK_DOWN);
                            break;
                        case XboxController.BUTTON_DPAD_RIGHT:
                            r.keyPress(java.awt.event.KeyEvent.VK_RIGHT);
                            break;
                        default:
                            break;
                    }

                }

                @Override
                public void onButtonReleased(String buttonId) {

                }
            });
            if (Main.GENERAL_SETTINGS.getBoolean(PredefinedSetting.ENABLE_XBOX_CONTROLLER_SUPPORT)) {
                xboxController.startThreads();
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
        FileUtils.clearFolder(Main.FILES_MAP.get("cache"));
        GENERAL_SETTINGS.saveSettings();

        System.exit(0);
    }

    private void initTrayIcon() {
        //Check the SystemTray is supported
        if (!SystemTray.isSupported()) {
            Main.LOGGER.error("SystemTray not supported");
            return;
        }

        final PopupMenu popup = new PopupMenu();
        Image fxImage = new Image("res/ui/icon/icon16.png");
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
        Platform.setImplicitExit(DEV_MODE);

        MAIN_SCENE.getParentStage().setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                if (event.getEventType().equals(WindowEvent.WINDOW_CLOSE_REQUEST)) {
                    if (!DEV_MODE) {
                        MAIN_SCENE.getParentStage().hide();
                        if (trayMessageCount < 2 && !GENERAL_SETTINGS.getBoolean(PredefinedSetting.NO_MORE_ICON_TRAY_WARNING) && !GENERAL_SETTINGS.getBoolean(PredefinedSetting.NO_NOTIFICATIONS)) {
                            TRAY_ICON.displayMessage("GameRoom"
                                    , RESSOURCE_BUNDLE.getString("tray_icon_still_running"), TrayIcon.MessageType.INFO);
                            trayMessageCount++;
                        } else {
                            if (!GENERAL_SETTINGS.getBoolean(PredefinedSetting.NO_MORE_ICON_TRAY_WARNING)) {
                                GENERAL_SETTINGS.setSettingValue(PredefinedSetting.NO_MORE_ICON_TRAY_WARNING, true);
                            }
                        }
                    }
                }
            }
        });
        final SystemTray tray = SystemTray.getSystemTray();

        // Create a pop-up menu components
        MenuItem openItem = new MenuItem(RESSOURCE_BUNDLE.getString("open"));
        openItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                open(MAIN_SCENE.getParentStage());
            }
        });
        MenuItem gameRoomFolderItem = new MenuItem(RESSOURCE_BUNDLE.getString("gameroom_folder"));
        gameRoomFolderItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Desktop.getDesktop().open(new File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile());
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }
        });
        MenuItem gamesFolderItem = new MenuItem(RESSOURCE_BUNDLE.getString("games_folder"));
        gamesFolderItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String dir = GENERAL_SETTINGS.getString(PredefinedSetting.GAMES_FOLDER);
                    File gamesFolder = new File(dir);
                    if (gamesFolder.exists() && gamesFolder.isDirectory())
                        Desktop.getDesktop().open(gamesFolder);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        MenuItem settingsItem = new MenuItem(RESSOURCE_BUNDLE.getString("Settings"));
        settingsItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MAIN_SCENE.fadeTransitionTo(new SettingsScene(new StackPane(), MAIN_SCENE.getParentStage(), MAIN_SCENE), MAIN_SCENE.getParentStage());
                open(MAIN_SCENE.getParentStage());
            }
        });
        //CheckboxMenuItem cb1 = new CheckboxMenuItem("Set auto size");
        //CheckboxMenuItem cb2 = new CheckboxMenuItem("Set tooltip");
        START_TRAY_MENU.setLabel(RESSOURCE_BUNDLE.getString("start"));
        MenuItem exitItem = new MenuItem(RESSOURCE_BUNDLE.getString("exit"));

        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                forceStop(MAIN_SCENE.getParentStage(), "tray icon exitItem called");
            }
        });

        //Add components to pop-up menu
        popup.add(openItem);
        popup.add(START_TRAY_MENU);
        popup.addSeparator();
        popup.add(gameRoomFolderItem);

        File gameFolder = new File(GENERAL_SETTINGS.getString(PredefinedSetting.GAMES_FOLDER));
        if (gameFolder.exists() && gameFolder.isDirectory()) {
            popup.add(gamesFolderItem);
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
            stage.getIcons().add(new Image("res/ui/icon/icon" + i + ".png"));
        }
    }

    private static void setCurrentProcessExplicitAppUserModelID(final String appID) {
        if (SetCurrentProcessExplicitAppUserModelID(new WString(appID)).longValue() != 0)
            throw new RuntimeException("unable to set current process explicit AppUserModelID to: " + appID);
    }

    private static native NativeLong SetCurrentProcessExplicitAppUserModelID(WString appID);

    static {
        Native.register("shell32");
    }

}

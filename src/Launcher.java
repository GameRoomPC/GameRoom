import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.WString;
import data.game.scrapper.IGDBScrapper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import system.application.settings.PredefinedSetting;
import system.device.ControllerButtonListener;
import system.device.XboxController;
import ui.Main;
import ui.dialog.ConsoleOutputDialog;
import ui.scene.BaseScene;
import ui.scene.MainScene;
import ui.scene.SettingsScene;

import java.awt.*;
import java.awt.MenuItem;
import java.awt.event.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;

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

    public static void main(String[] args) throws URISyntaxException {
        setCurrentProcessExplicitAppUserModelID("GameRoom");

        System.setErr(new PrintStream(System.err){
            public void print(final String string) {
                //System.err.print(string);
                LOGGER.error(string);
                if(DEV_MODE || GENERAL_SETTINGS.getBoolean(PredefinedSetting.DEBUG_MODE)) {
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
        System.setOut(new PrintStream(System.out){
            public void print(final String string) {
                //System.out.print(string);
                LOGGER.debug(string);
            }
        });

        System.out.println("\n\n==========================================NEW START============================================");

        Main.LOGGER.debug("Received args : ");
        for(String arg : args){
            Main.LOGGER.debug("\t\""+arg+"\"");
        }

        Main.DEV_MODE = getArg(ARGS_FLAG_DEV,args,false) != null;

        String igdbKey = getArg(ARGS_FLAG_IGDB_KEY,args,true);
        if(igdbKey!=null){
            IGDBScrapper.IGDB_BASIC_KEY = igdbKey;
            IGDBScrapper.IGDB_PRO_KEY = igdbKey;
            IGDBScrapper.key = igdbKey;
        }
        String showMode = getArg(ARGS_FLAG_SHOW,args,true);
        if(showMode!=null){
            switch (showMode){
                case "0" :
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



    @Override
    public void start(Stage primaryStage) throws Exception {
        MAIN_SCENE = new MainScene(primaryStage);
        initPrimaryStage(primaryStage,MAIN_SCENE,true);
        initTrayIcon();
        initXboxController(primaryStage);
        setFullScreen(primaryStage,GENERAL_SETTINGS.getBoolean(PredefinedSetting.FULL_SCREEN),true);
    }
    private void openStage(Stage primaryStage, boolean appStart){
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
        });
    }
    private void initPrimaryStage(Stage primaryStage, Scene initScene, boolean appStart){
        initIcons(primaryStage);
        primaryStage.setTitle("GameRoom");
        primaryStage.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                MAIN_SCENE.setChangeBackgroundNextTime(true);
                primaryStage.getScene().getRoot().setMouseTransparent(!newValue);

                if(newValue && Main.GENERAL_SETTINGS.getBoolean(PredefinedSetting.ENABLE_XBOX_CONTROLLER_SUPPORT)){
                    xboxController.startThreads();
                }else if(!newValue && Main.GENERAL_SETTINGS.getBoolean(PredefinedSetting.ENABLE_XBOX_CONTROLLER_SUPPORT)){
                    xboxController.stopThreads();
                }
            }
        });
        primaryStage.setScene(initScene);
        primaryStage.setFullScreenExitHint("");
        primaryStage.setFullScreenExitKeyCombination(null);
        MAIN_SCENE.setParentStage(primaryStage);
        if(initScene instanceof BaseScene){
            ((BaseScene)initScene).setParentStage(primaryStage);
        }
        primaryStage.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, new EventHandler<javafx.scene.input.KeyEvent>() {
            @Override
            public void handle(javafx.scene.input.KeyEvent event) {
                if(event.getCode()==KeyCode.F11){
                    setFullScreen(primaryStage,!GENERAL_SETTINGS.getBoolean(PredefinedSetting.FULL_SCREEN),false);
                }
            }
        });
    }
    private void setFullScreen(Stage primaryStage,boolean fullScreen, boolean appStart){
        GENERAL_SETTINGS.setSettingValue(PredefinedSetting.FULL_SCREEN,fullScreen);
        if(!appStart) {
            primaryStage.close();
        }
        Stage newStage = new Stage();
        if(appStart){
            newStage = primaryStage;
        }
        newStage.setFullScreen(fullScreen);
        if(fullScreen){
            widthBeforeFullScreen = GENERAL_SETTINGS.getWindowWidth();
            heightBeforeFullScreen = GENERAL_SETTINGS.getWindowHeight();
            newStage.setWidth(Main.SCREEN_WIDTH);
            newStage.setHeight(Main.SCREEN_HEIGHT);
            newStage.initStyle(StageStyle.UNDECORATED);
        }else{
            if(widthBeforeFullScreen!=-1){
                newStage.setWidth(widthBeforeFullScreen);
            }
            if(heightBeforeFullScreen!=-1){
                newStage.setHeight(heightBeforeFullScreen);
            }
            newStage.initStyle(StageStyle.DECORATED);
        }
        if(!appStart) {
            initPrimaryStage(newStage, primaryStage.getScene(), appStart);
        }
        openStage(newStage,appStart);
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
            if(Main.GENERAL_SETTINGS.getBoolean(PredefinedSetting.ENABLE_XBOX_CONTROLLER_SUPPORT)) {
                xboxController.startThreads();
            }

        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        Main.LOGGER.info("Closing app, saving settings.");
        for (int i = 0; i < CACHE_FOLDER.listFiles().length; i++) {
            File temp = CACHE_FOLDER.listFiles()[i];
            temp.delete();
        }
        GENERAL_SETTINGS.saveSettings();

        System.exit(0);
    }
    private void initTrayIcon(){
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
                                GENERAL_SETTINGS.setSettingValue(PredefinedSetting.NO_MORE_ICON_TRAY_WARNING,true);
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
                    if(gamesFolder.exists() && gamesFolder.isDirectory())
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
        if( gameFolder.exists() && gameFolder.isDirectory()){
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

    public static void setCurrentProcessExplicitAppUserModelID(final String appID) {
        if (SetCurrentProcessExplicitAppUserModelID(new WString(appID)).longValue() != 0)
            throw new RuntimeException("unable to set current process explicit AppUserModelID to: " + appID);
    }

    private static native NativeLong SetCurrentProcessExplicitAppUserModelID(WString appID);

    static {
        Native.register("shell32");
    }

}

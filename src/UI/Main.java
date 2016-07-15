package ui;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.WindowEvent;
import ui.scene.MainScene;
import data.game.AllGameEntries;
import system.application.GeneralSettings;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ui.scene.SettingsScene;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;

public class Main extends Application {
    public static double SCREEN_WIDTH;
    public static double SCREEN_HEIGHT;

    public static MainScene MAIN_SCENE;

    public final static String DEFAULT_IMAGES_PATH = "res/defaultImages/";

    public static ResourceBundle RESSOURCE_BUNDLE;
    public static AllGameEntries ALL_GAMES_ENTRIES = new AllGameEntries();
    public static GeneralSettings GENERAL_SETTINGS;

    public static final Logger logger = LogManager.getLogger(Main.class);
    public static final File CACHE_FOLDER = new File("cache");

    public static Menu START_TRAY_MENU = new Menu();
    public static TrayIcon TRAY_ICON;

    private int trayMessageCount = 0;

    //transition time in seconds

    @Override
    public void start(Stage primaryStage) throws Exception{
        MAIN_SCENE = new MainScene(primaryStage);
        initIcons(primaryStage);

        primaryStage.setTitle("GameRoom");
        primaryStage.setScene(MAIN_SCENE);
        primaryStage.setFullScreen(GENERAL_SETTINGS.isFullScreen());
        primaryStage.show();
        Platform.runLater(() -> {
            primaryStage.setWidth(primaryStage.getWidth());
            primaryStage.setHeight(primaryStage.getHeight());
        });
    }
    @Override
    public void stop(){
        logger.info("Closing app, saving settings.");
        for(int i = 0; i< CACHE_FOLDER.listFiles().length; i++){
            File temp = CACHE_FOLDER.listFiles()[i];
            temp.delete();
        }
        GENERAL_SETTINGS.saveSettings();

        System.exit(0);
    }


    public static void main(String[] args) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Main.SCREEN_WIDTH = (int) screenSize.getWidth();
        Main.SCREEN_HEIGHT = (int) screenSize.getHeight();

        logger.info("Started app with resolution : "+(int) SCREEN_WIDTH +"x"+(int) SCREEN_HEIGHT);
        GENERAL_SETTINGS = new GeneralSettings();
        RESSOURCE_BUNDLE = ResourceBundle.getBundle("strings", Locale.forLanguageTag(GENERAL_SETTINGS.getLocale()));

        CACHE_FOLDER.mkdirs();
        launch(args);
    }
    public static void forceStop(Stage stage){
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
    public static void open(Stage stage){
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                stage.show();
            }
        });
    }
    private void initIcons(Stage stage){

        for(int i = 32; i<513;i*=2){
            stage.getIcons().add(new Image("res/ui/icon/icon"+i+".png"));
        }

        //Check the SystemTray is supported
        if (!SystemTray.isSupported()) {
            Main.logger.error("SystemTray not supported");
            return;
        }

        final PopupMenu popup = new PopupMenu();
        Image fxImage = new Image("res/ui/icon/icon16.png");
        TRAY_ICON = new TrayIcon(SwingFXUtils.fromFXImage(fxImage,null));
        TRAY_ICON.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    open(stage);
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
        Platform.setImplicitExit(!GENERAL_SETTINGS.isAlwaysInBackground());
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                if(GENERAL_SETTINGS.isAlwaysInBackground()) {
                    if (event.getEventType().equals(WindowEvent.WINDOW_CLOSE_REQUEST)) {
                        stage.hide();
                        if (trayMessageCount < 2 && !GENERAL_SETTINGS.isNoMoreTrayMessage()) {
                            TRAY_ICON.displayMessage("GameRoom"
                                    , RESSOURCE_BUNDLE.getString("tray_icon_still_running_1")
                                            + RESSOURCE_BUNDLE.getString("always_in_background")
                                            + RESSOURCE_BUNDLE.getString("tray_icon_still_running_2"), TrayIcon.MessageType.INFO);
                            trayMessageCount++;
                        } else {
                            if (!GENERAL_SETTINGS.isNoMoreTrayMessage()) {
                                GENERAL_SETTINGS.setNoMoreTrayMessage(true);
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
                open(stage);
            }
        });
        MenuItem settingsItem = new MenuItem(RESSOURCE_BUNDLE.getString("Settings"));
        settingsItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MAIN_SCENE.fadeTransitionTo(new SettingsScene(new StackPane(),stage,MAIN_SCENE),stage);
                open(stage);
            }
        });
        //CheckboxMenuItem cb1 = new CheckboxMenuItem("Set auto size");
        //CheckboxMenuItem cb2 = new CheckboxMenuItem("Set tooltip");
        START_TRAY_MENU.setLabel(RESSOURCE_BUNDLE.getString("start"));
        MenuItem exitItem = new MenuItem(RESSOURCE_BUNDLE.getString("exit"));

        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               forceStop(stage);
            }
        });

        //Add components to pop-up menu
        popup.add(openItem);
        popup.add(START_TRAY_MENU);
        popup.addSeparator();
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
}

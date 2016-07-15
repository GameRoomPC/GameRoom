package ui;

import data.game.GameEntry;
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

public class Main{
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

    public static void main(String[] args) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Main.SCREEN_WIDTH = (int) screenSize.getWidth();
        Main.SCREEN_HEIGHT = (int) screenSize.getHeight();

        logger.info("Started app with resolution : "+(int) SCREEN_WIDTH +"x"+(int) SCREEN_HEIGHT);
        GENERAL_SETTINGS = new GeneralSettings();
        RESSOURCE_BUNDLE = ResourceBundle.getBundle("strings", Locale.forLanguageTag(GENERAL_SETTINGS.getLocale()));

        CACHE_FOLDER.mkdirs();
        GameEntry.ENTRIES_FOLDER.mkdirs();
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

}

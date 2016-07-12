package UI;

import UI.scene.MainScene;
import data.AllGameEntries;
import data.GeneralSettings;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
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

    //transition time in seconds
    public final static double FADE_IN_OUT_TIME = 0.1;

    @Override
    public void start(Stage primaryStage) throws Exception{
        MAIN_SCENE = new MainScene(primaryStage);

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
        GENERAL_SETTINGS.saveSettings();
    }


    public static void main(String[] args) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Main.SCREEN_WIDTH = (int) screenSize.getWidth();
        Main.SCREEN_HEIGHT = (int) screenSize.getHeight();

        logger.info("Started app with resolution : "+(int) SCREEN_WIDTH +"x"+(int) SCREEN_HEIGHT);
        GENERAL_SETTINGS = new GeneralSettings();
        RESSOURCE_BUNDLE = ResourceBundle.getBundle("strings", Locale.forLanguageTag(GENERAL_SETTINGS.getLocale()));

        launch(args);
    }
}

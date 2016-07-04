package UI;

import UI.scene.MainScene;
import data.AllGameEntries;
import data.GeneralSettings;
import javafx.application.Application;
import javafx.stage.Stage;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.Locale;
import java.util.ResourceBundle;

public class Main extends Application {
    public static int WIDTH;
    public static int HEIGHT;

    public final static String DEFAULT_IMAGES_PATH = "res/defaultImages/";

    public static ResourceBundle RESSOURCE_BUNDLE;
    public static AllGameEntries ALL_GAMES_ENTRIES = new AllGameEntries();
    public static final GeneralSettings GENERAL_SETTINGS = new GeneralSettings();

    //transition time in seconds
    public final static double FADE_IN_OUT_TIME = 0.1;

    @Override
    public void start(Stage primaryStage) throws Exception{
        MainScene scene = new MainScene(WIDTH, HEIGHT,primaryStage);

        primaryStage.setTitle("GameRoom");
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    public static void main(String[] args) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        WIDTH = (int) screenSize.getWidth();
        HEIGHT = (int) screenSize.getHeight();
        RESSOURCE_BUNDLE = ResourceBundle.getBundle("strings", Locale.forLanguageTag(GENERAL_SETTINGS.getLocale()));

        launch(args);
    }
}

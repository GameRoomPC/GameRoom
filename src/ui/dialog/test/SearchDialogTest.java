package ui.dialog.test;

import data.game.GameEntry;
import system.application.settings.GeneralSettings;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import system.application.settings.PredefinedSetting;
import ui.dialog.SearchDialog;

import java.awt.*;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

import static ui.Main.*;

/**
 * Created by LM on 12/07/2016.
 */
public class SearchDialogTest extends Application {

    private static StackPane contentPane;
    private static Scene scene;


    public static void initScene() {
        Button launchButton = new Button("Launch");

        SearchDialog dialog = new SearchDialog();

        launchButton.setOnAction(e -> {
                    Optional<GameEntry> result = dialog.showAndWait();
                    result.ifPresent(letter -> {
                        System.out.println("Choice : " + dialog.getResult());
                    });
                }
        );
        //dialog.setHeader("This is a test header");
        contentPane.getChildren().addAll(launchButton);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        contentPane = new StackPane();
        scene = new Scene(contentPane, 640, 480);
        initScene();
        primaryStage.setTitle("UITest");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        SCREEN_WIDTH = (int) screenSize.getWidth();
        SCREEN_HEIGHT = (int) screenSize.getHeight();
        GENERAL_SETTINGS = new GeneralSettings();
        RESSOURCE_BUNDLE = ResourceBundle.getBundle("strings", GENERAL_SETTINGS.getLocale(PredefinedSetting.LOCALE));


        if(!CACHE_FOLDER.exists()){
            CACHE_FOLDER.mkdirs();
        }

        launch(args);
    }
}
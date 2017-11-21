package com.gameroom.ui.dialog.test;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import com.gameroom.system.application.settings.PredefinedSetting;
import com.gameroom.ui.Main;
import com.gameroom.ui.dialog.SearchDialog;

import java.awt.*;
import java.util.Optional;
import java.util.ResourceBundle;

import static com.gameroom.system.application.settings.GeneralSettings.settings;
import static com.gameroom.ui.Main.*;

/**
 * Created by LM on 12/07/2016.
 */
public class SearchDialogTest extends Application {

    private static StackPane contentPane;
    private static Scene scene;


    private static void initScene() {
        Button launchButton = new Button("Launch");

        SearchDialog dialog = new SearchDialog("test");

        launchButton.setOnAction(e -> {
                    Optional<ButtonType> result = dialog.showAndWait();
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
        Main.setRessourceBundle(ResourceBundle.getBundle("strings", settings().getLocale(PredefinedSetting.LOCALE)));

        launch(args);
    }
}
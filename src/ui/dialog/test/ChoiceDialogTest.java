package ui.dialog.test;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import system.application.settings.PredefinedSetting;
import ui.Main;
import ui.dialog.ChoiceDialog;

import java.awt.*;
import java.util.Optional;
import java.util.ResourceBundle;

import static ui.Main.*;

/**
 * Created by LM on 05/07/2016.
 */

/**
 * Created by LM on 05/07/2016.
 */
public class ChoiceDialogTest extends Application {

    private static StackPane contentPane;
    private static Scene scene;


    private static void initScene() {
        Button launchButton = new Button("Launch");


        ChoiceDialog choiceDialog = new ChoiceDialog(
                new ChoiceDialog.ChoiceDialogButton("This is a test", "More specifically this is really a test, I hope that it will work"),
                new ChoiceDialog.ChoiceDialogButton("This is an other test", "I hope that it will work, blablablablabnalablablabalm, more specifically this is really a test."));
        choiceDialog.setResizable(true);
        launchButton.setOnAction(e -> {
                    Optional<ButtonType> result = choiceDialog.showAndWait();
                    result.ifPresent(letter -> {
                        System.out.println("Choice : " + letter.getText());
                    });
                }
        );
        choiceDialog.setHeader("This is a test header");
        contentPane.getChildren().addAll(launchButton);

    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        contentPane = new StackPane();
        scene = new Scene(contentPane, SCREEN_WIDTH, SCREEN_HEIGHT);
        initScene();
        primaryStage.setTitle("UITest");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        SCREEN_WIDTH = (int) 1920;
        SCREEN_HEIGHT = (int) 1080;
        Main.setRessourceBundle(ResourceBundle.getBundle("strings", GENERAL_SETTINGS.getLocale(PredefinedSetting.LOCALE)));
        launch(args);
    }
}
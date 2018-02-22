package com.gameroom.ui.dialog.test;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.*;

import static com.gameroom.ui.Main.SCREEN_HEIGHT;
import static com.gameroom.ui.Main.SCREEN_WIDTH;

/**
 * Created by LM on 06/01/2017.
 */
public class FileChooserTest extends Application {
    private static Scene scene;

    public static void main(String[] args) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        SCREEN_WIDTH = (int) 1920;
        SCREEN_HEIGHT = (int) 1080;

        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        StackPane scenePane = new StackPane();
        scene = new Scene(scenePane, SCREEN_WIDTH, SCREEN_HEIGHT);
        primaryStage.setTitle("UITest");
        primaryStage.setScene(scene);
        primaryStage.show();

        initDialog();
    }


    private static void initDialog() {
        DirectoryChooser chooser = new DirectoryChooser();

        Button testButton = new Button("Test");
        testButton.setOnAction((e) -> chooser.showDialog(scene.getWindow()));

        Dialog dialog = new Dialog();
        dialog.getDialogPane().setContent(testButton);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(scene.getWindow());
        dialog.showAndWait();
    }
}

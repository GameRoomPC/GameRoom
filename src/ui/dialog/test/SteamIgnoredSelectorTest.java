package ui.dialog.test;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import ui.Main;
import ui.dialog.selector.SteamIgnoredSelector;

import java.awt.*;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

import static ui.Main.*;

/**
 * Created by LM on 09/08/2016.
 */
public class SteamIgnoredSelectorTest extends Application {

    private static StackPane contentPane;
    private static Scene scene;


    private static void initScene() throws IOException {
        Button launchButton = new Button("Launch");

        SteamIgnoredSelector selector = new SteamIgnoredSelector();

        launchButton.setOnAction(e -> {
            Optional<ButtonType> ignoredOptionnal = selector.showAndWait();
            ignoredOptionnal.ifPresent(pairs -> {
                if(pairs.getButtonData().equals(ButtonBar.ButtonData.OK_DONE)) {
                    System.out.println(selector.getSelectedEntries());
                }
            });
                }
        );
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
        SCREEN_WIDTH = 1920;
        SCREEN_HEIGHT = 1080;
        Main.setRessourceBundle(ResourceBundle.getBundle("strings", Locale.getDefault()));

        System.setErr(new PrintStream(System.err){
            public void print(final String string) {
                //System.err.print(string);
                LOGGER.error(string);
            }
        });
        System.setOut(new PrintStream(System.out){
            public void print(final String string) {
                //System.out.print(string);
                LOGGER.debug(string);
            }
        });
        launch(args);
    }
}

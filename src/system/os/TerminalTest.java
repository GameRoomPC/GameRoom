package system.os;

import system.application.GeneralSettings;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import ui.Main;

import java.awt.*;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

import static ui.Main.*;

/**
 * Created by LM on 12/07/2016.
 */
public class TerminalTest extends Application {

    private static StackPane contentPane;
    private static Scene scene;


    public static void initScene() {
        Button launchButton = new Button("Launch");

        Terminal terminal = new Terminal();


        launchButton.setOnAction(e -> {
                    try {
                        String[] result = terminal.execute("powercfg", "-list");
                        for(String s : result){
                            Main.logger.debug("[cmd powercfg] "+s);
                        }
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
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
        SCREEN_WIDTH = (int) screenSize.getWidth();
        SCREEN_HEIGHT = (int) screenSize.getHeight();
        GENERAL_SETTINGS = new GeneralSettings();
        RESSOURCE_BUNDLE = ResourceBundle.getBundle("strings", Locale.forLanguageTag(GENERAL_SETTINGS.getLocale()));


        if(!CACHE_FOLDER.exists()){
            CACHE_FOLDER.mkdirs();
        }

        launch(args);
    }
}
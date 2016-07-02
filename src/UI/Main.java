package UI;

import UI.gamebuttons.GameButton;
import UI.scene.GameRoomScene;
import UI.scene.MainScene;
import UI.scene.SettingsScene;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Dimension;
import java.awt.Toolkit;

import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;

public class Main extends Application {
    public static int WIDTH;
    public static int HEIGHT;

    public final static double BUTTONS_SCALE_EFFECT_FACTOR = 1.05;
    public final static double BUTTONS_BLOOM_EFFECT_RADIUS = 0;
    public final static double BUTTONS_BRIGHTNESS_EFFECT_FACTOR = 0.8;

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
        launch(args);
    }
    public static void addEffectsToButton(ImageView button){
        ColorAdjust buttonColorAdjust = new ColorAdjust();
        buttonColorAdjust.setBrightness(0.0);
        Bloom buttonBloom = new Bloom(1);
        buttonBloom.setInput(buttonColorAdjust);
        button.setEffect(buttonBloom);

        button.setOnMouseEntered(e -> {
            Timeline fadeInTimeline = new Timeline(
                    new KeyFrame(Duration.seconds(0),
                            new KeyValue(buttonBloom.thresholdProperty(), buttonBloom.thresholdProperty().getValue(), Interpolator.LINEAR),
                            new KeyValue(button.scaleXProperty(), button.scaleXProperty().getValue(), Interpolator.LINEAR),
                            new KeyValue(button.scaleYProperty(), button.scaleYProperty().getValue(), Interpolator.LINEAR),
                            new KeyValue(buttonColorAdjust.brightnessProperty(), buttonColorAdjust.brightnessProperty().getValue(), Interpolator.LINEAR)),
                    new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                            new KeyValue(buttonBloom.thresholdProperty(), BUTTONS_BLOOM_EFFECT_RADIUS, Interpolator.LINEAR),
                            new KeyValue(button.scaleXProperty(), BUTTONS_SCALE_EFFECT_FACTOR, Interpolator.LINEAR),
                            new KeyValue(button.scaleYProperty(), BUTTONS_SCALE_EFFECT_FACTOR, Interpolator.LINEAR),
                            new KeyValue(buttonColorAdjust.brightnessProperty(), BUTTONS_BRIGHTNESS_EFFECT_FACTOR, Interpolator.LINEAR)
                    ));
            fadeInTimeline.setCycleCount(1);
            fadeInTimeline.setAutoReverse(false);

            fadeInTimeline.play();
        });
        button.setOnMouseExited(e -> {
            Timeline fadeOutTimeline = new Timeline(
                    new KeyFrame(Duration.seconds(0),
                            new KeyValue(buttonBloom.thresholdProperty(), buttonBloom.thresholdProperty().getValue(), Interpolator.LINEAR),
                            new KeyValue(button.scaleXProperty(), button.scaleXProperty().getValue(), Interpolator.LINEAR),
                            new KeyValue(button.scaleYProperty(), button.scaleYProperty().getValue(), Interpolator.LINEAR),
                            new KeyValue(buttonColorAdjust.brightnessProperty(), buttonColorAdjust.brightnessProperty().getValue(), Interpolator.LINEAR)),
                    new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                            new KeyValue(buttonBloom.thresholdProperty(), 1, Interpolator.LINEAR),
                            new KeyValue(button.scaleXProperty(), 1, Interpolator.LINEAR),
                            new KeyValue(button.scaleYProperty(), 1, Interpolator.LINEAR),
                            new KeyValue(buttonColorAdjust.brightnessProperty(), 0, Interpolator.LINEAR)
                    ));
            fadeOutTimeline.setCycleCount(1);
            fadeOutTimeline.setAutoReverse(false);

            fadeOutTimeline.play();
        });
    }
}

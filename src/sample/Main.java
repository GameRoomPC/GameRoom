package sample;

import UI.gamebuttons.GameButton;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Dimension;
import java.awt.Toolkit;

public class Main extends Application {
    public static int WIDTH;
    public static int HEIGHT;

    public final static double MAX_SCALE_FACTOR = 0.9;
    public final static double MIN_SCALE_FACTOR = 0.1;

    public final static double COVER_HEIGHT_WIDTH_RATIO = 1.48;

    public final static double BUTTONS_SCALE_EFFECT_FACTOR = 1.05;
    public final static double BUTTONS_BLOOM_EFFECT_RADIUS = 0;
    public final static double BUTTONS_BRIGHTNESS_EFFECT_FACTOR = 0.8;

    //transition time in seconds
    public final static double FADE_IN_OUT_TIME = 0.1;

    @Override
    public void start(Stage primaryStage) throws Exception{
        //primaryStage.setFullScreen(true);
        Button btn = new Button();
        btn.setText("Say 'Hello World'");
        btn.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                System.out.println("Hello World!");
            }
        });

        GameEntry bf4 = new GameEntry("Battlefield 4");
        Image image = new Image("res/bf4.bmp",WIDTH/4,WIDTH/4*COVER_HEIGHT_WIDTH_RATIO,false,true);
        bf4.setImages(new Image[]{image});

        GameEntry tw3 = new GameEntry("The Witcher 3");
        Image image2 = new Image("res/tw3.bmp",WIDTH/4,WIDTH/4*COVER_HEIGHT_WIDTH_RATIO,false,true);
        tw3.setImages(new Image[]{image2});

        GameEntry gtav = new GameEntry("GTA V");
        Image image3 = new Image("res/gtav.bmp",WIDTH/4,WIDTH/4*COVER_HEIGHT_WIDTH_RATIO,false,true);
        gtav.setImages(new Image[]{image3});

        TilePane tilePane = new TilePane();
        tilePane.setHgap(30);
        tilePane.setVgap(30);
        tilePane.setPrefColumns(5);
        tilePane.setPrefTileWidth(Main.WIDTH/4);
        tilePane.setPrefTileHeight(tilePane.getPrefTileWidth()*COVER_HEIGHT_WIDTH_RATIO);

        tilePane.getChildren().add(new GameButton(bf4, tilePane));
        tilePane.getChildren().add(new GameButton(tw3, tilePane));
        tilePane.getChildren().add(new GameButton(gtav, tilePane));

        Slider sizeSlider = new Slider();
        sizeSlider.setMin(MIN_SCALE_FACTOR);
        sizeSlider.setMax(MAX_SCALE_FACTOR);
        sizeSlider.setBlockIncrement(0.1);

        sizeSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                /*for(Node node : grid.getChildren()){
                    node.setScaleX(newValue.doubleValue());
                    node.setScaleY(newValue.doubleValue());
                }*/
                tilePane.setPrefTileWidth(Main.WIDTH/4*newValue.doubleValue());
                tilePane.setPrefTileHeight(Main.WIDTH/4*COVER_HEIGHT_WIDTH_RATIO*newValue.doubleValue());
            }
        });
        sizeSlider.setValue(0.4);

        sizeSlider.setPrefWidth(Main.WIDTH/4);
        sizeSlider.setMaxWidth(Main.WIDTH/4);

        HBox hbox = new HBox();
        hbox.setPadding(new Insets(15, 12, 15, 12));
        hbox.setSpacing(10);
        hbox.getChildren().add(sizeSlider);
        HBox.setMargin(sizeSlider, new Insets(15, 12, 15, 12));

        BorderPane root = new BorderPane();
        root.setTop(hbox);
        root.setCenter(tilePane);
        BorderPane.setMargin(tilePane, new Insets(50,50,50,50));

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.getStylesheets().add("res/flatterfx.css");

        //Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        tilePane.requestFocus();
        primaryStage.setTitle("Hello World");
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

package ui.control.button;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.control.Button;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import java.util.ArrayList;

import static ui.Main.GENERAL_SETTINGS;

/**
 * Created by LM on 03/07/2016.
 */
public class ImageButton extends Button {
    private final static double BUTTONS_SCALE_EFFECT_FACTOR = 1.05;
    private final static double BUTTONS_BLOOM_EFFECT_RADIUS = 0;
    private final static double BUTTONS_BRIGHTNESS_EFFECT_FACTOR = 0.8;

    private final static double FADE_IN_OUT_TIME = 0.1;

    private ImageView view;

    private ArrayList<MouseEnteredHandler> mouseEnteredHandlers = new ArrayList<>();
    private ArrayList<MouseExitedHandler> mouseExitedHandlers = new ArrayList<>();

    /*public ImageButton(Image image) {
        super();
        if(image!=null){
            view = new ImageView(image);
        }else{
            view = new ImageView();
        }
        view.setPreserveRatio(true);
        view.setVisible(true);
        setGraphic(view);
        setStyle("-fx-background-color: transparent;");
        addEffectsToButton(view);
    }*/
    public ImageButton(String cssId, double width, double height) {
        super();

        view = new ImageView();
        view.setSmooth(true);
        double scale = GENERAL_SETTINGS.getUIScale().getScale();
        view.setFitWidth(width*scale);
        view.setFitHeight(height*scale);

        view.setId(cssId);
        view.setPreserveRatio(true);
        view.setVisible(true);

        setGraphic(view);
        setStyle("-fx-background-color: transparent;");
        addEffectsToButton(view);
    }
    private void addEffectsToButton(ImageView buttonView) {
        ColorAdjust buttonColorAdjust = new ColorAdjust();
        buttonColorAdjust.setBrightness(0.0);
        Bloom buttonBloom = new Bloom(1);
        buttonBloom.setInput(buttonColorAdjust);
        buttonView.setEffect(buttonBloom);

        setOnMouseEntered(me ->{
            for(MouseEnteredHandler meh : mouseEnteredHandlers){
                meh.handle(me);
            }
        });
        setOnMouseExited(me -> {
            for(MouseExitedHandler meh : mouseExitedHandlers){
                meh.handle(me);
            }
        });
        addMouseEnteredHandler(new MouseEnteredHandler() {
            @Override
            public void handle(MouseEvent me) {
                Timeline fadeInTimeline = new Timeline(
                        new KeyFrame(Duration.seconds(0),
                                new KeyValue(buttonBloom.thresholdProperty(), buttonBloom.thresholdProperty().getValue(), Interpolator.LINEAR),
                                new KeyValue(buttonView.scaleXProperty(), buttonView.scaleXProperty().getValue(), Interpolator.LINEAR),
                                new KeyValue(buttonView.scaleYProperty(), buttonView.scaleYProperty().getValue(), Interpolator.LINEAR),
                                new KeyValue(buttonColorAdjust.brightnessProperty(), buttonColorAdjust.brightnessProperty().getValue(), Interpolator.LINEAR)),
                        new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                                new KeyValue(buttonBloom.thresholdProperty(), BUTTONS_BLOOM_EFFECT_RADIUS, Interpolator.LINEAR),
                                new KeyValue(buttonView.scaleXProperty(), BUTTONS_SCALE_EFFECT_FACTOR, Interpolator.LINEAR),
                                new KeyValue(buttonView.scaleYProperty(), BUTTONS_SCALE_EFFECT_FACTOR, Interpolator.LINEAR),
                                new KeyValue(buttonColorAdjust.brightnessProperty(), BUTTONS_BRIGHTNESS_EFFECT_FACTOR, Interpolator.LINEAR)
                        ));
                fadeInTimeline.setCycleCount(1);
                fadeInTimeline.setAutoReverse(false);

                fadeInTimeline.play();
            }
        });
        addMouseExitedHandler(new MouseExitedHandler() {
            @Override
            public void handle(MouseEvent me) {
                Timeline fadeOutTimeline = new Timeline(
                        new KeyFrame(Duration.seconds(0),
                                new KeyValue(buttonBloom.thresholdProperty(), buttonBloom.thresholdProperty().getValue(), Interpolator.LINEAR),
                                new KeyValue(buttonView.scaleXProperty(), buttonView.scaleXProperty().getValue(), Interpolator.LINEAR),
                                new KeyValue(buttonView.scaleYProperty(), buttonView.scaleYProperty().getValue(), Interpolator.LINEAR),
                                new KeyValue(buttonColorAdjust.brightnessProperty(), buttonColorAdjust.brightnessProperty().getValue(), Interpolator.LINEAR)),
                        new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                                new KeyValue(buttonBloom.thresholdProperty(), 1, Interpolator.LINEAR),
                                new KeyValue(buttonView.scaleXProperty(), 1, Interpolator.LINEAR),
                                new KeyValue(buttonView.scaleYProperty(), 1, Interpolator.LINEAR),
                                new KeyValue(buttonColorAdjust.brightnessProperty(), 0, Interpolator.LINEAR)
                        ));
                fadeOutTimeline.setCycleCount(1);
                fadeOutTimeline.setAutoReverse(false);

                fadeOutTimeline.play();
            }
        });
    }

    public void setFitWidth(double fitWidth) {
        view.setFitWidth(fitWidth);
    }

    public void setFitHeight(double fitHeight) {
        view.setFitHeight(fitHeight);
    }

    public void addMouseEnteredHandler(MouseEnteredHandler meh){
        mouseEnteredHandlers.add(meh);
    }
    public void addMouseExitedHandler(MouseExitedHandler meh){
        mouseExitedHandlers.add(meh);
    }

    void setImage(Image img){
        view.setImage(img);
    }

    protected void setImageViewId(String id){
        view.setId(id);
    }
}


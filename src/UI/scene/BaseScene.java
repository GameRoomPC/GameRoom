package UI.scene;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Created by LM on 03/07/2016.
 */
public abstract class BaseScene extends Scene {
    public final static double FADE_IN_OUT_TIME = 0.1;
    private StackPane rootStackPane;
    private Stage parentStage;

    public BaseScene(StackPane stackPane, int width, int height, Stage parentStage){
        super(stackPane, width, height);
        this.rootStackPane = stackPane;
        this.parentStage = parentStage;
        getStylesheets().add("res/flatterfx.css");
        initAndAddWrappingPaneToRoot();
    }
    public StackPane getRootStackPane(){
        return rootStackPane;
    }

    public void fadeTransitionTo(BaseScene scene2, Stage stage){
        Timeline fadeOutTimeline = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(getWrappingPane().opacityProperty(), getWrappingPane().opacityProperty().getValue(), Interpolator.LINEAR)),
                new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                        new KeyValue(getWrappingPane().opacityProperty(), 0, Interpolator.LINEAR)
                ));
        fadeOutTimeline.setCycleCount(1);
        fadeOutTimeline.setAutoReverse(false);
        fadeOutTimeline.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                scene2.getWrappingPane().setOpacity(0);
                stage.setScene(scene2);
                Timeline fadeInTimeline = new Timeline(
                        new KeyFrame(Duration.seconds(0),
                                new KeyValue(scene2.getWrappingPane().opacityProperty(), 0, Interpolator.LINEAR)),
                        new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                                new KeyValue(scene2.getWrappingPane().opacityProperty(), 1, Interpolator.LINEAR)
                        ));
                fadeInTimeline.setCycleCount(1);
                fadeInTimeline.setAutoReverse(false);
                fadeInTimeline.play();
            }
        });
        fadeOutTimeline.play();
    }

    /**
     *
     * @return wrapping pane, which is just under the root pane
     */
    public abstract Pane getWrappingPane();

    abstract void initAndAddWrappingPaneToRoot();

    public Stage getParentStage() {
        return parentStage;
    }
}

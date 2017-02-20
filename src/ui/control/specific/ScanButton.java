package ui.control.specific;

import data.game.GameWatcher;
import javafx.animation.*;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import ui.control.button.ImageButton;

import static ui.control.button.gamebutton.AddIgnoreGameButton.ROTATION_TIME;

/**
 * Created by LM on 07/01/2017.
 */
public class ScanButton extends ImageButton {
    private final static String CSS_ID = "scan-button";
    private Timeline rotateAnim;

    public ScanButton(double width, double height) {
        super(CSS_ID, width, height);
        setFocusTraversable(false);
        setOnAction(event -> {
            GameWatcher.getInstance().start();
        });
        GameWatcher.getInstance().addOnSearchStartedListener(() -> {
            setMouseTransparent(true);
            rotateAnim.play();
        });
        GameWatcher.getInstance().addOnSearchDoneListener(() -> {
            setMouseTransparent(false);
            rotateAnim.stop();
        });

        rotateAnim = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(rotateProperty(), rotateProperty().getValue(), Interpolator.LINEAR)),
                new KeyFrame(Duration.seconds(ROTATION_TIME),
                        new KeyValue(rotateProperty(), 360, Interpolator.LINEAR)
                ));
        rotateAnim.setCycleCount(Animation.INDEFINITE);
        rotateAnim.setAutoReverse(false);
        rotateAnim.setOnFinished(event -> rotateProperty().setValue(0));
    }
}

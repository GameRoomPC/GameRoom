package ui.control.specific;

import data.game.GameWatcher;
import javafx.animation.*;
import javafx.util.Duration;
import ui.control.drawer.DrawerButton;
import ui.control.drawer.DrawerMenu;

import static ui.control.button.gamebutton.AddIgnoreGameButton.ROTATION_TIME;

/**
 * Created by LM on 07/01/2017.
 */
public class ScanButton extends DrawerButton {
    private final static String CSS_ID = "scan-button";
    private Timeline rotateAnim;

    public ScanButton(DrawerMenu menu) {
        super(CSS_ID, menu);
        setStyle("-fx-background-color: transparent;");
        setFocusTraversable(false);
        setOnAction(event -> {
            GameWatcher.getInstance().start(true);
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

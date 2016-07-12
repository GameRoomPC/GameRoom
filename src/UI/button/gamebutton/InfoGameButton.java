package UI.button.gamebutton;

import UI.scene.BaseScene;
import data.GameEntry;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Pane;

import static UI.Main.GENERAL_SETTINGS;
import static UI.Main.SCREEN_HEIGHT;

/**
 * Created by LM on 12/07/2016.
 */
public class InfoGameButton extends GameButton {
    private final static double RATIO_PLAYBUTTON_COVER = 1 / 3.0;

    public InfoGameButton(GameEntry entry, BaseScene scene, Pane parent) {
        super(entry, scene, parent);
        COVER_SCALE_EFFECT_FACTOR = 1.03;
        disableInfoButton();
        disablePlayTimeLabel();
        disableTitle();
        setOnMouseExited(eh ->{
            setFocused(false);
        });
        setOnMouseEntered(eh ->{
            setFocused(true);
        });
        scene.heightProperty().addListener(cl ->{
            //initAll();
        });
    }

    @Override
    protected int getCoverHeight() {
        return (int) (GENERAL_SETTINGS.getWindowHeight() *2/3);
    }

    @Override
    protected int getCoverWidth() {
        return (int) (GENERAL_SETTINGS.getWindowHeight() *2/(3* COVER_HEIGHT_WIDTH_RATIO));
    }

    @Override
    protected int getInfoButtonHeight() {
        return 1;
    }

    @Override
    protected int getInfoButtonWidth() {
        return 1;
    }

    @Override
    protected int getPlayButtonHeight() {
        return (int) (getCoverHeight()* RATIO_PLAYBUTTON_COVER);
    }

    @Override
    protected int getPlayButtonWidth() {
        return (int) (getCoverWidth()* RATIO_PLAYBUTTON_COVER);
    }
}

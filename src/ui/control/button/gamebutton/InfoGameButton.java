package ui.control.button.gamebutton;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import ui.scene.BaseScene;
import data.game.entry.GameEntry;
import javafx.scene.layout.Pane;

import java.io.File;

import static ui.Main.GENERAL_SETTINGS;

/**
 * Created by LM on 12/07/2016.
 */
public class InfoGameButton extends GameButton {
    public InfoGameButton(GameEntry entry, BaseScene scene, Pane parent) {
        super(entry, scene, parent);
        COVER_SCALE_EFFECT_FACTOR = 1.03;
        disableNode(titleLabel,true);
        disableNode(infoButton,true);
        disableNode(playTimeLabel,true);
        setOnMouseExited(eh -> {
            setFocused(false);
        });
        setOnMouseEntered(eh -> {
            setFocused(true);
        });
        scene.heightProperty().addListener(cl -> {
            //initAll();
        });

        initContentSize(getCoverWidth(),getCoverHeight());
    }

    @Override
    protected int getCoverHeight() {
        return (int) (GENERAL_SETTINGS.getWindowHeight() * 2 / 3);
    }

    @Override
    protected int getCoverWidth() {
        return (int) (GENERAL_SETTINGS.getWindowHeight() * 2 / (3 * COVER_HEIGHT_WIDTH_RATIO));
    }

    @Override
    protected void initCoverView() {
        coverView = new ImageView();
        coverView.setFitWidth(getCoverWidth());
        coverView.setFitHeight(getCoverHeight());
    }

    @Override
    protected void onNewTileWidth(double width) {}

    @Override
    protected void onNewTileHeight(double height) {}

    public void setImage(String imagePath) {
        Image img = new Image("file:" + File.separator + File.separator + File.separator + imagePath, getCoverWidth(), getCoverHeight(), false, true);
        coverView.setImage(img);
    }

    public Image getImage() {
        return coverView.getImage();
    }
}

package ui.control.button.gamebutton;

import data.game.entry.GameEntry;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import ui.scene.BaseScene;

import static system.application.settings.GeneralSettings.settings;

/**
 * Created by LM on 12/07/2016.
 */
public class InfoGameButton extends GameButton {
    public InfoGameButton(GameEntry entry, BaseScene scene, Pane parent) {
        super(entry, scene, parent);
        COVER_SCALE_EFFECT_FACTOR = 1.03;
        disableNode(titleBox,true);
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
        return settings().getWindowHeight() * 2 / 3;
    }

    @Override
    protected int getCoverWidth() {
        return (int) (settings().getWindowHeight() * 2 / (3 * COVER_HEIGHT_WIDTH_RATIO));
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

    public void setImage(Image imageFile) {
        coverView.setImage(imageFile);
    }

    public Image getImage() {
        return coverView.getImage();
    }
}

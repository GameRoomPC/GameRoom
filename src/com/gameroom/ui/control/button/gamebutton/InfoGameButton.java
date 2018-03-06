package com.gameroom.ui.control.button.gamebutton;

import com.gameroom.data.game.entry.GameEntry;
import com.gameroom.ui.Main;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import com.gameroom.ui.scene.BaseScene;

import static com.gameroom.system.application.settings.GeneralSettings.settings;

/**
 * Created by LM on 12/07/2016.
 */
public class InfoGameButton extends GameButton {
    private int width;
    private int height;

    public InfoGameButton(GameEntry entry, BaseScene scene, Pane parent) {
        super(entry, scene, parent);
        COVER_SCALE_EFFECT_FACTOR = 1.03;
        disableNode(titleBox);
        disableNode(infoButton);
        disableNode(playTimeLabel);
        setOnMouseExited(eh -> {
            setFocused(false);
        });
        setOnMouseEntered(eh -> {
            setFocused(true);
        });
        scene.heightProperty().addListener(cl -> {
            height = (int) (scene.getParentStage().getHeight() * 2/ 3);
            width = (int) (height / COVER_HEIGHT_WIDTH_RATIO);
            setHeight(height);
            setWidth(width);
        });

        height = (int) (scene.getParentStage().getHeight() * 2/ 3);
        width = (int) (height / COVER_HEIGHT_WIDTH_RATIO);

        initContentSize(getCoverWidth(),getCoverHeight());
    }

    @Override
    protected int getCoverWidth() {
        return width;
    }

    @Override
    protected int getCoverHeight() {
        return height;
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

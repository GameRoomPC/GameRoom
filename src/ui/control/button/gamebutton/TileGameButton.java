package ui.control.button.gamebutton;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import ui.scene.BaseScene;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.TilePane;
import data.game.GameEntry;

import static ui.Main.*;

/**
 * Created by LM on 02/07/2016.
 */
public class TileGameButton extends GameButton {

    private final static double RATIO_PLAYBUTTON_COVER = 2 / 3.0;
    private final static double RATIO_INFOBUTTON_COVER = 1 / 6.0;

    public TileGameButton(GameEntry entry, TilePane parent, BaseScene scene) {
        super(entry, scene, parent);
        setTileWidth();
        setTileHeight();
        parent.prefTileWidthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                TileGameButton.this.setPrefWidth(newValue.doubleValue());
                TileGameButton.this.setWidth(newValue.doubleValue());
                coverView.setFitWidth(newValue.doubleValue());
                playButton.setFitWidth(newValue.doubleValue() * RATIO_PLAYBUTTON_COVER);
                infoButton.setFitWidth(newValue.doubleValue() * RATIO_INFOBUTTON_COVER);
            }
        });
        parent.prefTileHeightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                TileGameButton.this.setPrefHeight(newValue.doubleValue());
                TileGameButton.this.setHeight(newValue.doubleValue());
                coverView.setFitHeight(newValue.doubleValue());
                playButton.setFitHeight(newValue.doubleValue() * RATIO_PLAYBUTTON_COVER);
                infoButton.setFitHeight(newValue.doubleValue() * RATIO_INFOBUTTON_COVER);
            }
        });
    }


    private void setTileWidth() {
        TileGameButton.this.setPrefWidth(((TilePane) parent).getPrefTileWidth());
        TileGameButton.this.setWidth(((TilePane) parent).getPrefTileWidth());
        coverView.setFitWidth(((TilePane) parent).getPrefTileWidth());
        playButton.setFitWidth(((TilePane) parent).getPrefTileWidth() * RATIO_PLAYBUTTON_COVER);
        infoButton.setFitWidth(((TilePane) parent).getPrefTileWidth() * RATIO_INFOBUTTON_COVER);
    }

    private void setTileHeight() {
        TileGameButton.this.setPrefHeight(((TilePane) parent).getPrefTileHeight());
        TileGameButton.this.setHeight(((TilePane) parent).getPrefTileHeight());
        coverView.setFitHeight(((TilePane) parent).getPrefTileHeight());
        playButton.setFitHeight(((TilePane) parent).getPrefTileHeight() * RATIO_PLAYBUTTON_COVER);
        infoButton.setFitHeight(((TilePane) parent).getPrefTileHeight() * RATIO_INFOBUTTON_COVER);
    }

    @Override
    protected int getCoverHeight() {
        return (int) (SCREEN_WIDTH / 5 * COVER_HEIGHT_WIDTH_RATIO);
    }

    @Override
    protected int getCoverWidth() {
        return (int) (SCREEN_WIDTH / 5);
    }

    @Override
    protected int getInfoButtonHeight() {
        return (int) (((TilePane) parent).getPrefTileHeight() * RATIO_INFOBUTTON_COVER);
    }

    @Override
    protected int getInfoButtonWidth() {
        return (int) (((TilePane) parent).getPrefTileWidth() * RATIO_INFOBUTTON_COVER);
    }

    @Override
    protected int getPlayButtonHeight() {
        return (int) (((TilePane) parent).getPrefTileHeight() * RATIO_PLAYBUTTON_COVER);
    }

    @Override
    protected int getPlayButtonWidth() {
        return (int) (((TilePane) parent).getPrefTileWidth() * RATIO_PLAYBUTTON_COVER);
    }

    @Override
    protected void initCoverView() {
        if (DEFAULT_COVER_IMAGE == null || DEFAULT_COVER_IMAGE.getWidth() != getCoverWidth() || DEFAULT_COVER_IMAGE.getWidth() != getCoverHeight()) {
            for (int i = 256; i < 1025; i *= 2) {
                if (i > getCoverHeight()) {
                    DEFAULT_COVER_IMAGE = new Image("res/defaultImages/cover" + i + ".jpg", getCoverWidth(), getCoverHeight(), false, true);
                    break;
                }
            }
            if (DEFAULT_COVER_IMAGE == null) {
                DEFAULT_COVER_IMAGE = new Image("res/defaultImages/cover.jpg", getCoverWidth(), getCoverHeight(), false, true);
            }
        }
        coverView = new ImageView(DEFAULT_COVER_IMAGE);
    }
}

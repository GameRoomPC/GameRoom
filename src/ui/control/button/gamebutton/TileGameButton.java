package ui.control.button.gamebutton;

import javafx.geometry.Pos;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import ui.control.button.ImageButton;
import ui.scene.BaseScene;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.TilePane;
import data.game.entry.GameEntry;

import static ui.Main.*;

/**
 * Created by LM on 02/07/2016.
 */
public class TileGameButton extends GameButton {
    private ImageView notInstalledImage = new ImageView();
    private final static double RATIO_NOTINSTALLEDIMAGE_COVER = 1 / 3.0;
    private final static double RATIO_PLAYBUTTON_COVER = 2 / 3.0;
    private final static double RATIO_INFOBUTTON_COVER = 1 / 6.0;

    public TileGameButton(GameEntry entry, TilePane parent, BaseScene scene) {
        super(entry, scene, parent);
        setTileWidth();
        setTileHeight();
        notInstalledImage.setPreserveRatio(true);
        coverPane.getChildren().add(notInstalledImage);
        StackPane.setAlignment(notInstalledImage, Pos.TOP_RIGHT);
        setNotInstalledEffect();
        parent.prefTileWidthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                TileGameButton.this.setPrefWidth(newValue.doubleValue());
                TileGameButton.this.setWidth(newValue.doubleValue());
                coverView.setFitWidth(newValue.doubleValue());
                playButton.setFitWidth(newValue.doubleValue() * RATIO_PLAYBUTTON_COVER);
                infoButton.setFitWidth(newValue.doubleValue() * RATIO_INFOBUTTON_COVER);
                notInstalledImage.setFitWidth(newValue.doubleValue() * RATIO_NOTINSTALLEDIMAGE_COVER);
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
                notInstalledImage.setFitHeight(newValue.doubleValue() * RATIO_NOTINSTALLEDIMAGE_COVER);
            }
        });
    }


    private void setTileWidth() {
        TileGameButton.this.setPrefWidth(((TilePane) parent).getPrefTileWidth());
        TileGameButton.this.setWidth(((TilePane) parent).getPrefTileWidth());
        coverView.setFitWidth(((TilePane) parent).getPrefTileWidth());
        playButton.setFitWidth(((TilePane) parent).getPrefTileWidth() * RATIO_PLAYBUTTON_COVER);
        infoButton.setFitWidth(((TilePane) parent).getPrefTileWidth() * RATIO_INFOBUTTON_COVER);
        notInstalledImage.setFitWidth(((TilePane) parent).getPrefTileWidth() * RATIO_NOTINSTALLEDIMAGE_COVER);

    }

    private void setTileHeight() {
        TileGameButton.this.setPrefHeight(((TilePane) parent).getPrefTileHeight());
        TileGameButton.this.setHeight(((TilePane) parent).getPrefTileHeight());
        coverView.setFitHeight(((TilePane) parent).getPrefTileHeight());
        playButton.setFitHeight(((TilePane) parent).getPrefTileHeight() * RATIO_PLAYBUTTON_COVER);
        infoButton.setFitHeight(((TilePane) parent).getPrefTileHeight() * RATIO_INFOBUTTON_COVER);
        notInstalledImage.setFitHeight(((TilePane) parent).getPrefTileHeight() * RATIO_NOTINSTALLEDIMAGE_COVER);
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
    private void setNotInstalledEffect(){
        if (getEntry().isNotInstalled()){
            /*GaussianBlur blur = new GaussianBlur(0.6);
            blur.setRadius(4);
            blur.setInput(coverView.getEffect());

            ColorAdjust coverColorAdjust = new ColorAdjust();
            coverColorAdjust.setBrightness(-0.8);
            coverColorAdjust.setSaturation(-0.5);
            coverColorAdjust.setInput(blur);
            coverColorAdjust.setContrast(-0.3);*/

            Image addImage = new Image("res/ui/toDownloadIcon.png");
            notInstalledImage.setImage(addImage);

            //coverView.setEffect(coverColorAdjust);
        }else{
            notInstalledImage.setImage(null);
        }
    }
}

package ui.control.button.gamebutton;

import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
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

    public TileGameButton(GameEntry entry, TilePane parent, BaseScene scene) {
        super(entry, scene, parent);
        onNewTileWidth(parent.getPrefTileWidth());
        onNewTileHeight(parent.getPrefTileHeight());

        initContentSize(parent);
    }

    @Override
    protected int getCoverHeight() {
        return (int) (SCREEN_WIDTH / 10 * COVER_HEIGHT_WIDTH_RATIO);
    }

    @Override
    protected int getCoverWidth() {
        return (int) (SCREEN_WIDTH / 10);
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

    @Override
    protected void onNewTileWidth(double width) {
    }

    @Override
    protected void onNewTileHeight(double height) {
    }
}

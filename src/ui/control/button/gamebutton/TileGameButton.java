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
    private double imageCoverWidth = -1;
    private double imageCoverHeight = -1;


    public TileGameButton(GameEntry entry, TilePane parent, BaseScene scene) {
        this(entry,parent,scene,-1,-1);
    }
    public TileGameButton(GameEntry entry, TilePane parent, BaseScene scene,double imageCoverWidth,double imageCoverHeight) {
        super(entry, scene, parent);
        onNewTileWidth(parent.getPrefTileWidth());
        onNewTileHeight(parent.getPrefTileHeight());

        initContentSize(parent);

        this.imageCoverHeight = imageCoverHeight;
        this.imageCoverWidth = imageCoverWidth;
    }

    @Override
    protected int getCoverHeight() {
        return imageCoverHeight!= -1? (int) imageCoverHeight : (int) (SCREEN_WIDTH / 8 * COVER_HEIGHT_WIDTH_RATIO);
    }

    @Override
    protected int getCoverWidth() {
        return imageCoverWidth!= -1? (int) imageCoverWidth : (int) (SCREEN_WIDTH / 8);
    }

    @Override
    protected void initCoverView() {
        coverView = new ImageView();
    }

    @Override
    protected void onNewTileWidth(double width) {
    }

    @Override
    protected void onNewTileHeight(double height) {
    }
}

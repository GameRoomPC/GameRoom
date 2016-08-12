package ui.dialog;

import data.game.GameEntry;
import data.game.ImageUtils;
import data.game.OnDLDoneHandler;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;
import ui.Main;
import ui.pane.OnItemSelectedHandler;
import ui.pane.SelectListPane;
import ui.scene.BaseScene;

import java.io.File;

import static ui.control.button.gamebutton.GameButton.FADE_IN_OUT_TIME;

/**
 * Created by LM on 06/08/2016.
 */
public class IGDBImageSelector extends GameRoomDialog<ButtonType> {
    private ImageList imageList;
    private String selectedImageHash;

    public IGDBImageSelector(GameEntry entry,OnItemSelectedHandler onImageSelected) {
        this(entry.getIgdb_imageHashs(), entry.getIgdb_id(), onImageSelected);
    }

    public IGDBImageSelector(String[] igdbScreenshots, int igdb_id, OnItemSelectedHandler onImageSelected) {
        super();
        Label titleLabel = new Label(Main.RESSOURCE_BUNDLE.getString("select_a_wallpaper"));
        titleLabel.setPadding(new Insets(0 * Main.SCREEN_HEIGHT / 1080
                , 20 * Main.SCREEN_WIDTH / 1920
                , 20 * Main.SCREEN_HEIGHT / 1080
                , 20 * Main.SCREEN_WIDTH / 1920));
        mainPane.setTop(titleLabel);
        mainPane.setPadding(new Insets(30 * Main.SCREEN_HEIGHT / 1080
                , 30 * Main.SCREEN_WIDTH / 1920
                , 20 * Main.SCREEN_HEIGHT / 1080
                , 30 * Main.SCREEN_WIDTH / 1920));
        BorderPane.setAlignment(titleLabel, Pos.CENTER);

        mainPane.setPrefWidth(Main.SCREEN_WIDTH * 1 / 3 * Main.SCREEN_WIDTH / 1920);
        mainPane.setPrefHeight(Main.SCREEN_HEIGHT * 2 / 3 * Main.SCREEN_HEIGHT / 1080);

        if(igdbScreenshots != null && igdbScreenshots.length>0) {
            imageList = new ImageList(Main.SCREEN_HEIGHT/3.0,igdb_id,mainPane.prefWidthProperty(),onImageSelected);
            imageList.addItems(igdbScreenshots);
            mainPane.setCenter(imageList);
            setOnHiding(event -> {
                selectedImageHash = ((String) imageList.getSelectedValue());
            });
        }else{
            mainPane.setCenter(new Label(Main.RESSOURCE_BUNDLE.getString("no_screenshot_for_this_game")));
        }

        getDialogPane().getButtonTypes().addAll(new ButtonType(Main.RESSOURCE_BUNDLE.getString("ok"), ButtonBar.ButtonData.OK_DONE)
                ,new ButtonType(Main.RESSOURCE_BUNDLE.getString("cancel"),ButtonBar.ButtonData.CANCEL_CLOSE));
    }

    public String getSelectedImageHash() {
        return selectedImageHash;
    }

    private static class ImageList<String> extends SelectListPane{
        private ReadOnlyDoubleProperty prefRowWidth;
        private int igdb_id;
        private OnItemSelectedHandler onImageSelected;
        public ImageList(double prefHeight,int igdb_id, ReadOnlyDoubleProperty prefRowWidth, OnItemSelectedHandler onImageSelected) {
            super(prefHeight);
            this.prefRowWidth = prefRowWidth;
            this.igdb_id = igdb_id;
            this.onImageSelected = onImageSelected;
        }

        @Override
        protected ListItem createListItem(Object value) {
            ImageItem tile = new ImageItem(igdb_id,this, value,prefRowWidth);
            return tile;
        }

        @Override
        public void onItemSelected(ListItem item){
            onImageSelected.handle(item);
        }
    }
    private static class ImageItem<String> extends SelectListPane.ListItem{
        private Label loadingLabel = new Label(Main.RESSOURCE_BUNDLE.getString("loading")+"...");
        private StackPane imageViewHolder = new StackPane();
        private ImageView imageView = new ImageView();
        private ReadOnlyDoubleProperty prefRowWidth;
        private int igdb_id;

        public ImageItem(int igdb_id,SelectListPane parentList,String value,ReadOnlyDoubleProperty prefRowWidth) {
            super(value,parentList);
            this.igdb_id = igdb_id;
            this.prefRowWidth = prefRowWidth;
            addContent();
        }

        @Override
        protected void addContent() {
            double prefTileWidth =Main.SCREEN_WIDTH * 1 / 3 * Main.SCREEN_WIDTH / 1920*0.7;
            double prefTileHeight = Main.SCREEN_HEIGHT * (prefTileWidth / Main.SCREEN_WIDTH);
            ImageUtils.downloadIGDBImageToCache(igdb_id
                    , (java.lang.String) getValue()
                    , ImageUtils.IGDB_TYPE_SCREENSHOT
                    , ImageUtils.IGDB_SIZE_MED
                    , new OnDLDoneHandler() {
                        @Override
                        public void run(File outputfile) {
                            Image img = new Image("file:"+ File.separator + File.separator + File.separator + outputfile.getAbsolutePath(), prefTileWidth, prefTileHeight, false, true);
                            ImageUtils.transitionToImage(img, imageView, 1);
                        }
                    });
            prefWidthProperty().bind(prefRowWidth);
            imageViewHolder.getChildren().add(loadingLabel);
            imageViewHolder.getChildren().add(imageView);
            GridPane.setMargin(imageViewHolder, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 10 * Main.SCREEN_WIDTH / 1920));
            add(imageViewHolder, columnCount++, 0);
        }
    }

}

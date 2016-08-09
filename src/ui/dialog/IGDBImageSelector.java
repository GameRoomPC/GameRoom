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
import ui.pane.SelectListPane;

import java.io.File;

import static ui.control.button.gamebutton.GameButton.FADE_IN_OUT_TIME;

/**
 * Created by LM on 06/08/2016.
 */
public class IGDBImageSelector extends GameRoomDialog<String> {
    private ImageList imageList;

    public IGDBImageSelector(GameEntry entry) {
        this(entry.getIgdb_imageHashs(), entry.getIgdb_ID());
    }

    public IGDBImageSelector(String[] igdbScreenshots, int igdb_id) {
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
            imageList = new ImageList(Main.SCREEN_HEIGHT/3.0,igdb_id,mainPane.prefWidthProperty());
            imageList.addItems(igdbScreenshots);
            mainPane.setCenter(imageList);
            getDialogPane().getButtonTypes().addAll(new ButtonType(Main.RESSOURCE_BUNDLE.getString("ok"), ButtonBar.ButtonData.OK_DONE));
            setOnHiding(event -> {
                setResult((String) imageList.getSelectedValue());
            });
        }else{
            mainPane.setCenter(new Label(Main.RESSOURCE_BUNDLE.getString("no_screenshot_for_this_game")));
            getDialogPane().getButtonTypes().addAll(new ButtonType(Main.RESSOURCE_BUNDLE.getString("cancel"),ButtonBar.ButtonData.CANCEL_CLOSE));
        }
    }
    private static class ImageList<String> extends SelectListPane{
        private ReadOnlyDoubleProperty prefRowWidth;
        private int igdb_id;
        public ImageList(double prefHeight,int igdb_id, ReadOnlyDoubleProperty prefRowWidth) {
            super(prefHeight);
            this.prefRowWidth = prefRowWidth;
            this.igdb_id = igdb_id;
        }

        @Override
        protected ListItem createListItem(Object value) {
            ImageItem tile = new ImageItem(igdb_id,this, value,prefRowWidth);
            return tile;
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
                            imageView.setOpacity(0);
                            imageView.setImage(img);
                            Timeline fadeInTimeline = new Timeline(
                                    new KeyFrame(Duration.seconds(0),
                                            new KeyValue(imageView.opacityProperty(), 0, Interpolator.EASE_IN)),
                                    new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                                            new KeyValue(imageView.opacityProperty(), 1, Interpolator.EASE_OUT)
                                    ));
                            fadeInTimeline.setCycleCount(1);
                            fadeInTimeline.setAutoReverse(false);
                            fadeInTimeline.play();
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

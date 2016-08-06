package ui.dialog;

import data.game.GameEntry;
import data.game.ImageUtils;
import data.game.OnDLDoneHandler;
import data.http.SimpleImageInfo;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import ui.Main;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import static ui.control.button.gamebutton.GameButton.FADE_IN_OUT_TIME;

/**
 * Created by LM on 06/08/2016.
 */
public class IGDBImageSelector extends GameRoomDialog<String> {
    private FlowPane flowPane = new FlowPane();

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
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            StackPane flowPaneHolder = new StackPane();
            flowPaneHolder.minWidthProperty().bind(Bindings.createDoubleBinding(() ->
                    scrollPane.getViewportBounds().getWidth(), scrollPane.viewportBoundsProperty()));
            scrollPane.setContent(flowPaneHolder);
            flowPaneHolder.getChildren().addAll(flowPane);

            mainPane.setCenter(scrollPane);
            getDialogPane().setContent(mainPane);

            flowPane.setOrientation(Orientation.HORIZONTAL);
            double prefTileWidth = mainPane.getPrefWidth()*0.8;
            double prefTileHeight = Main.SCREEN_HEIGHT * (prefTileWidth / Main.SCREEN_WIDTH);
            for (String hash : igdbScreenshots) {
                try {
                    ImageSelectorTile tile = new ImageSelectorTile(igdb_id, hash, prefTileWidth, prefTileHeight);
                    tile.setOnMouseClicked(new EventHandler<MouseEvent>() {
                        @Override
                        public void handle(MouseEvent event) {
                            tile.requestFocus();
                            setResult(tile.getImageHash());
                        }
                    });
                    flowPane.getChildren().add(tile);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }else{
            mainPane.setCenter(new Label(Main.RESSOURCE_BUNDLE.getString("no_screenshot_for_this_game")));
            getDialogPane().getButtonTypes().addAll(new ButtonType(Main.RESSOURCE_BUNDLE.getString("cancel"),ButtonBar.ButtonData.CANCEL_CLOSE));
        }
        /*ButtonType nextButton = new ButtonType(Main.RESSOURCE_BUNDLE.getString("next"), ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(nextButton);*/
    }


    private static class ImageSelectorTile extends StackPane {
        private ImageView imageView = new ImageView();
        private String imageHash;
        private final static String cssBorder = "-fx-border-color:flatter-red ; \n" //#090a0c
                + "-fx-border-insets:3;\n"
                + "-fx-border-radius:7;\n"
                + "-fx-border-width:5.0";
        private final static String cssNoBorder = "-fx-border-color:flatter-red ; \n" //#090a0c
                + "-fx-border-insets:0;\n"
                + "-fx-border-radius:0;\n"
                + "-fx-border-width:0";

        ImageSelectorTile(int id, String imageHash, double width, double height) throws MalformedURLException {
            super();
            setPrefWidth(width);
            setPrefHeight(height);
            this.imageHash = imageHash;
            setPadding(new Insets(10 * Main.SCREEN_HEIGHT / 1080
                    , 10 * Main.SCREEN_WIDTH / 1920
                    , 10 * Main.SCREEN_HEIGHT / 1080
                    , 10 * Main.SCREEN_WIDTH / 1920));
            ImageUtils.downloadImageToCache(id
                    , imageHash
                    , ImageUtils.TYPE_SCREENSHOT
                    , ImageUtils.SIZE_MED
                    , new OnDLDoneHandler() {
                        @Override
                        public void run(File outputfile) {
                            Image img = new Image("file:"+ File.separator + File.separator + File.separator + outputfile.getAbsolutePath(), width, height, false, true);
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
            focusedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    //setStyle(newValue ? cssBorder : cssNoBorder);
                }
            });
            getChildren().add(imageView);
        }

        public String getImageHash() {
            return imageHash;
        }
    }
}

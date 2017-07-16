package ui.control.button.gamebutton;

import data.game.entry.GameEntry;
import data.http.images.ImageUtils;
import javafx.animation.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.util.Duration;
import system.application.settings.PredefinedSetting;
import ui.GeneralToast;
import ui.Main;
import ui.control.button.ImageButton;
import ui.pane.gamestilepane.ToAddRowTilePane;
import ui.scene.GameEditScene;
import ui.scene.MainScene;

import java.io.File;

import static system.application.settings.GeneralSettings.settings;
import static ui.Main.MAIN_SCENE;
import static ui.Main.SCREEN_WIDTH;

/**
 * Created by LM on 17/08/2016.
 */
public class AddIgnoreGameButton extends GameButton {
    private final static double RATIO_ADDBUTTON_COVER = 1 / 2.0;
    private final static double RATIO_IGNOREBUTTON_COVER = 1 / 5.0;

    public final static double ROTATION_TIME = 2;

    private ImageButton addButton;
    private ImageButton ignoreButton;
    private ImageButton scrapingButton;

    public AddIgnoreGameButton(GameEntry entry, MainScene mainScene, TilePane parent, ToAddRowTilePane parentPane) {
        super(entry, mainScene, parent);
        disableNode(infoButton, true);
        disableNode(playTimeLabel, true);
        disableNode(playButton, true);

        addButton = new ImageButton("toaddtile-add-button", Main.SCREEN_WIDTH / 10, Main.SCREEN_WIDTH / 10);
        addButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                GameEditScene addScene = new GameEditScene(mainScene, getEntry(), GameEditScene.MODE_ADD, null);
                mainScene.fadeTransitionTo(addScene, mainScene.getParentStage());
            }
        });

        ignoreButton = new ImageButton("toaddtile-ignore-button", Main.SCREEN_WIDTH / 10, Main.SCREEN_WIDTH / 10);
        ignoreButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                entry.setSavedLocally(true);
                entry.setIgnored(true);
                entry.setSavedLocally(false);

                mainScene.removeGame(entry);
                parentPane.removeGame(entry);

                GeneralToast.displayToast(entry.getName() + Main.getString("ignored"), mainScene.getParentStage());
            }
        });
        scrapingButton = new ImageButton("tile-loading-button", SCREEN_WIDTH / 10, SCREEN_WIDTH / 10);
        scrapingButton.setFocusTraversable(false);
        initScrapingGraphics(entry);
        rotateScrapingButton();

        addButton.setOpacity(0);
        addButton.setFocusTraversable(false);
        ignoreButton.setOpacity(0);
        ignoreButton.setFocusTraversable(false);

        StackPane stackPane = new StackPane();
        stackPane.getChildren().addAll(ignoreButton, addButton, scrapingButton);
        StackPane.setAlignment(ignoreButton, Pos.BOTTOM_RIGHT);
        StackPane.setAlignment(addButton, Pos.CENTER);
        StackPane.setAlignment(scrapingButton, Pos.CENTER);

        coverPane.getChildren().add(stackPane);

        focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    addButton.setMouseTransparent(false);
                    ignoreButton.setMouseTransparent(false);

                    Timeline fadeInTimeline = new Timeline(
                            new KeyFrame(Duration.seconds(0),
                                    new KeyValue(addButton.opacityProperty(), addButton.opacityProperty().getValue(), Interpolator.EASE_OUT),
                                    new KeyValue(ignoreButton.opacityProperty(), ignoreButton.opacityProperty().getValue(), Interpolator.EASE_OUT)),
                            new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                                    new KeyValue(addButton.opacityProperty(), 1, Interpolator.EASE_OUT),
                                    new KeyValue(ignoreButton.opacityProperty(), 1, Interpolator.EASE_OUT)
                            ));
                    fadeInTimeline.setCycleCount(1);
                    fadeInTimeline.setAutoReverse(false);
                    fadeInTimeline.play();

                    if (!settings().getBoolean(PredefinedSetting.DISABLE_MAINSCENE_WALLPAPER)) {
                        ImageUtils.getExecutorService().submit(() -> Main.runAndWait(() -> MAIN_SCENE.setImageBackground(entry.getImagePath(1))));
                    }

                } else {
                    //addButton.setMouseTransparent(true);
                    //ignoreButton.setMouseTransparent(true);

                    Timeline fadeOutTimeline = new Timeline(
                            new KeyFrame(Duration.seconds(0),
                                    new KeyValue(addButton.opacityProperty(), addButton.opacityProperty().getValue(), Interpolator.EASE_OUT),
                                    new KeyValue(ignoreButton.opacityProperty(), ignoreButton.opacityProperty().getValue(), Interpolator.EASE_OUT)),
                            new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                                    new KeyValue(addButton.opacityProperty(), 0, Interpolator.EASE_OUT),
                                    new KeyValue(ignoreButton.opacityProperty(), 0, Interpolator.EASE_OUT)
                            ));
                    fadeOutTimeline.setCycleCount(1);
                    fadeOutTimeline.setAutoReverse(false);

                    fadeOutTimeline.play();
                }
            }
        });

        initContentSize(parent);
    }

    @Override
    public void reloadWith(GameEntry entry) {
        super.reloadWith(entry);

        initScrapingGraphics(entry);
    }

    private void initScrapingGraphics(GameEntry entry) {
        boolean scraping = entry.isWaitingToBeScrapped() || entry.isBeingScraped();

        addButton.setVisible(!scraping);
        addButton.setMouseTransparent(scraping);

        scrapingButton.setVisible(entry.isBeingScraped());
        scrapingButton.setMouseTransparent(!entry.isBeingScraped());
    }

    @Override
    protected int getCoverHeight() {
        return (int) (SCREEN_WIDTH / 14 * COVER_HEIGHT_WIDTH_RATIO);
    }

    @Override
    protected int getCoverWidth() {
        return (int) (SCREEN_WIDTH / 14);
    }

    @Override
    protected void initCoverView() {
        coverView = new ImageView();
        coverView.setFitWidth(getCoverWidth());
        coverView.setFitHeight(getCoverHeight());
    }

    @Override
    protected void onNewTileWidth(double width) {
        addButton.setFitWidth(width * RATIO_ADDBUTTON_COVER);
        ignoreButton.setFitWidth(width * RATIO_IGNOREBUTTON_COVER);
        scrapingButton.setFitWidth(width * RATIO_ADDBUTTON_COVER);
    }

    @Override
    protected void onNewTileHeight(double height) {
        addButton.setFitHeight(height * RATIO_ADDBUTTON_COVER);
        ignoreButton.setFitHeight(height * RATIO_IGNOREBUTTON_COVER);
        scrapingButton.setFitHeight(height * RATIO_ADDBUTTON_COVER);
    }

    public void setImage(String imagePath) {
        Image img = new Image("file:" + File.separator + File.separator + File.separator + imagePath, getCoverWidth(), getCoverHeight(), false, true);
        coverView.setImage(img);
    }

    public Image getImage() {
        return coverView.getImage();
    }

    private void rotateScrapingButton() {
        Timeline fadeInTimeline = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(scrapingButton.rotateProperty(), scrapingButton.rotateProperty().getValue(), Interpolator.LINEAR)),
                new KeyFrame(Duration.seconds(ROTATION_TIME),
                        new KeyValue(scrapingButton.rotateProperty(), 360, Interpolator.LINEAR)
                ));
        fadeInTimeline.setCycleCount(Animation.INDEFINITE);
        fadeInTimeline.setAutoReverse(false);
        fadeInTimeline.play();
    }
}

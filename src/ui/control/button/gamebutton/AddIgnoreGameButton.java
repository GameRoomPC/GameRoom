package ui.control.button.gamebutton;

import data.game.entry.GameEntry;
import data.game.scrapper.SteamPreEntry;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import system.application.settings.PredefinedSetting;
import ui.Main;
import ui.control.button.ImageButton;
import ui.pane.gamestilepane.ToAddRowTilePane;
import ui.scene.BaseScene;
import ui.scene.GameEditScene;
import ui.scene.MainScene;

import java.io.File;

import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static ui.Main.GENERAL_SETTINGS;
import static ui.Main.MAIN_SCENE;

/**
 * Created by LM on 17/08/2016.
 */
public class AddIgnoreGameButton extends GameButton {
    private static Image ADD_IMAGE;
    private static Image IGNORE_IMAGE;

    private final static double RATIO_ADDBUTTON_COVER = 1 / 2.0;
    private final static double RATIO_IGNOREBUTTON_COVER = 1 / 5.0;
    private ImageButton addButton;
    private ImageButton ignoreButton;

    public AddIgnoreGameButton(GameEntry entry, MainScene mainScene, TilePane parent, ToAddRowTilePane parentPane) {
        super(entry, mainScene, parent);
        disableNode(infoButton, true);
        disableNode(playTimeLabel, true);
        disableNode(playButton, true);

        /*
        setOnMouseExited(eh -> {
            setFocused(false);
        });
        setOnMouseEntered(eh -> {
            setFocused(true);
        });
        scene.heightProperty().addListener(cl -> {
            //initAll();
        });*/
        if (ADD_IMAGE == null) {
            ADD_IMAGE = new Image("res/ui/validIcon.png", Main.SCREEN_WIDTH / 10, Main.SCREEN_WIDTH / 10, true, true);
        }
        if (IGNORE_IMAGE == null) {
            IGNORE_IMAGE = new Image("res/ui/invalidIcon.png", Main.SCREEN_WIDTH / 10, Main.SCREEN_WIDTH / 10, true, true);
        }
        addButton = new ImageButton(ADD_IMAGE);
        addButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                GameEditScene addScene = new GameEditScene(mainScene, entry, GameEditScene.MODE_ADD, null);
                mainScene.fadeTransitionTo(addScene, mainScene.getParentStage());
            }
        });
        ignoreButton = new ImageButton(IGNORE_IMAGE);
        ignoreButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                SteamPreEntry[] ignoredEntries = Main.GENERAL_SETTINGS.getSteamAppsIgnored();

                boolean inList = false;

                for (int i = 0; i < ignoredEntries.length && !inList; i++) {
                    inList = entry.getSteam_id() == ignoredEntries[i].getId();
                }
                if (!inList) {
                    SteamPreEntry[] futureEntries = new SteamPreEntry[ignoredEntries.length + 1];
                    for (int i = 0; i < ignoredEntries.length; i++) {
                        futureEntries[i] = ignoredEntries[i];
                    }
                    futureEntries[futureEntries.length - 1] = new SteamPreEntry(entry.getName(), entry.getSteam_id());
                    Main.GENERAL_SETTINGS.setSettingValue(PredefinedSetting.IGNORED_STEAM_APPS, futureEntries);
                }
                mainScene.removeGame(entry);
                parentPane.removeGame(entry);
            }
        });
        addButton.setOpacity(0);
        addButton.setFocusTraversable(false);
        ignoreButton.setOpacity(0);
        ignoreButton.setFocusTraversable(false);

        StackPane stackPane = new StackPane();
        stackPane.getChildren().addAll(addButton, ignoreButton);
        StackPane.setAlignment(addButton, Pos.CENTER);
        StackPane.setAlignment(ignoreButton, Pos.BOTTOM_RIGHT);

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

                    if (!GENERAL_SETTINGS.getBoolean(PredefinedSetting.DISABLE_MAINSCENE_WALLPAPER)) {
                        Task backGroundImageTask = new Task() {
                            @Override
                            protected Object call() throws Exception {
                                Thread.currentThread().sleep(300);
                                if (isFocused()) {
                                    Image screenshotImage = entry.getImage(1,
                                            Main.GENERAL_SETTINGS.getWindowWidth(),
                                            Main.GENERAL_SETTINGS.getWindowHeight()
                                            , false, true);

                                    Platform.runLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            MAIN_SCENE.setImageBackground(screenshotImage);
                                        }
                                    });
                                }
                                return null;
                            }
                        };
                        Thread setBackgroundThread = new Thread(backGroundImageTask);
                        setBackgroundThread.setDaemon(true);
                        setBackgroundThread.start();
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
    protected int getCoverHeight() {
        return (int) (GENERAL_SETTINGS.getWindowHeight() * 2 / 3);
    }

    @Override
    protected int getCoverWidth() {
        return (int) (GENERAL_SETTINGS.getWindowHeight() * 2 / (3 * COVER_HEIGHT_WIDTH_RATIO));
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
    }

    @Override
    protected void onNewTileHeight(double height) {
        addButton.setFitHeight(height * RATIO_ADDBUTTON_COVER);
        ignoreButton.setFitHeight(height * RATIO_IGNOREBUTTON_COVER);
    }

    public void setImage(String imagePath) {
        Image img = new Image("file:" + File.separator + File.separator + File.separator + imagePath, getCoverWidth(), getCoverHeight(), false, true);
        coverView.setImage(img);
    }

    public Image getImage() {
        return coverView.getImage();
    }
}

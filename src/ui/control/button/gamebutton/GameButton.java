package ui.control.button.gamebutton;

import ui.control.button.ImageButton;
import ui.scene.BaseScene;
import ui.scene.GameInfoScene;
import ui.scene.MainScene;
import data.game.GameEntry;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

import static ui.Main.*;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;

/**
 * Created by LM on 12/07/2016.
 */
public abstract class GameButton extends BorderPane {
    private static Image DEFAULT_COVER_IMAGE;
    private static Image DEFAULT_PLAY_IMAGE;
    private static Image DEFAULT_INFO_IMAGE;

    public final static double FADE_IN_OUT_TIME = 0.1;

    public final static double COVER_HEIGHT_WIDTH_RATIO = 127.0 / 90.0;

    protected static double COVER_SCALE_EFFECT_FACTOR = 1.1;
    private final static double COVER_BLUR_EFFECT_RADIUS = 10;
    private final static double COVER_BRIGHTNESS_EFFECT_FACTOR = 0.1;

    private BaseScene parentScene;

    private StackPane coverPane;
    private Label nameLabel;
    private ContextMenu contextMenu;
    protected ImageView coverView;
    protected ImageButton playButton;
    protected ImageButton infoButton;
    private Label playTimeLabel;

    protected Pane parent;


    private GameEntry entry;
    private boolean inContextMenu = false;


    public GameButton(GameEntry entry, BaseScene scene, Pane parent) {
        super();
        this.parent = parent;
        this.entry = entry;
        this.parentScene = scene;

        initAll();
    }

    protected void initAll() {
        if (coverPane != null) {
            coverPane.getChildren().clear();
        }

        initCoverPane(entry);
        initContextMenu();
        initNameText(entry);

        setOnKeyPressed(ke -> {
            if (ke.getCode() == KeyCode.ENTER) {
                playButton.fireEvent(new MouseEvent(MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 0, true, true, true, true, true, true, true, true, true, true, null));
            }
            if(ke.getCode() == KeyCode.I){
                infoButton.fireEvent(new MouseEvent(MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 0, true, true, true, true, true, true, true, true, true, true, null));
            }
        });
        setCenter(coverPane);
        setBottom(nameLabel);
    }

    private void initNameText(GameEntry entry) {
        nameLabel = new Label(entry.getName());
        BorderPane.setMargin(nameLabel, new Insets(10 * GENERAL_SETTINGS.getWindowHeight() / 1080, 0, 0, 0));
        setAlignment(nameLabel, Pos.CENTER);

        nameLabel.setScaleX(0.90f);
        nameLabel.setScaleY(0.90f);
    }

    private void initContextMenu() {
        contextMenu = new ContextMenu();
        MenuItem cmItem1 = new MenuItem(RESSOURCE_BUNDLE.getString("Play"));
        cmItem1.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                playButton.fireEvent(new MouseEvent(MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 0, true, true, true, true, true, true, true, true, true, true, null));
            }
        });
        contextMenu.getItems().add(cmItem1);
        MenuItem cmItem2 = new MenuItem(RESSOURCE_BUNDLE.getString("edit"));
        cmItem2.setOnAction(eh -> {
        });
        contextMenu.getItems().add(cmItem2);
        MenuItem cmItem3 = new MenuItem(RESSOURCE_BUNDLE.getString("About"));
        cmItem3.setOnAction(nh -> {
            infoButton.fireEvent(new MouseEvent(MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 0, true, true, true, true, true, true, true, true, true, true, null));
        });
        contextMenu.getItems().add(cmItem3);
        contextMenu.setOnShowing(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                inContextMenu = true;
            }
        });
        contextMenu.setOnHiding(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                inContextMenu = false;
                coverPane.fireEvent(new MouseEvent(MOUSE_EXITED, 0, 0, 0, 0, MouseButton.PRIMARY, 0, true, true, true, true, true, true, true, true, true, true, null));
            }
        });
        coverPane.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.isSecondaryButtonDown()) {
                    contextMenu.show(coverPane, event.getScreenX(), event.getScreenY());
                }
                event.consume();
            }
        });
    }

    private void initCoverPane(GameEntry entry) {
        coverPane = new StackPane();

        if (DEFAULT_PLAY_IMAGE == null) {
            DEFAULT_PLAY_IMAGE = new Image("res/ui/playButton.png", getPlayButtonWidth(), getPlayButtonHeight(), true, true);
        }
        if (DEFAULT_INFO_IMAGE == null) {
            DEFAULT_INFO_IMAGE = new Image("res/ui/infoButton.png", getInfoButtonWidth(), getInfoButtonHeight(), true, true);
        }
        playButton = new ImageButton(DEFAULT_PLAY_IMAGE);
        infoButton = new ImageButton(DEFAULT_INFO_IMAGE);

        playTimeLabel = new Label(entry.getPlayTimeFormatted(GameEntry.TIME_FORMAT_SHORT_HMS));
        playButton.setOpacity(0);
        infoButton.setOpacity(0);
        playTimeLabel.setOpacity(0);
        playButton.setFocusTraversable(false);
        infoButton.setFocusTraversable(false);
        playTimeLabel.setFocusTraversable(false);
        playTimeLabel.setMouseTransparent(true);

        /*if (DEFAULT_COVER_IMAGE == null) {
            for (int i = 256; i < 1025; i *= 2) {
                if (i > getCoverHeight()) {
                    DEFAULT_COVER_IMAGE = new Image("res/defaultImages/cover" + i + ".jpg", getCoverWidth(), getCoverHeight(), false, true);
                    break;
                }
            }
            if (DEFAULT_COVER_IMAGE == null) {
                DEFAULT_COVER_IMAGE = new Image("res/defaultImages/cover.jpg", getCoverWidth(), getCoverHeight(), false, true);
            }
        }*/
        coverView = new ImageView(entry.getImage(0, getCoverWidth(), getCoverHeight(), false, true));
        /*Platform.runLater(new Runnable() {
            @Override
            public void run() {
                //TODO implement real multithreading loading
                Image img = entry.getImage(0, getCoverWidth(), getCoverHeight(), false, true);
                coverView.setImage(img);

            }
        });*/

        coverView.setPreserveRatio(true);

        playButton.setOnMouseClicked(mc -> {
            entry.startGame();
        });
        infoButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                parentScene.fadeTransitionTo(new GameInfoScene(new StackPane(), parentScene.getParentStage(), parentScene, entry), parentScene.getParentStage());
            }
        });

        //COVER EFFECTS
        DropShadow dropShadow = new DropShadow();
        dropShadow.setOffsetX(6.0 * SCREEN_WIDTH / 1920);
        dropShadow.setOffsetY(4.0 * SCREEN_HEIGHT / 1080);

        ColorAdjust coverColorAdjust = new ColorAdjust();
        coverColorAdjust.setBrightness(0.0);

        coverColorAdjust.setInput(dropShadow);

        GaussianBlur blur = new GaussianBlur(0.0);
        blur.setInput(coverColorAdjust);
        coverView.setEffect(blur);
        setFocusTraversable(true);
        focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    playButton.setMouseTransparent(false);
                    infoButton.setMouseTransparent(false);

                    Timeline fadeInTimeline = new Timeline(
                            new KeyFrame(Duration.seconds(0),
                                    new KeyValue(blur.radiusProperty(), blur.radiusProperty().getValue(), Interpolator.LINEAR),
                                    new KeyValue(scaleXProperty(), scaleXProperty().getValue(), Interpolator.LINEAR),
                                    new KeyValue(playButton.opacityProperty(), playButton.opacityProperty().getValue(), Interpolator.EASE_OUT),
                                    new KeyValue(infoButton.opacityProperty(), infoButton.opacityProperty().getValue(), Interpolator.EASE_OUT),
                                    new KeyValue(scaleYProperty(), scaleYProperty().getValue(), Interpolator.LINEAR),
                                    new KeyValue(coverColorAdjust.brightnessProperty(), coverColorAdjust.brightnessProperty().getValue(), Interpolator.LINEAR)),
                            new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                                    new KeyValue(blur.radiusProperty(), COVER_BLUR_EFFECT_RADIUS, Interpolator.LINEAR),
                                    new KeyValue(scaleXProperty(), COVER_SCALE_EFFECT_FACTOR, Interpolator.LINEAR),
                                    new KeyValue(playButton.opacityProperty(), 1, Interpolator.EASE_OUT),
                                    new KeyValue(infoButton.opacityProperty(), 1, Interpolator.EASE_OUT),
                                    new KeyValue(scaleYProperty(), COVER_SCALE_EFFECT_FACTOR, Interpolator.LINEAR),
                                    new KeyValue(coverColorAdjust.brightnessProperty(), -COVER_BRIGHTNESS_EFFECT_FACTOR, Interpolator.LINEAR)
                            ));
                    fadeInTimeline.setCycleCount(1);
                    fadeInTimeline.setAutoReverse(false);

                    fadeInTimeline.play();

                    //coverPane.fireEvent(new MouseEvent(MOUSE_ENTERED,0,0,0,0, MouseButton.PRIMARY,0,false, false, false, false, false, false, false, false, false, false, null));
                    if (MainScene.INPUT_MODE == MainScene.INPUT_MODE_KEYBOARD) {
                        playButton.fireEvent(new MouseEvent(MOUSE_ENTERED, 0, 0, 0, 0, MouseButton.PRIMARY, 0, false, false, false, false, false, false, false, false, false, false, null));
                    }
                } else {
                    if (!inContextMenu) {
                        playButton.setMouseTransparent(true);
                        infoButton.setMouseTransparent(true);

                        Timeline fadeOutTimeline = new Timeline(
                                new KeyFrame(Duration.seconds(0),
                                        new KeyValue(blur.radiusProperty(), blur.radiusProperty().getValue(), Interpolator.LINEAR),
                                        new KeyValue(scaleXProperty(), scaleXProperty().getValue(), Interpolator.LINEAR),
                                        new KeyValue(playButton.opacityProperty(), playButton.opacityProperty().getValue(), Interpolator.EASE_OUT),
                                        new KeyValue(infoButton.opacityProperty(), infoButton.opacityProperty().getValue(), Interpolator.EASE_OUT),
                                        new KeyValue(playTimeLabel.opacityProperty(), playTimeLabel.opacityProperty().getValue(), Interpolator.EASE_OUT),
                                        new KeyValue(scaleYProperty(), scaleYProperty().getValue(), Interpolator.LINEAR),
                                        new KeyValue(coverColorAdjust.brightnessProperty(), coverColorAdjust.brightnessProperty().getValue(), Interpolator.LINEAR)),
                                new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                                        new KeyValue(blur.radiusProperty(), 0, Interpolator.LINEAR),
                                        new KeyValue(scaleXProperty(), 1, Interpolator.LINEAR),
                                        new KeyValue(playButton.opacityProperty(), 0, Interpolator.EASE_OUT),
                                        new KeyValue(infoButton.opacityProperty(), 0, Interpolator.EASE_OUT),
                                        new KeyValue(playTimeLabel.opacityProperty(), 0, Interpolator.EASE_OUT),
                                        new KeyValue(scaleYProperty(), 1, Interpolator.LINEAR),
                                        new KeyValue(coverColorAdjust.brightnessProperty(), 0, Interpolator.LINEAR)
                                ));
                        fadeOutTimeline.setCycleCount(1);
                        fadeOutTimeline.setAutoReverse(false);

                        fadeOutTimeline.play();
                    }
                    //coverPane.fireEvent(new MouseEvent(MOUSE_EXITED,0,0,0,0, MouseButton.PRIMARY,0,false, false, false, false, false, false, false, false, false, false, null));
                    if (MainScene.INPUT_MODE == MainScene.INPUT_MODE_KEYBOARD) {
                        playButton.fireEvent(new MouseEvent(MOUSE_EXITED, 0, 0, 0, 0, MouseButton.PRIMARY, 0, false, false, false, false, false, false, false, false, false, false, null));
                    }
                }
            }
        });
        coverPane.setOnMouseMoved(e -> {
            if (MainScene.INPUT_MODE == MainScene.INPUT_MODE_MOUSE) {
                if (this.contains(new Point2D(e.getX(), e.getY()))) {
                    requestFocus();
                }
            }
        });
        coverPane.setOnMouseEntered(e -> {
            if (getCursor() != null && !getCursor().equals(Cursor.NONE)) {
                requestFocus();
            }
        });

        coverPane.setOnMouseExited(e -> {
            if (getCursor() != null && !getCursor().equals(Cursor.NONE)) {
                requestFocus();
                Timeline fadeOutTimeline = new Timeline(
                        new KeyFrame(Duration.seconds(0),
                                new KeyValue(playTimeLabel.opacityProperty(), playTimeLabel.opacityProperty().getValue(), Interpolator.EASE_OUT)),
                        new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                                new KeyValue(playTimeLabel.opacityProperty(), 0, Interpolator.EASE_OUT)
                        ));
                fadeOutTimeline.setCycleCount(1);
                fadeOutTimeline.setAutoReverse(false);

                fadeOutTimeline.play();
            }
        });
        infoButton.addMouseEnteredHandler(e -> {
            playTimeLabel.setText(entry.getPlayTimeFormatted(GameEntry.TIME_FORMAT_SHORT_HMS));
            if (getCursor() == null) {
                setCursor(Cursor.DEFAULT);
            }
            if (getCursor() != null && !getCursor().equals(Cursor.NONE)) {

                playTimeLabel.setOpacity(0);
                playTimeLabel.setVisible(true);
                Timeline fadeInTimeline = new Timeline(
                        new KeyFrame(Duration.seconds(0),
                                new KeyValue(playTimeLabel.opacityProperty(), playTimeLabel.opacityProperty().getValue(), Interpolator.EASE_IN)),
                        new KeyFrame(Duration.seconds(5 * FADE_IN_OUT_TIME),
                                new KeyValue(playTimeLabel.opacityProperty(), 1, Interpolator.EASE_IN)
                        ));
                fadeInTimeline.setCycleCount(1);
                fadeInTimeline.setAutoReverse(false);

                fadeInTimeline.play();
            }
        });
        infoButton.addMouseExitedHandler(e -> {
            if (getCursor() == null) {
                setCursor(Cursor.DEFAULT);
            }
            if (getCursor() != null && !getCursor().equals(Cursor.NONE)) {
                Timeline fadeOutTimeline = new Timeline(
                        new KeyFrame(Duration.seconds(0),
                                new KeyValue(playTimeLabel.opacityProperty(), playTimeLabel.opacityProperty().getValue(), Interpolator.EASE_OUT)),
                        new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                                new KeyValue(playTimeLabel.opacityProperty(), 0, Interpolator.EASE_OUT)
                        ));
                fadeOutTimeline.setCycleCount(1);
                fadeOutTimeline.setAutoReverse(false);

                fadeOutTimeline.play();

                playTimeLabel.setOpacity(0);
                playTimeLabel.setVisible(false);
            }
        });

        coverPane.getChildren().addAll(
                coverView,
                playButton,
                infoButton,
                playTimeLabel);
        StackPane.setAlignment(infoButton, Pos.BOTTOM_RIGHT);
        StackPane.setAlignment(playTimeLabel, Pos.BOTTOM_CENTER);
    }

    protected abstract int getCoverHeight();

    protected abstract int getCoverWidth();

    protected abstract int getInfoButtonHeight();

    protected abstract int getInfoButtonWidth();

    protected abstract int getPlayButtonHeight();

    protected abstract int getPlayButtonWidth();

    public GameEntry getEntry() {
        return entry;
    }

    public void disableInfoButton() {
        infoButton.setDisable(true);
        infoButton.setVisible(false);
    }

    public void disableTitle() {
        nameLabel.setDisable(true);
        nameLabel.setVisible(false);
    }

    public void disablePlayTimeLabel() {
        playTimeLabel.setDisable(true);
        playTimeLabel.setVisible(false);
    }
}

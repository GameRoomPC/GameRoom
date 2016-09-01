package ui.control.button.gamebutton;

import data.ImageUtils;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import system.application.settings.PredefinedSetting;
import ui.Main;
import ui.control.button.ImageButton;
import ui.scene.BaseScene;
import ui.scene.GameInfoScene;
import ui.scene.MainScene;
import data.game.entry.GameEntry;
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
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

import java.text.SimpleDateFormat;

import static ui.Main.*;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;

/**
 * Created by LM on 12/07/2016.
 */
public abstract class GameButton extends BorderPane {
    public static Image DEFAULT_COVER_IMAGE;
    private static Image DEFAULT_PLAY_IMAGE;
    private static Image DEFAULT_INFO_IMAGE;


    private final static double RATIO_NOTINSTALLEDIMAGE_COVER = 1 / 3.0;
    private final static double RATIO_PLAYBUTTON_COVER = 2 / 3.0;
    private final static double RATIO_INFOBUTTON_COVER = 1 / 6.0;

    public final static double FADE_IN_OUT_TIME = 0.1;

    public final static double COVER_HEIGHT_WIDTH_RATIO = 127.0 / 90.0;

    protected static double COVER_SCALE_EFFECT_FACTOR = 1.1;
    private final static double COVER_BLUR_EFFECT_RADIUS = 10;
    private final static double COVER_BRIGHTNESS_EFFECT_FACTOR = 0.1;

    private BaseScene parentScene;

    protected StackPane coverPane;
    protected HBox titleBox;
    protected ImageView titleLogoView;
    protected Label titleLabel;
    protected Label playTimeLabel;
    protected Label ratingLabel;
    protected Label releaseDateLabel;
    private boolean keepTimeLabelVisible = false;

    private ContextMenu contextMenu;
    protected ImageView coverView;
    protected ImageView defaultCoverView;
    private ImageView notInstalledImage = new ImageView();

    protected ImageButton playButton;
    protected ImageButton infoButton;

    protected Pane parent;


    protected GameEntry entry;
    private boolean inContextMenu = false;


    public GameButton(GameEntry entry, BaseScene scene, Pane parent) {
        super();
        this.parent = parent;
        this.entry = entry;
        this.parentScene = scene;

        initAll();
        if(parent instanceof TilePane) {
            ((TilePane)parent).prefTileWidthProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    updateAllOnTileWidth(newValue.doubleValue());
                }
            });
            ((TilePane)parent).prefTileHeightProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    updateAllOnTileHeight(newValue.doubleValue());
                }
            });
        }
    }

    public void reloadWith(GameEntry entry) {
        this.entry = entry;
        playTimeLabel.setText(entry.getPlayTimeFormatted(GameEntry.TIME_FORMAT_ROUNDED_HMS));
        ratingLabel.setText(Integer.toString(entry.getAggregated_rating()));
        SimpleDateFormat buttonDateFormat = new SimpleDateFormat("MM.yyyy");
        releaseDateLabel.setText(entry.getReleaseDate()!=null ? buttonDateFormat.format(entry.getReleaseDate()) : "-");

        titleLabel.setText(entry.getName());
        titleLabel.setTooltip(new Tooltip(entry.getName()));
        setLauncherLogo();

        double width = getCoverWidth();
        double height = getCoverHeight();

        Task<Image> loadImageTask = new Task<Image>() {
            @Override
            protected Image call() throws Exception {
                Image img = entry.getImage(0, width, height, false, true);
                return img;
            }
        };
        loadImageTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                if(!ImageUtils.imagesEquals(loadImageTask.getValue(),coverView.getImage())) {
                    Main.runAndWait(() -> {
                        ImageUtils.transitionToImage(loadImageTask.getValue(), coverView);
                    });
                }
            }
        });
        Thread imageThread = new Thread(loadImageTask);
        imageThread.setDaemon(true);
        imageThread.start();
    }
    private void setLauncherLogo(){
        double width = 20*Main.SCREEN_WIDTH/1920;
        double height =  20*Main.SCREEN_HEIGHT/1080;

        Image titleLogoImage = null;
        if(entry.isSteamGame()){
            titleLogoImage = new Image("res/ui/launcherIcons/steamChar.png",width,height,true,true);
        }else if (entry.isGoGGame()){
            titleLogoImage = new Image("res/ui/launcherIcons/gogChar.png",width,height,true,true);
        }else if (entry.isUplayGame()){
            titleLogoImage = new Image("res/ui/launcherIcons/uplayChar.png",width,height,true,true);
        }else if (entry.isOriginGame()){
            titleLogoImage = new Image("res/ui/launcherIcons/originChar.png",width,height,true,true);
        }else if (entry.isBattlenetGame()){
            titleLogoImage = new Image("res/ui/launcherIcons/battle.netChar.png",width,height,true,true);
        }
        if(!ImageUtils.imagesEquals(titleLogoImage,titleLogoView.getImage())) {
            titleLogoView.setImage(titleLogoImage);
        }
    }

    protected void initAll() {
        if (coverPane != null) {
            coverPane.getChildren().clear();
        }

        initCoverPane();

        //initContextMenu();
        initNameText();

        setOnKeyPressed(ke -> {
            if (ke.getCode() == KeyCode.ENTER) {
                if(!playButton.isDisabled())
                    playButton.fireEvent(new MouseEvent(MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 0, true, true, true, true, true, true, true, true, true, true, null));
            }
            if (ke.getCode() == KeyCode.I) {
                if(!infoButton.isDisabled())
                    infoButton.fireEvent(new MouseEvent(MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 0, true, true, true, true, true, true, true, true, true, true, null));
            }
        });
        setCenter(coverPane);
        setBottom(titleBox);
    }

    private void initNameText() {
        titleBox = new HBox();
        titleBox.setSpacing(5*Main.SCREEN_WIDTH/1920);
        BorderPane.setMargin(titleBox, new Insets(10 * GENERAL_SETTINGS.getWindowHeight() / 1080, 0, 0, 0));
        setAlignment(titleBox, Pos.CENTER);

        titleLabel = new Label(entry.getName());
        titleLabel.setTooltip(new Tooltip(entry.getName()));

        titleLogoView = new ImageView();
        titleBox.setAlignment(Pos.CENTER);
        setLauncherLogo();

        titleLabel.setScaleX(0.90f);
        titleLabel.setScaleY(0.90f);

        titleBox.getChildren().addAll(titleLogoView,titleLabel);
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

    private void initCoverPane() {
        coverPane = new StackPane();

        if (DEFAULT_PLAY_IMAGE == null) {
            DEFAULT_PLAY_IMAGE = new Image("res/ui/playButton.png", SCREEN_WIDTH/10, SCREEN_WIDTH/10, true, true, true);
        }
        if (DEFAULT_INFO_IMAGE == null) {
            DEFAULT_INFO_IMAGE = new Image("res/ui/infoButton.png", SCREEN_WIDTH/20, SCREEN_WIDTH/20, true, true, true);
        }
        DropShadow ds = new DropShadow();
        ds.setOffsetY(2.0f);
        ds.setBlurType(BlurType.GAUSSIAN);
        ds.setSpread(0.55);
        //ds.setSpread(10);
        ds.setRadius(10);
        ds.setColor(Color.color(0.2f, 0.2f, 0.2f));

        playButton = new ImageButton(DEFAULT_PLAY_IMAGE);
        infoButton = new ImageButton(DEFAULT_INFO_IMAGE);

        playTimeLabel = new Label(entry.getPlayTimeFormatted(GameEntry.TIME_FORMAT_ROUNDED_HMS));
        playTimeLabel.setEffect(ds);
        playTimeLabel.setOpacity(0);
        playTimeLabel.setFocusTraversable(false);
        playTimeLabel.setMouseTransparent(true);
        ratingLabel = new Label(Integer.toString(entry.getAggregated_rating()));
        ratingLabel.setEffect(ds);
        ratingLabel.setFocusTraversable(false);
        ratingLabel.setMouseTransparent(true);

        SimpleDateFormat buttonDateFormat = new SimpleDateFormat("MM.yyyy");
        releaseDateLabel = new Label(entry.getReleaseDate()!=null ? buttonDateFormat.format(entry.getReleaseDate()) : "-");
        releaseDateLabel.setEffect(ds);
        releaseDateLabel.setFocusTraversable(false);
        releaseDateLabel.setMouseTransparent(true);

        playButton.setOpacity(0);
        infoButton.setOpacity(0);
        ratingLabel.setOpacity(0);
        releaseDateLabel.setOpacity(0);
        playButton.setFocusTraversable(false);
        infoButton.setFocusTraversable(false);

        StackPane.setMargin(ratingLabel, new Insets(10 * Main.SCREEN_HEIGHT / 1080
                , 10 * Main.SCREEN_WIDTH / 1920
                , 2 * Main.SCREEN_HEIGHT / 1080
                , 10 * Main.SCREEN_WIDTH / 1920));
        ;
        ratingLabel.setStyle("-fx-font-family: 'Helvetica Neue';\n" +
                "    -fx-font-size: 38.0px;\n" +
                "    -fx-stroke: black;\n" +
                "    -fx-stroke-width: 1;" +
                "    -fx-font-weight: 200;");
        StackPane.setMargin(releaseDateLabel, new Insets(10 * Main.SCREEN_HEIGHT / 1080
                , 10 * Main.SCREEN_WIDTH / 1920
                , 2 * Main.SCREEN_HEIGHT / 1080
                , 10 * Main.SCREEN_WIDTH / 1920));
        ;
        releaseDateLabel.setStyle("-fx-font-family: 'Helvetica Neue';\n" +
                "    -fx-font-size: 28.0px;\n" +
                "    -fx-stroke: black;\n" +
                "    -fx-stroke-width: 1;" +
                "    -fx-font-weight: 200;");

        initCoverView();

        if (DEFAULT_COVER_IMAGE == null || DEFAULT_COVER_IMAGE.getWidth() != getCoverWidth() || DEFAULT_COVER_IMAGE.getWidth() != getCoverHeight()) {
            boolean changed = false;
            /*for (int i = 256; i < 1025; i *= 2) {
                if (i > getCoverHeight()) {
                    DEFAULT_COVER_IMAGE = new Image("res/defaultImages/cover" + i + ".jpg", getCoverWidth(), getCoverHeight(), false, true);
                    changed = true;
                    break;
                }
            }*/
            if (!changed) {
                DEFAULT_COVER_IMAGE = new Image("res/defaultImages/cover1024.jpg", getCoverWidth(), getCoverHeight(), false, true, true);
            }
        }
        defaultCoverView = new ImageView(DEFAULT_COVER_IMAGE);

        Task<Image> loadImageTask = new Task<Image>() {
            @Override
            protected Image call() throws Exception {
                Image img = entry.getImage(0, getCoverWidth(), getCoverHeight(), false, true);
                return img;
            }
        };
        loadImageTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                Main.runAndWait(() -> {
                    ImageUtils.transitionToImage(loadImageTask.getValue(), coverView);
                });
            }
        });
        Thread imageThread = new Thread(loadImageTask);
        imageThread.setDaemon(true);
        imageThread.start();

        //coverView.setPreserveRatio(true);

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
        defaultCoverView.setEffect(blur);

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
                    if (MAIN_SCENE.getInputMode() == MainScene.INPUT_MODE_KEYBOARD) {
                        playButton.fireEvent(new MouseEvent(MOUSE_ENTERED, 0, 0, 0, 0, MouseButton.PRIMARY, 0, false, false, false, false, false, false, false, false, false, false, null));
                    }
                    if (!GENERAL_SETTINGS.getBoolean(PredefinedSetting.DISABLE_MAINSCENE_WALLPAPER)) {
                        Task backGroundImageTask = new Task() {
                            @Override
                            protected Object call() throws Exception {
                                Image screenshotImage = entry.getImage(1,
                                        Main.GENERAL_SETTINGS.getWindowWidth(),
                                        Main.GENERAL_SETTINGS.getWindowHeight()
                                        , false, true);

                                Main.runAndWait(() -> {
                                    MAIN_SCENE.setImageBackground(screenshotImage);
                                });
                                /*Thread.currentThread().sleep(300);
                                if (isFocused()) {

                                    Platform.runLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            MAIN_SCENE.setImageBackground(screenshotImage);
                                        }
                                    });
                                }*/
                                return null;
                            }
                        };
                        Thread setBackgroundThread = new Thread(backGroundImageTask);
                        setBackgroundThread.setDaemon(true);
                        setBackgroundThread.start();
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
                                        new KeyValue(playTimeLabel.opacityProperty(), keepTimeLabelVisible ? 1 : 0, Interpolator.EASE_OUT),
                                        new KeyValue(scaleYProperty(), 1, Interpolator.LINEAR),
                                        new KeyValue(coverColorAdjust.brightnessProperty(), 0, Interpolator.LINEAR)
                                ));
                        fadeOutTimeline.setCycleCount(1);
                        fadeOutTimeline.setAutoReverse(false);

                        fadeOutTimeline.play();
                    }
                    //coverPane.fireEvent(new MouseEvent(MOUSE_EXITED,0,0,0,0, MouseButton.PRIMARY,0,false, false, false, false, false, false, false, false, false, false, null));
                    if (MAIN_SCENE.getInputMode() == MainScene.INPUT_MODE_KEYBOARD) {
                        playButton.fireEvent(new MouseEvent(MOUSE_EXITED, 0, 0, 0, 0, MouseButton.PRIMARY, 0, false, false, false, false, false, false, false, false, false, false, null));
                    }
                }
            }
        });
        coverPane.setOnMouseMoved(e -> {
            if (MAIN_SCENE.getInputMode() == MainScene.INPUT_MODE_MOUSE) {
                if (this.contains(new Point2D(e.getX(), e.getY()))) {
                    requestFocus();
                }
            }
        });
        coverPane.setOnMouseEntered(e -> {
            if (MAIN_SCENE.getInputMode() == MainScene.INPUT_MODE_MOUSE) {
                requestFocus();
            }
        });

        coverPane.setOnMouseExited(e -> {
            if (MAIN_SCENE.getInputMode() == MainScene.INPUT_MODE_MOUSE) {
                //requestFocus();
                if(!keepTimeLabelVisible) {
                    Timeline fadeOutTimeline = new Timeline(
                            new KeyFrame(Duration.seconds(0),
                                    new KeyValue(playTimeLabel.opacityProperty(), playTimeLabel.opacityProperty().getValue(), Interpolator.EASE_OUT)),
                            new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                                    new KeyValue(playTimeLabel.opacityProperty(), keepTimeLabelVisible? 1 : 0, Interpolator.EASE_OUT)
                            ));
                    fadeOutTimeline.setCycleCount(1);
                    fadeOutTimeline.setAutoReverse(false);

                    fadeOutTimeline.play();
                }
            }
        });
        playButton.addMouseEnteredHandler(e -> {
            if (MAIN_SCENE.getInputMode() == MainScene.INPUT_MODE_MOUSE) {
                requestFocus();
            }
        });
        infoButton.addMouseEnteredHandler(e -> {
            playTimeLabel.setText(entry.getPlayTimeFormatted(GameEntry.TIME_FORMAT_ROUNDED_HMS));
            if (MAIN_SCENE.getInputMode() == MainScene.INPUT_MODE_MOUSE) {
                requestFocus();
                playTimeLabel.setOpacity(keepTimeLabelVisible? 1 : 0);
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
            if (MAIN_SCENE.getInputMode() == MainScene.INPUT_MODE_MOUSE) {
                if(!keepTimeLabelVisible) {
                    Timeline fadeOutTimeline = new Timeline(
                            new KeyFrame(Duration.seconds(0),
                                    new KeyValue(playTimeLabel.opacityProperty(), playTimeLabel.opacityProperty().getValue(), Interpolator.EASE_OUT)),
                            new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                                    new KeyValue(playTimeLabel.opacityProperty(), keepTimeLabelVisible? 1 : 0, Interpolator.EASE_OUT)
                            ));
                    fadeOutTimeline.setCycleCount(1);
                    fadeOutTimeline.setAutoReverse(false);

                    fadeOutTimeline.play();

                    playTimeLabel.setOpacity(0);
                }
            }
        });

        coverPane.getChildren().addAll(
                defaultCoverView,
                coverView,
                playButton,
                infoButton,
                playTimeLabel,
                ratingLabel,
                releaseDateLabel);
        StackPane.setAlignment(infoButton, Pos.BOTTOM_RIGHT);
        StackPane.setAlignment(playTimeLabel, Pos.BOTTOM_CENTER);
        StackPane.setAlignment(ratingLabel, Pos.BOTTOM_LEFT);
        StackPane.setAlignment(releaseDateLabel, Pos.BOTTOM_LEFT);

        notInstalledImage.setPreserveRatio(true);
        coverPane.getChildren().add(notInstalledImage);
        StackPane.setAlignment(notInstalledImage, Pos.TOP_RIGHT);
        setNotInstalledEffect();
    }

    protected abstract int getCoverHeight();

    protected abstract int getCoverWidth();

    protected abstract void initCoverView();

    public void showPlaytime() {
        keepTimeLabelVisible = true;
        playTimeLabel.setOpacity(1);
    }

    public void showRating() {
        ratingLabel.setOpacity(1);
    }

    public void hidePlaytime() {
        keepTimeLabelVisible = false;
        playTimeLabel.setOpacity(0);
    }

    public void hideRating() {
        ratingLabel.setOpacity(0);
    }

    public GameEntry getEntry() {
        return entry;
    }

    protected void disableNode(Node node, boolean disable) {
        node.setVisible(!disable);
        node.setDisable(disable);
        node.setManaged(!disable);
        node.setMouseTransparent(true);
    }
    public void hideReleaseDate() {
        releaseDateLabel.setOpacity(0);
    }

    public void showReleaseDate() {
        releaseDateLabel.setOpacity(1);
    }

    protected final void initContentSize(double width, double size){
        updateAllOnTileWidth(width);
        updateAllOnTileHeight(size);
    }
    protected final void initContentSize(TilePane pane){
        updateAllOnTileWidth(pane.getPrefTileWidth());
        updateAllOnTileHeight(pane.getPrefTileHeight());
    }
    private final void updateAllOnTileWidth(double width){
        setPrefWidth(width);
        setWidth(width);
        coverView.setFitWidth(width);
        defaultCoverView.setFitWidth(width);
        playButton.setFitWidth(width * RATIO_PLAYBUTTON_COVER);
        infoButton.setFitWidth(width * RATIO_INFOBUTTON_COVER);
        notInstalledImage.setFitWidth(width * RATIO_NOTINSTALLEDIMAGE_COVER);

        onNewTileWidth(width);
    }
    private final void updateAllOnTileHeight(double height){
        setPrefHeight(height);
        setHeight(height);
        coverView.setFitHeight(height);
        defaultCoverView.setFitHeight(height);
        playButton.setFitHeight(height * RATIO_PLAYBUTTON_COVER);
        infoButton.setFitHeight(height * RATIO_INFOBUTTON_COVER);
        notInstalledImage.setFitHeight(height * RATIO_NOTINSTALLEDIMAGE_COVER);

        onNewTileHeight(height);
    }

    protected abstract void onNewTileWidth(double width);

    protected abstract void onNewTileHeight(double height);

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

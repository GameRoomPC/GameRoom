package UI.button.gamebutton;

import UI.button.ImageButton;
import UI.scene.BaseScene;
import UI.scene.GameInfoScene;
import data.GameEntry;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
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
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;

import static UI.Main.*;
import static UI.Main.FADE_IN_OUT_TIME;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;

/**
 * Created by LM on 12/07/2016.
 */
public abstract class GameButton extends BorderPane {
    public final static double COVER_HEIGHT_WIDTH_RATIO = 1.48;

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
    protected void initAll(){
        if(coverPane!=null){
            coverPane.getChildren().clear();
        }

        initCoverPane(entry);
        initContextMenu();
        initNameText(entry);

        setCenter(coverPane);
        setBottom(nameLabel);
    }
    private void initNameText(GameEntry entry) {
        nameLabel = new Label(entry.getName());
        BorderPane.setMargin(nameLabel, new Insets(10*GENERAL_SETTINGS.getWindowHeight()/1080, 0, 0, 0));
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
        cmItem2.setOnAction(eh ->{
        });
        contextMenu.getItems().add(cmItem2);
        MenuItem cmItem3 = new MenuItem(RESSOURCE_BUNDLE.getString("About"));
        cmItem3.setOnAction(nh->{
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
        Image playImage = new Image("res/ui/playButton.png", getPlayButtonWidth(), getPlayButtonHeight(), true, true);
        Image infoImage = new Image("res/ui/infoButton.png", getInfoButtonWidth(), getInfoButtonHeight(), true, true);

        playButton = new ImageButton(playImage);
        infoButton = new ImageButton(infoImage);
        playTimeLabel = new Label(entry.getPlayTimeFormatted(false));
        playButton.setOpacity(0);
        infoButton.setOpacity(0);
        playTimeLabel.setOpacity(0);
        playButton.setFocusTraversable(false);
        infoButton.setFocusTraversable(false);
        playTimeLabel.setFocusTraversable(false);
        playTimeLabel.setMouseTransparent(true);

        coverView = new ImageView( entry.getImage(0, getCoverWidth(), getCoverHeight(), false, true));
        coverView.setPreserveRatio(true);

        playButton.setOnMouseClicked(mc -> {
            Task<Long> monitor = new Task() {
                @Override
                protected Object call() throws Exception {
                    Process process = new ProcessBuilder(entry.getPath()).start();
                    long startTime = System.currentTimeMillis();

                    boolean keepRunning = true;
                    logger.info("Monitoring "+ entry.getProcessName());
                    while (keepRunning) {

                        keepRunning = isProcessRunning(entry.getProcessName());
                        if(!keepRunning){
                            logger.info(entry.getProcessName()+" killed");
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    long stopTime = System.currentTimeMillis();
                    return stopTime - startTime;
                }
            };
            monitor.valueProperty().addListener(new ChangeListener<Long>() {
                @Override
                public void changed(ObservableValue<? extends Long> observable, Long oldValue, Long newValue) {
                    logger.debug("Adding "+Math.round(newValue/1000.0)+"s to game "+entry.getName());
                    entry.setSavedLocaly(true);
                    entry.addPlayTimeSeconds(Math.round(newValue/1000.0));
                    entry.setSavedLocaly(false);
                }
            });
            Thread th = new Thread(monitor);
            th.setDaemon(true);
            th.start();
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
                    playButton.setDisable(false);
                    infoButton.setDisable(false);
                    playTimeLabel.setDisable(false);

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
                    if (getCursor() != null && parentScene.getCursor().equals(Cursor.NONE)) {
                        playButton.fireEvent(new MouseEvent(MOUSE_ENTERED, 0, 0, 0, 0, MouseButton.PRIMARY, 0, false, false, false, false, false, false, false, false, false, false, null));
                    }
                } else {
                    if (!inContextMenu) {
                        playButton.setDisable(true);
                        infoButton.setDisable(true);
                        playTimeLabel.setDisable(true);

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
                    if(parentScene.getCursor() == null){
                        parentScene.setCursor(Cursor.DEFAULT);
                    }
                    if (parentScene.getCursor() != null && parentScene.getCursor().equals(Cursor.NONE)) {
                        playButton.fireEvent(new MouseEvent(MOUSE_EXITED, 0, 0, 0, 0, MouseButton.PRIMARY, 0, false, false, false, false, false, false, false, false, false, false, null));
                    }
                }
            }
        });
        coverPane.setOnMouseMoved(e -> {
            if (this.contains(new Point2D(e.getX(), e.getY()))) {
                requestFocus();
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
                /*Timeline fadeOutTimeline = new Timeline(
                        new KeyFrame(Duration.seconds(0),
                                new KeyValue(playTimeLabel.opacityProperty(), playTimeLabel.opacityProperty().getValue(), Interpolator.EASE_OUT)),
                        new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                                new KeyValue(playTimeLabel.opacityProperty(), 0, Interpolator.EASE_OUT)
                        ));
                fadeOutTimeline.setCycleCount(1);
                fadeOutTimeline.setAutoReverse(false);

                fadeOutTimeline.play();*/
            }
        });
        infoButton.addMouseEnteredHandler(e -> {
            if(getCursor() == null){
                setCursor(Cursor.DEFAULT);
            }
            if (getCursor() != null && !getCursor().equals(Cursor.NONE)) {
                Timeline fadeInTimeline = new Timeline(
                        new KeyFrame(Duration.seconds(0),
                                new KeyValue(playTimeLabel.opacityProperty(), playTimeLabel.opacityProperty().getValue(), Interpolator.EASE_IN)),
                        new KeyFrame(Duration.seconds(5*FADE_IN_OUT_TIME),
                                new KeyValue(playTimeLabel.opacityProperty(), 1, Interpolator.EASE_IN)
                        ));
                fadeInTimeline.setCycleCount(1);
                fadeInTimeline.setAutoReverse(false);

                fadeInTimeline.play();
            }
        });
        infoButton.addMouseExitedHandler(e -> {
            playTimeLabel.setText(entry.getPlayTimeFormatted(false));
            if(getCursor() == null){
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

    public void disableInfoButton(){
        infoButton.setDisable(true);
        infoButton.setVisible(false);
    }
    public void disableTitle(){
        nameLabel.setDisable(true);
        nameLabel.setVisible(false);
    }
    public void disablePlayTimeLabel(){
        playTimeLabel.setDisable(true);
        playTimeLabel.setVisible(false);

    }

    public static boolean isProcessRunning(String process) {
        boolean found = false;
        try {
            File file = File.createTempFile("process_watcher",".vbs");
            file.deleteOnExit();
            FileWriter fw = new java.io.FileWriter(file);

            String vbs = "Set WshShell = WScript.CreateObject(\"WScript.Shell\")\n"
                    + "Set locator = CreateObject(\"WbemScripting.SWbemLocator\")\n"
                    + "Set service = locator.ConnectServer()\n"
                    + "Set processes = service.ExecQuery _\n"
                    + " (\"select * from Win32_Process where name='" + process +"'\")\n"
                    + "For Each process in processes\n"
                    + "wscript.echo process.Name \n"
                    + "Next\n"
                    + "Set WSHShell = Nothing\n";

            fw.write(vbs);
            fw.close();
            Process p = Runtime.getRuntime().exec("cscript //NoLogo " + file.getPath());
            BufferedReader input =
                    new BufferedReader
                            (new InputStreamReader(p.getInputStream()));
            String line;
            line = input.readLine();
            if (line != null) {
                if (line.equals(process)) {
                    found = true;
                }
            }
            input.close();

        }
        catch(Exception e){
            e.printStackTrace();
        }
        return found;
    }
}

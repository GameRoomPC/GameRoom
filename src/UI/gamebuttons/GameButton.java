package UI.gamebuttons;

import UI.ImageButton;
import UI.scene.GameInfoScene;
import UI.scene.BaseScene;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import data.GameEntry;

import static UI.Main.*;
import static UI.scene.MainScene.COVER_HEIGHT_WIDTH_RATIO;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;

/**
 * Created by LM on 02/07/2016.
 */
public class GameButton extends BorderPane {
    private final static double COVER_SCALE_EFFECT_FACTOR = 1.1;
    private final static double COVER_BLUR_EFFECT_RADIUS = 10;
    private final static double COVER_BRIGHTNESS_EFFECT_FACTOR = 0.1;

    private final static double RATIO_PLAYBUTTON_COVER = 2/3.0;
    private final static double RATIO_INFOBUTTON_COVER = 1/6.0;

    private BaseScene parentScene;

    private StackPane coverPane;
    private Label nameLabel;
    private ContextMenu contextMenu;
    private ImageView coverView;
    private ImageButton playButton;
    private ImageButton infoButton;

    private GameEntry entry;
    private boolean inContextMenu = false;

    public GameButton(GameEntry entry, TilePane parent, BaseScene scene){
        super();
        this.entry = entry;
        this.parentScene = scene;
        initCoverPane(entry, parent);
        initContextMenu(entry);
        initNameText(entry);
        setSizeToPrefTileSize(parent);
        setCenter(coverPane);
        setBottom(nameLabel);
        setPrefWidth(parent.getPrefTileWidth());
        setPrefHeight(parent.getPrefHeight());
        parent.prefTileWidthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                GameButton.this.setPrefWidth(newValue.doubleValue());
                GameButton.this.setWidth(newValue.doubleValue());
                coverView.setFitWidth(newValue.doubleValue());
                playButton.setFitWidth(newValue.doubleValue()*RATIO_PLAYBUTTON_COVER);
                infoButton.setFitWidth(newValue.doubleValue()*RATIO_INFOBUTTON_COVER);
            }
        });
        parent.prefTileHeightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                GameButton.this.setPrefHeight(newValue.doubleValue());
                GameButton.this.setHeight(newValue.doubleValue());
                coverView.setFitHeight(newValue.doubleValue());
                playButton.setFitHeight(newValue.doubleValue()*RATIO_PLAYBUTTON_COVER);
                infoButton.setFitHeight(newValue.doubleValue()*RATIO_INFOBUTTON_COVER);
            }
        });
        setFocusTraversable(true);
        focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if(newValue){
                    coverPane.fireEvent(new MouseEvent(MOUSE_ENTERED,0,0,0,0, MouseButton.PRIMARY,0,false, false, false, false, false, false, false, false, false, false, null));
                    playButton.fireEvent(new MouseEvent(MOUSE_ENTERED,0,0,0,0, MouseButton.PRIMARY,0,false, false, false, false, false, false, false, false, false, false, null));
                    scene.setCursor(Cursor.NONE);
                    scene.addEventHandler(MouseEvent.MOUSE_MOVED, new EventHandler<MouseEvent>(){

                        @Override
                        public void handle(MouseEvent event) {
                            if(scene.getCursor().equals(Cursor.NONE)){
                                scene.setCursor(Cursor.DEFAULT);
                                for(Node node : getChildren()){
                                    node.fireEvent(new MouseEvent(MOUSE_EXITED,0,0,0,0, MouseButton.PRIMARY,0,false, false, false, false, false, false, false, false, false, false, null));
                                }
                                scene.removeEventHandler(MouseEvent.MOUSE_MOVED, this);
                            }
                        }
                    });

                }else{
                    coverPane.fireEvent(new MouseEvent(MOUSE_EXITED,0,0,0,0, MouseButton.PRIMARY,0,false, false, false, false, false, false, false, false, false, false, null));
                    playButton.fireEvent(new MouseEvent(MOUSE_EXITED,0,0,0,0, MouseButton.PRIMARY,0,false, false, false, false, false, false, false, false, false, false, null));
                }
            }
        });
    }

    private void setSizeToPrefTileSize(TilePane parent) {
        GameButton.this.setPrefWidth(parent.getPrefTileWidth());
        GameButton.this.setWidth(parent.getPrefTileWidth());
        coverView.setFitWidth(parent.getPrefTileWidth());
        playButton.setFitWidth(parent.getPrefTileWidth()*RATIO_PLAYBUTTON_COVER);
        infoButton.setFitWidth(parent.getPrefTileWidth()*RATIO_INFOBUTTON_COVER);

        GameButton.this.setPrefHeight(parent.getPrefTileHeight());
        GameButton.this.setHeight(parent.getPrefTileHeight());
        coverView.setFitHeight(parent.getPrefTileHeight());
        playButton.setFitHeight(parent.getPrefTileHeight()*RATIO_PLAYBUTTON_COVER);
        infoButton.setFitHeight(parent.getPrefTileHeight()*RATIO_INFOBUTTON_COVER);
    }

    private void initNameText(GameEntry entry){
        nameLabel = new Label(entry.getName());
        BorderPane.setMargin(nameLabel, new Insets(10,0,0,0));
        setAlignment(nameLabel,Pos.CENTER);

        nameLabel.setScaleX(0.90f);
        nameLabel.setScaleY(0.90f);
    }
    private void initContextMenu(GameEntry entry){
        contextMenu = new ContextMenu();
        MenuItem cmItem1 = new MenuItem(RESSOURCE_BUNDLE.getString("Play"));
        cmItem1.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {

            }
        });
        contextMenu.getItems().add(cmItem1);
        MenuItem cmItem2 = new MenuItem(RESSOURCE_BUNDLE.getString("Settings"));
        contextMenu.getItems().add(cmItem2);
        MenuItem cmItem3 = new MenuItem(RESSOURCE_BUNDLE.getString("About"));
        contextMenu.getItems().add(cmItem3);
        contextMenu.setOnShowing(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                inContextMenu=true;
            }
        });
        contextMenu.setOnHiding(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                inContextMenu=false;
                coverPane.fireEvent(new MouseEvent(MOUSE_EXITED,0,0,0,0, MouseButton.PRIMARY,0,true, true, true, true, true, true, true, true, true, true, null));
            }
        });
        coverPane.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.isSecondaryButtonDown()) {
                    contextMenu.show(coverPane, event.getScreenX(), event.getScreenY());
                }
                event.consume();
            }
        });
    }
    private void initCoverPane(GameEntry entry, TilePane parent){
        coverPane = new StackPane();
        Image playImage = new Image("res/ui/playButton.png", parent.getPrefTileWidth()*RATIO_PLAYBUTTON_COVER, parent.getPrefTileHeight()*RATIO_PLAYBUTTON_COVER, true, true);
        Image infoImage = new Image("res/ui/infoButton.png", parent.getPrefTileWidth()*RATIO_INFOBUTTON_COVER, parent.getPrefTileHeight()*RATIO_INFOBUTTON_COVER, true, true);

        playButton = new ImageButton(playImage);
        infoButton = new ImageButton(infoImage);
        playButton.setOpacity(0);
        infoButton.setOpacity(0);
        playButton.setFocusTraversable(false);
        infoButton.setFocusTraversable(false);

        coverView = new ImageView(new Image(entry.getImagePath(0),WIDTH/4,WIDTH/4*COVER_HEIGHT_WIDTH_RATIO,false,true));
        coverView.setPreserveRatio(true);

        infoButton.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                parentScene.fadeTransitionTo(new GameInfoScene(new StackPane(),WIDTH, HEIGHT,parentScene.getParentStage(),parentScene,entry), parentScene.getParentStage());
            }
        });

        //COVER EFFECTS
        DropShadow dropShadow = new DropShadow();
        dropShadow.setOffsetX(6.0*WIDTH/1920);
        dropShadow.setOffsetY(4.0*HEIGHT/1080);

        ColorAdjust coverColorAdjust = new ColorAdjust();
        coverColorAdjust.setBrightness(0.0);

        coverColorAdjust.setInput(dropShadow);

        GaussianBlur blur = new GaussianBlur(0.0);
        blur.setInput(coverColorAdjust);
        coverView.setEffect(blur);


        coverPane.setOnMouseEntered(e -> {
           // playButton.setVisible(true);
            //infoButton.setVisible(true);
            //coverPane.requestFocus();
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

        });

        coverPane.setOnMouseExited(e -> {
            if(!inContextMenu) {
                //playButton.setVisible(false);
                //infoButton.setVisible(false);
                Timeline fadeOutTimeline = new Timeline(
                        new KeyFrame(Duration.seconds(0),
                                new KeyValue(blur.radiusProperty(), blur.radiusProperty().getValue(), Interpolator.LINEAR),
                                new KeyValue(scaleXProperty(), scaleXProperty().getValue(), Interpolator.LINEAR),
                                new KeyValue(playButton.opacityProperty(), playButton.opacityProperty().getValue(), Interpolator.EASE_OUT),
                                new KeyValue(infoButton.opacityProperty(), infoButton.opacityProperty().getValue(), Interpolator.EASE_OUT),
                                new KeyValue(scaleYProperty(), scaleYProperty().getValue(), Interpolator.LINEAR),
                                new KeyValue(coverColorAdjust.brightnessProperty(), coverColorAdjust.brightnessProperty().getValue(), Interpolator.LINEAR)),
                        new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                                new KeyValue(blur.radiusProperty(), 0, Interpolator.LINEAR),
                                new KeyValue(scaleXProperty(), 1, Interpolator.LINEAR),
                                new KeyValue(playButton.opacityProperty(), 0, Interpolator.EASE_OUT),
                                new KeyValue(infoButton.opacityProperty(), 0, Interpolator.EASE_OUT),
                                new KeyValue(scaleYProperty(), 1, Interpolator.LINEAR),
                                new KeyValue(coverColorAdjust.brightnessProperty(), 0, Interpolator.LINEAR)
                        ));
                fadeOutTimeline.setCycleCount(1);
                fadeOutTimeline.setAutoReverse(false);

                fadeOutTimeline.play();
            }
        });

        coverPane.getChildren().add(coverView);
        coverPane.getChildren().add(playButton);
        coverPane.getChildren().add(infoButton);
        StackPane.setAlignment(infoButton, Pos.BOTTOM_RIGHT);
    }

    public GameEntry getEntry() {
        return entry;
    }
}

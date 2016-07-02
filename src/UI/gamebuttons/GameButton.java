package UI.gamebuttons;

import UI.Main;
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
import javafx.scene.Scene;
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

import static UI.Main.FADE_IN_OUT_TIME;
import static UI.Main.addEffectsToButton;
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

    private StackPane coverPane;
    private Label nameLabel;
    private ContextMenu contextMenu;
    private ImageView coverView;
    private ImageView playButton;
    private ImageView infoButton;

    private boolean inContextMenu = false;

    public GameButton(GameEntry entry, TilePane parent, Scene scene){
        super();
        initCoverPane(entry, parent);
        initContextMenu(entry);
        initNameText(entry);

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
    private void initNameText(GameEntry entry){
        nameLabel = new Label(entry.getName());
        BorderPane.setMargin(nameLabel, new Insets(10,0,0,0));
        setAlignment(nameLabel,Pos.CENTER);

        nameLabel.setScaleX(0.90f);
        nameLabel.setScaleY(0.90f);
    }
    private void initContextMenu(GameEntry entry){
        contextMenu = new ContextMenu();
        MenuItem cmItem1 = new MenuItem("Jouer");
        cmItem1.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {

            }
        });
        contextMenu.getItems().add(cmItem1);
        MenuItem cmItem2 = new MenuItem("Réglages");
        contextMenu.getItems().add(cmItem2);
        MenuItem cmItem3 = new MenuItem("A propos");
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
                }else{
                    System.out.println("Tile pressed ");
                }
                event.consume();
            }
        });
    }
    private void initCoverPane(GameEntry entry, TilePane parent){
        coverPane = new StackPane();
        Image playImage = new Image("res/ui/playButton3.png", parent.getPrefTileWidth()*RATIO_PLAYBUTTON_COVER, parent.getPrefTileHeight()*RATIO_PLAYBUTTON_COVER, true, true);
        Image infoImage = new Image("res/ui/infoButton2.png", parent.getPrefTileWidth()*RATIO_INFOBUTTON_COVER, parent.getPrefTileHeight()*RATIO_INFOBUTTON_COVER, true, true);

        playButton = new ImageView(playImage);
        infoButton = new ImageView(infoImage);
        coverView = new ImageView(entry.getImage(0));

        coverView.setPreserveRatio(true);
        playButton.setPreserveRatio(true);
        infoButton.setPreserveRatio(true);

        playButton.setVisible(false);
        infoButton.setVisible(false);

        //COVER EFFECTS
        DropShadow dropShadow = new DropShadow();
        dropShadow.setOffsetX(6.0);
        dropShadow.setOffsetY(4.0);

        ColorAdjust coverColorAdjust = new ColorAdjust();
        coverColorAdjust.setBrightness(0.0);

        coverColorAdjust.setInput(dropShadow);

        GaussianBlur blur = new GaussianBlur(0.0);
        blur.setInput(coverColorAdjust);
        coverView.setEffect(blur);

        addEffectsToButton(playButton);
        addEffectsToButton(infoButton);

        coverPane.setOnMouseEntered(e -> {
            playButton.setVisible(true);
            infoButton.setVisible(true);
            //coverPane.requestFocus();
            Timeline fadeInTimeline = new Timeline(
                    new KeyFrame(Duration.seconds(0),
                            new KeyValue(blur.radiusProperty(), blur.radiusProperty().getValue(), Interpolator.LINEAR),
                            new KeyValue(scaleXProperty(), scaleXProperty().getValue(), Interpolator.LINEAR),
                            new KeyValue(scaleYProperty(), scaleYProperty().getValue(), Interpolator.LINEAR),
                            new KeyValue(coverColorAdjust.brightnessProperty(), coverColorAdjust.brightnessProperty().getValue(), Interpolator.LINEAR)),
                    new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                            new KeyValue(blur.radiusProperty(), COVER_BLUR_EFFECT_RADIUS, Interpolator.LINEAR),
                            new KeyValue(scaleXProperty(), COVER_SCALE_EFFECT_FACTOR, Interpolator.LINEAR),
                            new KeyValue(scaleYProperty(), COVER_SCALE_EFFECT_FACTOR, Interpolator.LINEAR),
                            new KeyValue(coverColorAdjust.brightnessProperty(), -COVER_BRIGHTNESS_EFFECT_FACTOR, Interpolator.LINEAR)
                    ));
            fadeInTimeline.setCycleCount(1);
            fadeInTimeline.setAutoReverse(false);

            fadeInTimeline.play();

        });

        coverPane.setOnMouseExited(e -> {
            if(!inContextMenu) {
                playButton.setVisible(false);
                infoButton.setVisible(false);
                Timeline fadeOutTimeline = new Timeline(
                        new KeyFrame(Duration.seconds(0),
                                new KeyValue(blur.radiusProperty(), blur.radiusProperty().getValue(), Interpolator.LINEAR),
                                new KeyValue(scaleXProperty(), scaleXProperty().getValue(), Interpolator.LINEAR),
                                new KeyValue(scaleYProperty(), scaleYProperty().getValue(), Interpolator.LINEAR),
                                new KeyValue(coverColorAdjust.brightnessProperty(), coverColorAdjust.brightnessProperty().getValue(), Interpolator.LINEAR)),
                        new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                                new KeyValue(blur.radiusProperty(), 0, Interpolator.LINEAR),
                                new KeyValue(scaleXProperty(), 1, Interpolator.LINEAR),
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
}

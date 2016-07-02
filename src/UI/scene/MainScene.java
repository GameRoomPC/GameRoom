package UI.scene;

import UI.gamebuttons.GameButton;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import sample.GameEntry;
import sample.Main;

import static sample.Main.HEIGHT;
import static sample.Main.WIDTH;

/**
 * Created by LM on 03/07/2016.
 */
public class MainScene extends GameRoomScene {
    public final static double COVER_HEIGHT_WIDTH_RATIO = 1.48;

    public final static double MAX_SCALE_FACTOR = 0.9;
    public final static double MIN_SCALE_FACTOR = 0.1;

    private BorderPane wrappingPane;

    TilePane tilePane = new TilePane();
    public MainScene(int width, int height, Stage parentStage) {
        super(new StackPane(), width, height, parentStage);
        getStylesheets().add("res/flatterfx.css");
        tilePane.setHgap(30);
        tilePane.setVgap(30);
        tilePane.setPrefColumns(5);
        tilePane.setPrefTileWidth(WIDTH/4);
        tilePane.setPrefTileHeight(tilePane.getPrefTileWidth()*COVER_HEIGHT_WIDTH_RATIO);
        BorderPane.setMargin(tilePane, new Insets(50,50,50,50));
        initTestValues();
        initTop();
    }

    @Override
    public Pane getWrappingPane() {
        return wrappingPane;
    }

    @Override
    void initAndAddWrappingPaneToRoot() {
        wrappingPane = new BorderPane();
        getRootStackPane().getChildren().add(wrappingPane);
    }

    private void initTestValues(){
        GameEntry bf4 = new GameEntry("Battlefield 4");
        Image image = new Image("res/bf4.bmp", WIDTH/4,WIDTH/4*COVER_HEIGHT_WIDTH_RATIO,false,true);
        bf4.setImages(new Image[]{image});

        GameEntry tw3 = new GameEntry("The Witcher 3");
        Image image2 = new Image("res/tw3.bmp",WIDTH/4,WIDTH/4*COVER_HEIGHT_WIDTH_RATIO,false,true);
        tw3.setImages(new Image[]{image2});

        GameEntry gtav = new GameEntry("GTA V");
        Image image3 = new Image("res/gtav.bmp",WIDTH/4,WIDTH/4*COVER_HEIGHT_WIDTH_RATIO,false,true);
        gtav.setImages(new Image[]{image3});

        tilePane.getChildren().add(new GameButton(bf4, tilePane, this));
        tilePane.getChildren().add(new GameButton(tw3, tilePane, this));
        tilePane.getChildren().add(new GameButton(gtav, tilePane, this));

        wrappingPane.setCenter(tilePane);
    }
    private void initTop(){
        Slider sizeSlider = new Slider();
        sizeSlider.setMin(MIN_SCALE_FACTOR);
        sizeSlider.setMax(MAX_SCALE_FACTOR);
        sizeSlider.setBlockIncrement(0.1);
        sizeSlider.setFocusTraversable(false);

        sizeSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                tilePane.setPrefTileWidth(Main.WIDTH/4*newValue.doubleValue());
                tilePane.setPrefTileHeight(Main.WIDTH/4*COVER_HEIGHT_WIDTH_RATIO*newValue.doubleValue());
            }
        });
        sizeSlider.setValue(0.4);

        sizeSlider.setPrefWidth(Main.WIDTH/4);
        sizeSlider.setMaxWidth(Main.WIDTH/4);

        Image settingsImage = new Image("res/ui/settingsButton.png",WIDTH/30,WIDTH/30,true,true);
        ImageView settingsButton = new ImageView(settingsImage);
        settingsButton.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if(event.isPrimaryButtonDown()){
                    SettingsScene settingsScene = new SettingsScene(new StackPane(), WIDTH, HEIGHT,getParentStage());
                    fadeTransitionTo(settingsScene,getParentStage());
                }
            }
        });

        HBox hbox = new HBox();
        hbox.setPadding(new Insets(15, 12, 15, 12));
        hbox.setSpacing(10);
        hbox.getChildren().add(settingsButton);
        hbox.getChildren().add(sizeSlider);
        HBox.setMargin(sizeSlider, new Insets(15, 12, 15, 12));

        wrappingPane.setTop(hbox);
    }
}

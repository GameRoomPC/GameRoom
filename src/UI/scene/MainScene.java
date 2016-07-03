package UI.scene;

import UI.gamebuttons.GameButton;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import data.GameEntry;
import UI.Main;

import java.io.File;

import static UI.Main.HEIGHT;
import static UI.Main.RESSOURCE_BUNDLE;
import static UI.Main.WIDTH;

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

        initCenter();
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

    private void initCenter(){
        tilePane.setHgap(30);
        tilePane.setVgap(30);
        tilePane.setPrefColumns(5);
        tilePane.setPrefTileWidth(WIDTH/4);
        tilePane.setPrefTileHeight(tilePane.getPrefTileWidth()*COVER_HEIGHT_WIDTH_RATIO);
        BorderPane.setMargin(tilePane, new Insets(50,50,50,50));

        GameEntry bf4 = new GameEntry("Battlefield 4");
        bf4.setImagesPaths(new String[]{"res/bf4.bmp"});
        Image image = new Image(bf4.getImagePath(0), WIDTH/4,WIDTH/4*COVER_HEIGHT_WIDTH_RATIO,false,true);

        GameEntry tw3 = new GameEntry("The Witcher 3");
        tw3.setImagesPaths(new String[]{"res/tw3.bmp"});
        Image image2 = new Image(tw3.getImagePath(0),WIDTH/4,WIDTH/4*COVER_HEIGHT_WIDTH_RATIO,false,true);

        GameEntry gtav = new GameEntry("GTA V");
        gtav.setImagesPaths(new String[]{"res/gtav.bmp"});
        Image image3 = new Image(gtav.getImagePath(0),WIDTH/4,WIDTH/4*COVER_HEIGHT_WIDTH_RATIO,false,true);

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
        sizeSlider.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                Main.GENERAL_SETTINGS.setTileZoom(sizeSlider.getValue());
            }
        });
        sizeSlider.setValue(Main.GENERAL_SETTINGS.getTileZoom());

        sizeSlider.setPrefWidth(Main.WIDTH/8);
        sizeSlider.setMaxWidth(Main.WIDTH/8);
        sizeSlider.setPrefHeight(Main.WIDTH/160);
        sizeSlider.setMaxHeight(Main.WIDTH/160);

        /*sizeSlider.setScaleX(0.7);
        sizeSlider.setScaleY(0.7);*/

        Image settingsImage = new Image("res/ui/settingsButton.png",WIDTH/40,WIDTH/40,true,true);
        ImageView settingsButton = new ImageView(settingsImage);
        settingsButton.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if(event.isPrimaryButtonDown()){
                    SettingsScene settingsScene = new SettingsScene(new StackPane(), WIDTH, HEIGHT,getParentStage(), MainScene.this);
                    fadeTransitionTo(settingsScene,getParentStage());
                }
            }
        });
        Main.addEffectsToButton(settingsButton);
        /*settingsButton.setScaleX(0.7);
        settingsButton.setScaleY(0.7);*/

        Image addImage = new Image("res/ui/addButton.png",WIDTH/45,WIDTH/45,true,true);
        ImageView addButton = new ImageView(addImage);

        ContextMenu addMenu = new ContextMenu();
        MenuItem addExeItem = new MenuItem(RESSOURCE_BUNDLE.getString("Add_exe"));
        MenuItem addFolderItem = new MenuItem(RESSOURCE_BUNDLE.getString("Add_folder_ink"));
        addFolderItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setTitle(RESSOURCE_BUNDLE.getString("Select_folder_ink"));
                File selectedFile = directoryChooser.showDialog(getParentStage());
                if(selectedFile!=null){
                    //TODO IMPLEMENT CONFIGURATION HERE
                }
            }
        });
        addMenu.getItems().addAll(addExeItem, addFolderItem);
        addButton.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if(event.isPrimaryButtonDown()){
                    addMenu.show(addButton,addButton.getX(),addButton.getY()+addButton.getFitHeight());
                }
            }
        });
        Main.addEffectsToButton(addButton);

        HBox hbox = new HBox();
        hbox.setPadding(new Insets(15, 12, 15, 10));
        hbox.setSpacing(15);
        hbox.getChildren().addAll(addButton,settingsButton,sizeSlider);
        hbox.setAlignment(Pos.CENTER_LEFT);

        //HBox.setMargin(sizeSlider, new Insets(15, 12, 15, 12));

        wrappingPane.setTop(hbox);
    }
}

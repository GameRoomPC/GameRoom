package UI.scene;

import UI.ImageButton;
import UI.Main;
import data.GameEntry;
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static UI.Main.*;
import static UI.Main.FADE_IN_OUT_TIME;

/**
 * Created by LM on 03/07/2016.
 */
public class GameEditScene extends BaseScene {
    private final static double COVER_SCALE_EFFECT_FACTOR = 1.1;
    private final static double COVER_BLUR_EFFECT_RADIUS = 10;
    private final static double COVER_BRIGHTNESS_EFFECT_FACTOR = 0.1;
    private BorderPane wrappingPane;

    private GridPane contentPane;
    private MainScene previousScene;
    private GameEntry entry;

    public GameEditScene(StackPane stackPane, int width, int height, Stage parentStage, MainScene previousScene, File chosenFile) {
        super(stackPane, width, height, parentStage);
        entry = new GameEntry(chosenFile.getName());
        entry.setPath(chosenFile.getAbsolutePath());
        this.previousScene = previousScene;
        initTop();
        initLeft();
        initCenter();
        initBottom();
    }
    private void initBottom(){
        HBox hBox = new HBox();
        hBox.setSpacing(30*WIDTH/1920);
        Button addButton=new Button(RESSOURCE_BUNDLE.getString("add")+"!");
        addButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                previousScene.addGame(entry);
                fadeTransitionTo(previousScene, getParentStage());
            }
        });
        Button igdbButton = new Button(RESSOURCE_BUNDLE.getString("fetch_from_igdb"));
        igdbButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                //TODO open a search dialog here
            }
        });

        hBox.getChildren().addAll(igdbButton, addButton);

        wrappingPane.setBottom(hBox);
        BorderPane.setMargin(hBox, new Insets(15,15,15,15));
        BorderPane.setAlignment(addButton, Pos.BASELINE_RIGHT);
        BorderPane.setAlignment(igdbButton, Pos.BASELINE_RIGHT);

    }
    private void initCenter(){
        contentPane = new GridPane();
        contentPane.setVgap(20*WIDTH/1920);
        contentPane.setHgap(10*WIDTH/1920);
        contentPane.add(new Label(RESSOURCE_BUNDLE.getString("game_name")+" :"),0,0);
        TextField gameNameField = new TextField(entry.getName());
        gameNameField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                entry.setName(newValue);
            }
        });
        contentPane.add(gameNameField,1,0);
        contentPane.add(new Label(RESSOURCE_BUNDLE.getString("game_path")+" :"),0,1);
        TextField gamePathField = new TextField(entry.getPath());
        gamePathField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                entry.setPath(newValue);
            }
        });
        contentPane.add(gamePathField,1,1);
        contentPane.add(new Label(RESSOURCE_BUNDLE.getString("game_description")+" :"),0,2);
        TextField gameDescriptionField = new TextField(entry.getDescription());
        gameDescriptionField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                entry.setDescription(newValue);
            }
        });
        contentPane.add(gameDescriptionField,1,2);

        wrappingPane.setCenter(contentPane);
        BorderPane.setMargin(contentPane, new Insets(50*WIDTH/1920,50*WIDTH/1920,50*WIDTH/1920,50*WIDTH/1920));
    }
    private void initLeft(){
        StackPane pane = new StackPane();
        ImageView view = new ImageView(new Image(Main.DEFAULT_IMAGES_PATH+"cover.jpg", HEIGHT*2/(3*MainScene.COVER_HEIGHT_WIDTH_RATIO), HEIGHT*2/3 , false, true));
        ImageButton changeImageButton = new ImageButton(new Image("res/ui/folderButton.png", WIDTH/12, WIDTH/12, false, true));
        changeImageButton.setOpacity(0);
        changeImageButton.setFocusTraversable(false);
        changeImageButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                FileChooser imageChooser = new FileChooser();
                imageChooser.setTitle(RESSOURCE_BUNDLE.getString("select_picture"));
                imageChooser.setInitialDirectory(
                        new File(System.getProperty("user.home"))
                );
                imageChooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("All Images", "*.*"),
                        new FileChooser.ExtensionFilter("JPG", "*.jpg"),
                        new FileChooser.ExtensionFilter("BMP", "*.bmp"),
                        new FileChooser.ExtensionFilter("PNG", "*.png")
                );
                File chosenfile = imageChooser.showOpenDialog(getParentStage());
                //TODO code local file storage for images and properties files
                File localCoverFile = new File(entry.getUuid().toString()+File.separator+"cover."+getExtension(chosenfile.getName()));
                try {
                    if(!localCoverFile.exists()){
                        localCoverFile.createNewFile();
                    }
                    Files.copy(chosenfile.toPath().toAbsolutePath(), localCoverFile.toPath().toAbsolutePath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                view.setImage(new Image("file:"+File.separator+File.separator+File.separator+localCoverFile.getAbsolutePath(), HEIGHT*2/(3*MainScene.COVER_HEIGHT_WIDTH_RATIO), HEIGHT*2/3 , false, true));
                entry.setImagePath(0,"file:"+File.separator+File.separator+File.separator+localCoverFile.getAbsolutePath());
            }
        });
        BorderPane.setMargin(view, new Insets(50,50,50,50));
        //COVER EFFECTS
        DropShadow dropShadow = new DropShadow();
        dropShadow.setOffsetX(6.0*WIDTH/1920);
        dropShadow.setOffsetY(4.0*HEIGHT/1080);

        ColorAdjust coverColorAdjust = new ColorAdjust();
        coverColorAdjust.setBrightness(0.0);

        coverColorAdjust.setInput(dropShadow);

        GaussianBlur blur = new GaussianBlur(0.0);
        blur.setInput(coverColorAdjust);
        view.setEffect(blur);


        pane.setOnMouseEntered(e -> {
            // playButton.setVisible(true);
            //infoButton.setVisible(true);
            //coverPane.requestFocus();
            Timeline fadeInTimeline = new Timeline(
                    new KeyFrame(Duration.seconds(0),
                            new KeyValue(blur.radiusProperty(), blur.radiusProperty().getValue(), Interpolator.LINEAR),
                            new KeyValue(changeImageButton.opacityProperty(), changeImageButton.opacityProperty().getValue(), Interpolator.EASE_OUT),
                            new KeyValue(coverColorAdjust.brightnessProperty(), coverColorAdjust.brightnessProperty().getValue(), Interpolator.LINEAR)),
                    new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                            new KeyValue(blur.radiusProperty(), COVER_BLUR_EFFECT_RADIUS, Interpolator.LINEAR),
                            new KeyValue(changeImageButton.opacityProperty(), 1, Interpolator.EASE_OUT),
                            new KeyValue(coverColorAdjust.brightnessProperty(), -COVER_BRIGHTNESS_EFFECT_FACTOR, Interpolator.LINEAR)
                    ));
            fadeInTimeline.setCycleCount(1);
            fadeInTimeline.setAutoReverse(false);

            fadeInTimeline.play();

        });

        pane.setOnMouseExited(e -> {
                //playButton.setVisible(false);
                //infoButton.setVisible(false);
                Timeline fadeOutTimeline = new Timeline(
                        new KeyFrame(Duration.seconds(0),
                                new KeyValue(blur.radiusProperty(), blur.radiusProperty().getValue(), Interpolator.LINEAR),
                                new KeyValue(changeImageButton.opacityProperty(), changeImageButton.opacityProperty().getValue(), Interpolator.EASE_OUT),
                                new KeyValue(coverColorAdjust.brightnessProperty(), coverColorAdjust.brightnessProperty().getValue(), Interpolator.LINEAR)),
                        new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                                new KeyValue(blur.radiusProperty(), 0, Interpolator.LINEAR),
                                new KeyValue(changeImageButton.opacityProperty(), 0, Interpolator.EASE_OUT),
                                new KeyValue(coverColorAdjust.brightnessProperty(), 0, Interpolator.LINEAR)
                        ));
                fadeOutTimeline.setCycleCount(1);
                fadeOutTimeline.setAutoReverse(false);

                fadeOutTimeline.play();
        });
        pane.getChildren().addAll(view,changeImageButton);
        wrappingPane.setLeft(pane);
    }
    private void initTop(){
        Image leftArrowImage = new Image("res/ui/arrowLeft.png",WIDTH/45,WIDTH/45,true,true);
        ImageButton backButton = new ImageButton(leftArrowImage);
        backButton.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                File file = new File(entry.getUuid().toString());
                String[]entries = file.list();
                for(String s: entries){
                    File currentFile = new File(file.getPath(),s);
                    currentFile.delete();
                }
                file.delete();
                try {
                    Main.ALL_GAMES_ENTRIES.removeEntry(entry);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                fadeTransitionTo(previousScene,getParentStage());
            }
        });

        Label titleLabel = new Label(RESSOURCE_BUNDLE.getString("add_a_game"));
        titleLabel.setScaleX(2.5);
        titleLabel.setScaleY(2.5);

        BorderPane topPane = new BorderPane();
        topPane.setPadding(new Insets(15, 12, 15, 10));
        topPane.setLeft(backButton);
        BorderPane.setMargin(backButton, new Insets(15,0,0,0));
        topPane.setCenter(titleLabel);
        BorderPane.setAlignment(backButton, Pos.TOP_LEFT);
        BorderPane.setAlignment(titleLabel, Pos.BOTTOM_CENTER);


        //HBox.setMargin(sizeSlider, new Insets(15, 12, 15, 12));

        wrappingPane.setTop(topPane);
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
    public static String getExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int extensionPos = filename.lastIndexOf('.');
        int lastUnixPos = filename.lastIndexOf('/');
        int lastWindowsPos = filename.lastIndexOf('\\');
        int lastSeparator = Math.max(lastUnixPos, lastWindowsPos);
        int index = lastSeparator > extensionPos ? -1 : extensionPos;
        if (index == -1) {
            return "";
        } else {
            return filename.substring(index + 1);
        }
    }
}

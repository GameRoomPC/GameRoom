package UI.scene;

import UI.Main;
import UI.button.ImageButton;
import UI.button.gamebutton.GameButton;
import UI.button.gamebutton.InfoGameButton;
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
import javafx.scene.control.*;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import static UI.Main.*;

/**
 * Created by LM on 03/07/2016.
 */
public class GameEditScene extends BaseScene {
    private final static int MODE_ADD = 0;
    private final static int MODE_EDIT = 1;

    private final static double COVER_SCALE_EFFECT_FACTOR = 1.1;
    private final static double COVER_BLUR_EFFECT_RADIUS = 10;
    private final static double COVER_BRIGHTNESS_EFFECT_FACTOR = 0.1;

    private BorderPane wrappingPane;
    private GridPane contentPane;

    private File chosenImageFile;

    private BaseScene previousScene;
    private GameEntry entry;
    private int mode;

    public GameEditScene(StackPane stackPane, int width, int height, Stage parentStage, BaseScene previousScene, File chosenFile) {
        super(stackPane, parentStage);
        mode = MODE_ADD;
        entry = new GameEntry(chosenFile.getName());
        entry.setPath(chosenFile.getAbsolutePath());
        this.previousScene = previousScene;
        initTop();
        initCenter();
        initBottom();
    }
    public GameEditScene(StackPane stackPane, int width, int height, Stage parentStage, BaseScene previousScene, GameEntry entry) {
        super(stackPane, parentStage);
        mode = MODE_EDIT;
        this.entry = entry;
        this.entry.setSavedLocaly(false);
        this.chosenImageFile = entry.getImagePath(0);
        this.previousScene = previousScene;
        initTop();
        initCenter();
        initBottom();
    }
    private void initBottom(){
        HBox hBox = new HBox();
        hBox.setSpacing(30* SCREEN_WIDTH /1920);
        Button addButton=new Button(RESSOURCE_BUNDLE.getString("add")+"!");
        addButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                switch (mode){
                    case MODE_ADD:
                        File localCoverFile = new File(entry.getUuid().toString()+File.separator+"cover."+getExtension(chosenImageFile.getName()));
                        try {
                            if(!localCoverFile.exists()){
                                localCoverFile.mkdirs();
                                localCoverFile.createNewFile();
                            }
                            Files.copy(chosenImageFile.toPath().toAbsolutePath(), localCoverFile.toPath().toAbsolutePath(), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        entry.setSavedLocaly(true);
                        MAIN_SCENE.addGame(entry);
                        break;
                    case MODE_EDIT:
                        localCoverFile = new File(entry.getUuid().toString()+File.separator+"cover."+getExtension(chosenImageFile.getName()));
                        try {
                            if(!localCoverFile.exists()){
                                localCoverFile.mkdirs();
                                localCoverFile.createNewFile();
                            }
                            Files.copy(chosenImageFile.toPath().toAbsolutePath(), localCoverFile.toPath().toAbsolutePath(), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        entry.setSavedLocaly(true);
                        MAIN_SCENE.updateGame(entry);
                        break;
                    default:
                        break;
                }
                fadeTransitionTo(MAIN_SCENE, getParentStage());
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

        BorderPane.setMargin(hBox, new Insets(10* SCREEN_WIDTH /1920,30* SCREEN_WIDTH /1920,30* SCREEN_WIDTH /1920,30* SCREEN_WIDTH /1920));
        hBox.setAlignment(Pos.BOTTOM_RIGHT);
        wrappingPane.setBottom(hBox);

    }
    private void initCenter(){
        contentPane = new GridPane();
        //contentPane.setGridLinesVisible(true);
        contentPane.setVgap(20* SCREEN_WIDTH /1920);
        contentPane.setHgap(10* SCREEN_WIDTH /1920);
        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setPercentWidth(15);
        contentPane.getColumnConstraints().add(cc1);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setPercentWidth(85);
        contentPane.getColumnConstraints().add(cc2);

        contentPane.add(new Label(RESSOURCE_BUNDLE.getString("game_name")+" :"),0,0);
        TextField gameNameField = new TextField(entry.getName());
        gameNameField.setPrefColumnCount(50);
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

        contentPane.add(new Label(RESSOURCE_BUNDLE.getString("year")+" :"),0,2);
        TextField gameYearField = new TextField(entry.getYear());
        gameYearField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                entry.setYear(newValue);
            }
        });
        contentPane.add(gameYearField,1,2);

        contentPane.add(new Label(RESSOURCE_BUNDLE.getString("editor")+" :"),0,3);
        TextField gameEditorField = new TextField(entry.getYear());
        gameEditorField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                entry.setEditor(newValue);
            }
        });
        contentPane.add(gameEditorField,1,3);

        contentPane.add(new Label(RESSOURCE_BUNDLE.getString("game_description")+" :"),0,4);
        TextArea gameDescriptionField = new TextArea(entry.getDescription());
        gameDescriptionField.setWrapText(true);
        gameDescriptionField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                entry.setDescription(newValue);
            }
        });
        contentPane.add(gameDescriptionField,1,4);

        GridPane coverAndPropertiesPane = new GridPane();

        coverAndPropertiesPane.setVgap(20* SCREEN_WIDTH /1920);
        coverAndPropertiesPane.setHgap(60* SCREEN_WIDTH /1920);

        Pane coverPane  = createLeft();
        coverAndPropertiesPane.add(coverPane,0,0);
        coverAndPropertiesPane.add(contentPane,1,0);
        coverAndPropertiesPane.setPadding(new Insets(50* SCREEN_HEIGHT /1080,50* SCREEN_WIDTH /1920,20* SCREEN_HEIGHT /1080,50* SCREEN_WIDTH /1920));

        ScrollPane centerPane = new ScrollPane();
        centerPane.setFitToWidth(true);
        centerPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        centerPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        centerPane.setContent(coverAndPropertiesPane);

        wrappingPane.setCenter(centerPane);
    }
    private void createLineForProperty(String property, String initialValue,ChangeListener<String> changeListener, int row ){
        contentPane.add(new Label(RESSOURCE_BUNDLE.getString(property)+" :"),0,row);
        TextField gamePathField = new TextField(initialValue);
        gamePathField.textProperty().addListener(changeListener);
        contentPane.add(gamePathField,1,row);
    }
    private Pane createLeft(){
        StackPane pane = new StackPane();
        ImageView view = new ImageView(entry.getImage(0, GENERAL_SETTINGS.getWindowHeight() *2/(3* GameButton.COVER_HEIGHT_WIDTH_RATIO), GENERAL_SETTINGS.getWindowHeight() *2/3 , false, true));
        ImageButton changeImageButton = new ImageButton(new Image("res/ui/folderButton.png", GENERAL_SETTINGS.getWindowWidth() /12, GENERAL_SETTINGS.getWindowWidth() /12, false, true));
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
                chosenImageFile = imageChooser.showOpenDialog(getParentStage());
                File localCoverFile = new File(entry.getUuid().toString()+File.separator+"cover."+getExtension(chosenImageFile.getName()));
                view.setImage(new Image("file:"+File.separator+File.separator+File.separator+chosenImageFile.getAbsolutePath(), SCREEN_HEIGHT *2/(3*GameButton.COVER_HEIGHT_WIDTH_RATIO), SCREEN_HEIGHT *2/3 , false, true));
                entry.setImagePath(0,localCoverFile);
            }
        });
        //COVER EFFECTS
        DropShadow dropShadow = new DropShadow();
        dropShadow.setOffsetX(6.0* SCREEN_WIDTH /1920);
        dropShadow.setOffsetY(4.0* SCREEN_HEIGHT /1080);

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
        BorderPane.setMargin(pane, new Insets(50* SCREEN_HEIGHT /1080,50* SCREEN_WIDTH /1920,50* SCREEN_HEIGHT /1080,50* SCREEN_WIDTH /1920));
        return pane;
    }
    private void initTop(){
        Image leftArrowImage = new Image("res/ui/arrowLeft.png", SCREEN_WIDTH /45, SCREEN_WIDTH /45,true,true);
        ImageButton backButton = new ImageButton(leftArrowImage);
        backButton.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setHeaderText(null);
                alert.initStyle(StageStyle.UNDECORATED);
                alert.getDialogPane().getStylesheets().add("res/flatterfx.css");
                alert.initModality(Modality.APPLICATION_MODAL);
                alert.setContentText(RESSOURCE_BUNDLE.getString("ignore_changes?"));

                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK){
                    switch (mode){
                        case MODE_ADD:
                            entry.deleteFiles(); //just in case, should not be useful in any way
                            break;
                        case MODE_EDIT:
                            try {
                                entry.loadEntry();
                            } catch (IOException e) {
                                e.printStackTrace();
                                break;
                            }
                            break;
                        default:
                            break;
                    }
                    fadeTransitionTo(previousScene,getParentStage());
                } else {
                    // ... user chose CANCEL or closed the dialog
                }
            }
        });

        Label titleLabel = new Label(RESSOURCE_BUNDLE.getString("add_a_game"));
        titleLabel.setScaleX(2.5);
        titleLabel.setScaleY(2.5);

        StackPane topPane = new StackPane();

        topPane.getChildren().addAll(backButton,titleLabel);
        StackPane.setAlignment(backButton, Pos.TOP_LEFT);
        StackPane.setAlignment(titleLabel,Pos.TOP_CENTER);
        StackPane.setMargin(titleLabel, new Insets(55* Main.SCREEN_HEIGHT /1080
                ,12* Main.SCREEN_WIDTH /1920
                , 15* Main.SCREEN_HEIGHT /1080
                , 15* Main.SCREEN_WIDTH /1920));

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

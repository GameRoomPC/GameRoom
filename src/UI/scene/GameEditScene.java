package ui.scene;

import ui.Main;
import ui.control.button.ImageButton;
import ui.control.button.gamebutton.GameButton;
import ui.control.textfield.PathTextField;
import ui.dialog.SearchDialog;
import data.game.GameEntry;
import data.io.HTTPDownloader;
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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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

import static ui.Main.*;

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

    private ImageView coverView;
    private File chosenImageFile;

    private GameEntry entry;
    private int mode;

    private int row_count = 0;

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

    private void initBottom() {
        HBox hBox = new HBox();
        hBox.setSpacing(30 * SCREEN_WIDTH / 1920);
        Button addButton = new Button(RESSOURCE_BUNDLE.getString("add") + "!");
        if (mode == MODE_EDIT) {
            addButton.setText(RESSOURCE_BUNDLE.getString("save") + "!");
        }
        addButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if(chosenImageFile!=null) {
                    File localCoverFile = new File(GameEntry.ENTRIES_FOLDER+File.separator+entry.getUuid().toString() + File.separator + "cover." + getExtension(chosenImageFile.getName()));
                    try {
                        if (!localCoverFile.exists()) {
                            localCoverFile.mkdirs();
                            localCoverFile.createNewFile();
                        }
                        Files.copy(chosenImageFile.toPath().toAbsolutePath(), localCoverFile.toPath().toAbsolutePath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                entry.setSavedLocaly(true);
                switch (mode) {
                    case MODE_ADD:
                        MAIN_SCENE.addGame(entry);
                        break;
                    case MODE_EDIT:
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
                                       SearchDialog dialog = new SearchDialog();
                                       Optional<GameEntry> result = dialog.showAndWait();
                                       result.ifPresent(val -> {
                                           GameEntry gameEntry = val;
                                           if (val != null) {
                                               updateLineProperty("game_name", gameEntry.getName());
                                               updateLineProperty("year", gameEntry.getYear());
                                               updateLineProperty("developer", gameEntry.getDeveloper());
                                               updateLineProperty("publisher", gameEntry.getPublisher());
                                               updateLineProperty("game_description", gameEntry.getDescription());

                                               String imageURL = gameEntry.getIgdb_imageURL(0);
                                               String fileName = gameEntry.getIgdb_ID() + "_cover_big_2x." + GameEditScene.getExtension(imageURL);
                                               File outputFile = new File(Main.CACHE_FOLDER+File.separator+fileName);
                                               outputFile.deleteOnExit();

                                               if (!outputFile.exists()) {
                                                   Task<String> task = new Task<String>() {
                                                       @Override
                                                       protected String call() throws Exception {
                                                           Main.logger.debug("Downloading " + imageURL);
                                                           HTTPDownloader.downloadFile(imageURL, Main.CACHE_FOLDER.getAbsolutePath(),fileName);
                                                           Main.logger.debug("Cover downloaded");
                                                           return null;
                                                       }
                                                   };
                                                   task.setOnSucceeded(eh->{
                                                       Platform.runLater(new Runnable() {
                                                           @Override
                                                           public void run() {
                                                               Main.logger.info(outputFile.getAbsolutePath());
                                                               coverView.setImage(new Image("file:" + File.separator + File.separator + File.separator + outputFile.getAbsolutePath(), GENERAL_SETTINGS.getWindowHeight() * 2 / (3 * GameButton.COVER_HEIGHT_WIDTH_RATIO), GENERAL_SETTINGS.getWindowHeight() * 2 / 3, false, true));
                                                           }
                                                       });
                                                   });
                                                   Thread th = new Thread(task);
                                                   th.setDaemon(true);
                                                   th.start();
                                               }else{
                                                   coverView.setImage(new Image("file:" + File.separator + File.separator + File.separator + outputFile.getAbsolutePath(), GENERAL_SETTINGS.getWindowHeight() * 2 / (3 * GameButton.COVER_HEIGHT_WIDTH_RATIO), GENERAL_SETTINGS.getWindowHeight() * 2 / 3, false, true));
                                               }
                                               chosenImageFile = outputFile;
                                               File localImageFile = new File(GameEntry.ENTRIES_FOLDER+File.separator+entry.getUuid().toString()+File.separator+"cover."+GameEditScene.getExtension(imageURL));
                                               entry.setImagePath(0,localImageFile);
                                           }
                                       });

                                   }
                               }

        );

        hBox.getChildren().addAll(igdbButton, addButton);

        BorderPane.setMargin(hBox, new Insets(10 * SCREEN_WIDTH / 1920, 30 * SCREEN_WIDTH / 1920, 30 * SCREEN_WIDTH / 1920, 30 * SCREEN_WIDTH / 1920));
        hBox.setAlignment(Pos.BOTTOM_RIGHT);
        wrappingPane.setBottom(hBox);

    }

    private void initCenter() {
        contentPane = new GridPane();
        //contentPane.setGridLinesVisible(true);
        contentPane.setVgap(20 * SCREEN_WIDTH / 1920);
        contentPane.setHgap(10 * SCREEN_WIDTH / 1920);
        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setPercentWidth(15);
        contentPane.getColumnConstraints().add(cc1);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setPercentWidth(85);
        contentPane.getColumnConstraints().add(cc2);

        createLineForProperty("game_name", entry.getName(), new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                entry.setName(newValue);
            }
        });

        contentPane.add(new Label(RESSOURCE_BUNDLE.getString("game_path") + " :"), 0, row_count);
        PathTextField gamePathField = new PathTextField(entry.getPath(),this);
        gamePathField.getTextField().setPrefColumnCount(50);
        gamePathField.setId("game_path");
        gamePathField.getTextField().textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                entry.setPath(newValue);
            }
        });
        contentPane.add(gamePathField, 1, row_count);
        row_count++;

        createLineForProperty("year", entry.getYear(), new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                entry.setYear(newValue);
            }
        });

        createLineForProperty("developer", entry.getDeveloper(), new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                entry.setDeveloper(newValue);
            }
        });

        createLineForProperty("publisher", entry.getPublisher(), new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                entry.setPublisher(newValue);
            }
        });
        contentPane.add(new Label(RESSOURCE_BUNDLE.getString("game_description") + " :"), 0, row_count);
        TextArea gameDescriptionField = new TextArea(entry.getDescription());
        gameDescriptionField.setWrapText(true);
        gameDescriptionField.setId("game_description");
        gameDescriptionField.setPrefRowCount(4);
        gameDescriptionField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                entry.setDescription(newValue);
            }
        });
        contentPane.add(gameDescriptionField, 1, row_count);
        row_count++;

        GridPane coverAndPropertiesPane = new GridPane();

        coverAndPropertiesPane.setVgap(20 * SCREEN_WIDTH / 1920);
        coverAndPropertiesPane.setHgap(60 * SCREEN_WIDTH / 1920);

        Pane coverPane = createLeft();
        coverAndPropertiesPane.add(coverPane, 0, 0);
        coverAndPropertiesPane.add(contentPane, 1, 0);
        coverAndPropertiesPane.setPadding(new Insets(50 * SCREEN_HEIGHT / 1080, 50 * SCREEN_WIDTH / 1920, 20 * SCREEN_HEIGHT / 1080, 50 * SCREEN_WIDTH / 1920));

        ScrollPane centerPane = new ScrollPane();
        centerPane.setFitToWidth(true);
        centerPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        centerPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        centerPane.setContent(coverAndPropertiesPane);

        wrappingPane.setCenter(centerPane);
    }

    private void createLineForProperty(String property, String initialValue, ChangeListener<String> changeListener) {
        contentPane.add(new Label(RESSOURCE_BUNDLE.getString(property) + " :"), 0, row_count);
        TextField textField = new TextField(initialValue);
        textField.setPrefColumnCount(50);
        textField.setId(property);
        textField.textProperty().addListener(changeListener);
        contentPane.add(textField, 1, row_count);
        row_count++;
    }

    private void updateLineProperty(String property, String newValue) {
        if (!newValue.equals("")) {
            for (Node node : contentPane.getChildren()) {
                if (node.getId() != null && node.getId().equals(property)) {
                    if(node instanceof TextField) {
                        ((TextField) node).setText(newValue);
                    }else if(node instanceof  TextArea) {
                        ((TextArea) node).setText(newValue);
                    }else if(node instanceof  PathTextField) {
                        ((PathTextField) node).setText(newValue);
                    }
                    break;
                }
            }
        }
    }

    private Pane createLeft() {
        StackPane pane = new StackPane();
        coverView = new ImageView(entry.getImage(0, GENERAL_SETTINGS.getWindowHeight() * 2 / (3 * GameButton.COVER_HEIGHT_WIDTH_RATIO), GENERAL_SETTINGS.getWindowHeight() * 2 / 3, false, true));
        ImageButton changeImageButton = new ImageButton(new Image("res/ui/folderButton.png", GENERAL_SETTINGS.getWindowWidth() / 12, GENERAL_SETTINGS.getWindowWidth() / 12, false, true));
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
                File localCoverFile = new File(GameEntry.ENTRIES_FOLDER+File.separator+entry.getUuid().toString() + File.separator + "cover." + getExtension(chosenImageFile.getName()));
                coverView.setImage(new Image("file:" + File.separator + File.separator + File.separator + chosenImageFile.getAbsolutePath(), GENERAL_SETTINGS.getWindowHeight() * 2 / (3 * GameButton.COVER_HEIGHT_WIDTH_RATIO), GENERAL_SETTINGS.getWindowHeight() * 2 / 3, false, true));
                entry.setImagePath(0, localCoverFile);
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
        pane.getChildren().addAll(coverView, changeImageButton);
        wrappingPane.setLeft(pane);
        BorderPane.setMargin(pane, new Insets(50 * SCREEN_HEIGHT / 1080, 50 * SCREEN_WIDTH / 1920, 50 * SCREEN_HEIGHT / 1080, 50 * SCREEN_WIDTH / 1920));
        return pane;
    }

    private void initTop() {
        EventHandler<MouseEvent> backButtonHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setHeaderText(null);
                alert.initStyle(StageStyle.UNDECORATED);
                alert.getDialogPane().getStylesheets().add("res/flatterfx.css");
                alert.initModality(Modality.APPLICATION_MODAL);
                alert.setContentText(RESSOURCE_BUNDLE.getString("ignore_changes?"));

                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK) {
                    switch (mode) {
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
                    fadeTransitionTo(previousScene, getParentStage());
                } else {
                    // ... user chose CANCEL or closed the dialog
                }
            }
        };
        String title = RESSOURCE_BUNDLE.getString("add_a_game");
        if(mode == MODE_EDIT){
            title = RESSOURCE_BUNDLE.getString("edit_a_game");
        }

        wrappingPane.setTop(createTop(backButtonHandler,title));
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

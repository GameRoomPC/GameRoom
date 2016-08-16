package ui.scene;

import data.game.entry.GameGenre;
import data.game.entry.GameTheme;
import data.game.scrapper.IGDBScrapper;
import data.game.ImageUtils;
import data.game.OnDLDoneHandler;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import org.apache.commons.lang.StringEscapeUtils;
import org.controlsfx.control.CheckComboBox;
import org.json.JSONException;
import ui.Main;
import ui.control.ValidEntryCondition;
import ui.control.button.ImageButton;
import ui.control.button.gamebutton.GameButton;
import ui.control.textfield.PathTextField;
import ui.control.textfield.PlayTimeField;
import ui.dialog.GameRoomAlert;
import ui.dialog.IGDBImageSelector;
import ui.dialog.SearchDialog;
import data.game.entry.GameEntry;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
import javafx.util.Duration;
import ui.pane.OnItemSelectedHandler;
import ui.pane.SelectListPane;
import ui.scene.exitaction.ClassicExitAction;
import ui.scene.exitaction.ExitAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Pattern;

import static ui.Main.*;
import static ui.Main.GENERAL_SETTINGS;

/**
 * Created by LM on 03/07/2016.
 */
public class GameEditScene extends BaseScene {
    public final static int MODE_ADD = 0;
    private final static int MODE_EDIT = 1;

    private final static double COVER_BLUR_EFFECT_RADIUS = 10;
    private final static double COVER_BRIGHTNESS_EFFECT_FACTOR = 0.1;

    private BorderPane wrappingPane;
    private GridPane contentPane;

    private HBox buttonsBox;

    private ImageView coverView;
    private File[] chosenImageFiles = new File[GameEntry.DEFAULT_IMAGES_PATHS.length];
    private FileChooser imageChooser = new FileChooser();

    private ArrayList<ValidEntryCondition> validEntriesConditions = new ArrayList<>();

    private GameEntry entry;
    private int mode;

    private ExitAction onExitAction;

    private int row_count = 0;

    public GameEditScene(BaseScene previousScene, File chosenFile) {
        super(new StackPane(), previousScene.getParentStage());
        mode = MODE_ADD;
        entry = new GameEntry(chosenFile.getName());
        entry.setPath(chosenFile.getAbsolutePath());
        init(previousScene, null);
    }

    public GameEditScene(BaseScene previousScene, GameEntry entry, Image coverImage) {
        this(previousScene, entry, MODE_EDIT, coverImage);
    }

    public GameEditScene(BaseScene previousScene, GameEntry entry, int mode, Image coverImage) {
        super(new StackPane(), previousScene.getParentStage());
        this.mode = mode;
        this.entry = entry;
        this.entry.setSavedLocaly(false);
        this.chosenImageFiles[0] = entry.getImagePath(0);
        this.chosenImageFiles[1] = entry.getImagePath(1);
        init(previousScene, coverImage);
    }

    private void init(BaseScene previousScene, Image coverImage) {
        onExitAction = new ClassicExitAction(this, previousScene.getParentStage(), previousScene);

        imageChooser = new FileChooser();
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
        this.previousScene = previousScene;
        initTop();
        initCenter(coverImage);
        initBottom();
    }

    private void initBottom() {
        buttonsBox = new HBox();
        buttonsBox.setSpacing(30 * SCREEN_WIDTH / 1920);
        Button addButton = new Button(RESSOURCE_BUNDLE.getString("add") + "!");
        if (mode == MODE_EDIT) {
            addButton.setText(RESSOURCE_BUNDLE.getString("save") + "!");
        }
        addButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                boolean allConditionsMet = true;
                for (ValidEntryCondition condition : validEntriesConditions) {
                    allConditionsMet = allConditionsMet && condition.isValid();
                    if (!condition.isValid()) {
                        condition.onInvalid();
                        GameRoomAlert alert = new GameRoomAlert(Alert.AlertType.ERROR, condition.message.toString());
                        alert.showAndWait();
                    }
                }
                if (allConditionsMet) {
                    for (int i = 0; i < chosenImageFiles.length; i++) {
                        if (chosenImageFiles[i] != null) {
                            String type = i == 0 ? ImageUtils.IGDB_TYPE_COVER : ImageUtils.IGDB_TYPE_SCREENSHOT;
                            File localCoverFile = new File(GameEntry.ENTRIES_FOLDER + File.separator + entry.getUuid().toString() + File.separator + type + "." + getExtension(chosenImageFiles[i].getName()));
                            try {
                                if (!localCoverFile.exists()) {
                                    localCoverFile.mkdirs();
                                    localCoverFile.createNewFile();
                                }
                                Files.copy(chosenImageFiles[i].toPath().toAbsolutePath(), localCoverFile.toPath().toAbsolutePath(), StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    entry.setSavedLocaly(true);
                    switch (mode) {
                        case MODE_ADD:
                            entry.setAddedDate(new Date());
                            MAIN_SCENE.addGame(entry);
                            break;
                        case MODE_EDIT:
                            MAIN_SCENE.updateGame(entry);
                            break;
                        default:
                            break;
                    }
                    //fadeTransitionTo(MAIN_SCENE, getParentStage());
                    if (previousScene instanceof GameInfoScene) {
                        ((GameInfoScene) previousScene).updateWithEditedEntry(entry);
                    }
                    onExitAction.run();
                }
            }
        });
        Button igdbButton = new Button(RESSOURCE_BUNDLE.getString("fetch_from_igdb"));
        igdbButton.setOnAction(new EventHandler<ActionEvent>() {
                                   @Override
                                   public void handle(ActionEvent event) {
                                       SearchDialog dialog = new SearchDialog(createDoNotUpdateFielsMap());
                                       Optional<ButtonType> result = dialog.showAndWait();
                                       result.ifPresent(val -> {
                                           if (!val.getButtonData().isCancelButton()) {
                                               GameEntry gameEntry = dialog.getSelectedEntry();
                                               if (gameEntry != null) {
                                                   onNewEntryData(gameEntry, dialog.getDoNotUpdateFieldsMap());
                                               }
                                           }
                                       });

                                   }
                               }

        );

        buttonsBox.getChildren().addAll(igdbButton, addButton);

        BorderPane.setMargin(buttonsBox, new Insets(10 * SCREEN_WIDTH / 1920, 30 * SCREEN_WIDTH / 1920, 30 * SCREEN_WIDTH / 1920, 30 * SCREEN_WIDTH / 1920));
        buttonsBox.setAlignment(Pos.BOTTOM_RIGHT);
        wrappingPane.setBottom(buttonsBox);

    }

    private void initCenter(Image coverImage) {
        GaussianBlur blur = new GaussianBlur(BACKGROUND_IMAGE_BLUR);

        backgroundView.setEffect(blur);
        backgroundView.setOpacity(BACKGROUND_IMAGE_MAX_OPACITY);
        if (entry.getImagePath(1) != null) {
            //Main.LOGGER.debug("Screenshot available : "+entry.getImagePath(1));
            Image screenshotImage = entry.getImage(1,
                    Main.GENERAL_SETTINGS.getWindowWidth(),
                    Main.GENERAL_SETTINGS.getWindowHeight()
                    , false, true);
            backgroundView.setImage(screenshotImage);
        }

        contentPane = new GridPane();
        //contentPane.setGridLinesVisible(true);
        contentPane.setVgap(20 * SCREEN_WIDTH / 1920);
        contentPane.setHgap(10 * SCREEN_WIDTH / 1920);
        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setPercentWidth(20);
        contentPane.getColumnConstraints().add(cc1);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setPercentWidth(80);
        contentPane.getColumnConstraints().add(cc2);

        /**************************NAME*********************************************/
        createLineForProperty("game_name", entry.getName(), new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                entry.setName(newValue);
            }
        });
        validEntriesConditions.add(new ValidEntryCondition() {

            @Override
            public boolean isValid() {
                if (entry.getName().equals("")) {
                    message.replace(0, message.length(), Main.RESSOURCE_BUNDLE.getString("invalid_name_empty"));
                    return false;
                }
                return true;
            }

            @Override
            public void onInvalid() {
                setLineInvalid("game_name");
            }
        });
        /**************************SERIE*********************************************/
        createLineForProperty("serie", entry.getSerie(), new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                entry.setSerie(newValue);
            }
        });

        /**************************PATH*********************************************/
        contentPane.add(new Label(RESSOURCE_BUNDLE.getString("game_path") + " :"), 0, row_count);
        PathTextField gamePathField = new PathTextField(entry.getPath(), this, PathTextField.FILE_CHOOSER_APPS, RESSOURCE_BUNDLE.getString("select_picture"));
        gamePathField.getTextField().setPrefColumnCount(50);
        gamePathField.setId("game_path");
        gamePathField.getTextField().textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                entry.setPath(newValue);
            }
        });
        validEntriesConditions.add(new ValidEntryCondition() {
            @Override
            public boolean isValid() {
                if (entry.getPath().equals("")) {
                    message.replace(0, message.length(), Main.RESSOURCE_BUNDLE.getString("invalid_path_empty"));
                    return false;
                }
                File file = new File(entry.getPath());
                Pattern pattern = Pattern.compile("^steam:\\/\\/rungameid\\/\\d*$");

                if (!pattern.matcher(entry.getPath().trim()).matches() && !file.exists()) {
                    message.replace(0, message.length(), Main.RESSOURCE_BUNDLE.getString("invalid_path_not_file"));
                    return false;
                }
                return true;
            }

            @Override
            public void onInvalid() {
                setLineInvalid("game_path");
            }
        });

        contentPane.add(gamePathField, 1, row_count);
        row_count++;

        /**************************PLAYTIME*********************************************/
        Label titlePlayTimeLabel = new Label(RESSOURCE_BUNDLE.getString("play_time") + " :");
        titlePlayTimeLabel.setTooltip(new Tooltip(RESSOURCE_BUNDLE.getString("play_time")));
        contentPane.add(titlePlayTimeLabel, 0, row_count);
        PlayTimeField playTimeField = new PlayTimeField(entry);

        playTimeField.setId("play_time");
        contentPane.add(playTimeField, 1, row_count);
        row_count++;

        /***************************SEPARATORS******************************************/
        Separator s1 = new Separator();
        contentPane.add(s1, 0, row_count);
        row_count++;

        /**************************RELEASE DATE*********************************************/
        TextField releaseDateField = createLineForProperty("release_date", entry.getReleaseDate() != null ? GameEntry.DATE_DISPLAY_FORMAT.format(entry.getReleaseDate()) : "", null);
        releaseDateField.setPromptText(Main.RESSOURCE_BUNDLE.getString("date_example"));
        releaseDateField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (!releaseDateField.getText().equals("")) {
                    try {
                        Date date = GameEntry.DATE_DISPLAY_FORMAT.parse(releaseDateField.getText());
                        entry.setReleaseDate(date);
                    } catch (ParseException e) {
                        //well date not valid yet
                    }
                }
            }
        });
        validEntriesConditions.add(new ValidEntryCondition() {
            @Override
            public boolean isValid() {
                if (!releaseDateField.getText().equals("")) {
                    try {
                        Date date = GameEntry.DATE_DISPLAY_FORMAT.parse(releaseDateField.getText());
                        entry.setReleaseDate(date);
                    } catch (ParseException e) {
                        message.replace(0, message.length(), Main.RESSOURCE_BUNDLE.getString("invalid_release_date"));
                        return false;
                    }
                }
                return true;
            }

            @Override
            public void onInvalid() {
                setLineInvalid("year");
            }
        });


        /**************************DEVELOPER*********************************************/
        createLineForProperty("developer", entry.getDeveloper(), new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                entry.setDeveloper(newValue);
            }
        });

        /**************************PUBLISHER*********************************************/
        createLineForProperty("publisher", entry.getPublisher(), new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                entry.setPublisher(newValue);
            }
        });

        /**************************GENRE*********************************************/

        // create the data to show in the CheckComboBox
        final ObservableList<GameGenre> allGamesGenre = FXCollections.observableArrayList();
        for (GameGenre genre : GameGenre.values()) {
            allGamesGenre.add(genre);
        }
        allGamesGenre.sort(new Comparator<GameGenre>() {
            @Override
            public int compare(GameGenre o1, GameGenre o2) {
                return o1.getDisplayName().compareTo(o2.getDisplayName());
            }
        });
        // Create the CheckComboBox with the data
        final CheckComboBox<GameGenre> genreComboBox = new CheckComboBox<GameGenre>(allGamesGenre);
        genreComboBox.setId("genre");
        if (entry.getGenres() != null) {
            for (GameGenre genre : entry.getGenres()) {
                genreComboBox.getCheckModel().check(genreComboBox.getCheckModel().getItemIndex(genre));
            }
        }
        genreComboBox.getCheckModel().getCheckedItems().addListener(new ListChangeListener<GameGenre>() {
            @Override
            public void onChanged(Change<? extends GameGenre> c) {
                GameGenre[] newGenres = new GameGenre[genreComboBox.getCheckModel().getCheckedIndices().size()];
                newGenres = genreComboBox.getCheckModel().getCheckedItems().toArray(newGenres);
                entry.setGenres(newGenres);
            }
        });
        Label genreLabel = new Label(RESSOURCE_BUNDLE.getString("genre") + " :");
        genreLabel.setTooltip(new Tooltip(RESSOURCE_BUNDLE.getString("genre")));
        contentPane.add(genreLabel, 0, row_count);
        contentPane.add(genreComboBox, 1, row_count);
        row_count++;

        /**************************THEME*********************************************/

        // create the data to show in the CheckComboBox
        final ObservableList<GameTheme> allGamesTheme = FXCollections.observableArrayList();
        for (GameTheme theme : GameTheme.values()) {
            allGamesTheme.add(theme);
        }
        allGamesTheme.sort(new Comparator<GameTheme>() {
            @Override
            public int compare(GameTheme o1, GameTheme o2) {
                return o1.getDisplayName().compareTo(o2.getDisplayName());
            }
        });
        // Create the CheckComboBox with the data
        final CheckComboBox<GameTheme> themeComboBox = new CheckComboBox<GameTheme>(allGamesTheme);
        themeComboBox.setId("theme");
        if (entry.getThemes() != null) {
            for (GameTheme theme : entry.getThemes()) {
                themeComboBox.getCheckModel().check(themeComboBox.getCheckModel().getItemIndex(theme));
            }
        }
        themeComboBox.getCheckModel().getCheckedItems().addListener(new ListChangeListener<GameTheme>() {
            @Override
            public void onChanged(Change<? extends GameTheme> c) {
                GameTheme[] newTheme = new GameTheme[themeComboBox.getCheckModel().getCheckedIndices().size()];
                newTheme = themeComboBox.getCheckModel().getCheckedItems().toArray(newTheme);
                entry.setThemes(newTheme);
            }
        });
        Label themeLabel = new Label(RESSOURCE_BUNDLE.getString("theme") + " :");
        themeLabel.setTooltip(new Tooltip(RESSOURCE_BUNDLE.getString("theme")));
        contentPane.add(themeLabel, 0, row_count);
        contentPane.add(themeComboBox, 1, row_count);
        row_count++;

        /**************************DESCRIPTION*********************************************/
        Label titleDescriptionLabel = new Label(RESSOURCE_BUNDLE.getString("game_description") + " :");
        titleDescriptionLabel.setTooltip(new Tooltip(RESSOURCE_BUNDLE.getString("game_description")));
        contentPane.add(titleDescriptionLabel, 0, row_count);
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

        /**************************SCREENSHOT*********************************************/
        OnDLDoneHandler screenshotDlDoneHandler = new OnDLDoneHandler() {
            @Override
            public void run(File outputfile) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        Image img = new Image("file:" + File.separator + File.separator + File.separator + outputfile.getAbsolutePath(), GENERAL_SETTINGS.getWindowWidth(), GENERAL_SETTINGS.getWindowHeight(), false, true);
                        ImageUtils.transitionToImage(img, backgroundView, BaseScene.BACKGROUND_IMAGE_MAX_OPACITY);
                        chosenImageFiles[1] = outputfile;
                        File coverLocalImageFile = new File(GameEntry.ENTRIES_FOLDER + File.separator + entry.getUuid().toString() + File.separator + ImageUtils.IGDB_TYPE_SCREENSHOT + "." + getExtension(outputfile));
                        entry.setImagePath(1, coverLocalImageFile);
                    }
                });
            }
        };
        Label screenshotLabel = new Label(RESSOURCE_BUNDLE.getString("wallpaper") + " :");
        screenshotLabel.setTooltip(new Tooltip(RESSOURCE_BUNDLE.getString("wallpaper")));
        contentPane.add(screenshotLabel, 0, row_count);

        HBox screenShotButtonsBox = new HBox();
        screenShotButtonsBox.setSpacing(20 * Main.SCREEN_WIDTH / 1920);
        screenShotButtonsBox.setAlignment(Pos.CENTER_LEFT);

        ImageButton screenshotFileButton = new ImageButton(new Image("res/ui/folderButton.png", GENERAL_SETTINGS.getWindowWidth() / 24, GENERAL_SETTINGS.getWindowWidth() / 24, false, true));
        screenshotFileButton.setOnAction(event -> {
            chosenImageFiles[1] = imageChooser.showOpenDialog(getParentStage());
            screenshotDlDoneHandler.run(chosenImageFiles[1]);
            //backgroundView.setImage(new Image("file:" + File.separator + File.separator + File.separator + chosenImageFiles[1].getAbsolutePath(), GENERAL_SETTINGS.getWindowWidth(), GENERAL_SETTINGS.getWindowHeight(), false, true));
            File localScreenshotFile = new File(GameEntry.ENTRIES_FOLDER + File.separator + entry.getUuid().toString() + File.separator + ImageUtils.IGDB_TYPE_SCREENSHOT + "." + getExtension(chosenImageFiles[1].getName()));
            entry.setImagePath(1, localScreenshotFile);
        });
        Label orLabel = new Label(RESSOURCE_BUNDLE.getString("or"));

        Button screenshotIGDBButton = new Button(Main.RESSOURCE_BUNDLE.getString("IGDB"));
        screenshotIGDBButton.setOnAction(new EventHandler<ActionEvent>() {
                                             @Override
                                             public void handle(ActionEvent event) {
                                                 if (entry.getIgdb_id() != -1) {
                                                     GameEntry gameEntry = entry;
                                                     try {
                                                         gameEntry.setIgdb_imageHashs(IGDBScrapper.getScreenshotHash(IGDBScrapper.getGameData(gameEntry.getIgdb_id())));
                                                         openImageSelector(gameEntry);
                                                     } catch (JSONException jse) {
                                                         if (jse.toString().contains("[\"screenshots\"] not found")) {
                                                             GameRoomAlert alert = new GameRoomAlert(Alert.AlertType.ERROR, Main.RESSOURCE_BUNDLE.getString("no_screenshot_for_this_game"));
                                                             alert.showAndWait();
                                                         } else {
                                                             jse.printStackTrace();
                                                         }
                                                     }
                                                 } else {
                                                     SearchDialog dialog = new SearchDialog(createDoNotUpdateFielsMap());
                                                     Optional<ButtonType> result = dialog.showAndWait();
                                                     result.ifPresent(val -> {
                                                         if (!val.getButtonData().isCancelButton()) {
                                                             GameEntry gameEntry = dialog.getSelectedEntry();
                                                             if (val != null) {
                                                                 openImageSelector(gameEntry);
                                                             }
                                                         }
                                                     });
                                                 }
                                             }
                                         }

        );

        screenShotButtonsBox.getChildren().addAll(screenshotFileButton, orLabel, screenshotIGDBButton);

        contentPane.add(screenShotButtonsBox, 1, row_count);
        row_count++;

        /*validEntriesConditions.add(new ValidEntryCondition() {

            @Override
            public boolean isValid() {
                if (!entry.getImagePath(1).exists()) {
                    message.replace(0,message.length(),Main.RESSOURCE_BUNDLE.getString("background_picture_still_downloading"));
                    return false;
                }
                return true;
            }

            @Override
            public void onInvalid() {
                setLineInvalid("game_name");
            }
        });*/

        /********************END FOR PROPERTIES********************************************/

        GridPane coverAndPropertiesPane = new GridPane();

        coverAndPropertiesPane.setVgap(20 * SCREEN_WIDTH / 1920);
        coverAndPropertiesPane.setHgap(60 * SCREEN_WIDTH / 1920);

        Pane coverPane = createLeft(coverImage);
        coverAndPropertiesPane.add(coverPane, 0, 0);
        coverAndPropertiesPane.setPadding(new Insets(50 * SCREEN_HEIGHT / 1080, 50 * SCREEN_WIDTH / 1920, 20 * SCREEN_HEIGHT / 1080, 50 * SCREEN_WIDTH / 1920));

        contentPane.setPadding(new Insets(30 * SCREEN_HEIGHT / 1080, 30 * SCREEN_WIDTH / 1920, 30 * SCREEN_HEIGHT / 1080, 30 * SCREEN_WIDTH / 1920));
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setContent(contentPane);
        coverAndPropertiesPane.add(scrollPane, 1, 0);

        wrappingPane.setCenter(coverAndPropertiesPane);
    }

    private TextField createLineForProperty(String property, String initialValue, ChangeListener<String> changeListener) {
        Label titleLabel = new Label(RESSOURCE_BUNDLE.getString(property) + " :");
        titleLabel.setTooltip(new Tooltip(RESSOURCE_BUNDLE.getString(property)));
        contentPane.add(titleLabel, 0, row_count);
        TextField textField = new TextField(initialValue);
        textField.setPrefColumnCount(50);
        textField.setId(property);
        if (changeListener != null) {
            textField.textProperty().addListener(changeListener);
        }
        contentPane.add(textField, 1, row_count);
        row_count++;
        return textField;
    }

    private void setLineInvalid(String property_key) {
        String style = "-fx-text-inner-color: red;\n";
        for (Node node : contentPane.getChildren()) {
            if (node.getId() != null && node.getId().equals(property_key)) {
                node.setStyle(style);
                node.focusedProperty().addListener(new ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                        if (newValue) {
                            node.setStyle("");
                        }
                    }
                });
                break;
            }
        }
    }

    private void updateLineProperty(String property, Object newValue, HashMap<String, Boolean> doNotUpdateFieldsMap) {
        if (doNotUpdateFieldsMap.get(property)==null || !doNotUpdateFieldsMap.get(property))
            if (newValue != null && !newValue.equals("")) {
                for (Node node : contentPane.getChildren()) {
                    if (node.getId() != null && node.getId().equals(property)) {
                        if (newValue instanceof String) {
                            if (node instanceof TextField) {
                                ((TextField) node).setText((String) newValue);
                            } else if (node instanceof TextArea) {
                                ((TextArea) node).setText((String) newValue);
                            } else if (node instanceof PathTextField) {
                                ((PathTextField) node).setText((String) newValue);
                            }
                        } else if (newValue instanceof GameGenre[]) {
                            if (node instanceof CheckComboBox) {
                                for (GameGenre genre : (GameGenre[]) newValue) {
                                    ((CheckComboBox) node).getCheckModel().check(genre);
                                }
                            }
                        } else if (newValue instanceof GameTheme[]) {
                            if (node instanceof CheckComboBox) {
                                for (GameTheme theme : (GameTheme[]) newValue) {
                                    ((CheckComboBox) node).getCheckModel().check(theme);
                                }
                            }
                        } else if (newValue instanceof Date) {
                            if (node instanceof TextField) {
                                ((TextField) node).setText(newValue != null ? GameEntry.DATE_DISPLAY_FORMAT.format(newValue) : "");
                            }
                        }
                        break;
                    }
                }
            }
    }

    private Pane createLeft(Image coverImage) {
        StackPane pane = new StackPane();
        double coverWidth = GENERAL_SETTINGS.getWindowHeight() * 2 / (3 * GameButton.COVER_HEIGHT_WIDTH_RATIO);
        double coverHeight = GENERAL_SETTINGS.getWindowHeight() * 2 / 3;

        coverView = new ImageView();
        coverView.setFitWidth(coverWidth);
        coverView.setFitHeight(coverHeight);

        if (coverImage != null) {
            ImageUtils.transitionToImage(coverImage, coverView);
        } else {
            Task<Image> loadImageTask = new Task<Image>() {
                @Override
                protected Image call() throws Exception {
                    Image img = entry.getImage(0, coverWidth, coverHeight, false, true);
                    return img;
                }
            };
            loadImageTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent event) {
                    Platform.runLater(() -> {
                        ImageUtils.transitionToImage(loadImageTask.getValue(), coverView);
                    });
                }
            });
            Thread imageThread = new Thread(loadImageTask);
            imageThread.setDaemon(true);
            imageThread.start();
        }


        ImageButton changeImageButton = new ImageButton(new Image("res/ui/folderButton.png", GENERAL_SETTINGS.getWindowWidth() / 12, GENERAL_SETTINGS.getWindowWidth() / 12, false, true));
        changeImageButton.setOpacity(0);
        changeImageButton.setFocusTraversable(false);
        changeImageButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                chosenImageFiles[0] = imageChooser.showOpenDialog(getParentStage());
                File localCoverFile = new File(GameEntry.ENTRIES_FOLDER + File.separator + entry.getUuid().toString() + File.separator + ImageUtils.IGDB_TYPE_COVER + "." + getExtension(chosenImageFiles[0].getName()));
                Image img = new Image("file:" + File.separator + File.separator + File.separator + chosenImageFiles[0].getAbsolutePath(), GENERAL_SETTINGS.getWindowHeight() * 2 / (3 * GameButton.COVER_HEIGHT_WIDTH_RATIO), GENERAL_SETTINGS.getWindowHeight() * 2 / 3, false, true);
                ImageUtils.transitionToImage(img, coverView);

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
                GameRoomAlert alert = new GameRoomAlert(Alert.AlertType.CONFIRMATION);
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
        if (mode == MODE_EDIT) {
            title = RESSOURCE_BUNDLE.getString("edit_a_game");
        }

        wrappingPane.setTop(createTop(backButtonHandler, title));
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

    private HashMap<String, Boolean> createDoNotUpdateFielsMap() {
        HashMap<String, Boolean> map = new HashMap<>();
        map.put("game_name", false);
        map.put("serie", !entry.getSerie().equals(""));
        map.put("release_date", entry.getReleaseDate() != null);
        map.put("developer", !entry.getDeveloper().equals(""));
        map.put("game_description", !entry.getDescription().equals(""));
        map.put("publisher", !entry.getPublisher().equals(""));
        map.put("genre", entry.getGenres() != null);
        map.put("theme", entry.getThemes() != null);
        map.put("cover", entry.getImagePath(0) != null && !entry.getImagePath(0).equals(GameEntry.DEFAULT_IMAGES_PATHS[0]));
        return map;
    }

    private static String getExtension(File file) {
        return getExtension(file.getAbsolutePath());
    }

    private static String getExtension(String filename) {
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

    //called when user selected a igdb game or when a steam game is added
    private void onNewEntryData(GameEntry gameEntry, HashMap<String, Boolean> doNotUpdateFieldsMap) {
        updateLineProperty("game_name", gameEntry.getName(),doNotUpdateFieldsMap);
        updateLineProperty("serie", gameEntry.getSerie(),doNotUpdateFieldsMap);
        updateLineProperty("release_date", gameEntry.getReleaseDate(),doNotUpdateFieldsMap);
        updateLineProperty("developer", gameEntry.getDeveloper(),doNotUpdateFieldsMap);
        updateLineProperty("publisher", gameEntry.getPublisher(),doNotUpdateFieldsMap);
        updateLineProperty("game_description", StringEscapeUtils.unescapeHtml(gameEntry.getDescription()),doNotUpdateFieldsMap);
        updateLineProperty("genre", gameEntry.getGenres(),doNotUpdateFieldsMap);
        updateLineProperty("theme", gameEntry.getThemes(),doNotUpdateFieldsMap);
        entry.setIgdb_id(gameEntry.getIgdb_id());
        entry.setAggregated_rating(gameEntry.getAggregated_rating());

        /*****************COVER DOWNLOAD***************************/
        if (!doNotUpdateFieldsMap.get("cover")) {
            ImageUtils.downloadIGDBImageToCache(gameEntry.getIgdb_id()
                    , gameEntry.getIgdb_imageHash(0)
                    , ImageUtils.IGDB_TYPE_COVER
                    , ImageUtils.IGDB_SIZE_BIG_2X
                    , new OnDLDoneHandler() {
                        @Override
                        public void run(File outputfile) {
                            Image img = new Image("file:" + File.separator + File.separator + File.separator + outputfile.getAbsolutePath(), GENERAL_SETTINGS.getWindowHeight() * 2 / (3 * GameButton.COVER_HEIGHT_WIDTH_RATIO), GENERAL_SETTINGS.getWindowHeight() * 2 / 3, false, true);
                            ImageUtils.transitionToImage(img, coverView);

                            chosenImageFiles[0] = outputfile;
                            File coverLocalImageFile = new File(GameEntry.ENTRIES_FOLDER + File.separator + entry.getUuid().toString() + File.separator + ImageUtils.IGDB_TYPE_COVER + "." + getExtension(outputfile));
                            entry.setImagePath(0, coverLocalImageFile);

                        }
                    });
        }
        openImageSelector(gameEntry);
    }

    private void openImageSelector(GameEntry gameEntry) {
        IGDBImageSelector screenshotSelector = new IGDBImageSelector(gameEntry, new OnItemSelectedHandler() {
            @Override
            public void handle(SelectListPane.ListItem item) {
                ImageUtils.downloadIGDBImageToCache(gameEntry.getIgdb_id()
                        , (java.lang.String) item.getValue()
                        , ImageUtils.IGDB_TYPE_SCREENSHOT
                        , ImageUtils.IGDB_SIZE_MED
                        , new OnDLDoneHandler() {
                            @Override
                            public void run(File outputfile) {
                                Image img = new Image("file:" + File.separator + File.separator + File.separator + outputfile.getAbsolutePath(), GENERAL_SETTINGS.getWindowWidth(), GENERAL_SETTINGS.getWindowHeight(), false, true);
                                ImageUtils.transitionToImage(img, backgroundView, BaseScene.BACKGROUND_IMAGE_MAX_OPACITY);

                            }
                        });
            }
        });
        Optional<ButtonType> screenShotSelectedHash = screenshotSelector.showAndWait();
        screenShotSelectedHash.ifPresent(button -> {
            if (!button.getButtonData().isCancelButton()) {
                Task donwloadTask = ImageUtils.downloadIGDBImageToCache(gameEntry.getIgdb_id()
                        , screenshotSelector.getSelectedImageHash()
                        , ImageUtils.IGDB_TYPE_SCREENSHOT
                        , ImageUtils.IGDB_SIZE_BIG_2X
                        , new OnDLDoneHandler() {
                            @Override
                            public void run(File outputfile) {
                                Image img = new Image("file:" + File.separator + File.separator + File.separator + outputfile.getAbsolutePath(), GENERAL_SETTINGS.getWindowWidth(), GENERAL_SETTINGS.getWindowHeight(), false, true);
                                ImageUtils.transitionToImage(img, backgroundView, BaseScene.BACKGROUND_IMAGE_MAX_OPACITY);

                                chosenImageFiles[1] = outputfile;
                                File coverLocalImageFile = new File(GameEntry.ENTRIES_FOLDER + File.separator + entry.getUuid().toString() + File.separator + ImageUtils.IGDB_TYPE_SCREENSHOT + "." + getExtension(outputfile));
                                entry.setImagePath(1, coverLocalImageFile);

                            }
                        });
                validEntriesConditions.add(new ValidEntryCondition() {
                    @Override
                    public boolean isValid() {
                        if (!donwloadTask.isDone()) {
                            message.replace(0, message.length(), Main.RESSOURCE_BUNDLE.getString("background_picture_still_downloading"));
                            return false;
                        }
                        return true;
                    }

                    @Override
                    public void onInvalid() {

                    }
                });

                if (gameEntry.getIgdb_id() != -1) {
                    entry.setIgdb_id(gameEntry.getIgdb_id());
                }
            } else {
                if (chosenImageFiles[1] != null) {
                    Image img = new Image("file:" + File.separator + File.separator + File.separator + chosenImageFiles[1].getAbsolutePath(), GENERAL_SETTINGS.getWindowWidth(), GENERAL_SETTINGS.getWindowHeight(), false, true);
                    ImageUtils.transitionToImage(img, backgroundView, BaseScene.BACKGROUND_IMAGE_MAX_OPACITY);
                } else {
                    ImageUtils.transitionToImage(null, backgroundView, BaseScene.BACKGROUND_IMAGE_MAX_OPACITY);
                }

            }
        });
    }

    protected void setOnExitAction(ExitAction onExitAction) {
        this.onExitAction = onExitAction;
    }

    protected void addCancelButton(ExitAction onAction) {
        boolean alreadyExists = false;
        for (Node n : buttonsBox.getChildren()) {
            if (n.getId() != null && n.getId().equals("cancelButton")) {
                alreadyExists = true;
                break;
            }
        }
        if (!alreadyExists) {
            Button cancelButton = new Button(RESSOURCE_BUNDLE.getString("cancel"));
            cancelButton.setId("cancelButton");
            cancelButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    GameRoomAlert alert = new GameRoomAlert(Alert.AlertType.CONFIRMATION);
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
                        onAction.run();
                    } else {
                        // ... user chose CANCEL or closed the dialog
                    }
                    onAction.run();
                }
            });
            buttonsBox.getChildren().add(cancelButton);
        }
    }

}

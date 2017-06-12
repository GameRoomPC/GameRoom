package ui.scene;

import com.mashape.unirest.http.exceptions.UnirestException;
import data.game.entry.*;
import data.game.scraper.IGDBScraper;
import data.game.scraper.OnDLDoneHandler;
import data.http.YoutubeSoundtrackScrapper;
import data.http.images.ImageUtils;
import data.io.FileUtils;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
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
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.apache.commons.lang.StringEscapeUtils;
import org.controlsfx.control.CheckComboBox;
import org.json.JSONException;
import org.json.JSONObject;
import system.application.settings.PredefinedSetting;
import system.os.Terminal;
import system.os.WindowsShortcut;
import ui.GeneralToast;
import ui.Main;
import ui.control.ValidEntryCondition;
import ui.control.button.ImageButton;
import ui.control.button.gamebutton.GameButton;
import ui.control.textfield.AppPathField;
import ui.control.textfield.CMDTextField;
import ui.control.textfield.PathTextField;
import ui.control.textfield.PlayTimeField;
import ui.dialog.GameRoomAlert;
import ui.dialog.SearchDialog;
import ui.dialog.selector.AppSelectorDialog;
import ui.dialog.selector.IGDBImageSelector;
import ui.pane.OnItemSelectedHandler;
import ui.pane.SelectListPane;
import ui.scene.exitaction.ClassicExitAction;
import ui.scene.exitaction.ExitAction;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Optional;
import java.util.regex.Pattern;

import static data.io.FileUtils.getExtension;
import static system.application.settings.GeneralSettings.settings;
import static ui.Main.*;

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

        String name = chosenFile.getName();
        if (!getExtension(name).equals("")) {
            int extensionIndex = name.lastIndexOf(getExtension(name));
            if (extensionIndex != -1) {
                name = name.substring(0, extensionIndex - 1);
            }
        }
        String path = chosenFile.getAbsolutePath();
        try {
            if (FileUtils.getExtension(chosenFile).equals("lnk")) {
                WindowsShortcut shortcut = new WindowsShortcut(chosenFile);
                chosenFile = new File(shortcut.getRealFilename());
                path = chosenFile.getAbsolutePath();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            //not a shortcut :)
        }
        entry = new GameEntry(name);
        entry.setPath(path);
        init(previousScene, null);
    }

    public GameEditScene(BaseScene previousScene, GameEntry entry, Image coverImage) {
        this(previousScene, entry, MODE_EDIT, coverImage);
    }

    public GameEditScene(BaseScene previousScene, GameEntry entry, int mode, Image coverImage) {
        super(new StackPane(), previousScene.getParentStage());
        this.mode = mode;
        this.entry = entry;
        this.entry.setSavedLocally(false);
        init(previousScene, coverImage);
    }

    private void init(BaseScene previousScene, Image coverImage) {
        onExitAction = new ClassicExitAction(this, previousScene.getParentStage(), previousScene);

        for (int i = 0; i < chosenImageFiles.length; i++) {
            if (entry.getImagePath(i) != null) {
                this.chosenImageFiles[i] = entry.getImagePath(i);
            }
        }

        imageChooser = new FileChooser();
        imageChooser.setTitle(Main.getString("select_picture"));
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
        Button addButton = new Button(Main.getString("add") + "!");
        if (mode == MODE_EDIT) {
            addButton.setText(Main.getString("save") + "!");
        }
        addButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                boolean allConditionsMet = true;
                for (ValidEntryCondition condition : validEntriesConditions) {
                    boolean conditionValid = condition.isValid();
                    allConditionsMet = allConditionsMet && conditionValid;
                    if (!conditionValid) {
                        condition.onInvalid();
                        GameRoomAlert.error(condition.message.toString());
                    }
                }
                if (allConditionsMet) {
                    if (entry.isToAdd()) {
                        entry.setToAdd(false);
                    }
                    GeneralToast.displayToast(Main.getString("saving") + " " + entry.getName(), getParentStage(), GeneralToast.DURATION_SHORT);

                    entry.setSavedLocally(true);
                    entry.saveEntry();

                    for (int i = 0; i < chosenImageFiles.length; i++) {
                        if (chosenImageFiles[i] != null) {
                            try {
                                entry.updateImage(i, chosenImageFiles[i]);
                            } catch (IOException e) {
                                GameRoomAlert.error(Main.getString("error_move_images"));
                            }
                        }
                    }

                    switch (mode) {
                        case MODE_ADD:
                            entry.setAddedDate(LocalDateTime.now());
                            MAIN_SCENE.addGame(entry);
                            GeneralToast.displayToast(entry.getName() + Main.getString("added_to_your_lib"), getParentStage());
                            break;
                        case MODE_EDIT:
                            MAIN_SCENE.updateGame(entry);
                            GeneralToast.displayToast(Main.getString("changes_saved"), getParentStage());
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
        Button igdbButton = new Button(Main.getString("fetch_from_igdb"));
        igdbButton.setOnAction(new EventHandler<ActionEvent>() {
                                   @Override
                                   public void handle(ActionEvent event) {
                                       SearchDialog dialog = new SearchDialog(createDoNotUpdateFielsMap(), entry.getName());
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
                    settings().getWindowWidth() * BACKGROUND_IMAGE_LOAD_RATIO,
                    settings().getWindowHeight() * BACKGROUND_IMAGE_LOAD_RATIO
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
                    message.replace(0, message.length(), Main.getString("invalid_name_empty"));
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

        // create the data to show in the CheckComboBox
        final ObservableList<Serie> allSeries = FXCollections.observableArrayList();
        allSeries.addAll(Serie.values());
        allSeries.sort(new Comparator<Serie>() {
            @Override
            public int compare(Serie o1, Serie o2) {
                if (o1 == null || o1.getName() == null) {
                    return -1;
                }
                if (o2 == null || o2.getName() == null) {
                    return 1;
                }
                return o1.getName().compareTo(o2.getName());
            }
        });
        // Create the CheckComboBox with the data
        final ComboBox<Serie> serieComboBox = new ComboBox<Serie>(allSeries);
        serieComboBox.setId("serie");
        if (entry.getGenres() != null) {
            serieComboBox.getSelectionModel().select(entry.getSerie());
        }
        serieComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            entry.setSerie(newValue);
        });

        serieComboBox.getSelectionModel().select(entry.getSerie());

        Label serieLabel = new Label(Main.getString("serie") + " :");
        serieLabel.setTooltip(new Tooltip(Main.getString("serie")));
        contentPane.add(serieLabel, 0, row_count);
        contentPane.add(serieComboBox, 1, row_count);
        row_count++;

        /**************************PATH*********************************************/
        contentPane.add(new Label(Main.getString("game_path") + " :"), 0, row_count);
        Node pathNode = new Label();
        if (!entry.isSteamGame()) {
            AppPathField gamePathField = new AppPathField(entry.getPath(), getWindow(), PathTextField.FILE_CHOOSER_APPS, Main.getString("select_picture"));
            gamePathField.getTextField().setPrefColumnCount(50);
            gamePathField.setId("game_path");
            gamePathField.getTextField().textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    entry.setPath(newValue);
                }
            });
            pathNode = gamePathField;
        } else {
            pathNode = new Label(entry.getPath());
            pathNode.setFocusTraversable(false);
        }

        validEntriesConditions.add(new ValidEntryCondition() {
            @Override
            public boolean isValid() {
                if (entry.getPath().equals("")) {
                    message.replace(0, message.length(), Main.getString("invalid_path_empty"));
                    return false;
                }
                File file = new File(entry.getPath());
                Pattern pattern = Pattern.compile("^steam:\\/\\/rungameid\\/\\d*$");
                boolean isSteamGame = pattern.matcher(entry.getPath().trim()).matches();
                if (!isSteamGame && !file.exists()) {
                    message.replace(0, message.length(), Main.getString("invalid_path_not_file"));
                    return false;
                } else if (!isSteamGame && file.isDirectory()) {
                    try {
                        AppSelectorDialog selector = new AppSelectorDialog(new File(entry.getPath()));
                        selector.searchApps();
                        Optional<ButtonType> appOptionnal = selector.showAndWait();

                        final boolean[] result = {true};

                        appOptionnal.ifPresent(pairs -> {
                            if (pairs.getButtonData().equals(ButtonBar.ButtonData.OK_DONE)) {
                                entry.setPath(selector.getSelectedFile().getAbsolutePath());
                            } else {
                                result[0] = false;
                            }
                        });
                        if (!result[0]) {
                            message.replace(0, message.length(), Main.getString("invalid_path_not_file"));
                            return result[0];
                        }
                    } catch (IllegalArgumentException e) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public void onInvalid() {
                setLineInvalid("game_path");
            }
        });

        contentPane.add(pathNode, 1, row_count);
        row_count++;

        /**************************RUN AS ADMIN ****************************************/
        contentPane.add(new Label(Main.getString("run_as_admin") + " :"), 0, row_count);
        CheckBox adminCheckBox = new CheckBox();
        adminCheckBox.setSelected(entry.mustRunAsAdmin());
        adminCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            entry.setRunAsAdmin(newValue);
        });
        adminCheckBox.setDisable(!checkPowershellAvailable());

        contentPane.add(adminCheckBox, 1, row_count);
        row_count++;
        /**************************PLAYTIME*********************************************/
        Label titlePlayTimeLabel = new Label(Main.getString("play_time") + " :");
        titlePlayTimeLabel.setTooltip(new Tooltip(Main.getString("play_time")));
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
        releaseDateField.setPromptText(Main.getString("date_example"));
        releaseDateField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (!releaseDateField.getText().equals("")) {
                    try {
                        LocalDateTime time = LocalDateTime.from(LocalDate.parse(releaseDateField.getText(), GameEntry.DATE_DISPLAY_FORMAT).atStartOfDay());
                        entry.setReleaseDate(time);
                    } catch (DateTimeParseException e) {
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
                        LocalDateTime time = LocalDateTime.from(LocalDate.parse(releaseDateField.getText(), GameEntry.DATE_DISPLAY_FORMAT).atStartOfDay());
                        entry.setReleaseDate(time);
                    } catch (DateTimeParseException e) {
                        message.replace(0, message.length(), Main.getString("invalid_release_date"));
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

        /**************************PLATFORM*********************************************/
        if (!entry.getPlatform().isPC() || entry.getPlatform().equals(data.game.entry.Platform.NONE)) {
            try {
                data.game.entry.Platform.initWithDb();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            final ObservableList<data.game.entry.Platform> platforms = FXCollections.observableArrayList(data.game.entry.Platform.getEmulablePlatforms());
            platforms.add(0,data.game.entry.Platform.NONE);

            // Create the CheckComboBox with the data
            final ComboBox<data.game.entry.Platform> platformComboBox = new ComboBox<data.game.entry.Platform>(platforms);
            platformComboBox.setId("platform");
            if (entry.getPlatform() != null) {
                platformComboBox.getSelectionModel().select(entry.getPlatform());
            }
            platformComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                entry.setPlatform(newValue);
            });

            Label platformLabel = new Label(Main.getString("platform") + " :");
            platformLabel.setTooltip(new Tooltip(Main.getString("platform")));
            contentPane.add(platformLabel, 0, row_count);
            contentPane.add(platformComboBox, 1, row_count);
            row_count++;
        }

        /************************** DEVS AND PUBLISHER*********************************************/

        final ObservableList<Company> allCompanies = FXCollections.observableArrayList();
        allCompanies.addAll(Company.values());
        allCompanies.sort(Comparator.comparing(Company::getName));

        /**************************DEVELOPER*********************************************/
        // Create the CheckComboBox with the data
        final CheckComboBox<Company> devComboBox = new CheckComboBox<Company>(allCompanies);
        devComboBox.setId("developer");
        if (entry.getDevelopers() != null) {
            for (Company developer : entry.getDevelopers()) {
                devComboBox.getCheckModel().check(devComboBox.getCheckModel().getItemIndex(developer));
            }
        }
        devComboBox.getCheckModel().getCheckedItems().addListener(new ListChangeListener<Company>() {
            @Override
            public void onChanged(Change<? extends Company> c) {
                ArrayList<Company> newDevs = new ArrayList<>();
                newDevs.addAll(devComboBox.getCheckModel().getCheckedItems());
                entry.setDevelopers(newDevs);
            }
        });
        Label devLabel = new Label(Main.getString("developer") + " :");
        devLabel.setTooltip(new Tooltip(Main.getString("developer")));
        contentPane.add(devLabel, 0, row_count);
        contentPane.add(devComboBox, 1, row_count);
        row_count++;

        /**************************PUBLISHER*********************************************/
        // Create the CheckComboBox with the data
        final CheckComboBox<Company> pubComboBox = new CheckComboBox<Company>(allCompanies);
        pubComboBox.setId("publisher");
        if (entry.getPublishers() != null) {
            for (Company publisher : entry.getPublishers()) {
                pubComboBox.getCheckModel().check(pubComboBox.getCheckModel().getItemIndex(publisher));
            }
        }
        pubComboBox.getCheckModel().getCheckedItems().addListener(new ListChangeListener<Company>() {
            @Override
            public void onChanged(Change<? extends Company> c) {
                ArrayList<Company> newPublishers = new ArrayList<>();
                newPublishers.addAll(pubComboBox.getCheckModel().getCheckedItems());
                entry.setPublishers(newPublishers);
            }
        });
        Label pubLabel = new Label(Main.getString("publisher") + " :");
        pubLabel.setTooltip(new Tooltip(Main.getString("publisher")));
        contentPane.add(pubLabel, 0, row_count);
        contentPane.add(pubComboBox, 1, row_count);
        row_count++;

        /**************************GENRE*********************************************/

        // create the data to show in the CheckComboBox
        final ObservableList<GameGenre> allGamesGenre = FXCollections.observableArrayList();
        allGamesGenre.addAll(GameGenre.values());
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
                ArrayList<GameGenre> newGenres = new ArrayList<>();
                newGenres.addAll(genreComboBox.getCheckModel().getCheckedItems());
                entry.setGenres(newGenres);
            }
        });
        Label genreLabel = new Label(Main.getString("genre") + " :");
        genreLabel.setTooltip(new Tooltip(Main.getString("genre")));
        contentPane.add(genreLabel, 0, row_count);
        contentPane.add(genreComboBox, 1, row_count);
        row_count++;

        /**************************THEME*********************************************/

        // create the data to show in the CheckComboBox
        final ObservableList<GameTheme> allGamesTheme = FXCollections.observableArrayList();
        allGamesTheme.addAll(GameTheme.values());
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
                ArrayList<GameTheme> newThemes = new ArrayList<>();
                newThemes.addAll(themeComboBox.getCheckModel().getCheckedItems());
                entry.setThemes(newThemes);
            }
        });
        Label themeLabel = new Label(Main.getString("theme") + " :");
        themeLabel.setTooltip(new Tooltip(Main.getString("theme")));
        contentPane.add(themeLabel, 0, row_count);
        contentPane.add(themeComboBox, 1, row_count);
        row_count++;

        /**************************SCREENSHOT*********************************************/
        OnDLDoneHandler screenshotDlDoneHandler = new OnDLDoneHandler() {
            @Override
            public void run(File outputfile) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        Image img = new Image("file:" + File.separator + File.separator + File.separator + outputfile.getAbsolutePath(), settings().getWindowWidth() * BACKGROUND_IMAGE_LOAD_RATIO, settings().getWindowHeight() * BACKGROUND_IMAGE_LOAD_RATIO, false, true);
                        ImageUtils.transitionToWindowBackground(img, backgroundView);
                        chosenImageFiles[1] = outputfile;
                    }
                });
            }
        };
        Label screenshotLabel = new Label(Main.getString("wallpaper") + " :");
        screenshotLabel.setTooltip(new Tooltip(Main.getString("wallpaper")));
        contentPane.add(screenshotLabel, 0, row_count);

        HBox screenShotButtonsBox = new HBox();
        screenShotButtonsBox.setSpacing(20 * Main.SCREEN_WIDTH / 1920);
        screenShotButtonsBox.setAlignment(Pos.CENTER_LEFT);

        double imgSize = settings().getWindowWidth() / 24;
        //ImageButton screenshotFileButton = new ImageButton(new Image("res/ui/folderButton.png", , settings().getWindowWidth() / 24, false, true));
        ImageButton screenshotFileButton = new ImageButton("folder-button", imgSize, imgSize);
        screenshotFileButton.setOnAction(event -> {
            chosenImageFiles[1] = imageChooser.showOpenDialog(getParentStage());
            screenshotDlDoneHandler.run(chosenImageFiles[1]);
        });
        Label orLabel = new Label(Main.getString("or"));

        Button screenshotIGDBButton = new Button(Main.getString("IGDB"));
        screenshotIGDBButton.setOnAction(new EventHandler<ActionEvent>() {
                                             @Override
                                             public void handle(ActionEvent event) {
                                                 if (entry.getIgdb_id() != -1) {
                                                     GameEntry gameEntry = entry;
                                                     try {
                                                         JSONObject gameData = IGDBScraper.getGameData(gameEntry.getIgdb_id());
                                                         if (gameData != null) {
                                                             gameEntry.setIgdb_imageHashs(IGDBScraper.getScreenshotHash(gameData));
                                                             openImageSelector(gameEntry);
                                                         } else {
                                                             GameRoomAlert.info(Main.getString("error_no_screenshot_igdb"));
                                                         }
                                                     } catch (JSONException jse) {
                                                         if (jse.toString().contains("[\"screenshots\"] not found")) {
                                                             GameRoomAlert.error(Main.getString("no_screenshot_for_this_game"));
                                                         } else {
                                                             jse.printStackTrace();
                                                         }
                                                     } catch (UnirestException e) {
                                                         GameRoomAlert.errorIGDB();
                                                         LOGGER.error(e.getMessage());
                                                     }
                                                 } else {
                                                     SearchDialog dialog = new SearchDialog(createDoNotUpdateFielsMap(), entry.getName());
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
                if (!entry.getImagePath(1).isValid()) {
                    message.replace(0,message.length(),Main.getString("background_picture_still_downloading"));
                    return false;
                }
                return true;
            }

            @Override
            public void onInvalid() {
                setLineInvalid("game_name");
            }
        });*/
        /**************************DESCRIPTION*********************************************/
        Label titleDescriptionLabel = new Label(Main.getString("game_description") + " :");
        titleDescriptionLabel.setTooltip(new Tooltip(Main.getString("game_description")));
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

        /**************************YOUTUBE*********************************************/
        if (!settings().getBoolean(PredefinedSetting.DISABLE_GAME_MAIN_THEME)) {
            Label youtubeSoundtrackLabel = new Label(Main.getString("youtube_soundtrack_label") + " :");
            youtubeSoundtrackLabel.setTooltip(new Tooltip(Main.getString("youtube_soundtrack_tooltip")));
            //youtubeSoundtrackLabel.setStyle(SettingsScene.ADVANCE_MODE_LABEL_STYLE);
            youtubeSoundtrackLabel.setId("advanced-setting-label");
            contentPane.add(youtubeSoundtrackLabel, 0, row_count);
            TextField youtubeSoundtrackField = new TextField(entry.getYoutubeSoundtrackHash().equals("") ? "" : YoutubeSoundtrackScrapper.toYoutubeUrl(entry.getYoutubeSoundtrackHash()));
            youtubeSoundtrackField.setId("youtube_soundtrack");
            youtubeSoundtrackField.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    if (newValue.equals("")) {
                        entry.setYoutubeSoundtrackHash("");
                    }
                    String hash = YoutubeSoundtrackScrapper.hashFromYoutubeUrl(newValue);
                    if (hash != null) {
                        entry.setYoutubeSoundtrackHash(hash);
                    }
                }
            });
            contentPane.add(youtubeSoundtrackField, 1, row_count);
            row_count++;
        }


        /**************************CMD & ARGS*********************************************/
        if (settings().getBoolean(PredefinedSetting.ADVANCED_MODE)) {
            Label argsLabel = new Label(Main.getString("args_label") + " :");
            argsLabel.setTooltip(new Tooltip(Main.getString("args_tooltip")));
            //argsLabel.setStyle(SettingsScene.ADVANCE_MODE_LABEL_STYLE);
            argsLabel.setId("advanced-setting-label");
            contentPane.add(argsLabel, 0, row_count);
            TextField argsField = new TextField(entry.getArgs());
            argsField.setId("args");
            argsField.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    entry.setArgs(newValue);
                }
            });
            contentPane.add(argsField, 1, row_count);
            row_count++;

            Label cmdBeforeLabel = new Label(Main.getString("cmd_before_label") + " :");
            cmdBeforeLabel.setTooltip(new Tooltip(Main.getString("cmd_before_tooltip")));
            //cmdBeforeLabel.setStyle(SettingsScene.ADVANCE_MODE_LABEL_STYLE);
            cmdBeforeLabel.setId("advanced-setting-label");
            contentPane.add(cmdBeforeLabel, 0, row_count);
            CMDTextField cmdBeforeField = new CMDTextField(entry.getCmd(GameEntry.CMD_BEFORE_START));
            cmdBeforeField.setWrapText(true);
            cmdBeforeField.setId("cmd_before");
            cmdBeforeField.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    entry.setCmd(GameEntry.CMD_BEFORE_START, newValue);
                }
            });
            contentPane.add(cmdBeforeField, 1, row_count);
            row_count++;

            Label cmdAfterLabel = new Label(Main.getString("cmd_after_label") + " :");
            cmdAfterLabel.setTooltip(new Tooltip(Main.getString("cmd_after_tooltip")));
            //cmdAfterLabel.setStyle(SettingsScene.ADVANCE_MODE_LABEL_STYLE);
            cmdAfterLabel.setId("advanced-setting-label");
            contentPane.add(cmdAfterLabel, 0, row_count);
            CMDTextField cmdAfterField = new CMDTextField(entry.getCmd(GameEntry.CMD_AFTER_END));
            cmdAfterField.setWrapText(true);
            cmdAfterField.setId("cmd_after");
            cmdAfterField.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    entry.setCmd(GameEntry.CMD_AFTER_END, newValue);
                }
            });
            contentPane.add(cmdAfterField, 1, row_count);
            row_count++;
        }

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
        Label titleLabel = new Label(Main.getString(property) + " :");
        titleLabel.setTooltip(new Tooltip(Main.getString(property)));
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
        if (doNotUpdateFieldsMap.get(property) == null || !doNotUpdateFieldsMap.get(property))
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
                        } else if (property.equals("genre")) {
                            if (node instanceof CheckComboBox) {
                                for (GameGenre genre : (ArrayList<GameGenre>) newValue) {
                                    ((CheckComboBox) node).getCheckModel().check(genre);
                                }
                            }
                        } else if (property.equals("theme")) {
                            if (node instanceof CheckComboBox) {
                                for (GameTheme theme : (ArrayList<GameTheme>) newValue) {
                                    ((CheckComboBox) node).getCheckModel().check(theme);
                                }
                            }
                        } else if (property.equals("developer")) {
                            if (node instanceof CheckComboBox) {
                                for (Company dev : (ArrayList<Company>) newValue) {
                                    ((CheckComboBox) node).getCheckModel().check(dev);
                                }
                            }
                        } else if (property.equals("publisher")) {
                            if (node instanceof CheckComboBox) {
                                for (Company pub : (ArrayList<Company>) newValue) {
                                    ((CheckComboBox) node).getCheckModel().check(pub);
                                }
                            }
                        } else if (property.equals("serie")) {
                            if (node instanceof ComboBox) {
                                ((ComboBox<Serie>) node).getSelectionModel().select((Serie) newValue);
                            }
                        } else if (newValue instanceof LocalDateTime) {
                            if (node instanceof TextField) {
                                ((TextField) node).setText(newValue != null ? GameEntry.DATE_DISPLAY_FORMAT.format((LocalDateTime) newValue) : "");
                            }
                        }
                        break;
                    }
                }
            }
    }

    private Pane createLeft(Image coverImage) {
        StackPane pane = new StackPane();
        double coverWidth = settings().getWindowHeight() * 2 / (3 * GameButton.COVER_HEIGHT_WIDTH_RATIO);
        double coverHeight = settings().getWindowHeight() * 2 / 3;

        coverView = new ImageView();
        coverView.setFitWidth(coverWidth);
        coverView.setFitHeight(coverHeight);

        Image defaultImage = new Image("res/defaultImages/cover1024.jpg", coverWidth, coverHeight, false, true);

        ImageView defaultImageView = new ImageView(defaultImage);
        defaultImageView.setFitWidth(coverWidth);
        defaultImageView.setFitHeight(coverHeight);


        if (coverImage != null) {
            ImageUtils.transitionToImage(coverImage, coverView);
        } else {
            Task<Image> loadImageTask = new Task<Image>() {
                @Override
                protected Image call() throws Exception {
                    return entry.getImage(0, coverWidth, coverHeight, false, true);
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

        double imgSize = settings().getWindowWidth() / 15;
        //ImageButton changeImageButton = new ImageButton(new Image("res/ui/folderButton.png", settings().getWindowWidth() / 12, settings().getWindowWidth() / 12, false, true));
        ImageButton changeImageButton = new ImageButton("folder-button", imgSize, imgSize);
        changeImageButton.setOpacity(0);
        changeImageButton.setFocusTraversable(false);
        changeImageButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                chosenImageFiles[0] = imageChooser.showOpenDialog(getParentStage());
                String localCoverPath = GameEntryUtils.coverPath(entry) + "." + getExtension(chosenImageFiles[0].getName());

                Image img = new Image("file:" + File.separator + File.separator + File.separator + chosenImageFiles[0].getAbsolutePath(), settings().getWindowHeight() * 2 / (3 * GameButton.COVER_HEIGHT_WIDTH_RATIO), settings().getWindowHeight() * 2 / 3, false, true);
                ImageUtils.transitionToImage(img, coverView);
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
        defaultImageView.setEffect(blur);


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
        pane.getChildren().addAll(defaultImageView, coverView, changeImageButton);
        wrappingPane.setLeft(pane);
        BorderPane.setMargin(pane, new Insets(50 * SCREEN_HEIGHT / 1080, 50 * SCREEN_WIDTH / 1920, 50 * SCREEN_HEIGHT / 1080, 50 * SCREEN_WIDTH / 1920));
        return pane;
    }

    private void initTop() {
        EventHandler<ActionEvent> backButtonHandler = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                GameRoomAlert alert = new GameRoomAlert(Alert.AlertType.CONFIRMATION);
                alert.setContentText(Main.getString("ignore_changes?"));

                Optional<ButtonType> result = alert.showAndWait();
                result.ifPresent(buttonType -> {
                    if (buttonType.equals(ButtonType.OK)) {
                        switch (mode) {
                            case MODE_ADD:
                                entry.reloadFromDB();
                                break;
                            case MODE_EDIT:
                                entry.reloadFromDB();
                                //was previously entry.loadEntry();
                                break;
                            default:
                                break;
                        }
                        fadeTransitionTo(previousScene, getParentStage());
                    } else {
                        // ... user chose CANCEL or closed the dialog
                    }
                });
            }
        };
        String title = Main.getString("add_a_game");
        if (mode == MODE_EDIT) {
            title = Main.getString("edit_a_game");
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
        /*map.put("game_name", false);
        map.put("serie", entry.getSerie() != null && !entry.getSerie().equals(""));
        map.put("release_date", entry.getReleaseDate() != null);
        map.put("developer", entry.getDeveloper() != null && !entry.getDeveloper().equals(""));
        map.put("game_description", entry.getDescription() != null && !entry.getDescription().equals(""));
        map.put("publisher", entry.getPublisher() != null && !entry.getPublisher().equals(""));
        map.put("genre", entry.getGenres() != null);
        map.put("theme", entry.getThemes() != null);
        map.put("cover", mode != MODE_ADD && entry.getImagePath(0) != null);*/
        return map;
    }

    //called when user selected a igdb game or when a steam game is added
    private void onNewEntryData(GameEntry gameEntry, HashMap<String, Boolean> doNotUpdateFieldsMap) {
        updateLineProperty("game_name", gameEntry.getName(), doNotUpdateFieldsMap);
        updateLineProperty("serie", gameEntry.getSerie(), doNotUpdateFieldsMap);
        updateLineProperty("release_date", gameEntry.getReleaseDate(), doNotUpdateFieldsMap);
        updateLineProperty("developer", gameEntry.getDevelopers(), doNotUpdateFieldsMap);
        updateLineProperty("publisher", gameEntry.getPublishers(), doNotUpdateFieldsMap);
        updateLineProperty("game_description", StringEscapeUtils.unescapeHtml(gameEntry.getDescription()), doNotUpdateFieldsMap);
        updateLineProperty("genre", gameEntry.getGenres(), doNotUpdateFieldsMap);
        updateLineProperty("theme", gameEntry.getThemes(), doNotUpdateFieldsMap);
        entry.setIgdb_id(gameEntry.getIgdb_id());
        entry.setAggregated_rating(gameEntry.getAggregated_rating());

        /*****************COVER DOWNLOAD***************************/
        if (!doNotUpdateFieldsMap.containsKey("cover") || !doNotUpdateFieldsMap.get("cover")) {
            GeneralToast.displayToast(Main.getString("downloading_images"), getParentStage(), GeneralToast.DURATION_SHORT);
            ImageUtils.downloadIGDBImageToCache(gameEntry.getIgdb_id()
                    , gameEntry.getIgdb_imageHash(0)
                    , ImageUtils.IGDB_TYPE_COVER
                    , ImageUtils.IGDB_SIZE_BIG_2X
                    , new OnDLDoneHandler() {
                        @Override
                        public void run(File outputfile) {
                            Image img = new Image("file:" + File.separator + File.separator + File.separator + outputfile.getAbsolutePath(), settings().getWindowHeight() * 2 / (3 * GameButton.COVER_HEIGHT_WIDTH_RATIO), settings().getWindowHeight() * 2 / 3, false, true);
                            ImageUtils.transitionToImage(img, coverView);

                            chosenImageFiles[0] = outputfile;
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
                                Image img = new Image("file:" + File.separator + File.separator + File.separator + outputfile.getAbsolutePath(), settings().getWindowWidth() * BACKGROUND_IMAGE_LOAD_RATIO, settings().getWindowHeight() * BACKGROUND_IMAGE_LOAD_RATIO, false, true);
                                ImageUtils.transitionToWindowBackground(img, backgroundView);
                            }
                        });
            }
        });
        Optional<ButtonType> screenShotSelectedHash = screenshotSelector.showAndWait();
        screenShotSelectedHash.ifPresent(button -> {
            if (!button.getButtonData().isCancelButton()) {
                GeneralToast.displayToast(Main.getString("downloading_images"), getParentStage(), GeneralToast.DURATION_SHORT);

                Task donwloadTask = ImageUtils.downloadIGDBImageToCache(gameEntry.getIgdb_id()
                        , screenshotSelector.getSelectedImageHash()
                        , ImageUtils.IGDB_TYPE_SCREENSHOT
                        , ImageUtils.IGDB_SIZE_BIG_2X
                        , new OnDLDoneHandler() {
                            @Override
                            public void run(File outputfile) {
                                Image img = new Image("file:" + File.separator + File.separator + File.separator + outputfile.getAbsolutePath(), settings().getWindowWidth() * BACKGROUND_IMAGE_LOAD_RATIO, settings().getWindowHeight() * BACKGROUND_IMAGE_LOAD_RATIO, false, true);
                                ImageUtils.transitionToWindowBackground(img, backgroundView);

                                chosenImageFiles[1] = outputfile;
                            }
                        });
                validEntriesConditions.add(new ValidEntryCondition() {
                    @Override
                    public boolean isValid() {
                        if (donwloadTask != null && !donwloadTask.isDone()) {
                            message.replace(0, message.length(), Main.getString("background_picture_still_downloading"));
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
                    Image img = new Image("file:" + File.separator + File.separator + File.separator + chosenImageFiles[1].getAbsolutePath(), settings().getWindowWidth() * BACKGROUND_IMAGE_LOAD_RATIO, settings().getWindowHeight() * BACKGROUND_IMAGE_LOAD_RATIO, false, true);
                    ImageUtils.transitionToWindowBackground(img, backgroundView);
                    LOGGER.debug(chosenImageFiles[1].getAbsolutePath());
                } else {
                    ImageUtils.transitionToImage(null, backgroundView, BaseScene.BACKGROUND_IMAGE_MAX_OPACITY);
                }

            }
        });
    }

    void setOnExitAction(ExitAction onExitAction) {
        this.onExitAction = onExitAction;
    }


    void addCancelAllButton() {
        addCancelButton(new ClassicExitAction(GameEditScene.this, getParentStage(), previousScene), "cancel_all_button", "ignore_changes_all");
    }

    void addCancelButton(ExitAction onAction) {
        addCancelButton(onAction, "cancel_button", "ignore_changes?");
    }

    private void addCancelButton(ExitAction action, String idAndTitleKey, String warningMessageKey) {
        boolean alreadyExists = false;
        for (Node n : buttonsBox.getChildren()) {
            if (n.getId() != null && n.getId().equals(idAndTitleKey)) {
                alreadyExists = true;
                break;
            }
        }
        if (!alreadyExists) {
            Button cancelButton = new Button(Main.getString(idAndTitleKey));
            cancelButton.setId(idAndTitleKey);
            cancelButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    GameRoomAlert alert = new GameRoomAlert(Alert.AlertType.CONFIRMATION);
                    alert.setContentText(Main.getString(warningMessageKey));

                    Optional<ButtonType> result = alert.showAndWait();
                    result.ifPresent(buttonType -> {
                        if (buttonType.equals(ButtonType.OK)) {
                            switch (mode) {
                                case MODE_ADD:
                                    entry.reloadFromDB();
                                    break;
                                case MODE_EDIT:
                                    entry.reloadFromDB();
                                    break;
                                default:
                                    break;
                            }
                            action.run();
                        } else {
                            // ... user chose CANCEL or closed the dialog
                        }
                        //onAction.run();
                    });
                }
            });
            buttonsBox.getChildren().add(cancelButton);
        }
    }

    private static boolean checkPowershellAvailable() {
        Terminal t = new Terminal(false);
        try {
            String[] output = t.execute("powershell", "/?");
            if (output != null) {
                for (String s : output) {
                    if (s.contains("PowerShell[.exe]")) {
                        t.getProcess().destroy();
                        return true;
                    }
                }
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }
}

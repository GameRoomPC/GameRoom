package ui.scene;

import data.ImageUtils;
import data.game.GameWatcher;
import data.game.entry.AllGameEntries;
import data.game.entry.GameEntry;
import data.game.scanner.FolderGameScanner;
import data.game.scanner.OnGameFoundHandler;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import system.application.settings.PredefinedSetting;
import ui.Main;
import ui.control.button.ImageButton;
import ui.control.button.gamebutton.GameButton;
import ui.control.specific.GeneralToast;
import ui.control.textfield.PathTextField;
import ui.dialog.ChoiceDialog;
import ui.dialog.GameRoomAlert;
import ui.dialog.GameRoomCustomAlert;
import ui.dialog.selector.GameScannerSelector;
import ui.pane.gamestilepane.*;
import ui.scene.exitaction.ClassicExitAction;
import ui.scene.exitaction.ExitAction;
import ui.scene.exitaction.MultiAddExitAction;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

import static ui.Main.*;
import static ui.control.button.gamebutton.GameButton.COVER_HEIGHT_WIDTH_RATIO;

/**
 * Created by LM on 03/07/2016.
 */
public class MainScene extends BaseScene {
    private int input_mode = 0;

    public final static int INPUT_MODE_MOUSE = 0;
    public final static int INPUT_MODE_KEYBOARD = 1;

    private final static double MAX_TILE_ZOOM = 0.675;
    private final static double MIN_TILE_ZOOM = 0.10;

    private static boolean GARBAGE_COLLECTED_RECENTLY = false;

    private VBox tilesPaneWrapper = new VBox();
    private ScrollPane scrollPane;
    private BorderPane wrappingPane;
    private StackPane topPane;

    private Slider sizeSlider = new Slider();

    private GamesTilePane tilePane;
    private RowCoverTilePane lastPlayedTilePane;
    private RowCoverTilePane recentlyAddedTilePane;
    private ToAddRowTilePane toAddTilePane;

    private GameWatcher gameWatcher = null;

    private ArrayList<GroupRowTilePane> groupRowList = new ArrayList<>();

    private TextField searchField;
    private boolean showTilesPaneAgainAfterCancelSearch = false;

    private Label statusLabel;

    private boolean changeBackgroundNextTime = false;

    public MainScene(Stage parentStage) {
        super(new StackPane(), parentStage);
        setCursor(Cursor.DEFAULT);
        addEventHandler(MouseEvent.MOUSE_MOVED, new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {
                if (getInputMode() == MainScene.INPUT_MODE_KEYBOARD) {
                    setInputMode(MainScene.INPUT_MODE_MOUSE);
                }
            }
        });
        initAll();
    }

    private void initAll() {
        initCenter();
        initTop();
        displayWelcomeMessage();
        loadGames();
        loadPreviousUIValues();
        configureAutomaticCaching();
    }

    private void configureAutomaticCaching() {
        //to empty ram usage
        getParentStage().focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                tilePane.setCacheGameButtons(newValue);
                recentlyAddedTilePane.setCacheGameButtons(newValue);
                toAddTilePane.setCacheGameButtons(newValue);
                lastPlayedTilePane.setCacheGameButtons(newValue);

                for (GroupRowTilePane g : groupRowList) {
                    g.setCacheGameButtons(newValue);
                }
            }
        });
    }

    private void loadPreviousUIValues() {
        Main.runAndWait(() -> {
            if (Main.GENERAL_SETTINGS.getBoolean(PredefinedSetting.FOLDED_ROW_LAST_PLAYED)) {
                lastPlayedTilePane.fold();
            } else {
                lastPlayedTilePane.unfold();
            }
            if (Main.GENERAL_SETTINGS.getBoolean(PredefinedSetting.FOLDED_ROW_RECENTLY_ADDED)) {
                recentlyAddedTilePane.fold();
            } else {
                recentlyAddedTilePane.unfold();
            }
            if (Main.GENERAL_SETTINGS.getBoolean(PredefinedSetting.FOLDED_TOADD_ROW)) {
                toAddTilePane.fold();
            } else {
                toAddTilePane.unfold();
            }
            double scrollBarVValue = GENERAL_SETTINGS.getDouble(PredefinedSetting.SCROLLBAR_VVALUE);
            scrollPane.setVvalue(scrollBarVValue);

            double sizeSliderValue = Main.GENERAL_SETTINGS.getDouble(PredefinedSetting.TILE_ZOOM);
            if (sizeSliderValue <= MIN_TILE_ZOOM) {
                sizeSliderValue = MIN_TILE_ZOOM + 0.00001; //extreme values of the slider are buggy
            } else if (sizeSliderValue >= MAX_TILE_ZOOM) {
                sizeSliderValue = MAX_TILE_ZOOM + 0.00001; //extreme values of the slider are buggy
            }
            sizeSlider.setValue(sizeSliderValue);
        });
    }

    public void saveScrollBarVValue() {
        double scrollBarVValue = scrollPane.getVvalue();
        GENERAL_SETTINGS.setSettingValue(PredefinedSetting.SCROLLBAR_VVALUE, scrollBarVValue);
    }

    private void displayWelcomeMessage() {
        if (GENERAL_SETTINGS.getBoolean(PredefinedSetting.DISPLAY_WELCOME_MESSAGE)) {
            Platform.runLater(() -> {
                GameRoomAlert welcomeAlert = new GameRoomAlert(Alert.AlertType.INFORMATION, Main.getString("Welcome_message"));
                welcomeAlert.showAndWait();

                GameRoomAlert configureScannersAlert = new GameRoomAlert(Alert.AlertType.INFORMATION, Main.getString("configure_scanner_messages"));
                configureScannersAlert.showAndWait();

                GameScannerSelector selector = new GameScannerSelector();
                Optional<ButtonType> ignoredOptionnal = selector.showAndWait();
                ignoredOptionnal.ifPresent(pairs -> {
                    if (pairs.getButtonData().equals(ButtonBar.ButtonData.OK_DONE)) {
                        GENERAL_SETTINGS.setSettingValue(PredefinedSetting.ENABLED_GAME_SCANNERS, selector.getDisabledScanners());
                    }
                });
                GameRoomCustomAlert alert = new GameRoomCustomAlert();
                Label text = new Label(Main.getString("welcome_input_folder"));
                text.setWrapText(true);
                text.setPadding(new Insets(20 * Main.SCREEN_HEIGHT / 1080
                        , 20 * Main.SCREEN_WIDTH / 1920
                        , 20 * Main.SCREEN_HEIGHT / 1080
                        , 20 * Main.SCREEN_WIDTH / 1920));
                PathTextField field = new PathTextField(GENERAL_SETTINGS.getString(PredefinedSetting.GAMES_FOLDER), getWindow(), PathTextField.FILE_CHOOSER_FOLDER, "");

                alert.setBottom(field);
                alert.setCenter(text);
                alert.setPrefWidth(Main.SCREEN_WIDTH * 1 / 3 * Main.SCREEN_WIDTH / 1920);
                field.setPadding(new Insets(0 * Main.SCREEN_HEIGHT / 1080
                        , 20 * Main.SCREEN_WIDTH / 1920
                        , 20 * Main.SCREEN_HEIGHT / 1080
                        , 20 * Main.SCREEN_WIDTH / 1920));

                alert.getDialogPane().getButtonTypes().addAll(new ButtonType(Main.getString("ok"), ButtonBar.ButtonData.OK_DONE)
                        , new ButtonType(Main.getString("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE));
                Optional<ButtonType> result = alert.showAndWait();
                if (result != null && result.isPresent() && result.get().getButtonData().equals(ButtonBar.ButtonData.OK_DONE)) {
                    GENERAL_SETTINGS.setSettingValue(PredefinedSetting.GAMES_FOLDER, field.getTextField().getText());
                } else {
                    // ... user chose CANCEL or closed the dialog
                }
                GENERAL_SETTINGS.setSettingValue(PredefinedSetting.DISPLAY_WELCOME_MESSAGE, false);
                startGameLookerService();
            });
        }
    }

    @Override
    public Pane getWrappingPane() {
        return wrappingPane;
    }

    @Override
    void initAndAddWrappingPaneToRoot() {
        GaussianBlur blur = new GaussianBlur(BACKGROUND_IMAGE_BLUR);
        backgroundView.setEffect(blur);
        backgroundView.setOpacity(BACKGROUND_IMAGE_MAX_OPACITY);

        maskView.setOpacity(0);
        setChangeBackgroundNextTime(true);

        wrappingPane = new BorderPane();
        getRootStackPane().getChildren().add(wrappingPane);
        statusLabel = new Label();
        getRootStackPane().getChildren().add(statusLabel);
    }

    private void initCenter() {
        scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        //centerPane.setPrefViewportHeight(tilePane.getPrefHeight());
        scrollPane.setFocusTraversable(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        tilesPaneWrapper.setSpacing(5 * Main.SCREEN_HEIGHT / 1080);
        tilePane = new CoverTilePane(this, Main.getString("all_games"));
        tilePane.setId("mainTilePane");
        tilePane.setQuickSearchEnabled(true);

        lastPlayedTilePane = new RowCoverTilePane(this, RowCoverTilePane.TYPE_LAST_PLAYED);
        lastPlayedTilePane.setId("lastPlayedTilePane");
        lastPlayedTilePane.setDisplayGamesCount(false);
        recentlyAddedTilePane = new RowCoverTilePane(this, RowCoverTilePane.TYPE_RECENTLY_ADDED);
        recentlyAddedTilePane.setId("recentlyAddedTilePane");
        recentlyAddedTilePane.setDisplayGamesCount(false);
        toAddTilePane = new ToAddRowTilePane(this) {
            @Override
            protected void batchAddEntries(ArrayList<GameEntry> entries) {
                batchAddGameEntries(entries, 0).run();
            }
        };
        toAddTilePane.setId("toAddTilePane");

        lastPlayedTilePane.addOnFoldedChangeListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                Main.GENERAL_SETTINGS.setSettingValue(PredefinedSetting.FOLDED_ROW_LAST_PLAYED, newValue);
            }
        });
        recentlyAddedTilePane.addOnFoldedChangeListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                Main.GENERAL_SETTINGS.setSettingValue(PredefinedSetting.FOLDED_ROW_RECENTLY_ADDED, newValue);
            }
        });
        toAddTilePane.addOnFoldedChangeListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                Main.GENERAL_SETTINGS.setSettingValue(PredefinedSetting.FOLDED_TOADD_ROW, newValue);
            }
        });

        statusLabel.setText(Main.getString("loading") + "...");
        wrappingPane.setOpacity(0);

        try {
            remapArrowKeys(scrollPane);
        } catch (AWTException e) {
            e.printStackTrace();
        }
        GridPane topTilesPaneGridPane = new GridPane();
        ColumnConstraints halfConstraint = new ColumnConstraints();
        halfConstraint.setPercentWidth(50);
        //halfConstraint.maxWidthProperty().bind(lastPlayedTilePane.maxWidthProperty());
        lastPlayedTilePane.managedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    halfConstraint.setPercentWidth(50);
                } else {
                    halfConstraint.setPercentWidth(0);
                }
            }
        });
        lastPlayedTilePane.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {

            }
        });
        topTilesPaneGridPane.getColumnConstraints().add(halfConstraint);
        topTilesPaneGridPane.add(lastPlayedTilePane, 0, 0);
        topTilesPaneGridPane.add(recentlyAddedTilePane, 1, 0);
        topTilesPaneGridPane.setHgap(50 * Main.SCREEN_WIDTH / 1920);
        /*HBox topTilesPanes = new HBox();
        topTilesPanes.setAlignment(Pos.CENTER_LEFT);
        topTilesPanes.setSpacing(50*Main.SCREEN_WIDTH/1920);
        topTilesPanes.getChildren().addAll(lastPlayedTilePane,recentlyAddedTilePane);*/

        tilesPaneWrapper.getChildren().addAll(toAddTilePane, topTilesPaneGridPane, tilePane);
        scrollPane.setContent(tilesPaneWrapper);
        scrollPane.setStyle("-fx-background-color: transparent;");
        wrappingPane.setCenter(scrollPane);
        wrappingPane.setStyle("-fx-background-color: transparent;");

    }

    private void loadGames() {
        backgroundView.setVisible(false);
        maskView.setVisible(false);
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                tilePane.setAutomaticSort(false);
                recentlyAddedTilePane.setAutomaticSort(false);
                lastPlayedTilePane.setAutomaticSort(false);
                toAddTilePane.setAutomaticSort(false);

                int i = 0;
                for (GameEntry entry : AllGameEntries.ENTRIES_LIST) {
                    int finalI = i;

                    Main.runAndWait(new Runnable() {
                        @Override
                        public void run() {
                            setChangeBackgroundNextTime(true);
                            addGame(entry);
                        }
                    });
                    updateProgress(finalI, AllGameEntries.ENTRIES_LIST.size() - 1);
                    i++;
                }
                return null;
            }
        };
        task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                tilePane.setAutomaticSort(true);
                recentlyAddedTilePane.setAutomaticSort(true);
                lastPlayedTilePane.setAutomaticSort(true);
                backgroundView.setOpacity(0);
                backgroundView.setVisible(true);
                maskView.setOpacity(0);
                maskView.setVisible(true);
                setChangeBackgroundNextTime(false);

                //dialog.getDialogStage().close();
                statusLabel.setText("");
                fadeTransitionTo(MainScene.this, getParentStage(), false);
                Platform.runLater(() -> {
                    startGameLookerService();
                });
                home();

                double scrollBarVValue = GENERAL_SETTINGS.getDouble(PredefinedSetting.SCROLLBAR_VVALUE);
                scrollPane.setVvalue(scrollBarVValue);
            }
        });
        task.progressProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                Platform.runLater(() -> {
                    if (newValue.doubleValue() == 1.0) {
                        statusLabel.setText("");
                    } else {
                        statusLabel.setText(Main.getString("loading") + " " + Math.round(newValue.doubleValue() * 100) + "%...");
                    }
                });
            }
        });
        //dialog.activateProgressBar(task);

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    public void centerGameButtonInScrollPane(Node n, GamesTilePane pane) {
        //TODO fix here, input the right calculation to center gameButton
        double h = scrollPane.getContent().getBoundsInLocal().getHeight();
        double y = pane.getBoundsInParent().getMinY() + (n.getBoundsInParent().getMaxY() +
                n.getBoundsInParent().getMinY()) / 2.0;

        double v = scrollPane.getViewportBounds().getHeight();
        scrollPane.setVvalue(scrollPane.getVmax() * ((y - 0.5 * v) / (h - v)));
    }

    private void initTop() {
        sizeSlider.setMin(MIN_TILE_ZOOM);
        sizeSlider.setMax(MAX_TILE_ZOOM);
        sizeSlider.setBlockIncrement(0.1);
        sizeSlider.setFocusTraversable(false);

        sizeSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                tilePane.setPrefTileWidth(Main.SCREEN_WIDTH / 4 * newValue.doubleValue());
                tilePane.setPrefTileHeight(Main.SCREEN_WIDTH / 4 * COVER_HEIGHT_WIDTH_RATIO * newValue.doubleValue());

                lastPlayedTilePane.setPrefTileWidth(Main.SCREEN_WIDTH / 7 * newValue.doubleValue());
                lastPlayedTilePane.setPrefTileHeight(Main.SCREEN_WIDTH / 7 * COVER_HEIGHT_WIDTH_RATIO * newValue.doubleValue());


                recentlyAddedTilePane.setPrefTileWidth(Main.SCREEN_WIDTH / 7 * newValue.doubleValue());
                recentlyAddedTilePane.setPrefTileHeight(Main.SCREEN_WIDTH / 7 * COVER_HEIGHT_WIDTH_RATIO * newValue.doubleValue());

                toAddTilePane.setPrefTileWidth(Main.SCREEN_WIDTH / 7 * newValue.doubleValue());
                toAddTilePane.setPrefTileHeight(Main.SCREEN_WIDTH / 7 * COVER_HEIGHT_WIDTH_RATIO * newValue.doubleValue());
            }
        });
        sizeSlider.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                Main.GENERAL_SETTINGS.setSettingValue(PredefinedSetting.TILE_ZOOM, sizeSlider.getValue());
            }
        });
        sizeSlider.setOnMouseDragExited(new EventHandler<MouseDragEvent>() {
            @Override
            public void handle(MouseDragEvent event) {
                Main.GENERAL_SETTINGS.setSettingValue(PredefinedSetting.TILE_ZOOM, sizeSlider.getValue());
            }
        });
        sizeSlider.setPrefWidth(Main.SCREEN_WIDTH / 12);
        sizeSlider.setMaxWidth(Main.SCREEN_WIDTH / 12);
        sizeSlider.setPrefHeight(Main.SCREEN_WIDTH / 160);
        sizeSlider.setMaxHeight(Main.SCREEN_WIDTH / 160);

        //Image settingsImage = new Image("res/ui/settingsButton.png", SCREEN_WIDTH / 35, SCREEN_WIDTH / 35, true, true);
        //ImageButton settingsButton = new ImageButton(settingsImage);
        ImageButton settingsButton = new ImageButton("main-settings-button", SCREEN_WIDTH / 35.0, SCREEN_WIDTH / 35.0);
        settingsButton.setFocusTraversable(false);
        settingsButton.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.isPrimaryButtonDown()) {
                    SettingsScene settingsScene = new SettingsScene(new StackPane(), getParentStage(), MainScene.this);
                    fadeTransitionTo(settingsScene, getParentStage(), true);
                }
            }
        });

        //Image sortImage = new Image("res/ui/sortIcon.png", SCREEN_WIDTH / 35, SCREEN_WIDTH / 35, true, true);
        //ImageButton sortButton = new ImageButton(sortImage);

        ImageButton sortButton = new ImageButton("main-sort-button", SCREEN_WIDTH / 35, SCREEN_WIDTH / 35);
        sortButton.setFocusTraversable(false);
        final ContextMenu sortMenu = new ContextMenu();
        MenuItem sortByNameItem = new MenuItem(Main.getString("sort_by_name"));
        MenuItem sortByRatingItem = new MenuItem(Main.getString("sort_by_rating"));
        MenuItem sortByTimePlayedItem = new MenuItem(Main.getString("sort_by_playtime"));
        MenuItem sortByReleaseDateItem = new MenuItem(Main.getString("sort_by_release_date"));
        sortByNameItem.setOnAction(event -> {
            showTilesPaneAgainAfterCancelSearch = false;

            tilePane.sortByName();
            tilePane.setForcedHidden(false);
            lastPlayedTilePane.setForcedHidden(true);
            recentlyAddedTilePane.setForcedHidden(true);
            toAddTilePane.setForcedHidden(true);

            for (GroupRowTilePane groupPane : groupRowList) {
                groupPane.sortByName();
            }

            scrollPane.setVvalue(scrollPane.getVmin());
        });
        sortByRatingItem.setOnAction(event -> {
            showTilesPaneAgainAfterCancelSearch = false;

            tilePane.sortByRating();
            tilePane.setForcedHidden(false);
            lastPlayedTilePane.setForcedHidden(true);
            recentlyAddedTilePane.setForcedHidden(true);
            toAddTilePane.setForcedHidden(true);

            for (GroupRowTilePane groupPane : groupRowList) {
                groupPane.sortByRating();
            }

            scrollPane.setVvalue(scrollPane.getVmin());
        });
        sortByTimePlayedItem.setOnAction(event -> {
            showTilesPaneAgainAfterCancelSearch = false;

            tilePane.sortByTimePlayed();
            tilePane.setForcedHidden(false);
            lastPlayedTilePane.setForcedHidden(true);
            recentlyAddedTilePane.setForcedHidden(true);
            toAddTilePane.setForcedHidden(true);

            for (GroupRowTilePane groupPane : groupRowList) {
                groupPane.sortByTimePlayed();
            }

            scrollPane.setVvalue(scrollPane.getVmin());
        });
        sortByReleaseDateItem.setOnAction(event -> {
            showTilesPaneAgainAfterCancelSearch = false;

            tilePane.sortByReleaseDate();
            tilePane.setForcedHidden(false);
            lastPlayedTilePane.setForcedHidden(true);
            recentlyAddedTilePane.setForcedHidden(true);
            toAddTilePane.setForcedHidden(true);

            for (GroupRowTilePane groupPane : groupRowList) {
                groupPane.sortByReleaseDate();
            }

            scrollPane.setVvalue(scrollPane.getVmin());
        });
        sortMenu.getItems().addAll(sortByNameItem, sortByRatingItem, sortByTimePlayedItem, sortByReleaseDateItem);

        sortButton.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.isPrimaryButtonDown()) {
                    if (sortMenu.isShowing()) {
                        sortMenu.hide();
                    } else {
                        Bounds bounds = sortButton.getBoundsInLocal();
                        Bounds screenBounds = sortButton.localToScreen(bounds);
                        int x = (int) (screenBounds.getMinX() + 0.25 * bounds.getWidth());
                        int y = (int) (screenBounds.getMaxY() - 0.22 * bounds.getHeight());

                        sortMenu.show(sortButton, x, y);
                    }
                }
            }
        });


        //Image groupImage = new Image("res/ui/groupbyIcon.png", SCREEN_WIDTH / 35, SCREEN_WIDTH / 35, true, true);
        //ImageButton groupButton = new ImageButton(groupImage);
        ImageButton groupButton = new ImageButton("main-group-button", SCREEN_WIDTH / 35, SCREEN_WIDTH / 35);
        groupButton.setFocusTraversable(false);
        final ContextMenu groupMenu = new ContextMenu();
        MenuItem groupByAll = new MenuItem(Main.getString("group_by_all"));
        groupByAll.setOnAction(event -> {
            showTilesPaneAgainAfterCancelSearch = false;

            tilePane.setForcedHidden(false);
            lastPlayedTilePane.setForcedHidden(true);
            recentlyAddedTilePane.setForcedHidden(true);
            toAddTilePane.setForcedHidden(true);

            tilesPaneWrapper.getChildren().removeAll(groupRowList);
            groupRowList.clear();

            scrollPane.setVvalue(scrollPane.getVmin());
        });
        MenuItem groupByTheme = new MenuItem(Main.getString("group_by_theme"));
        groupByTheme.setOnAction(event -> {
            showTilesPaneAgainAfterCancelSearch = false;

            tilePane.setForcedHidden(true);
            lastPlayedTilePane.setForcedHidden(true);
            recentlyAddedTilePane.setForcedHidden(true);
            toAddTilePane.setForcedHidden(true);

            tilesPaneWrapper.getChildren().removeAll(groupRowList);
            groupRowList.clear();
            groupRowList = GroupsFactory.createGroupsByTheme(lastPlayedTilePane, this);
            tilesPaneWrapper.getChildren().addAll(groupRowList);

            scrollPane.setVvalue(scrollPane.getVmin());
        });
        MenuItem groupByGenre = new MenuItem(Main.getString("group_by_genre"));
        groupByGenre.setOnAction(event -> {
            showTilesPaneAgainAfterCancelSearch = false;

            tilePane.setForcedHidden(true);
            lastPlayedTilePane.setForcedHidden(true);
            recentlyAddedTilePane.setForcedHidden(true);
            toAddTilePane.setForcedHidden(true);

            tilesPaneWrapper.getChildren().removeAll(groupRowList);
            groupRowList.clear();
            groupRowList = GroupsFactory.createGroupsByGenre(lastPlayedTilePane, this);
            tilesPaneWrapper.getChildren().addAll(groupRowList);

            scrollPane.setVvalue(scrollPane.getVmin());
        });
        MenuItem groupBySerie = new MenuItem(Main.getString("group_by_serie"));
        groupBySerie.setOnAction(event -> {
            showTilesPaneAgainAfterCancelSearch = false;

            tilePane.setForcedHidden(true);
            lastPlayedTilePane.setForcedHidden(true);
            recentlyAddedTilePane.setForcedHidden(true);
            toAddTilePane.setForcedHidden(true);

            tilesPaneWrapper.getChildren().removeAll(groupRowList);
            groupRowList.clear();
            groupRowList = GroupsFactory.createGroupsBySerie(lastPlayedTilePane, this);
            tilesPaneWrapper.getChildren().addAll(groupRowList);

            scrollPane.setVvalue(scrollPane.getVmin());
        });

        MenuItem groupByLauncher = new MenuItem(Main.getString("group_by_launcher"));
        groupByLauncher.setOnAction(event -> {
            showTilesPaneAgainAfterCancelSearch = false;

            tilePane.setForcedHidden(true);
            lastPlayedTilePane.setForcedHidden(true);
            recentlyAddedTilePane.setForcedHidden(true);
            toAddTilePane.setForcedHidden(true);

            tilesPaneWrapper.getChildren().removeAll(groupRowList);
            groupRowList.clear();
            groupRowList = GroupsFactory.createGroupsByLaunchers(lastPlayedTilePane, this);
            tilesPaneWrapper.getChildren().addAll(groupRowList);

            scrollPane.setVvalue(scrollPane.getVmin());
        });

        groupMenu.getItems().addAll(groupByAll, groupByGenre, groupByTheme, groupBySerie, groupByLauncher);

        groupButton.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.isPrimaryButtonDown()) {
                    if (groupMenu.isShowing()) {
                        groupMenu.hide();
                    } else {
                        Bounds bounds = groupButton.getBoundsInLocal();
                        Bounds screenBounds = groupButton.localToScreen(bounds);
                        int x = (int) (screenBounds.getMinX() + 0.25 * bounds.getWidth());
                        int y = (int) (screenBounds.getMaxY() - 0.22 * bounds.getHeight());

                        groupMenu.show(groupButton, x, y);
                    }
                }
            }
        });


        //Image addImage = new Image("res/ui/addButton.png", SCREEN_WIDTH / 45, SCREEN_WIDTH / 45, true, true);
        //ImageButton addButton = new ImageButton(addImage);
        ImageButton addButton = new ImageButton("main-add-button", SCREEN_WIDTH / 45, SCREEN_WIDTH / 45);
        addButton.setFocusTraversable(false);

        addButton.setOnAction(event -> {
            getRootStackPane().setMouseTransparent(true);
            ChoiceDialog choiceDialog = new ChoiceDialog(
                    new ChoiceDialog.ChoiceDialogButton(Main.getString("Add_exe"), Main.getString("add_exe_long")),
                    new ChoiceDialog.ChoiceDialogButton(Main.getString("Add_folder"), Main.getString("add_symlink_long"))
            );
            choiceDialog.setTitle(Main.getString("add_a_game"));
            choiceDialog.setHeader(Main.getString("choose_action"));

            Optional<ButtonType> result = choiceDialog.showAndWait();
            result.ifPresent(letter -> {
                if (letter.getText().equals(Main.getString("Add_exe"))) {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle(Main.getString("select_program"));
                    fileChooser.setInitialDirectory(
                            new File(System.getProperty("user.home"))
                    );
                    //TODO fix internet shorcuts problem (bug submitted)
                    fileChooser.getExtensionFilters().addAll(
                            new FileChooser.ExtensionFilter("EXE", "*.exe"),
                            new FileChooser.ExtensionFilter("JAR", "*.jar")
                    );
                    try {
                        File selectedFile = fileChooser.showOpenDialog(getParentStage());
                        if (selectedFile != null) {
                            fadeTransitionTo(new GameEditScene(MainScene.this, selectedFile), getParentStage());
                        }
                    } catch (NullPointerException ne) {
                        ne.printStackTrace();
                        GameRoomAlert alert = new GameRoomAlert(Alert.AlertType.WARNING);
                        alert.setContentText(Main.getString("warning_internet_shortcut"));
                        alert.showAndWait();
                    }
                } else if (letter.getText().equals(Main.getString("Add_folder"))) {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle(Main.getString("Select_folder_ink"));
                    directoryChooser.setInitialDirectory(
                            new File(System.getProperty("user.home"))
                    );
                    File selectedFolder = directoryChooser.showDialog(getParentStage());
                    if (selectedFolder != null) {
                        ArrayList<File> files = new ArrayList<File>();
                        files.addAll(Arrays.asList(selectedFolder.listFiles()));
                        if (files.size() != 0) {
                            batchAddFolderEntries(files, 0).run();
                            //startMultiAddScenes(files);
                        }
                    }
                }
            });
            getRootStackPane().setMouseTransparent(false);
        });

        ImageButton scanButton = new ImageButton("tile-loading-button", SCREEN_WIDTH / 38.0, SCREEN_WIDTH / 38.0);
        scanButton.setFocusTraversable(false);
        scanButton.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.isPrimaryButtonDown()) {
                    GameWatcher.getInstance().start();
                }
            }
        });
        GameWatcher.getInstance().addOnSearchStartedListener(() -> scanButton.setDisable(true));
        GameWatcher.getInstance().addOnSearchDoneListener(() -> scanButton.setDisable(false));

        HBox hbox = new HBox();
        hbox.setPadding(new Insets(15, 12, 15, 10));
        hbox.setSpacing(0);
        hbox.getChildren().addAll(addButton,scanButton, sortButton, groupButton, settingsButton, sizeSlider);
        hbox.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();

        double imgSize = SCREEN_WIDTH / 28;
        //Image searchImage = new Image("res/ui/searchButton.png", SCREEN_WIDTH / 28, SCREEN_WIDTH / 28, true, true);
        ImageButton searchButton = new ImageButton("search-button", imgSize, imgSize);
        searchButton.setFocusTraversable(false);

        HBox searchBox = new HBox();
        searchBox.setAlignment(Pos.CENTER_RIGHT);
        searchBox.getChildren().addAll(searchField, searchButton);
        searchField.setFocusTraversable(false);
        searchField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (newValue != null && !newValue.equals("")) {
                    searchGame(newValue);
                } else if (newValue != null && newValue.equals("")) {
                    cancelSearch();
                }
            }
        });

        //hbox.getChildren().add(searchBox);

        //HBox.setMargin(sizeSlider, new Insets(15, 12, 15, 12));
        topPane = new StackPane();
        //topPane.setFocusTraversable(false);
        double width = 500 * SCREEN_WIDTH / 1920;
        double height = 94 * SCREEN_HEIGHT / 1080;
        //Image logoImage = new Image("res/ui/title-medium.png", , , true, true);
        ImageButton homeButton = new ImageButton("home-button", width, height);
        homeButton.setFocusTraversable(false);
        homeButton.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                home();
            }
        });
        StackPane.setAlignment(homeButton, Pos.BOTTOM_CENTER);
        topPane.getChildren().add(homeButton);
        /*StackPane.setMargin(titleView, new Insets(55 * Main.SCREEN_HEIGHT / 1080
                , 12 * Main.SCREEN_WIDTH / 1920
                , 15 * Main.SCREEN_HEIGHT / 1080
                , 15 * Main.SCREEN_WIDTH / 1920));*/
        topPane.getChildren().add(searchBox);
        topPane.getChildren().add(hbox);
        topPane.getStyleClass().add("header");

        StackPane.setAlignment(hbox, Pos.CENTER_LEFT);


        hbox.setPickOnBounds(false);
        searchBox.setPickOnBounds(false);
        homeButton.setPickOnBounds(false);
        wrappingPane.setTop(topPane);
    }

    public void toggleTopBar(){
        if(topPane!=null) {
            topPane.setVisible(!topPane.isVisible());
            topPane.setManaged(!topPane.isManaged());
        }
    }

    private void refreshTrayMenu() {
        Main.START_TRAY_MENU.removeAll();

        ArrayList<java.awt.MenuItem> newItems = new ArrayList<>();
        for (GameEntry entry : AllGameEntries.ENTRIES_LIST) {
            java.awt.MenuItem gameItem = new java.awt.MenuItem(entry.getName());
            gameItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    entry.startGame();
                }
            });
            newItems.add(gameItem);
        }
        newItems.sort(new Comparator<java.awt.MenuItem>() {
            @Override
            public int compare(java.awt.MenuItem o1, java.awt.MenuItem o2) {
                return o1.getLabel().compareTo(o2.getLabel());
            }
        });
        for (java.awt.MenuItem item : newItems) {
            Main.START_TRAY_MENU.add(item);
        }
        Main.START_TRAY_MENU.setEnabled(true);

    }

    private void home() {
        tilePane.sortByName();
        tilePane.setForcedHidden(false);
        tilePane.show();
        if (tilePane.isSearching()) {
            searchField.clear();
        }
        lastPlayedTilePane.setForcedHidden(false);
        recentlyAddedTilePane.setForcedHidden(false);
        toAddTilePane.setForcedHidden(false);

        tilesPaneWrapper.getChildren().removeAll(groupRowList);
        groupRowList.clear();

        scrollPane.setVvalue(scrollPane.getVmin());
    }

    private void cancelSearch() {
        if (showTilesPaneAgainAfterCancelSearch) {
            lastPlayedTilePane.setForcedHidden(false);
            recentlyAddedTilePane.setForcedHidden(false);
            toAddTilePane.setForcedHidden(false);
        }
        tilePane.setTitle(Main.getString("all_games"));
        tilePane.cancelSearchText();
        if (groupRowList.size() > 0) {
            for (GroupRowTilePane tilePane : groupRowList) {
                tilePane.show();
                tilePane.cancelSearchText();
            }
            tilePane.hide();
        }
    }

    private void searchGame(String text) {
        tilePane.show();
        if (!tilePane.isSearching()) {
            showTilesPaneAgainAfterCancelSearch = lastPlayedTilePane.isManaged();
        }
        for (GroupRowTilePane tilePane : groupRowList) {
            tilePane.hide();
            tilePane.searchText(text);
        }
        lastPlayedTilePane.setForcedHidden(true);
        recentlyAddedTilePane.setForcedHidden(true);
        toAddTilePane.setForcedHidden(true);
        int found = tilePane.searchText(text);
        tilePane.setTitle(found + " " + Main.getString("results_found_for") + " \"" + text + "\"");
    }

    public void removeGame(GameEntry entry) {
        tilePane.removeGame(entry);
        lastPlayedTilePane.removeGame(entry);
        recentlyAddedTilePane.removeGame(entry);
        toAddTilePane.removeGame(entry);
        if (gameWatcher != null) {
            gameWatcher.removeGame(entry);
        }
        for (GroupRowTilePane tilePane : groupRowList) {
            tilePane.removeGame(entry);
        }

        AllGameEntries.removeGame(entry);
        refreshTrayMenu();
    }

    public void updateGame(GameEntry entry) {
        tilePane.updateGame(entry);
        lastPlayedTilePane.updateGame(entry);
        recentlyAddedTilePane.updateGame(entry);
        toAddTilePane.updateGame(entry);
        for (GroupRowTilePane tilePane : groupRowList) {
            tilePane.updateGame(entry);
        }
        AllGameEntries.updateGame(entry);
        refreshTrayMenu();
    }

    public void addGame(GameEntry entry) {
        tilePane.addGame(entry);
        lastPlayedTilePane.addGame(entry);
        recentlyAddedTilePane.addGame(entry);
        toAddTilePane.removeGame(entry);
        if (gameWatcher != null) {
            gameWatcher.removeGame(entry);
        }
        for (GroupRowTilePane tilePane : groupRowList) {
            tilePane.addGame(entry);
        }
        AllGameEntries.addGame(entry);
        refreshTrayMenu();
    }

    private ExitAction batchAddGameEntries(ArrayList<GameEntry> entries, int entriesCount) {
        if (entriesCount < entries.size()) {
            GameEntry currentEntry = entries.get(entriesCount);
            GameEditScene gameEditScene = new GameEditScene(MainScene.this, currentEntry, GameEditScene.MODE_ADD, null);
            gameEditScene.disableBackButton();
            return new MultiAddExitAction(new Runnable() {
                @Override
                public void run() {
                    ExitAction action = batchAddGameEntries(entries, entriesCount + 1);
                    gameEditScene.setOnExitAction(action); //create interface runnable to access property GameEditScene
                    gameEditScene.addCancelButton(action);
                    gameEditScene.addCancelAllButton();
                    fadeTransitionTo(gameEditScene, getParentStage());
                }
            }, gameEditScene);
        } else {
            return new ClassicExitAction(this, getParentStage(), MAIN_SCENE);
        }
    }

    private ExitAction batchAddFolderEntries(ArrayList<File> files, int fileCount) {
        if (fileCount < files.size()) {
            File currentFile = files.get(fileCount);
            if (FolderGameScanner.isPotentiallyAGame(currentFile)) {
                GameEditScene gameEditScene = new GameEditScene(MainScene.this, currentFile);
                gameEditScene.disableBackButton();
                return new MultiAddExitAction(new Runnable() {
                    @Override
                    public void run() {
                        ExitAction action = batchAddFolderEntries(files, fileCount + 1);
                        gameEditScene.setOnExitAction(action); //create interface runnable to access property GameEditScene
                        gameEditScene.addCancelButton(action);
                        gameEditScene.addCancelAllButton();
                        fadeTransitionTo(gameEditScene, getParentStage());
                    }
                }, gameEditScene);
            }
            return batchAddFolderEntries(files, fileCount + 1);
        } else {
            return new ClassicExitAction(this, getParentStage(), MAIN_SCENE);
        }
    }

    private ExitAction createSteamEntryAddExitAction(ArrayList<GameEntry> entries, int entryCount) {
        if (entryCount < entries.size()) {
            GameEntry currentEntry = entries.get(entryCount);
            GameEditScene gameEditScene = new GameEditScene(MainScene.this, currentEntry, GameEditScene.MODE_ADD, null);
            gameEditScene.disableBackButton();
            return new MultiAddExitAction(new Runnable() {
                @Override
                public void run() {
                    ExitAction action = createSteamEntryAddExitAction(entries, entryCount + 1);
                    gameEditScene.setOnExitAction(action); //create interface runnable to access property GameEditScene
                    gameEditScene.addCancelButton(action);
                    gameEditScene.addCancelAllButton();
                    fadeTransitionTo(gameEditScene, getParentStage());
                }
            }, gameEditScene);
        } else {
            return new ClassicExitAction(this, getParentStage(), MAIN_SCENE);
        }
    }

    private void startGameLookerService() {
        if (GENERAL_SETTINGS.getBoolean(PredefinedSetting.DISPLAY_WELCOME_MESSAGE)){
            return;
        }
        //toAddTilePane.disableFoldButton(true);
        toAddTilePane.setAutomaticSort(false);

        gameWatcher = GameWatcher.getInstance();
        gameWatcher.addOnSearchStartedListener(() -> {
            toAddTilePane.enableSearchingIcon(true);
            GeneralToast.displayToast(Main.getString("search_started"),getParentStage(),GeneralToast.DURATION_SHORT);
        });
        gameWatcher.addOnSearchDoneListener(() -> {
            toAddTilePane.enableSearchingIcon(false);
            GeneralToast.displayToast(Main.getString("search_done"),getParentStage(),GeneralToast.DURATION_SHORT);
        });
        toAddTilePane.getIconButton().setOnAction(event -> gameWatcher.start());
        gameWatcher.setOnGameFoundHandler(new OnGameFoundHandler() {
            @Override
            public GameButton gameToAddFound(GameEntry entry) {
                toAddTilePane.addGame(entry);
                return toAddTilePane.getGameButton(entry);
            }

            @Override
            public void onAllGamesFound() {
                //toAddTilePane.disableFoldButton(false);
                toAddTilePane.show();
                Platform.runLater(() -> {
                    toAddTilePane.unfold();
                });
            }
        });
        gameWatcher.start();
    }


    public int getInputMode() {
        return input_mode;
    }

    public void setInputMode(int input_mode) {
        this.input_mode = input_mode;
        switch (input_mode) {
            case INPUT_MODE_KEYBOARD:
                setCursor(Cursor.NONE);
                wrappingPane.setMouseTransparent(true);
                break;

            default:
            case INPUT_MODE_MOUSE:
                setCursor(Cursor.DEFAULT);
                wrappingPane.setMouseTransparent(false);
                break;
        }
    }

    public void setChangeBackgroundNextTime(boolean changeBackgroundNextTime) {
        this.changeBackgroundNextTime = changeBackgroundNextTime;
    }

    private void remapArrowKeys(ScrollPane scrollPane) throws AWTException {
        java.util.List<KeyEvent> mappedEvents = new ArrayList<>();
        scrollPane.addEventFilter(KeyEvent.ANY, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (mappedEvents.remove(event))
                    return;
                if (!event.isShiftDown()) {
                    switch (event.getCode()) {
                        case UP:
                        case DOWN:
                        case LEFT:
                        case RIGHT:
                        case ENTER:
                            setInputMode(INPUT_MODE_KEYBOARD);

                            KeyEvent newEvent = remap(event);
                            mappedEvents.add(newEvent);
                            event.consume();
                            javafx.event.Event.fireEvent(event.getTarget(), newEvent);
                            break;
                        default:
                            /*if(event.getEventType().equals(KeyEvent.KEY_PRESSED)) {
                                tilePane.getOnKeyTyped().handle(event);
                            }*/
                            break;
                    }
                }
            }

            private KeyEvent remap(KeyEvent event) {
                KeyEvent newEvent = new KeyEvent(
                        event.getEventType(),
                        event.getCharacter(),
                        event.getText(),
                        event.getCode(),
                        !event.isShiftDown(),
                        event.isControlDown(),
                        event.isAltDown(),
                        event.isMetaDown()
                );

                return newEvent.copyFor(event.getSource(), event.getTarget());
            }
        });
    }

    public void triggerKeyPressedOnMainPane(KeyEvent keyPressed) {
        tilePane.getOnKeyPressed().handle(keyPressed);
    }

    public void setImageBackground(Image img) {
        if (!GENERAL_SETTINGS.getBoolean(PredefinedSetting.DISABLE_MAINSCENE_WALLPAPER)) {
            if (!backgroundView.isVisible()) {
                backgroundView.setVisible(true);
            }
            if (!maskView.isVisible()) {
                maskView.setVisible(true);
            }
            if (!changeBackgroundNextTime) {
                if (img != null) {
                    if (backgroundView.getImage() == null || !backgroundView.getImage().equals(img)) {
                        ImageUtils.transitionToWindowBackground(img, backgroundView);
                        if (maskView.getOpacity() != 1) {
                            Timeline fadeInTimeline = new Timeline(
                                    new KeyFrame(Duration.seconds(0),
                                            new KeyValue(maskView.opacityProperty(), maskView.opacityProperty().getValue(), Interpolator.EASE_IN)),
                                    new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                                            new KeyValue(maskView.opacityProperty(), 1, Interpolator.EASE_OUT)
                                    ));
                            fadeInTimeline.setCycleCount(1);
                            fadeInTimeline.setAutoReverse(false);
                            fadeInTimeline.play();
                        }
                    }
                } else {
                    Timeline fadeOutTimeline = new Timeline(
                            new KeyFrame(Duration.seconds(0),
                                    new KeyValue(backgroundView.opacityProperty(), backgroundView.opacityProperty().getValue(), Interpolator.EASE_IN),
                                    new KeyValue(maskView.opacityProperty(), maskView.opacityProperty().getValue(), Interpolator.EASE_IN)),
                            new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                                    new KeyValue(backgroundView.opacityProperty(), 0, Interpolator.EASE_OUT),
                                    new KeyValue(maskView.opacityProperty(), 0, Interpolator.EASE_OUT)
                            ));
                    fadeOutTimeline.setCycleCount(1);
                    fadeOutTimeline.setAutoReverse(false);
                    fadeOutTimeline.play();
                }
            } else {
                changeBackgroundNextTime = false;
            }
        } else {
            if (backgroundView.isVisible()) {
                backgroundView.setVisible(false);
            }
            if (maskView.isVisible()) {
                maskView.setVisible(false);
            }
        }
    }
}

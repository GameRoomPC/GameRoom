package ui.scene;

import data.game.*;
import data.game.entry.GameEntry;
import data.game.scrapper.SteamOnlineScrapper;
import data.game.scrapper.SteamPreEntry;
import data.game.scrapper.SteamLocalScrapper;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.geometry.Bounds;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import system.application.settings.PredefinedSetting;
import system.os.WindowsShortcut;
import ui.control.button.ImageButton;
import ui.dialog.ChoiceDialog;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.*;
import ui.Main;
import ui.dialog.GameRoomAlert;
import ui.dialog.SteamIgnoredSelector;
import ui.pane.gamestilepane.CoverTilePane;
import ui.pane.gamestilepane.GamesTilePane;
import ui.scene.exitaction.ClassicExitAction;
import ui.scene.exitaction.ExitAction;
import ui.scene.exitaction.MultiAddExitAction;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static ui.Main.*;
import static ui.control.button.gamebutton.GameButton.COVER_HEIGHT_WIDTH_RATIO;

/**
 * Created by LM on 03/07/2016.
 */
public class MainScene extends BaseScene {
    private int input_mode = 0;

    public final static int INPUT_MODE_MOUSE = 0;
    public final static int INPUT_MODE_KEYBOARD = 1;

    public final static double MAX_TILE_ZOOM = 0.675;
    public final static double MIN_TILE_ZOOM = 0.25;

    private BorderPane wrappingPane;
    private GamesTilePane tilePane;

    private Label statusLabel;

    private final static int SORT_MODE_NAME = 0;
    private final static int SORT_MODE_RATING = 1;
    private final static int SORT_MODE_PLAY_TIME = 2;

    private static int SORT_MODE = SORT_MODE_NAME;

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
        initCenter();
        initTop();
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
        tilePane = new CoverTilePane(this);

        statusLabel.setText(RESSOURCE_BUNDLE.getString("loading") + "...");
        wrappingPane.setOpacity(0);
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        ArrayList<UUID> uuids = AllGameEntries.readUUIDS();
                        int i = 0;
                        for (UUID uuid : uuids) {
                            statusLabel.setText(RESSOURCE_BUNDLE.getString("loading") + " " + (i + 1) + "/" + uuids.size() + "...");
                            updateProgress(i, uuids.size() - 1);
                            addGame(new GameEntry(uuid));
                            i++;
                        }
                    }
                });
                return null;
            }
        };
        task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                //dialog.getDialogStage().close();
                statusLabel.setText("");
                setChangeBackgroundNextTime(false);
                fadeTransitionTo(MainScene.this, getParentStage(),false);
                Platform.runLater(() -> {
                    checkSteamGamesInstalled();
                });
            }
        });
        //dialog.activateProgressBar(task);

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();

        ScrollPane centerPane = new ScrollPane();
        try {
            remapArrowKeys(tilePane);
        } catch (AWTException e) {
            e.printStackTrace();
        }
        centerPane.setFitToWidth(true);
        centerPane.setFitToHeight(true);
        //centerPane.setPrefViewportHeight(tilePane.getPrefHeight());
        centerPane.setFocusTraversable(false);
        centerPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        centerPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        //StackPane.setMargin(tilePane, new Insets(50* SCREEN_HEIGHT /1080, 25* SCREEN_WIDTH /1920, 60* SCREEN_HEIGHT /1080, 25* SCREEN_WIDTH /1920));
        //tilePane.setPadding(new Insets(50 * SCREEN_HEIGHT / 1080, 30 * SCREEN_WIDTH / 1920, 30 * SCREEN_HEIGHT / 1080, 30 * SCREEN_WIDTH / 1920));

        centerPane.setContent(tilePane);
        //centerPane.getStylesheets().add("res/flatterfx.css");

        wrappingPane.setCenter(centerPane);

    }

    private void initTop() {
        Slider sizeSlider = new Slider();
        sizeSlider.setMin(MIN_TILE_ZOOM);
        sizeSlider.setMax(MAX_TILE_ZOOM);
        sizeSlider.setBlockIncrement(0.1);
        sizeSlider.setFocusTraversable(false);

        sizeSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                tilePane.setPrefTileWidth(Main.SCREEN_WIDTH / 4 * newValue.doubleValue());
                tilePane.setPrefTileHeight(Main.SCREEN_WIDTH / 4 * COVER_HEIGHT_WIDTH_RATIO * newValue.doubleValue());
            }
        });
        sizeSlider.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                Main.GENERAL_SETTINGS.setSettingValue(PredefinedSetting.TILE_ZOOM, sizeSlider.getValue());
            }
        });
        sizeSlider.setValue(Main.GENERAL_SETTINGS.getDouble(PredefinedSetting.TILE_ZOOM));

        sizeSlider.setPrefWidth(Main.SCREEN_WIDTH / 8);
        sizeSlider.setMaxWidth(Main.SCREEN_WIDTH / 8);
        sizeSlider.setPrefHeight(Main.SCREEN_WIDTH / 160);
        sizeSlider.setMaxHeight(Main.SCREEN_WIDTH / 160);

        /*sizeSlider.setScaleX(0.7);
        sizeSlider.setScaleY(0.7);*/

        Image settingsImage = new Image("res/ui/settingsButton.png", SCREEN_WIDTH / 35, SCREEN_WIDTH / 35, true, true);
        ImageButton settingsButton = new ImageButton(settingsImage);
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
        /*settingsButton.setScaleX(0.7);
        settingsButton.setScaleY(0.7);*/
        Image sortImage = new Image("res/ui/sortIcon.png", SCREEN_WIDTH / 35, SCREEN_WIDTH / 35, true, true);
        ImageButton sortButton = new ImageButton(sortImage);
        sortButton.setFocusTraversable(false);
        final ContextMenu sortMenu = new ContextMenu();
        MenuItem byNameItem = new MenuItem(Main.RESSOURCE_BUNDLE.getString("sort_by_name"));
        MenuItem byRatingItem = new MenuItem(Main.RESSOURCE_BUNDLE.getString("sort_by_rating"));
        MenuItem byTimePlayedItem = new MenuItem(Main.RESSOURCE_BUNDLE.getString("sort_by_playtime"));
        MenuItem byReleaseDateItem = new MenuItem(Main.RESSOURCE_BUNDLE.getString("sort_by_release_date"));
        sortMenu.getItems().addAll(byNameItem, byRatingItem, byTimePlayedItem,byReleaseDateItem);
        byNameItem.setOnAction(event -> tilePane.sortByName());
        byRatingItem.setOnAction(event -> tilePane.sortByRating());
        byTimePlayedItem.setOnAction(event -> tilePane.sortByTimePlayed());
        byReleaseDateItem.setOnAction(event -> tilePane.sortByReleaseDate());

        sortButton.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.isPrimaryButtonDown()) {
                    if(sortMenu.isShowing()){
                        sortMenu.hide();
                    }else{
                        Bounds bounds = sortButton.getBoundsInLocal();
                        Bounds screenBounds = sortButton.localToScreen(bounds);
                        int x = (int) (screenBounds.getMinX() + 0.25 *bounds.getWidth());
                        int y = (int) (screenBounds.getMaxY() - 0.22 *bounds.getHeight());

                        sortMenu.show(sortButton,x,y);
                    }
                }
            }
        });

        Image addImage = new Image("res/ui/addButton.png", SCREEN_WIDTH / 45, SCREEN_WIDTH / 45, true, true);
        ImageButton addButton = new ImageButton(addImage);
        addButton.setFocusTraversable(false);

        addButton.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.isPrimaryButtonDown()) {
                    getRootStackPane().setMouseTransparent(true);
                    ChoiceDialog choiceDialog = new ChoiceDialog(
                            new ChoiceDialog.ChoiceDialogButton(RESSOURCE_BUNDLE.getString("Add_exe"), RESSOURCE_BUNDLE.getString("add_exe_long")),
                            new ChoiceDialog.ChoiceDialogButton(RESSOURCE_BUNDLE.getString("Add_folder"), RESSOURCE_BUNDLE.getString("add_symlink_long"))
                    );
                    choiceDialog.setTitle(RESSOURCE_BUNDLE.getString("add_a_game"));
                    choiceDialog.setHeader(RESSOURCE_BUNDLE.getString("choose_action"));

                    Optional<ButtonType> result = choiceDialog.showAndWait();
                    result.ifPresent(letter -> {
                        if (letter.getText().equals(RESSOURCE_BUNDLE.getString("Add_exe"))) {
                            FileChooser fileChooser = new FileChooser();
                            fileChooser.setTitle(RESSOURCE_BUNDLE.getString("select_program"));
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
                                alert.setContentText(RESSOURCE_BUNDLE.getString("warning_internet_shortcut"));
                                alert.showAndWait();
                            }
                        } else if (letter.getText().equals(RESSOURCE_BUNDLE.getString("Add_folder"))) {
                            DirectoryChooser directoryChooser = new DirectoryChooser();
                            directoryChooser.setTitle(RESSOURCE_BUNDLE.getString("Select_folder_ink"));
                            directoryChooser.setInitialDirectory(
                                    new File(System.getProperty("user.home"))
                            );
                            File selectedFolder = directoryChooser.showDialog(getParentStage());
                            if (selectedFolder != null) {
                                ArrayList<File> files = new ArrayList<File>();
                                files.addAll(Arrays.asList(selectedFolder.listFiles()));
                                if (files.size() != 0) {
                                    createFolderAddExitAction(files, 0).run();
                                    //startMultiAddScenes(files);
                                }
                            }
                        }
                    });
                    getRootStackPane().setMouseTransparent(false);
                }
            }
        });

        HBox hbox = new HBox();
        hbox.setPadding(new Insets(15, 12, 15, 10));
        hbox.setSpacing(0);
        hbox.getChildren().addAll(addButton, sortButton, settingsButton, sizeSlider);
        hbox.setAlignment(Pos.CENTER_LEFT);

        //HBox.setMargin(sizeSlider, new Insets(15, 12, 15, 12));
        StackPane topPane = new StackPane();
        topPane.setFocusTraversable(false);
        ImageView titleView = new ImageView(new Image("res/ui/title-medium.png", 500 * SCREEN_WIDTH / 1920, 94 * SCREEN_HEIGHT / 1080, true, true));
        titleView.setMouseTransparent(true);
        titleView.setFocusTraversable(false);
        StackPane.setAlignment(titleView, Pos.BOTTOM_CENTER);
        topPane.getChildren().add(titleView);
        /*StackPane.setMargin(titleView, new Insets(55 * Main.SCREEN_HEIGHT / 1080
                , 12 * Main.SCREEN_WIDTH / 1920
                , 15 * Main.SCREEN_HEIGHT / 1080
                , 15 * Main.SCREEN_WIDTH / 1920));*/
        topPane.getChildren().add(hbox);
        StackPane.setAlignment(hbox, Pos.CENTER_LEFT);

        wrappingPane.setTop(topPane);
    }

    private void refreshTrayMenu() {
        Main.START_TRAY_MENU.removeAll();

        for (GameEntry entry : AllGameEntries.ENTRIES_LIST) {
            java.awt.MenuItem gameItem = new java.awt.MenuItem(entry.getName());
            gameItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    entry.startGame();
                }
            });
            Main.START_TRAY_MENU.add(gameItem);
        }
    }

    public void removeGame(GameEntry entry) {
        tilePane.removeGame(entry);
        AllGameEntries.removeGame(entry);
        refreshTrayMenu();
    }

    public void updateGame(GameEntry entry) {
        tilePane.updateGame(entry);
        AllGameEntries.updateGame(entry);
        refreshTrayMenu();
    }

    public void addGame(GameEntry entry) {
        tilePane.addGame(entry);
        AllGameEntries.addGame(entry);
        refreshTrayMenu();
    }

    private ExitAction createFolderAddExitAction(ArrayList<File> files, int fileCount) {
        if (fileCount < files.size()) {
            File currentFile = files.get(fileCount);
            try {
                WindowsShortcut shortcut = new WindowsShortcut(currentFile);
                currentFile = new File(shortcut.getRealFilename());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            GameEditScene gameEditScene = new GameEditScene(MainScene.this, currentFile);
            gameEditScene.disableBackButton();
            return new MultiAddExitAction(new Runnable() {
                @Override
                public void run() {
                    ExitAction action = createFolderAddExitAction(files, fileCount + 1);
                    gameEditScene.setOnExitAction(action); //create interface runnable to access property GameEditScene
                    gameEditScene.addCancelButton(action);
                    fadeTransitionTo(gameEditScene, getParentStage());
                }
            }, gameEditScene);
        } else {
            return new ClassicExitAction(this, getParentStage(), MAIN_SCENE);
        }
    }

    private ExitAction createSteamEntryAddExitAction(ArrayList<GameEntry> entries, int entryCount) {
        if (entryCount < entries.size()) {
            GameEntry currentEntry = entries.get(entryCount);
            GameEditScene gameEditScene = new GameEditScene(MainScene.this, currentEntry, GameEditScene.MODE_ADD,null);
            gameEditScene.disableBackButton();
            return new MultiAddExitAction(new Runnable() {
                @Override
                public void run() {
                    ExitAction action = createSteamEntryAddExitAction(entries, entryCount + 1);
                    gameEditScene.setOnExitAction(action); //create interface runnable to access property GameEditScene
                    gameEditScene.addCancelButton(action);
                    fadeTransitionTo(gameEditScene, getParentStage());
                }
            }, gameEditScene);
        } else {
            return new ClassicExitAction(this, getParentStage(), MAIN_SCENE);
        }
    }

    private void checkSteamGamesInstalled() {
        try {
            ArrayList<SteamPreEntry> steamEntries = SteamLocalScrapper.getSteamAppsInstalled();
            ArrayList<GameEntry> steamEntriesToAdd = new ArrayList<GameEntry>();
            for (SteamPreEntry steamEntry : steamEntries) {
                boolean doNotAdd = false;
                for (GameEntry entry : AllGameEntries.ENTRIES_LIST) {
                    doNotAdd = steamEntry.getId() == entry.getSteam_id();
                    if (doNotAdd) {
                        break;
                    }
                }
                if(!doNotAdd) {
                    SteamPreEntry[] ignoredSteamApps = GENERAL_SETTINGS.getSteamAppsIgnored();
                    for (SteamPreEntry ignoredEntry : ignoredSteamApps) {
                        doNotAdd = steamEntry.getId() == ignoredEntry.getId();
                        if (doNotAdd) {
                            break;
                        }
                    }
                }
                if (!doNotAdd) {
                    Main.LOGGER.debug("To add : "+steamEntry.getName());
                    GameEntry toAdd = SteamOnlineScrapper.getEntryForSteamId(steamEntry.getId());
                    toAdd.setSteam_id(steamEntry.getId());
                    steamEntriesToAdd.add(toAdd);
                }
            }
            Main.LOGGER.info(steamEntriesToAdd.size() + " steam games to add");
            if (steamEntriesToAdd.size() != 0) {
                GameRoomAlert alert = new GameRoomAlert(Alert.AlertType.CONFIRMATION, steamEntriesToAdd.size()+" "+Main.RESSOURCE_BUNDLE.getString("steam_games_to_add_detected"));
                alert.getButtonTypes().add(new ButtonType(RESSOURCE_BUNDLE.getString("ignore")+"...", ButtonBar.ButtonData.LEFT));
                Optional<ButtonType> result = alert.showAndWait();
                result.ifPresent(buttonType -> {
                    if(buttonType.getButtonData().equals(ButtonBar.ButtonData.OK_DONE)){
                        createSteamEntryAddExitAction(steamEntriesToAdd, 0).run();
                    }else if(buttonType.getButtonData().equals(ButtonBar.ButtonData.LEFT)){
                        try {
                            SteamIgnoredSelector selector = new SteamIgnoredSelector();
                            Optional<ButtonType> ignoredOptionnal = selector.showAndWait();
                            ignoredOptionnal.ifPresent(pairs -> {
                                if(pairs.getButtonData().equals(ButtonBar.ButtonData.OK_DONE)) {
                                    GENERAL_SETTINGS.setSettingValue(PredefinedSetting.IGNORED_STEAM_APPS,selector.getSelectedEntries());
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
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
                if(!event.isShiftDown()) {
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
    public void setImageBackground(Image img) {
        if (!GENERAL_SETTINGS.getBoolean(PredefinedSetting.DISABLE_MAINSCENE_WALLPAPER)) {
            if (!changeBackgroundNextTime) {
                if (img != null) {
                    if (backgroundView.getImage() == null || !backgroundView.getImage().equals(img)) {
                        //TODO fix the blinking issue where an identical image is being set twice. The above compareason does not work.
                        ImageUtils.transitionToImage(img, backgroundView, BaseScene.BACKGROUND_IMAGE_MAX_OPACITY);
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
            if (backgroundView.getOpacity() != 0) {
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
        }
    }
}

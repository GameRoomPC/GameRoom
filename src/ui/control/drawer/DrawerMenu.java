package ui.control.drawer;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import ui.Main;
import ui.control.specific.ScanButton;
import ui.dialog.ChoiceDialog;
import ui.dialog.GameRoomAlert;
import ui.scene.GameEditScene;
import ui.scene.MainScene;
import ui.scene.SettingsScene;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

import static ui.Main.GENERAL_SETTINGS;
import static ui.Main.LOGGER;

/**
 * Created by LM on 09/02/2017.
 */
public class DrawerMenu extends BorderPane {
    public static double ANIMATION_TIME = 0.04;
    public static final double WIDTH_RATIO = 0.025;

    private Timeline openAnim;
    private Timeline closeAnim;
    private AnchorPane topMenuPane = new AnchorPane();
    private VBox topButtonsBox = new VBox();
    private VBox bottomButtonsBox = new VBox();

    private SubMenu currentSubMenu;

    private HashMap<String, SubMenu> subMenus = new HashMap<>();

    public DrawerMenu(MainScene mainScene) {
        super();
        setFocusTraversable(false);
        setMaxWidth(GENERAL_SETTINGS.getWindowWidth() * WIDTH_RATIO);
        setPrefWidth(GENERAL_SETTINGS.getWindowWidth() * WIDTH_RATIO);
        setFocusTraversable(false);
        //setEffect(new InnerShadow());
        setId("menu-bar");

        setOnMouseEntered(event -> {
            if (translateXProperty().getValue() != 0) {
                open(mainScene);
            }
        });

        setOnMouseExited(event -> {
            if (event.getX() > getWidth()) {
                close(mainScene);
            }
        });
        setCache(true);
        init(mainScene);
    }

    /**
     * Opens the menu drawer
     *
     * @param mainScene the mainscene containing this drawer
     */
    public void open(MainScene mainScene) {
        setManaged(true);
        if (closeAnim != null) {
            closeAnim.stop();
        }
        openAnim = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(translateXProperty(), translateXProperty().getValue(), Interpolator.LINEAR),
                        new KeyValue(mainScene.getBackgroundView().translateXProperty(), mainScene.getBackgroundView().getTranslateX(), Interpolator.LINEAR)),
                new KeyFrame(Duration.seconds(ANIMATION_TIME),
                        new KeyValue(translateXProperty(), 0, Interpolator.LINEAR),
                        new KeyValue(mainScene.getBackgroundView().translateXProperty(), getWidth(), Interpolator.LINEAR)
                ));
        openAnim.setCycleCount(1);
        openAnim.setAutoReverse(false);
        openAnim.play();
    }

    /**
     * Closes the menu drawer
     *
     * @param mainScene the mainScene containing this menu drawer
     */
    public void close(MainScene mainScene) {
        if (openAnim != null) {
            openAnim.stop();
        }
        closeAnim = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(translateXProperty(), translateXProperty().getValue(), Interpolator.LINEAR),
                        //new KeyValue(mainScene.getScrollPane().translateXProperty(), mainScene.getBackgroundView().getTranslateX(), Interpolator.LINEAR),
                        new KeyValue(mainScene.getBackgroundView().translateXProperty(), mainScene.getBackgroundView().getTranslateX(), Interpolator.LINEAR)),
                new KeyFrame(Duration.seconds(ANIMATION_TIME),
                        new KeyValue(translateXProperty(), -getWidth() + 2, Interpolator.LINEAR),
                        //new KeyValue(mainScene.getScrollPane().translateXProperty(), 0, Interpolator.LINEAR),
                        new KeyValue(mainScene.getBackgroundView().translateXProperty(), 0, Interpolator.LINEAR)
                ));
        closeAnim.setCycleCount(1);
        closeAnim.setAutoReverse(false);
        closeAnim.setOnFinished(event -> {
            setManaged(false);
        });
        closeAnim.play();
    }

    private void init(MainScene mainScene) {
        initAddButton(mainScene);
        initScanButton(mainScene);
        initSortButton(mainScene);

        initGroupButton(mainScene);
        //initScaleSlider();
        initSettingsButton(mainScene);

        AnchorPane.setTopAnchor(topButtonsBox, 20.0 * Main.SCREEN_HEIGHT / 1080);
        AnchorPane.setBottomAnchor(bottomButtonsBox, 20.0 * Main.SCREEN_HEIGHT / 1080);

        topMenuPane.getChildren().addAll(topButtonsBox, bottomButtonsBox);
        topMenuPane.setId("menu-button-bar");
        setCenter(topMenuPane);

        initButtonSelectListeners();
    }

    private void initAddButton(MainScene mainScene) {
        DrawerButton addButton = new DrawerButton("main-add-button", this);
        addButton.setFocusTraversable(false);

        addButton.setOnAction(event -> {
            mainScene.getRootStackPane().setMouseTransparent(true);
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
                        File selectedFile = fileChooser.showOpenDialog(mainScene.getParentStage());
                        if (selectedFile != null) {
                            mainScene.fadeTransitionTo(new GameEditScene(mainScene, selectedFile), mainScene.getParentStage());
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
                    File selectedFolder = directoryChooser.showDialog(mainScene.getParentStage());
                    if (selectedFolder != null) {
                        ArrayList<File> files = new ArrayList<File>();
                        files.addAll(Arrays.asList(selectedFolder.listFiles()));
                        if (files.size() != 0) {
                            mainScene.batchAddFolderEntries(files, 0).run();
                            //startMultiAddScenes(files);
                        }
                    }
                }
            });
            mainScene.getRootStackPane().setMouseTransparent(false);
        });

        topButtonsBox.getChildren().add(addButton);
    }

    private void initScanButton(MainScene mainScene) {
        ScanButton b = new ScanButton(this);

        topButtonsBox.getChildren().add(b);
    }

    private void initSortButton(MainScene mainScene) {
        DrawerButton sortButton = new DrawerButton("main-sort-button", this);
        sortButton.setFocusTraversable(false);
        sortButton.setSelectionable(true);

        final ContextMenu sortMenu = new ContextMenu();
        MenuItem sortByNameItem = new MenuItem(Main.getString("sort_by_name"));
        MenuItem sortByRatingItem = new MenuItem(Main.getString("sort_by_rating"));
        MenuItem sortByTimePlayedItem = new MenuItem(Main.getString("sort_by_playtime"));
        MenuItem sortByReleaseDateItem = new MenuItem(Main.getString("sort_by_release_date"));
        sortByNameItem.setOnAction(event -> {
            mainScene.sortBy(SortType.NAME);
        });
        sortByRatingItem.setOnAction(event -> {
            mainScene.sortBy(SortType.RATING);
        });
        sortByTimePlayedItem.setOnAction(event -> {
            mainScene.sortBy(SortType.PLAY_TIME);
        });
        sortByReleaseDateItem.setOnAction(event -> {
            mainScene.sortBy(SortType.RELEASE_DATE);
        });
        sortMenu.getItems().addAll(sortByNameItem, sortByRatingItem, sortByTimePlayedItem, sortByReleaseDateItem);

        sortButton.setOnAction(event -> {
            if (isMenuActive("sortBy")) {
                closeSubMenu(mainScene);
            } else {
                openSubMenu(mainScene, "sortBy");
            }
            /*if (sortMenu.isShowing()) {
                sortMenu.hide();
            } else {
                Bounds bounds = sortButton.getBoundsInLocal();
                Bounds screenBounds = sortButton.localToScreen(bounds);
                int x = (int) (screenBounds.getMinX() + 0.25 * bounds.getWidth());
                int y = (int) (screenBounds.getMaxY() - 0.22 * bounds.getHeight());

                sortMenu.show(sortButton, x, y);
            }*/
        });

        subMenus.put("sortBy", new SubMenu("sortBy"));

        topButtonsBox.getChildren().add(sortButton);
    }

    private void openSubMenu(MainScene mainScene, String subMenuId) {
        openSubMenu(mainScene, getSubMenu(subMenuId));
    }

    private void openSubMenu(MainScene mainScene, SubMenu subMenu) {
        if (subMenu == null) {
            throw new IllegalArgumentException("SubMenu is null");
        }
        currentSubMenu = subMenu;

        if (currentSubMenu.getCloseAnim() != null) {
            currentSubMenu.getCloseAnim().stop();
        }

        setRight(currentSubMenu);

        Timeline openAnim = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(currentSubMenu.translateXProperty(), -currentSubMenu.getWidth(), Interpolator.LINEAR),
                        new KeyValue(currentSubMenu.opacityProperty(), currentSubMenu.opacityProperty().doubleValue(), Interpolator.LINEAR),
                        //new KeyValue(mainScene.getScrollPane().translateXProperty(), mainScene.getBackgroundView().getTranslateX(), Interpolator.LINEAR),
                        new KeyValue(mainScene.getBackgroundView().translateXProperty(), topMenuPane.getWidth(), Interpolator.LINEAR)),
                new KeyFrame(Duration.seconds(ANIMATION_TIME),
                        new KeyValue(currentSubMenu.translateXProperty(), 0, Interpolator.LINEAR),
                        new KeyValue(currentSubMenu.opacityProperty(), 1.0, Interpolator.EASE_IN),
                        //new KeyValue(mainScene.getScrollPane().translateXProperty(), 0, Interpolator.LINEAR),
                        new KeyValue(mainScene.getBackgroundView().translateXProperty(), topMenuPane.getWidth() + subMenu.getWidth(), Interpolator.LINEAR)
                ));
        openAnim.setCycleCount(1);
        openAnim.setAutoReverse(false);
        openAnim.setOnFinished(event -> {
            subMenu.setActive(true);
        });
        currentSubMenu.setOpenAnim(openAnim);
        openAnim.play();

        currentSubMenu.setManaged(true);
        currentSubMenu.setVisible(true);
    }

    private void closeSubMenu(MainScene mainScene) {
        if (currentSubMenu != null) {
            if (currentSubMenu.getOpenAnim() != null) {
                currentSubMenu.getOpenAnim().stop();
            }

            Timeline closeAnim = new Timeline(
                    new KeyFrame(Duration.seconds(0),
                            new KeyValue(currentSubMenu.translateXProperty(), currentSubMenu.translateXProperty().getValue(), Interpolator.LINEAR),
                            new KeyValue(currentSubMenu.opacityProperty(), currentSubMenu.opacityProperty().getValue(), Interpolator.LINEAR),
                            //new KeyValue(mainScene.getScrollPane().translateXProperty(), mainScene.getBackgroundView().getTranslateX(), Interpolator.LINEAR),
                            new KeyValue(mainScene.getBackgroundView().translateXProperty(), mainScene.getBackgroundView().getTranslateX(), Interpolator.LINEAR)),
                    new KeyFrame(Duration.seconds(ANIMATION_TIME),
                            new KeyValue(currentSubMenu.translateXProperty(), -currentSubMenu.getWidth(), Interpolator.LINEAR),
                            new KeyValue(currentSubMenu.opacityProperty(), 0, Interpolator.EASE_IN),
                            //new KeyValue(mainScene.getScrollPane().translateXProperty(), 0, Interpolator.LINEAR),
                            new KeyValue(mainScene.getBackgroundView().translateXProperty(), getWidth() - currentSubMenu.getWidth(), Interpolator.LINEAR)
                    ));
            closeAnim.setCycleCount(1);
            closeAnim.setAutoReverse(false);
            closeAnim.setOnFinished(event -> {
                currentSubMenu.setManaged(false);
                currentSubMenu.setVisible(false);
                currentSubMenu.setActive(false);
            });
            currentSubMenu.setCloseAnim(closeAnim);
            closeAnim.play();
        }
    }

    private void initGroupButton(MainScene mainScene) {
        DrawerButton groupButton = new DrawerButton("main-group-button", this);
        groupButton.setFocusTraversable(false);
        groupButton.setSelectionable(true);

        final ContextMenu groupMenu = new ContextMenu();
        MenuItem groupByAll = new MenuItem(Main.getString("group_by_all"));
        groupByAll.setOnAction(event -> {
            mainScene.groupBy(GroupType.ALL);
        });

        MenuItem groupByTheme = new MenuItem(Main.getString("group_by_theme"));
        groupByTheme.setOnAction(event -> {
            mainScene.groupBy(GroupType.THEME);
        });
        MenuItem groupByGenre = new MenuItem(Main.getString("group_by_genre"));
        groupByGenre.setOnAction(event -> {
            mainScene.groupBy(GroupType.GENRE);
        });
        MenuItem groupBySerie = new MenuItem(Main.getString("group_by_serie"));
        groupBySerie.setOnAction(event -> {
            mainScene.groupBy(GroupType.SERIE);
        });

        MenuItem groupByLauncher = new MenuItem(Main.getString("group_by_launcher"));
        groupByLauncher.setOnAction(event -> {
            mainScene.groupBy(GroupType.LAUNCHER);
        });

        groupMenu.getItems().addAll(groupByAll, groupByGenre, groupByTheme, groupBySerie, groupByLauncher);

        groupButton.setOnAction(event -> {
            if (isMenuActive("groupBy")) {
                closeSubMenu(mainScene);
            } else {
                openSubMenu(mainScene, "groupBy");
            }
            /*if (groupMenu.isShowing()) {
                groupMenu.hide();
            } else {
                Bounds bounds = groupButton.getBoundsInLocal();
                Bounds screenBounds = groupButton.localToScreen(bounds);
                int x = (int) (screenBounds.getMinX() + 0.25 * bounds.getWidth());
                int y = (int) (screenBounds.getMaxY() - 0.22 * bounds.getHeight());

                groupMenu.show(groupButton, x, y);
            }*/
        });

        subMenus.put("groupBy", new SubMenu("groupBy"));

        topButtonsBox.getChildren().add(groupButton);
    }

    private void initSettingsButton(MainScene mainScene) {
        DrawerButton settingsButton = new DrawerButton("main-settings-button", this);
        settingsButton.setFocusTraversable(false);
        settingsButton.setOnAction(event -> {
            long start = System.currentTimeMillis();
            SettingsScene settingsScene = new SettingsScene(new StackPane(), mainScene.getParentStage(), mainScene);
            LOGGER.debug("SettingsScene : init = " + (System.currentTimeMillis() - start) + "ms");
            mainScene.fadeTransitionTo(settingsScene, mainScene.getParentStage(), true);
        });

        bottomButtonsBox.getChildren().add(settingsButton);
    }

    private boolean isMenuActive(String id) {
        if (currentSubMenu == null || id == null || currentSubMenu.getMenuId() == null) {
            return false;
        }
        return currentSubMenu.getMenuId().equals(id) && currentSubMenu.isActive();
    }

    private SubMenu getSubMenu(String id) {
        if (subMenus == null || subMenus.isEmpty()) {
            return null;
        }
        return subMenus.get(id);
    }

    private void initButtonSelectListeners() {
        for (Node n : topButtonsBox.getChildren()) {
            if (n instanceof DrawerButton) {
                DrawerButton b = (DrawerButton) n;
                b.addEventHandler(ActionEvent.ACTION, event -> {
                    if (b.isSelectionable()) {
                        unselectAllButtons();
                        LOGGER.debug("kuntz");
                        b.setSelected(true);
                    }
                });
            }
        }

        for (Node n : bottomButtonsBox.getChildren()) {
            if (n instanceof DrawerButton) {
                DrawerButton b = (DrawerButton) n;
                b.addEventHandler(ActionEvent.ACTION,event -> {
                    if (b.isSelectionable()) {
                        unselectAllButtons();
                        LOGGER.debug("kuntz");
                        b.setSelected(true);
                    }
                });
            }
        }
    }

    private void unselectAllButtons() {
        for (Node n : topButtonsBox.getChildren()) {
            if (n instanceof DrawerButton) {
                DrawerButton b = (DrawerButton) n;
                b.setSelected(false);
            }
        }
        for (Node n : bottomButtonsBox.getChildren()) {
            if (n instanceof DrawerButton) {
                DrawerButton b = (DrawerButton) n;
                b.setSelected(false);
            }
        }
    }


}

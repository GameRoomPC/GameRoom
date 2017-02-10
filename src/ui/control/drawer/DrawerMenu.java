package ui.control.drawer;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
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
    private VBox topButtonsBox = new VBox(10);
    private VBox bottomButtonsBox = new VBox(10);

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

        SubMenu sortMenu = new SubMenu("sortBy");
        for (SortType s : SortType.values()) {
            TextItem item = new TextItem(s.getId());
            item.setOnAction(event -> {
                mainScene.sortBy(s);
                sortMenu.unselectAllItems();
                item.setSelected(true);
            });
            sortMenu.addItem(item);
        }
        sortButton.setOnAction(event -> {
            if (isMenuActive("sortBy")) {
                closeSubMenu(mainScene);
                sortButton.setSelected(false);
            } else {
                openSubMenu(mainScene, "sortBy");
            }
        });

        subMenus.put("sortBy", sortMenu);

        topButtonsBox.getChildren().add(sortButton);
    }

    private void openSubMenu(MainScene mainScene, String subMenuId) {
        openSubMenu(mainScene, getSubMenu(subMenuId));
    }

    private void openSubMenu(MainScene mainScene, SubMenu subMenu) {
        if (subMenu == null) {
            throw new IllegalArgumentException("SubMenu is null");
        }
        if (currentSubMenu != null) {
            currentSubMenu.setOpacity(0);
        }

        subMenu.open(mainScene,this);
    }

    private void closeSubMenu(MainScene mainScene) {
        if (currentSubMenu != null) {
            currentSubMenu.close(mainScene,this);
        }
    }

    private void initGroupButton(MainScene mainScene) {
        DrawerButton groupButton = new DrawerButton("main-group-button", this);
        groupButton.setFocusTraversable(false);
        groupButton.setSelectionable(true);

        SubMenu groupMenu = new SubMenu("groupBy");
        for (GroupType g : GroupType.values()) {
            TextItem item = new TextItem(g.getId());
            item.setOnAction(event -> {
                mainScene.groupBy(g);
                groupMenu.unselectAllItems();
                item.setSelected(true);
            });
            groupMenu.addItem(item);
        }

        groupButton.setOnAction(event -> {
            if (isMenuActive("groupBy")) {
                closeSubMenu(mainScene);
                groupButton.setSelected(false);
            } else {
                openSubMenu(mainScene, "groupBy");
            }
        });

        subMenus.put("groupBy", groupMenu);

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
                b.addEventHandler(ActionEvent.ACTION, event -> {
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


    public SubMenu getCurrentSubMenu() {
        return currentSubMenu;
    }

    public void setCurrentSubMenu(SubMenu currentSubMenu) {
        this.currentSubMenu = currentSubMenu;
        setRight(currentSubMenu);
    }

    public double getButtonsPaneWidth(){
        return topMenuPane.getWidth();
    }
}

package ui.control.drawer;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import system.application.settings.PredefinedSetting;
import ui.Main;
import ui.control.drawer.submenu.SubMenu;
import ui.control.specific.ScanButton;
import ui.scene.MainScene;
import ui.scene.SettingsScene;

import java.util.HashMap;

import static ui.Main.*;
import static ui.control.drawer.submenu.SubMenuFactory.*;

/**
 * Created by LM on 09/02/2017.
 */
public class DrawerMenu extends BorderPane {
    public static double ANIMATION_TIME = 0.04;
    public static final double WIDTH_RATIO = 0.02;
    private static final double MIN_WIDTH_RATIO = 0.01;
    private static final double MAX_WIDTH_RATIO = 0.03;

    private Timeline openAnim;
    private Timeline closeAnim;
    private AnchorPane topMenuPane = new AnchorPane();
    private VBox topButtonsBox = new VBox(10);
    private VBox bottomButtonsBox = new VBox(10);
    private VBox resizePane = new VBox();

    private boolean resizing = false;

    private SubMenu currentSubMenu;

    private HashMap<String, SubMenu> subMenus = new HashMap<>();

    public DrawerMenu(MainScene mainScene) {
        super();
        setFocusTraversable(false);

        double widthRatio = Main.GENERAL_SETTINGS.getDouble(PredefinedSetting.DRAWER_MENU_WIDTH);
        setMaxWidth(GENERAL_SETTINGS.getWindowWidth() * widthRatio);
        setPrefWidth(GENERAL_SETTINGS.getWindowWidth() * widthRatio);
        setFocusTraversable(false);
        //setEffect(new InnerShadow());
        setId("menu-bar");

        setOnMouseEntered(event -> {
            if (translateXProperty().getValue() != 0) {
                open(mainScene);
            }
        });

        setOnMouseExited(event -> {
            if (GENERAL_SETTINGS.getBoolean(PredefinedSetting.HIDE_TOOLBAR)) {
                if (event.getX() > getWidth()) {
                    close(mainScene);
                }
            }
            setCursor(Cursor.DEFAULT);
        });

        setOnMouseDragged(event -> {
            if (resizing || (getCursor() != null && getCursor().equals(Cursor.E_RESIZE))) {
                resizing = true;
                double newWidth = event.getX() - 30;
                if(getCursor() != null && !getCursor().equals(Cursor.E_RESIZE)){
                    setCursor(Cursor.E_RESIZE);
                }

                double newRatio = newWidth / SCREEN_WIDTH;

                if (newRatio >= MIN_WIDTH_RATIO && newRatio <= MAX_WIDTH_RATIO) {
                    setPrefWidth(newWidth);
                    setMaxWidth(newWidth);
                    GENERAL_SETTINGS.setSettingValue(PredefinedSetting.DRAWER_MENU_WIDTH, newRatio);
                }
            }
        });
        setOnMouseDragExited(event -> {
            setCursor(Cursor.DEFAULT);
            resizing = false;
        });

        resizePane.setOnMouseEntered(event -> {
            setCursor(Cursor.E_RESIZE);
        });
        resizePane.setOnMouseExited(event -> {
                setCursor(Cursor.DEFAULT);
        });

        setCache(true);
        init(mainScene);

        widthProperty().addListener((observable, oldValue, newValue) -> {
            if (getButtonsPaneWidth() > 0) {
                double newOpacity = 3 * getButtonsPaneWidth() / newValue.doubleValue();
                if (getTranslateX() < 0) {
                    newOpacity = 1;
                }
                mainScene.getScrollPane().setOpacity(newOpacity);
            }
            /*double newTranslateX = newValue.doubleValue() + getTranslateX() - getButtonsPaneWidth();
            if(newTranslateX <0 ){
                newTranslateX = 0;
            }
            mainScene.getBackgroundView().setTranslateX(newTranslateX);*/
        });
        translateXProperty().addListener((observable, oldValue, newValue) -> {
            if (getButtonsPaneWidth() > 0) {
                double newOpacity = 3 * getButtonsPaneWidth() / getWidth();
                if (newValue.doubleValue() < 0) {
                    newOpacity = 1;
                }
                mainScene.getScrollPane().setOpacity(newOpacity);
            }
            /*double newTranslateX = getWidth() + newValue.doubleValue() - getButtonsPaneWidth();
            if(newTranslateX <0 ){
                newTranslateX = 0;
            }
            mainScene.getBackgroundView().setTranslateX(newTranslateX);*/
        });
    }

    /**
     * Opens the menu drawer
     *
     * @param mainScene the mainscene containing this drawer
     */
    public void open(MainScene mainScene) {
        setManaged(true);
        setVisible(true);
        if (closeAnim != null) {
            closeAnim.stop();
        }
        openAnim = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(translateXProperty(), translateXProperty().getValue(), Interpolator.LINEAR)),
                new KeyFrame(Duration.seconds(ANIMATION_TIME),
                        new KeyValue(translateXProperty(), 0, Interpolator.LINEAR)
                ));
        openAnim.setCycleCount(1);
        openAnim.setAutoReverse(false);
        openAnim.play();
        resizePane.setManaged(true);
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
                        new KeyValue(translateXProperty(), translateXProperty().getValue(), Interpolator.LINEAR)),
                new KeyFrame(Duration.seconds(ANIMATION_TIME),
                        new KeyValue(translateXProperty(), -getWidth() + 2, Interpolator.LINEAR)
                ));
        closeAnim.setCycleCount(1);
        closeAnim.setAutoReverse(false);
        closeAnim.setOnFinished(event -> {
            setManaged(false);
            resizePane.setManaged(false);
        });
        closeAnim.play();
    }

    private void init(MainScene mainScene) {
        initAddButton(mainScene);
        initScanButton(mainScene);
        initSortButton(mainScene);

        initGroupButton(mainScene);
        //initScaleSlider();

        initEditButton(mainScene);
        initSettingsButton(mainScene);

        Rectangle r = new Rectangle(2.0, getHeight());
        r.heightProperty().bind(heightProperty());
        resizePane.getChildren().add(r);
        resizePane.setOpacity(0);

        AnchorPane.setTopAnchor(topButtonsBox, 20.0 * Main.SCREEN_HEIGHT / 1080);
        AnchorPane.setBottomAnchor(bottomButtonsBox, 20.0 * Main.SCREEN_HEIGHT / 1080);

        topMenuPane.getChildren().addAll(topButtonsBox, bottomButtonsBox);
        topMenuPane.setId("menu-button-bar");

        BorderPane p = new BorderPane();
        p.setFocusTraversable(false);
        p.setCenter(topMenuPane);
        p.setRight(resizePane);
        setCenter(p);

        initButtonSelectListeners();
    }

    private void initAddButton(MainScene mainScene) {
        DrawerButton addButton = new DrawerButton("main-add-button", this);
        addButton.setSelectionable(true);

        SubMenu addMenu = createAddGameSubMenu(mainScene, this);

        addButton.setOnAction(event -> {
            if (isMenuActive(addMenu.getMenuId())) {
                closeSubMenu(mainScene);
                addButton.setSelected(false);
            } else {
                openSubMenu(mainScene, addMenu.getMenuId());
            }
        });

        subMenus.put(addMenu.getMenuId(), addMenu);

        topButtonsBox.getChildren().add(addButton);
    }

    private void initScanButton(MainScene mainScene) {
        ScanButton b = new ScanButton(this);

        topButtonsBox.getChildren().add(b);
    }

    private void initSortButton(MainScene mainScene) {
        DrawerButton sortButton = new DrawerButton("main-sort-button", this);
        sortButton.setSelectionable(true);

        SubMenu sortMenu = createSortBySubMenu(mainScene, this);

        sortButton.setOnAction(event -> {
            if (isMenuActive(sortMenu.getMenuId())) {
                closeSubMenu(mainScene);
                sortButton.setSelected(false);
            } else {
                openSubMenu(mainScene, sortMenu.getMenuId());
            }
        });

        subMenus.put(sortMenu.getMenuId(), sortMenu);

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
        resizePane.setManaged(false);

        subMenu.open(mainScene, this);
    }

    private void closeSubMenu(MainScene mainScene) {
        if (currentSubMenu != null) {
            currentSubMenu.close(mainScene, this);
        }
        resizePane.setManaged(true);
    }

    private void initGroupButton(MainScene mainScene) {
        DrawerButton groupButton = new DrawerButton("main-group-button", this);
        groupButton.setFocusTraversable(false);
        groupButton.setSelectionable(true);

        SubMenu groupMenu = createGroupBySubMenu(mainScene, this);

        groupButton.setOnAction(event -> {
            if (isMenuActive(groupMenu.getMenuId())) {
                closeSubMenu(mainScene);
                groupButton.setSelected(false);
            } else {
                openSubMenu(mainScene, groupMenu.getMenuId());
            }
        });

        subMenus.put(groupMenu.getMenuId(), groupMenu);

        topButtonsBox.getChildren().add(groupButton);
    }

    private void initEditButton(MainScene mainScene) {
        DrawerButton editButton = new DrawerButton("main-edit-button", this);
        editButton.setFocusTraversable(false);
        editButton.setSelectionable(true);

        SubMenu editSubMenu = createEditSubMenu(mainScene, this);

        editButton.setOnAction(event -> {
            if (isMenuActive(editSubMenu.getMenuId())) {
                closeSubMenu(mainScene);
                editButton.setSelected(false);
            } else {
                openSubMenu(mainScene, editSubMenu.getMenuId());
            }
        });

        subMenus.put(editSubMenu.getMenuId(), editSubMenu);

        bottomButtonsBox.getChildren().add(editButton);
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
                        b.setSelected(true);
                    }
                });
            }
        }
    }

    public void unselectAllButtons() {
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

    public double getButtonsPaneWidth() {
        return topMenuPane.getWidth();
    }
}

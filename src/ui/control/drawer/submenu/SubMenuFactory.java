package ui.control.drawer.submenu;

import data.io.FileUtils;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.image.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import net.lingala.zip4j.exception.ZipException;
import system.application.settings.PredefinedSetting;
import ui.Main;
import ui.control.drawer.DrawerMenu;
import ui.control.drawer.GroupType;
import ui.control.drawer.SortType;
import ui.dialog.GameRoomAlert;
import ui.scene.GameEditScene;
import ui.scene.MainScene;
import ui.scene.SettingsScene;
import ui.theme.Theme;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;

import static ui.scene.BaseScene.BACKGROUND_IMAGE_LOAD_RATIO;

/**
 * Created by LM on 10/02/2017.
 */
public final class SubMenuFactory {
    private final static double MAX_TILE_ZOOM = 0.675;
    private final static double MIN_TILE_ZOOM = 0.10;

    public static SubMenu createAddGameSubMenu(MainScene mainScene, DrawerMenu drawerMenu) {
        SubMenu addMenu = new SubMenu("addGames",mainScene,drawerMenu);
        TextItem singleAppItem = new TextItem("add_single_app");
        singleAppItem.setTooltip(new Tooltip(Main.getString("add_single_app_long")));
        singleAppItem.setOnAction(event -> {
            mainScene.getRootStackPane().setMouseTransparent(true);

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
            mainScene.getRootStackPane().setMouseTransparent(false);
        });
        addMenu.addItem(singleAppItem);

        TextItem folderItem = new TextItem("add_folder_app");
        folderItem.setTooltip(new Tooltip(Main.getString("add_folder_app_long")));
        folderItem.setOnAction(event -> {
            mainScene.getRootStackPane().setMouseTransparent(true);

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
            mainScene.getRootStackPane().setMouseTransparent(false);
        });

        addMenu.addItem(folderItem);

        return addMenu;
    }

    public static SubMenu createGroupBySubMenu(MainScene mainScene, DrawerMenu drawerMenu) {
        SubMenu groupMenu = new SubMenu("groupBy", mainScene, drawerMenu);
        TextItem defaultItem = new TextItem("default");
        defaultItem.setOnAction(event -> {
            mainScene.home();
            groupMenu.unselectAllItems();
            defaultItem.setSelected(true);
        });
        defaultItem.setSelected(true);
        groupMenu.addItem(defaultItem);

        for (GroupType g : GroupType.values()) {
            TextItem item = new TextItem(g.getId());
            item.setOnAction(event -> {
                mainScene.groupBy(g);
                groupMenu.unselectAllItems();
                item.setSelected(true);
            });
            groupMenu.addItem(item);
        }

        return groupMenu;
    }

    public static SubMenu createSortBySubMenu(MainScene mainScene, DrawerMenu drawerMenu) {
        SubMenu sortMenu = new SubMenu("sortBy", mainScene, drawerMenu);

        TextItem defaultItem = new TextItem("default");
        defaultItem.setOnAction(event -> {
            mainScene.home();
            sortMenu.unselectAllItems();
            defaultItem.setSelected(true);
        });
        defaultItem.setSelected(true);
        sortMenu.addItem(defaultItem);
        for (SortType s : SortType.values()) {
            TextItem item = new TextItem(s.getId());
            item.setOnAction(event -> {
                mainScene.sortBy(s);
                sortMenu.unselectAllItems();
                item.setSelected(true);
            });
            sortMenu.addItem(item);
        }
        return sortMenu;
    }

    public static SubMenu createEditSubMenu(MainScene mainScene, DrawerMenu drawerMenu) {
        SubMenu editMenu = new SubMenu("editMenu", mainScene, drawerMenu);
        CheckBoxItem keepDrawerCheckBox = new CheckBoxItem("keep_drawer_opened");
        keepDrawerCheckBox.setSelected(!Main.GENERAL_SETTINGS.getBoolean(PredefinedSetting.HIDE_TOOLBAR));
        keepDrawerCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            Main.GENERAL_SETTINGS.setSettingValue(PredefinedSetting.HIDE_TOOLBAR, !newValue);
        });
        editMenu.addItem(keepDrawerCheckBox);

        CheckBoxItem hidePanesCheckBox = new CheckBoxItem("show_hide_top_panes");
        hidePanesCheckBox.setSelected(Main.GENERAL_SETTINGS.getBoolean(PredefinedSetting.HIDE_TILES_ROWS));
        hidePanesCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            Main.GENERAL_SETTINGS.setSettingValue(PredefinedSetting.HIDE_TILES_ROWS, newValue);
            mainScene.forceHideTilesRows(newValue);
        });
        editMenu.addItem(hidePanesCheckBox);

        CheckBoxItem fullScreenCheckBox = new CheckBoxItem("fullscreen");
        fullScreenCheckBox.setSelected(Main.GENERAL_SETTINGS.getBoolean(PredefinedSetting.FULL_SCREEN));
        fullScreenCheckBox.setOnAction(event -> {
            try {
                Robot r = new Robot();
                r.keyPress(java.awt.event.KeyEvent.VK_F11);
            } catch (AWTException e) {
                e.printStackTrace();
            }
        });
        mainScene.getParentStage().fullScreenProperty().addListener((observable, oldValue, newValue) -> {
            fullScreenCheckBox.setSelected(newValue);
        });
        //TODO fix F11 and this checkbox sync
        //editMenu.addItem(fullScreenCheckBox);

        Slider sizeSlider = new Slider();
        sizeSlider.setMin(MIN_TILE_ZOOM);
        sizeSlider.setMax(MAX_TILE_ZOOM);
        sizeSlider.setBlockIncrement(0.1);
        sizeSlider.setFocusTraversable(false);

        sizeSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                mainScene.newTileZoom(newValue.doubleValue());
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

        double sizeSliderValue = Main.GENERAL_SETTINGS.getDouble(PredefinedSetting.TILE_ZOOM);
        if (sizeSliderValue <= MIN_TILE_ZOOM) {
            sizeSliderValue = MIN_TILE_ZOOM + 0.00001; //extreme values of the slider are buggy
        } else if (sizeSliderValue >= MAX_TILE_ZOOM) {
            sizeSliderValue = MAX_TILE_ZOOM + 0.00001; //extreme values of the slider are buggy
        }
        sizeSlider.setValue(sizeSliderValue);

        HBox sizeBox = new HBox();
        sizeBox.getChildren().add(new TextItem("zoom"));
        sizeBox.getChildren().add(sizeSlider);

        editMenu.addItem(sizeBox);

        ButtonItem browseButton = new ButtonItem("browse");
        browseButton.setManaged(Main.GENERAL_SETTINGS.getBoolean(PredefinedSetting.ENABLE_STATIC_WALLPAPER));
        browseButton.setVisible(Main.GENERAL_SETTINGS.getBoolean(PredefinedSetting.ENABLE_STATIC_WALLPAPER));
        browseButton.setMouseTransparent(!Main.GENERAL_SETTINGS.getBoolean(PredefinedSetting.ENABLE_STATIC_WALLPAPER));
        browseButton.setOnAction(event -> {
            FileChooser imageChooser = new FileChooser();
            imageChooser.setTitle(Main.getString("select_picture"));
            imageChooser.setInitialDirectory(
                    new File(System.getProperty("user.home"))
            );
            imageChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("JPEG (*.jpg, *.jpeg)", "*.jpg","*.jpeg"),
                    new FileChooser.ExtensionFilter("PNG (*.png)", "*.png")
            );
            try {
                File selectedFile = imageChooser.showOpenDialog(mainScene.getParentStage());
                if (selectedFile != null) {
                    String copiedPath = Main.FILES_MAP.get("working_dir") + File.separator + "wallpaper." + FileUtils.getExtension(selectedFile);
                    File copiedFile = new File(copiedPath);

                    Files.copy(selectedFile.toPath().toAbsolutePath()
                            , copiedFile.toPath().toAbsolutePath()
                            , StandardCopyOption.REPLACE_EXISTING);

                    mainScene.setChangeBackgroundNextTime(false);
                    mainScene.setImageBackground(new Image("file:///"+copiedPath,
                            Main.GENERAL_SETTINGS.getWindowWidth(),
                            Main.GENERAL_SETTINGS.getWindowHeight()
                            , false, true),true);
                }
            } catch (NullPointerException | IOException ne) {
                ne.printStackTrace();
            }
        });
        CheckBoxItem backgroundImageCheckBox = new CheckBoxItem("use_a_static_wallpaper");

        backgroundImageCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            boolean registered = SettingsScene.checkAndDisplayRegisterDialog();
            if (registered) {
                Main.GENERAL_SETTINGS.setSettingValue(PredefinedSetting.ENABLE_STATIC_WALLPAPER, newValue);
                browseButton.setManaged(newValue);
                browseButton.setVisible(newValue);
                browseButton.setMouseTransparent(!newValue);
                mainScene.setImageBackground(null,true);
            }
        });
        backgroundImageCheckBox.setSelected(Main.GENERAL_SETTINGS.getBoolean(PredefinedSetting.ENABLE_STATIC_WALLPAPER));
        editMenu.addItem(backgroundImageCheckBox);
        editMenu.addItem(browseButton);

        return editMenu;
    }
}

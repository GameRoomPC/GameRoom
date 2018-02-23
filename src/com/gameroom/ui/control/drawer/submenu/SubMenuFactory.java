package com.gameroom.ui.control.drawer.submenu;

import com.gameroom.data.game.entry.GameEntry;
import com.gameroom.data.game.entry.Platform;
import com.gameroom.data.io.FileUtils;
import com.gameroom.system.application.settings.PredefinedSetting;
import com.gameroom.ui.Main;
import com.gameroom.ui.control.drawer.DrawerMenu;
import com.gameroom.ui.control.drawer.GroupType;
import com.gameroom.ui.control.drawer.SortType;
import com.gameroom.ui.dialog.GameRoomAlert;
import com.gameroom.ui.dialog.GamesFoldersDialog;
import com.gameroom.ui.dialog.selector.MSStoreAppSelector;
import com.gameroom.ui.scene.GameEditScene;
import com.gameroom.ui.scene.MainScene;
import com.gameroom.ui.scene.SettingsScene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;

import static com.gameroom.system.application.settings.GeneralSettings.settings;
import static com.gameroom.ui.Main.LOGGER;
import static com.gameroom.ui.Main.MAIN_SCENE;

/**
 * Created by LM on 10/02/2017.
 */
public final class SubMenuFactory {
    private final static double MAX_TILE_ZOOM = 0.675;
    private final static double MIN_TILE_ZOOM = 0.10;

    public static SubMenu createAddGameSubMenu(MainScene mainScene, DrawerMenu drawerMenu) {
        SubMenu addMenu = new SubMenu("addGames", mainScene, drawerMenu);
        TextItem singleAppItem = new TextItem("add_single_app");
        singleAppItem.setTooltip(new Tooltip(Main.getString("add_single_app_long")));
        singleAppItem.setOnAction(event -> {
            if (!settings().getBoolean(PredefinedSetting.NO_MORE_ADD_APP_WARNING)) {
                GameRoomAlert.info(Main.getString("add_single_app_long"));
                settings().setSettingValue(PredefinedSetting.NO_MORE_ADD_APP_WARNING, true);
            }
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

        //************ MICROSOFT STORE ****************
        TextItem MSAppItem = new TextItem("add_MS_apps");
        //TODO add a true localized tooltip
        MSAppItem.setTooltip(new Tooltip(Main.getString("add_MS_apps")));
        MSAppItem.setOnAction(event -> {
            mainScene.getRootStackPane().setMouseTransparent(true);

            MSStoreAppSelector selector = new MSStoreAppSelector();
            Optional<ButtonType> optional = selector.showAndWait();
            optional.ifPresent(pairs -> {
                if (pairs.getButtonData().equals(ButtonBar.ButtonData.OK_DONE)) {
                    ArrayList<GameEntry> entries = new ArrayList<GameEntry>();

                    selector.getSelectedEntries().forEach(msStoreEntry -> {
                        GameEntry gameEntry = new GameEntry(msStoreEntry.getName());
                        gameEntry.setSavedLocally(true);
                        gameEntry.setToAdd(true);
                        gameEntry.setPath(msStoreEntry.getStartCommand());
                        gameEntry.setPlatform(Platform.MICROSOFT_STORE_ID);
                        gameEntry.saveEntry();
                        if(msStoreEntry.getIconPath() != null) {
                            try {
                                gameEntry.updateImage(0, new File(msStoreEntry.getIconPath()));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        entries.add(gameEntry);

                        gameEntry.setAddedDate(LocalDateTime.now());
                        LOGGER.debug("Chosen MSStoreApp: " + msStoreEntry.getName());
                    });
                    MAIN_SCENE.batchAddGameEntries(entries, 0).run();
                }
            });
            mainScene.getRootStackPane().setMouseTransparent(false);
        });
        addMenu.addItem(MSAppItem);

        //************FOLDER****************
        TextItem folderItem = new TextItem("add_folder_app");
        folderItem.setOnAction(event -> {
            new GamesFoldersDialog().showAndWait();
        });

        addMenu.addItem(folderItem);

        //************EMULATOR****************
        TextItem emulatedItem = new TextItem("add_emulated_game");
        emulatedItem.setOnAction(event -> {
            boolean registered = SettingsScene.checkAndDisplayRegisterDialog();
            if (registered) {
                if (!settings().getBoolean(PredefinedSetting.NO_MORE_ADD_APP_WARNING)) {
                    GameRoomAlert.info(Main.getString("add_single_app_long"));
                    settings().setSettingValue(PredefinedSetting.NO_MORE_ADD_APP_WARNING, true);
                }

                mainScene.getRootStackPane().setMouseTransparent(true);

                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle(Main.getString("select_program"));
                fileChooser.setInitialDirectory(
                        new File(System.getProperty("user.home"))
                );

                Set<String> allExt = new HashSet<>();
                Platform.getEmulablePlatforms().forEach(platform -> {
                    Collections.addAll(allExt, platform.getSupportedExtensions());
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(platform.getName(), platform.getSupportedExtensions()));
                });
                ArrayList<String> sortedExts = new ArrayList<String>();
                sortedExts.addAll(allExt);
                sortedExts.sort(String::compareTo);

                fileChooser.getExtensionFilters().add(0, new FileChooser.ExtensionFilter(Main.getString("all_rom_exts"), sortedExts));
                fileChooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter(Main.getString("all_files"), "*")
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
            }
        });
        addMenu.addItem(emulatedItem);

        return addMenu;
    }

    public static SubMenu createGroupBySubMenu(MainScene mainScene, DrawerMenu drawerMenu) {
        SubMenu groupMenu = new SubMenu("groupBy", mainScene, drawerMenu);

        GroupType savedGroup = GroupType.fromId(settings().getString(PredefinedSetting.GROUP_BY));

        for (GroupType g : GroupType.values()) {
            TextItem item = new TextItem(g.getId());
            item.setOnAction(event -> {
                mainScene.groupBy(g);
                groupMenu.unselectAllItems();
                item.setSelected(true);
            });
            item.setSelected(g.equals(savedGroup));
            groupMenu.addItem(item);
        }

        return groupMenu;
    }

    public static SubMenu createSortBySubMenu(MainScene mainScene, DrawerMenu drawerMenu) {
        SubMenu sortMenu = new SubMenu("sortBy", mainScene, drawerMenu);

        SortType savedSort = SortType.fromId(settings().getString(PredefinedSetting.SORT_BY));

        for (SortType s : SortType.values()) {
            TextItem item = new TextItem(s.getId());
            item.setOnAction(event -> {
                mainScene.sortBy(s);
                sortMenu.unselectAllItems();
                item.setSelected(true);
            });
            item.setSelected(s.equals(savedSort));
            sortMenu.addItem(item);
        }
        return sortMenu;
    }

    public static SubMenu createEditSubMenu(MainScene mainScene, DrawerMenu drawerMenu) {
        SubMenu editMenu = new SubMenu("customize", mainScene, drawerMenu);
        CheckBoxItem keepDrawerCheckBox = new CheckBoxItem("keep_drawer_opened", true);
        keepDrawerCheckBox.setSelected(!settings().getBoolean(PredefinedSetting.HIDE_TOOLBAR));
        keepDrawerCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            settings().setSettingValue(PredefinedSetting.HIDE_TOOLBAR, !newValue);
        });
        editMenu.addItem(keepDrawerCheckBox);

        CheckBoxItem hidePanesCheckBox = new CheckBoxItem("show_hide_top_panes", true);
        hidePanesCheckBox.setSelected(settings().getBoolean(PredefinedSetting.HIDE_TILES_ROWS));
        hidePanesCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            settings().setSettingValue(PredefinedSetting.HIDE_TILES_ROWS, newValue);
            mainScene.forceHideTilesRows(newValue);
        });
        editMenu.addItem(hidePanesCheckBox);

        CheckBoxItem keepRatioCheckBox = new CheckBoxItem("keep_cover_ratio", true);
        keepRatioCheckBox.setSelected(settings().getBoolean(PredefinedSetting.KEEP_COVER_RATIO));
        keepRatioCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            settings().setSettingValue(PredefinedSetting.KEEP_COVER_RATIO, newValue);
            mainScene.reloadCovers();
        });
        editMenu.addItem(keepRatioCheckBox);

        CheckBoxItem showPCIconCheckBox = new CheckBoxItem("show_pc_icon", true);
        showPCIconCheckBox.setSelected(settings().getBoolean(PredefinedSetting.SHOW_PC_ICON));
        showPCIconCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            settings().setSettingValue(PredefinedSetting.SHOW_PC_ICON, newValue);
            mainScene.reloadLauncherLogos();
        });
        editMenu.addItem(showPCIconCheckBox);

        CheckBoxItem fullScreenCheckBox = new CheckBoxItem("fullscreen", true);
        fullScreenCheckBox.selectedProperty().bindBidirectional(settings().getBooleanProperty(PredefinedSetting.FULL_SCREEN));

        editMenu.addItem(fullScreenCheckBox);

        Slider sizeSlider = new Slider();
        sizeSlider.setMin(MIN_TILE_ZOOM);
        sizeSlider.setMax(MAX_TILE_ZOOM);
        sizeSlider.setBlockIncrement(0.1);
        sizeSlider.setFocusTraversable(false);

        sizeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            mainScene.newTileZoom(newValue.doubleValue());
        });
        sizeSlider.setOnMouseReleased(event -> {
            editMenu.setManaged(true);
            editMenu.setOpacity(1.0);
            if (settings().getBoolean(PredefinedSetting.HIDE_TOOLBAR)) {
                drawerMenu.setManaged(true);
                drawerMenu.setOpacity(1.0);
            }
            settings().setSettingValue(PredefinedSetting.TILE_ZOOM, sizeSlider.getValue());
        });
        sizeSlider.setOnMousePressed(event -> {
            editMenu.setManaged(false);
            editMenu.setOpacity(0.7);
            if (settings().getBoolean(PredefinedSetting.HIDE_TOOLBAR)) {
                drawerMenu.setManaged(false);
                drawerMenu.setOpacity(0.7);
            }
        });

        sizeSlider.setOnMouseDragExited(event -> {
            settings().setSettingValue(PredefinedSetting.TILE_ZOOM, sizeSlider.getValue());
        });
        sizeSlider.setPrefWidth(Main.SCREEN_WIDTH / 12);
        sizeSlider.setMaxWidth(Main.SCREEN_WIDTH / 12);
        sizeSlider.setPrefHeight(Main.SCREEN_WIDTH / 160);
        sizeSlider.setMaxHeight(Main.SCREEN_WIDTH / 160);

        double sizeSliderValue = settings().getDouble(PredefinedSetting.TILE_ZOOM);
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
        browseButton.setManaged(settings().getBoolean(PredefinedSetting.ENABLE_STATIC_WALLPAPER));
        browseButton.setVisible(settings().getBoolean(PredefinedSetting.ENABLE_STATIC_WALLPAPER));
        browseButton.setMouseTransparent(!settings().getBoolean(PredefinedSetting.ENABLE_STATIC_WALLPAPER));
        browseButton.setOnAction(event -> {
            FileChooser imageChooser = new FileChooser();
            imageChooser.setTitle(Main.getString("select_picture"));
            imageChooser.setInitialDirectory(
                    new File(System.getProperty("user.home"))
            );
            imageChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("JPEG (*.jpg, *.jpeg)", "*.jpg", "*.jpeg"),
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
                    mainScene.setImageBackground(copiedFile, true);
                }
            } catch (NullPointerException | IOException ne) {
                ne.printStackTrace();
            }
        });
        boolean staticWallPaper = settings().getBoolean(PredefinedSetting.ENABLE_STATIC_WALLPAPER);
        boolean disabledWallpaper = settings().getBoolean(PredefinedSetting.DISABLE_MAINSCENE_WALLPAPER);

        CheckBoxItem noWallPaperCheckBox = new CheckBoxItem(Main.getSettingsString("disableMainSceneWallpaper_label"), false);
        noWallPaperCheckBox.setTooltip(new Tooltip(Main.getSettingsString("disableMainSceneWallpaper_tooltip")));
        noWallPaperCheckBox.setSelected(disabledWallpaper && !staticWallPaper);
        settings().setSettingValue(PredefinedSetting.DISABLE_MAINSCENE_WALLPAPER, disabledWallpaper && !staticWallPaper);
        noWallPaperCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            settings().setSettingValue(PredefinedSetting.DISABLE_MAINSCENE_WALLPAPER, newValue);
            if (newValue) {
                mainScene.setChangeBackgroundNextTime(false);
                mainScene.setImageBackground(null);
            }
        });
        noWallPaperCheckBox.setDisable(staticWallPaper);
        editMenu.addItem(noWallPaperCheckBox);

        CheckBoxItem backgroundImageCheckBox = new CheckBoxItem("use_a_static_wallpaper", true);

        backgroundImageCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            boolean registered = SettingsScene.checkAndDisplayRegisterDialog();
            if (registered) {
                settings().setSettingValue(PredefinedSetting.ENABLE_STATIC_WALLPAPER, newValue);
                browseButton.setManaged(newValue);
                browseButton.setVisible(newValue);
                browseButton.setMouseTransparent(!newValue);
                mainScene.setImageBackground(null, true);
                noWallPaperCheckBox.setDisable(newValue);
                if (newValue) {
                    noWallPaperCheckBox.setSelected(false);
                }
            }
        });
        backgroundImageCheckBox.setSelected(staticWallPaper);
        editMenu.addItem(backgroundImageCheckBox);
        editMenu.addItem(browseButton);

        return editMenu;
    }
}

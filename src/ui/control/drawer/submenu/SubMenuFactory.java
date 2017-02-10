package ui.control.drawer.submenu;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import system.application.settings.PredefinedSetting;
import ui.Main;
import ui.control.drawer.GroupType;
import ui.control.drawer.SortType;
import ui.scene.MainScene;

import static ui.control.button.gamebutton.GameButton.COVER_HEIGHT_WIDTH_RATIO;

/**
 * Created by LM on 10/02/2017.
 */
public final class SubMenuFactory {
    private final static double MAX_TILE_ZOOM = 0.675;
    private final static double MIN_TILE_ZOOM = 0.10;

    public static SubMenu createGroupBySubMenu(MainScene mainScene){
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
        return groupMenu;
    }

    public static SubMenu createSortBySubMenu(MainScene mainScene){
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
        return sortMenu;
    }

    public static SubMenu createEditSubMenu(MainScene mainScene){
        SubMenu editMenu = new SubMenu("editMenu");
        CheckBoxItem keepDrawerCheckBox = new CheckBoxItem("keep_drawer_opened");
        keepDrawerCheckBox.setSelected(!Main.GENERAL_SETTINGS.getBoolean(PredefinedSetting.HIDE_TOOLBAR));
        keepDrawerCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            Main.GENERAL_SETTINGS.setSettingValue(PredefinedSetting.HIDE_TOOLBAR,!newValue);
        });
        editMenu.addItem(keepDrawerCheckBox);

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
        sizeBox.getChildren().add(sizeSlider);
        sizeBox.getChildren().add(new TextItem("tile_size"));

        editMenu.addItem(sizeBox);

        return editMenu;
    }
}

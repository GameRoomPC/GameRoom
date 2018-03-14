package com.gameroom.ui.dialog;

import com.gameroom.data.game.entry.GameEntry;
import com.gameroom.data.game.entry.GameEntryUtils;
import com.gameroom.data.http.images.ImageUtils;
import com.gameroom.ui.Main;
import com.gameroom.ui.UIValues;
import com.gameroom.ui.control.button.gamebutton.GameButton;
import com.gameroom.ui.dialog.selector.IgnoredEntrySelector;
import com.gameroom.ui.pane.SelectListPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import static com.gameroom.system.application.settings.GeneralSettings.settings;

/**
 * Created by LM on 11/08/2016.
 */
public class NonScrapedListDialog extends GameRoomDialog<ButtonType> {
    private ArrayList<GameEntry> selectedList = new ArrayList<>();
    private ArrayList<GameEntry> unselectedList = new ArrayList<>();
    private Label statusLabel;

    public NonScrapedListDialog(Collection<GameEntry> entries) {
        statusLabel = new Label(Main.getString("loading") + "...");
        rootStackPane.getChildren().add(statusLabel);
        //TODO localize
        Label titleLabel = new Label(Main.getString("following_games_not_scraped"));
        titleLabel.setWrapText(true);
        titleLabel.setTooltip(new Tooltip(Main.getString("following_games_not_scraped")));
        titleLabel.setPadding(new Insets(0 * Main.SCREEN_HEIGHT / 1080
                , 20 * Main.SCREEN_WIDTH / 1920
                , 20 * Main.SCREEN_HEIGHT / 1080
                , 20 * Main.SCREEN_WIDTH / 1920));
        mainPane.setTop(titleLabel);
        mainPane.setPadding(new Insets(30 * Main.SCREEN_HEIGHT / 1080
                , 30 * Main.SCREEN_WIDTH / 1920
                , 20 * Main.SCREEN_HEIGHT / 1080
                , 30 * Main.SCREEN_WIDTH / 1920));
        BorderPane.setAlignment(titleLabel, Pos.CENTER);

        mainPane.setPrefWidth(1.0 / 3.5 * Main.SCREEN_WIDTH);
        mainPane.setPrefHeight(2.0 / 3 * Main.SCREEN_HEIGHT);

        NonScrapedListDialog.GameEntryList list = new NonScrapedListDialog.GameEntryList();
        list.addItems(entries);
        statusLabel.setText(null);

        mainPane.setCenter(list);
        CheckBox selectAllBox = new CheckBox(Main.getString("select_all"));
        selectAllBox.setPadding(UIValues.CONTROL_SMALL.insets());
        selectAllBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(!oldValue && newValue){
                list.selectAllValues(true);
            } else if (oldValue && !newValue){
                list.selectAllValues(false);
            }
        });
        list.setPrefWidth(mainPane.getPrefWidth());

        getDialogPane().getButtonTypes().addAll(
                new ButtonType(Main.getString("ok"), ButtonBar.ButtonData.OK_DONE)
                , new ButtonType(Main.getString("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE));

        setOnHiding(event -> {
            selectedList.addAll(list.getSelectedValues());
            unselectedList.addAll(list.getUnselectedValues());
        });
    }

    public ArrayList<GameEntry> getSelectedEntries() {
        return selectedList;
    }
    public ArrayList<GameEntry> getUnselectedEntries() {
        return unselectedList;
    }

    private static class GameEntryList<T> extends SelectListPane {

        public GameEntryList() {
            super(true);

        }

        @Override
        protected ListItem createListItem(Object value) {
            NonScrapedListDialog.GameFolderItem item = new NonScrapedListDialog.GameFolderItem(value, this);
            item.setSelected(((GameEntry) value).isIgnored());
            return item;
        }
    }

    private static class GameFolderItem extends SelectListPane.ListItem {
        private GameEntry entry;
        private final static int IMAGE_WIDTH = 64;
        private final static int IMAGE_HEIGHT = 64;
        private StackPane coverPane = new StackPane();

        public GameFolderItem(Object value, SelectListPane parentList) {
            super(value, parentList);
            entry = ((GameEntry) value);
            addContent();
        }

        @Override
        protected void addContent() {
            ImageView iconView = new ImageView();
            double scale = settings().getUIScale().getScale();
            iconView.setFitHeight(IMAGE_HEIGHT * scale);
            iconView.setFitWidth(IMAGE_WIDTH * scale);

            File gamePath = new File(entry.getPath());
            if (!gamePath.exists() && entry.getImagePath(0) != null && entry.getImagePath(0).exists()) {
                iconView.setImage(entry.getImage(0, IMAGE_WIDTH * scale, IMAGE_HEIGHT * scale * GameButton.COVER_HEIGHT_WIDTH_RATIO, true, false));
                iconView.setFitHeight(IMAGE_HEIGHT * scale * GameButton.COVER_HEIGHT_WIDTH_RATIO);
            }  else if (entry.isSteamGame()) {
                ImageUtils.downloadSteamImageToCache(entry.getPlatformGameID(),ImageUtils.STEAM_TYPE_CAPSULE,ImageUtils.STEAM_SIZE_SMALL,outputFile -> {
                    iconView.setImage(new Image("file:///"+outputFile.getAbsolutePath(),IMAGE_WIDTH * scale,IMAGE_HEIGHT * scale,true,true));
                    iconView.setFitHeight(0);
                });
            } else {
                ImageUtils.transitionToFileThumbnail(gamePath,iconView,IMAGE_WIDTH);
            }
            coverPane.getChildren().add(iconView);

            GridPane.setMargin(coverPane, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 10 * Main.SCREEN_WIDTH / 1920));
            add(coverPane, columnCount++, 0);

            VBox vbox = new VBox(5 * Main.SCREEN_HEIGHT / 1080);
            Label titleLabel = new Label(entry.getName());
            titleLabel.setTooltip(new Tooltip(entry.getPath()));

            Label idLabel = new Label(entry.getPath());
            idLabel.setStyle("-fx-font-size: 0.7em;");

            vbox.getChildren().addAll(titleLabel, idLabel);
            GridPane.setMargin(vbox, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 10 * Main.SCREEN_WIDTH / 1920));
            add(vbox, columnCount++, 0);
        }

    }
}
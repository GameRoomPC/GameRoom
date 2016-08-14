package ui.dialog;

import data.game.*;
import data.game.scrapper.SteamPreEntry;
import data.game.scrapper.SteamLocalScrapper;
import data.http.SimpleImageInfo;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import ui.Main;
import ui.pane.SelectListPane;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static ui.Main.GENERAL_SETTINGS;

/**
 * Created by LM on 09/08/2016.
 */
public class SteamIgnoredSelector extends GameRoomDialog<ButtonType> {
    public final static int MODE_REMOVE_FROM_LIST = 0;
    public final static int MODE_ADD_TO_LIST = 1;
    private SteamPreEntry[] selectedList;


    public SteamIgnoredSelector() throws IOException {
        ArrayList<SteamPreEntry> steamEntries = SteamLocalScrapper.getSteamAppsInstalled();

        Label titleLabel = new Label(Main.RESSOURCE_BUNDLE.getString("select_steam_games_ignore"));
        titleLabel.setWrapText(true);
        titleLabel.setTooltip(new Tooltip(Main.RESSOURCE_BUNDLE.getString("select_steam_games_ignore")));
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

        mainPane.setPrefWidth(Main.SCREEN_WIDTH * 1 / 3 * Main.SCREEN_WIDTH / 1920);
        mainPane.setPrefHeight(Main.SCREEN_HEIGHT * 2 / 3 * Main.SCREEN_HEIGHT / 1080);

        SteamAppsList list = new SteamAppsList(Main.SCREEN_HEIGHT / 3.0, mainPane.prefWidthProperty());
        list.addItems(steamEntries);
        mainPane.setCenter(list);

        getDialogPane().getButtonTypes().addAll(
                new ButtonType(Main.RESSOURCE_BUNDLE.getString("ok"), ButtonBar.ButtonData.OK_DONE)
                , new ButtonType(Main.RESSOURCE_BUNDLE.getString("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE));

        setOnHiding(event -> {
            SteamPreEntry[] temp_entries = new SteamPreEntry[list.getSelectedValues().size()];
            for(int i = 0; i< temp_entries.length; i++){
                temp_entries[i] = (SteamPreEntry) list.getSelectedValues().get(i);
            }
            selectedList = temp_entries;
        });
    }

    public SteamPreEntry[] getSelectedEntries() {
        return selectedList;
    }

    private static class SteamAppsList<SteamPreEntry> extends SelectListPane {
        private ReadOnlyDoubleProperty prefRowWidth;

        public SteamAppsList(double prefHeight, ReadOnlyDoubleProperty prefRowWidth) {
            super(prefHeight, true);
            this.prefRowWidth = prefRowWidth;

        }

        @Override
        protected ListItem createListItem(Object value) {
            SteamAppItem item = new SteamAppItem(value,this);
            item.prefWidthProperty().bind(prefRowWidth);
            data.game.scrapper.SteamPreEntry[] ignoredSteamApps = GENERAL_SETTINGS.getSteamAppsIgnored();
            for (data.game.scrapper.SteamPreEntry steamPreEntry : ignoredSteamApps) {
                if (((data.game.scrapper.SteamPreEntry) item.getValue()).getId() == steamPreEntry.getId()) {
                    item.setSelected(true);
                    //item.getRadioButton().fire();
                    Main.LOGGER.debug(getSelectedValue());

                    break;
                }
            }
            return item;
        }
    }

    private static class SteamAppItem extends SelectListPane.ListItem {
        private String name;
        private int steam_id;
        private final static int IMAGE_WIDTH = 100;
        private final static int IMAGE_HEIGHT = IMAGE_WIDTH *45 /120;
        private StackPane coverPane = new StackPane();
        private ImageView coverView = new ImageView();

        public SteamAppItem(Object value, SelectListPane parentList) {
            super(value,parentList);

            name = ((SteamPreEntry) value).getName();
            steam_id = ((SteamPreEntry) value).getId();

            addContent();
        }

        @Override
        protected void addContent() {
            ImageUtils.downloadSteamImageToCache(steam_id, ImageUtils.STEAM_TYPE_CAPSULE, ImageUtils.STEAM_SIZE_SMALL, new OnDLDoneHandler() {
                @Override
                public void run(File outputfile) {
                    boolean keepRatio = true;
                    try {
                        SimpleImageInfo imageInfo = new SimpleImageInfo(outputfile);
                        keepRatio = Math.abs(((double) imageInfo.getHeight() / imageInfo.getWidth()) - (double)IMAGE_HEIGHT/IMAGE_WIDTH) > 0.2;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    boolean finalKeepRatio = keepRatio;
                    Platform.runLater(() -> {
                        ImageUtils.transitionToImage(new Image("file:" + File.separator + File.separator + File.separator + outputfile.getAbsolutePath(), IMAGE_WIDTH,IMAGE_HEIGHT, finalKeepRatio, true),coverView);
                    });
                }
            });
            coverPane.getChildren().add(new ImageView(new Image("res/defaultImages/barTile120.jpg", IMAGE_WIDTH, IMAGE_WIDTH *45 /120, false, true)));
            coverPane.getChildren().add(coverView);
            GridPane.setMargin(coverPane, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 10 * Main.SCREEN_WIDTH / 1920));
            add(coverPane, columnCount++, 0);
            Label titleLabel = new Label(name);
            Label idLabel = new Label(", id:" + steam_id);

            GridPane.setMargin(titleLabel, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 10 * Main.SCREEN_WIDTH / 1920));
            GridPane.setMargin(idLabel, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 10 * Main.SCREEN_WIDTH / 1920));
            add(titleLabel, columnCount++, 0);
            //add(idLabel, columnCount++, 0);
        }

    }
}

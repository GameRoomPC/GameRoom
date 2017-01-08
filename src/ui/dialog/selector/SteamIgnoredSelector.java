package ui.dialog.selector;

import data.http.images.ImageUtils;
import data.game.scraper.OnDLDoneHandler;
import data.game.scraper.SteamPreEntry;
import data.http.SimpleImageInfo;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
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
import ui.dialog.GameRoomDialog;
import ui.pane.SelectListPane;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import static ui.Main.GENERAL_SETTINGS;

/**
 * Created by LM on 09/08/2016.
 */
public class SteamIgnoredSelector extends GameRoomDialog<ButtonType> {
    public final static int MODE_REMOVE_FROM_LIST = 0;
    public final static int MODE_ADD_TO_LIST = 1;
    private SteamPreEntry[] selectedList;
    private Label statusLabel;

    private SteamIgnoredSelector(ArrayList<SteamPreEntry> ownedSteamEntries) throws IOException {
        statusLabel = new Label(Main.getString("loading")+"...");
        rootStackPane.getChildren().add(statusLabel);
        Label titleLabel = new Label(Main.getString("select_steam_games_ignore"));
        titleLabel.setWrapText(true);
        titleLabel.setTooltip(new Tooltip(Main.getString("select_steam_games_ignore")));
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
        SteamAppsList list = new SteamAppsList();

        if(ownedSteamEntries!=null){
            list.addItems(ownedSteamEntries);
            statusLabel.setText(null);
        }else{
            Task<ArrayList<SteamPreEntry>> loadOwnedGames = new Task() {
                @Override
                protected Object call() throws Exception {
                    ArrayList<SteamPreEntry> preEntries = new ArrayList<>();
                    Collections.addAll(preEntries, GENERAL_SETTINGS.getSteamAppsIgnored());
                    return preEntries;
                }
            };
            loadOwnedGames.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent event) {
                    Platform.runLater(() -> {
                        statusLabel.setText(null);
                        list.addItems(loadOwnedGames.getValue());
                    });
                }
            });
            loadOwnedGames.setOnFailed(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent event) {
                    Platform.runLater(() -> {
                        statusLabel.setText(Main.getString("no_internet")+" ?");
                    });
                }
            });
            Thread th = new Thread(loadOwnedGames);
            th.setDaemon(true);
            th.start();
        }
        mainPane.setCenter(list);

        getDialogPane().getButtonTypes().addAll(
                new ButtonType(Main.getString("ok"), ButtonBar.ButtonData.OK_DONE)
                , new ButtonType(Main.getString("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE));

        setOnHiding(event -> {
            SteamPreEntry[] temp_entries = new SteamPreEntry[list.getSelectedValues().size()];
            for(int i = 0; i< temp_entries.length; i++){
                temp_entries[i] = (SteamPreEntry) list.getSelectedValues().get(i);
            }
            selectedList = temp_entries;
        });
    }
    public SteamIgnoredSelector() throws IOException {
        this(null);
    }

    public SteamPreEntry[] getSelectedEntries() {
        return selectedList;
    }

    private static class SteamAppsList<SteamPreEntry> extends SelectListPane {

        public SteamAppsList() {
            super(true);

        }

        @Override
        protected ListItem createListItem(Object value) {
            SteamAppItem item = new SteamAppItem(value,this);
            data.game.scraper.SteamPreEntry[] ignoredSteamApps = GENERAL_SETTINGS.getSteamAppsIgnored();
            for (data.game.scraper.SteamPreEntry steamPreEntry : ignoredSteamApps) {
                if (((data.game.scraper.SteamPreEntry) item.getValue()).getId() == steamPreEntry.getId()) {
                    item.setSelected(true);

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
                    } catch (IOException ignored) {
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

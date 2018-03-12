package com.gameroom.ui.dialog.selector;

import com.gameroom.data.game.GameWatcher;
import com.gameroom.data.game.entry.GameEntry;
import com.gameroom.data.game.entry.GameEntryUtils;
import com.gameroom.data.game.entry.Platform;
import com.gameroom.data.game.scanner.FolderGameScanner;
import com.gameroom.data.game.scraper.MSStoreScraper;
import com.gameroom.data.http.images.ImageUtils;
import com.gameroom.ui.Main;
import com.gameroom.ui.control.button.gamebutton.GameButton;
import com.gameroom.ui.dialog.GameRoomDialog;
import com.gameroom.ui.pane.SelectListPane;
import edu.umd.cs.findbugs.annotations.NonNull;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.gameroom.system.application.settings.GeneralSettings.settings;

/**
 * UI element used to select {@link com.gameroom.data.game.scraper.MSStoreScraper.MSStoreEntry}
 *
 * @author LM. Garret (admin@gameroom.me)
 * @date 22/02/2018.
 */
public class MSStoreAppSelector extends GameRoomDialog<ButtonType> {
    private ArrayList<MSStoreScraper.MSStoreEntry> selectedList = new ArrayList<>();
    private Label statusLabel;

    public MSStoreAppSelector() {
        statusLabel = new Label(Main.getString("loading") + "...");
        rootStackPane.getChildren().add(statusLabel);
        Label titleLabel = new Label(Main.getString("select_MSStore_apps"));
        titleLabel.setWrapText(true);
        titleLabel.setTooltip(new Tooltip(Main.getString("select_games_ignore")));
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

        MSStoreAppSelector.MSEntryList list = new MSStoreAppSelector.MSEntryList();
        Main.getExecutorService().submit(() -> {
            MSStoreScraper.getApps(msStoreEntry -> {
                if (msStoreEntry == null) {
                    return;
                }
                if (!GameEntryUtils.pathAlreadyIn(msStoreEntry.getStartCommand(), GameEntryUtils.ENTRIES_LIST)) {
                    Main.runAndWait(() -> {
                        list.addItem(msStoreEntry);
                        if(list.getListItems().size() > 0){
                            statusLabel.setText(null);
                        }
                    });
                }
            });
            if(list.getListItems().size() == 0){
                Main.runAndWait(() -> {
                    statusLabel.setText(Main.getString("error_loading_apps"));
                });
            }
        });

        statusLabel.setText(Main.getString("loading")+"...");

        mainPane.setCenter(list);
        list.setPrefWidth(mainPane.getPrefWidth());

        getDialogPane().getButtonTypes().addAll(
                new ButtonType(Main.getString("ok"), ButtonBar.ButtonData.OK_DONE)
                , new ButtonType(Main.getString("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE));

        setOnHiding(event -> {
            selectedList.addAll(list.getSelectedValues());
        });
    }

    public ArrayList<MSStoreScraper.MSStoreEntry> getSelectedEntries() {
        return selectedList;
    }

    private static class MSEntryList<T> extends SelectListPane {

        public MSEntryList() {
            super(true);

        }

        @Override
        protected ListItem createListItem(Object value) {
            return new MSStoreAppSelector.MSStoreAppItem(value, this);
        }
    }

    private static class MSStoreAppItem extends SelectListPane.ListItem {
        private MSStoreScraper.MSStoreEntry entry;
        private final static int IMAGE_WIDTH = 64;
        private final static int IMAGE_HEIGHT = 64;
        private StackPane coverPane = new StackPane();

        public MSStoreAppItem(@NonNull Object value, SelectListPane parentList) {
            super(value, parentList);
            entry = ((MSStoreScraper.MSStoreEntry) value);
            addContent();
        }

        @Override
        protected void addContent() {
            ImageView iconView = new ImageView();
            double scale = settings().getUIScale().getScale();
            iconView.setFitHeight(IMAGE_HEIGHT * scale);
            iconView.setFitWidth(IMAGE_WIDTH * scale);

            if (entry.getIconPath() != null) {
                File iconPath = new File(entry.getIconPath());
                if (iconPath.exists()) {
                    ImageUtils.transitionToCover(iconPath, IMAGE_WIDTH, IMAGE_HEIGHT, iconView);
                    iconView.setFitHeight(IMAGE_HEIGHT * scale);
                }
                //iconView.setImage(AppSelectorDialog.getIcon(new File(entry.getStartCommand())));
            }

            coverPane.getChildren().add(iconView);

            GridPane.setMargin(coverPane, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 10 * Main.SCREEN_WIDTH / 1920));
            add(coverPane, columnCount++, 0);

            Label titleLabel = new Label(entry.getName());
            titleLabel.setTooltip(new Tooltip(entry.getName()));

            GridPane.setMargin(titleLabel, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 10 * Main.SCREEN_WIDTH / 1920));
            add(titleLabel, columnCount++, 0);
        }

    }
}

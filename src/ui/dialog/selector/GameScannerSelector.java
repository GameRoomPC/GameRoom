package ui.dialog.selector;

import data.game.entry.Platform;
import data.game.scanner.ScannerProfile;
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
import ui.Main;
import ui.dialog.GameRoomDialog;
import ui.pane.SelectListPane;

import java.util.ArrayList;

/**
 * Created by LM on 04/01/2017.
 */
public class GameScannerSelector extends GameRoomDialog<ButtonType> {
    private ScannerProfile[] disabledScanners;

    public GameScannerSelector() {

        Label titleLabel = new Label(Main.getString("select_game_scanners_title"));
        titleLabel.setWrapText(true);
        titleLabel.setTooltip(new Tooltip(Main.getString("select_game_scanners_title")));
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

        GameScannerSelector.ScannerList list = new GameScannerSelector.ScannerList<>();
        list.addItems(ScannerProfile.values());

        mainPane.setPrefWidth(1.0 / 3.5 * Main.SCREEN_WIDTH);
        mainPane.setPrefHeight(1.0 / 2.2 * Main.SCREEN_HEIGHT);
        mainPane.setCenter(list);

        getDialogPane().getButtonTypes().addAll(
                new ButtonType(Main.getString("ok"), ButtonBar.ButtonData.OK_DONE));

        setOnHiding(event -> {
            ArrayList<ScannerProfile> profiles = list.getSelectedValues();
            disabledScanners = new ScannerProfile[profiles.size()];

            for (int i = 0; i < disabledScanners.length; i++) {
                disabledScanners[i] = profiles.get(i);
            }
        });

    }

    public ScannerProfile[] getDisabledScanners() {
        return disabledScanners;
    }

    private static class ScannerList<File> extends SelectListPane {

        public ScannerList() {
            super(true);
        }

        @Override
        protected ListItem createListItem(Object value) {
            GameScannerSelector.ScannerItem item = new GameScannerSelector.ScannerItem(value, this);
            if(((ScannerProfile)value).isEnabled()) {
                item.setSelected(true);
            }
            return item;
        }
    }

    private static class ScannerItem extends SelectListPane.ListItem {
        private ScannerProfile profile;
        private StackPane coverPane = new StackPane();
        private ImageView coverView = new ImageView();

        public ScannerItem(Object value, SelectListPane parentList) {
            super(value, parentList);
            profile = ((ScannerProfile) value);
            addContent();
        }

        @Override
        protected void addContent() {
            coverView.setFitWidth(Main.SCREEN_HEIGHT/35);
            coverView.setFitHeight(Main.SCREEN_HEIGHT/35);
            if(profile != null){
                Platform p = Platform.getFromId(profile.getPlatformId());
                if(p != null){
                    p.setCSSIcon(coverView,false);
                }
            }
            coverPane.getChildren().add(coverView);

            GridPane.setMargin(coverPane, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 10 * Main.SCREEN_WIDTH / 1920));
            add(coverPane, columnCount++, 0);
            Label titleLabel = new Label(profile.toString());

            GridPane.setMargin(titleLabel, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 10 * Main.SCREEN_WIDTH / 1920));
            add(titleLabel, columnCount++, 0);
        }
    }
}

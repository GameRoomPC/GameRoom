package ui.dialog.selector;

import data.game.scanner.FolderGameScanner;
import data.game.scanner.ScannerProfile;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.embed.swing.SwingFXUtils;
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
import sun.awt.shell.ShellFolder;
import ui.Main;
import ui.dialog.GameRoomDialog;
import ui.pane.SelectListPane;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by LM on 04/01/2017.
 */
public class GameScannerSelector extends GameRoomDialog<ButtonType> {
    private ScannerProfile[] disabledScanners;

    public GameScannerSelector() {

        Label titleLabel = new Label(Main.RESSOURCE_BUNDLE.getString("select_game_scanners_title"));
        titleLabel.setWrapText(true);
        titleLabel.setTooltip(new Tooltip(Main.RESSOURCE_BUNDLE.getString("select_game_scanners_title")));
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

        mainPane.setPrefWidth(Main.SCREEN_WIDTH * 1 / 4 * Main.SCREEN_WIDTH / 1920);
        mainPane.setPrefHeight(Main.SCREEN_HEIGHT * 1 / 2 * Main.SCREEN_HEIGHT / 1080);

        GameScannerSelector.ScannerList list = new GameScannerSelector.ScannerList<>(Main.SCREEN_HEIGHT / 3.0, mainPane.prefWidthProperty());
        list.addItems(ScannerProfile.values());

        mainPane.setCenter(list);

        getDialogPane().getButtonTypes().addAll(
                new ButtonType(Main.RESSOURCE_BUNDLE.getString("ok"), ButtonBar.ButtonData.OK_DONE));

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
        private ReadOnlyDoubleProperty prefRowWidth;

        public ScannerList(double prefHeight, ReadOnlyDoubleProperty prefRowWidth) {
            super(prefHeight, true);
            this.prefRowWidth = prefRowWidth;

        }

        @Override
        protected ListItem createListItem(Object value) {
            GameScannerSelector.ScannerItem item = new GameScannerSelector.ScannerItem(value, this);
            item.prefWidthProperty().bind(prefRowWidth);
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
            coverView.setId(profile.getIconCSSID());
            coverPane.getChildren().add(coverView);

            GridPane.setMargin(coverPane, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 10 * Main.SCREEN_WIDTH / 1920));
            add(coverPane, columnCount++, 0);
            Label titleLabel = new Label(profile.toString());

            GridPane.setMargin(titleLabel, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 10 * Main.SCREEN_WIDTH / 1920));
            add(titleLabel, columnCount++, 0);
        }
    }
}

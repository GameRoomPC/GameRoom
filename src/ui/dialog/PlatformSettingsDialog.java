package ui.dialog;

import data.game.entry.Emulator;
import data.game.entry.Platform;
import data.game.scraper.SteamProfile;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.ColorInput;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import ui.Main;
import ui.control.specific.FloatingSearchBar;
import ui.control.specific.SearchBar;
import ui.pane.platform.PlatformSettingsPane;

import java.util.Comparator;

import static system.application.settings.GeneralSettings.settings;

/**
 * @author LM. Garret (admin@gameroom.me)
 * @date 09/06/2017.
 */
public class PlatformSettingsDialog extends GameRoomDialog<ButtonType> {
    private PlatformSettingsPane currentCenterPane;

    public PlatformSettingsDialog() {
        this(null);
    }

    public PlatformSettingsDialog(Platform focusedPlatform){
        this(focusedPlatform,null);
    }

    public PlatformSettingsDialog(Platform focusedPlatform, ButtonType buttonType) {
        super();
        mainPane.getStyleClass().add("container");
        ButtonType okButton = new ButtonType(Main.getString("close"), ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().add(okButton);

        if(buttonType != null){
            getDialogPane().getButtonTypes().add(buttonType);
        }

        mainPane.setLeft(createLeftPane(focusedPlatform));
    }

    private Node createLeftPane(Platform focusedPlatform) {

        ListView<Platform> listView = new ListView<Platform>();
        ObservableList<Platform> items = FXCollections.observableArrayList (Platform.getEmulablePlatforms());
        listView.setItems(items);
        listView.getStyleClass().add("dark-list-view");

        listView.setCellFactory(param -> new ListCell<Platform>() {
            private ImageView imageView = new ImageView();
            @Override
            public void updateItem(Platform platform, boolean empty) {
                super.updateItem(platform, empty);
                if (empty || platform == null) {
                    imageView.setId("");
                    setText(null);
                    setGraphic(null);
                } else {
                    double width = 30*Main.SCREEN_WIDTH/1920;
                    double height =  30*Main.SCREEN_HEIGHT/1080;

                    imageView.setId(platform.getIconCSSId());
                    imageView.setFitWidth(width);
                    imageView.setFitHeight(height);
                    imageView.setSmooth(true);

                    setText(platform.getName());
                    setGraphic(imageView);
                }
                getStyleClass().add("dark-list-cell");
            }
        });
        listView.setEditable(false);
        listView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue != null) {
                currentCenterPane = new PlatformSettingsPane(newValue,getOwner());
                currentCenterPane.setMaxWidth(3 * settings().getWindowWidth() / 5.0);
                currentCenterPane.setPrefWidth(2.5 * settings().getWindowWidth() / 5.0);
                currentCenterPane.setPadding(new Insets(10 * Main.SCREEN_WIDTH / 1920,
                        20 * Main.SCREEN_HEIGHT / 1080,
                        10 * Main.SCREEN_WIDTH / 1920,
                        20 * Main.SCREEN_HEIGHT / 1080
                ));
                mainPane.setCenter(currentCenterPane);
            }
        });
        listView.getSelectionModel().select(focusedPlatform == null ? 0 : items.indexOf(focusedPlatform));

        SearchBar bar = new SearchBar((observable, oldValue, newValue) -> {
            listView.setItems(
                    items.filtered(platform -> platform.getName().trim().toLowerCase().contains(newValue.trim().toLowerCase()))
            );
            listView.refresh();
        });
        bar.setId("search-bar-embedded");

        VBox box = new VBox();
        box.setSpacing(5*Main.SCREEN_HEIGHT/1080);
        box.getChildren().addAll(bar,listView);
        box.setPadding(new Insets(10 * Main.SCREEN_WIDTH / 1920,
                0 * Main.SCREEN_HEIGHT / 1080,
                10 * Main.SCREEN_WIDTH / 1920,
                20 * Main.SCREEN_HEIGHT / 1080
        ));
        return box;
    }
}

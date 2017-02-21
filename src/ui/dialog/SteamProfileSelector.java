package ui.dialog;

import data.game.scraper.SteamProfile;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.util.StringConverter;
import system.application.settings.PredefinedSetting;
import ui.Main;

import java.util.List;

import static ui.Main.GENERAL_SETTINGS;
import static ui.Main.LOGGER;

/**
 * Created by LM on 21/02/2017.
 */
public class SteamProfileSelector extends GameRoomDialog<ButtonType> {
    private ComboBox<SteamProfile> comboBox;

    public SteamProfileSelector(List<SteamProfile> profiles) {
        super();
        mainPane.getStyleClass().add("container");
        ButtonType okButton = new ButtonType(Main.getString("close"), ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(okButton);

        mainPane.setCenter(createCenterPane(profiles));
        mainPane.setTop(createTopPane());
    }

    private Node createTopPane() {
        //TODO replace by a localized string
        Label titleLabel = new Label("Select a Steam account to scan :");
        titleLabel.setPadding(new Insets(20 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920));
        return titleLabel;
    }

    private Node createCenterPane(List<SteamProfile> profiles) {
        comboBox = new ComboBox<>();
        comboBox.getItems().addAll(profiles);

        comboBox.setConverter(new StringConverter<SteamProfile>() {
            @Override
            public String toString(SteamProfile object) {
                return object.getAccountName();
            }

            @Override
            public SteamProfile fromString(String string) {
                return null;
            }
        });

        SteamProfile selectedProfile = GENERAL_SETTINGS.getSteamProfileToScan();
        if(selectedProfile != null && profiles.contains(selectedProfile)){
            comboBox.setValue(selectedProfile);
        }else if(profiles.size() > 1){
            comboBox.setValue(profiles.get(0));
            GENERAL_SETTINGS.setSettingValue(PredefinedSetting.STEAM_PROFILE,profiles.get(0));
        }

        comboBox.setOnAction(event -> {
            GENERAL_SETTINGS.setSettingValue(PredefinedSetting.STEAM_PROFILE,comboBox.getValue());
        });
        return comboBox;
    }
}

package com.gameroom.ui.dialog;

import com.gameroom.data.game.scraper.SteamProfile;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.util.StringConverter;
import com.gameroom.system.application.settings.PredefinedSetting;
import com.gameroom.ui.Main;

import java.util.List;

import static com.gameroom.system.application.settings.GeneralSettings.settings;

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
        Label titleLabel = new Label(Main.getString("select_steam_profile"));
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

        SteamProfile selectedProfile = settings().getSteamProfileToScan();
        if(selectedProfile != null && profiles.contains(selectedProfile)){
            comboBox.setValue(selectedProfile);
        }else if(profiles.size() > 1){
            comboBox.setValue(profiles.get(0));
            settings().setSettingValue(PredefinedSetting.STEAM_PROFILE,profiles.get(0));
        }

        comboBox.setOnAction(event -> {
            settings().setSettingValue(PredefinedSetting.STEAM_PROFILE,comboBox.getValue());
        });
        return comboBox;
    }
}

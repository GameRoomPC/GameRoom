package ui.dialog;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import ui.Main;

import static system.application.settings.GeneralSettings.settings;

/**
 * Created by LM on 05/08/2016.
 */
public class ActivationKeyDialog extends GameRoomDialog{

    private String supporterKey = "";
    private TextField keyField = new TextField();

    public ActivationKeyDialog(){
        super();
        getDialogPane().setPrefWidth(Main.SCREEN_WIDTH*(1/3.0)*Main.SCREEN_WIDTH/1920);

        setTitle(Main.getString("supporter_key"));
        keyField.setPromptText(Main.getString("supporter_key_visit_gameroom_me"));
        keyField.textProperty().addListener((observable, oldValue, newValue) -> {
            supporterKey = newValue;
        });
        BorderPane.setMargin(keyField, new Insets(20 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920, 20 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920));

        initTopPane();
        mainPane.setCenter(keyField);

        ButtonType buyButton = new ButtonType(ui.Main.getString("more_infos")+"!", ButtonBar.ButtonData.LEFT);
        ButtonType cancelButton = new ButtonType(ui.Main.getString("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType activateButton = new ButtonType(Main.getString("activate"), ButtonBar.ButtonData.OK_DONE);

        getDialogPane().getButtonTypes().addAll(buyButton,cancelButton,activateButton);
    }

    private void initTopPane() {
        Label infoLabel = new Label(Main.getString("input_here_supporter_key")+" "+Main.getString("supporter_key_infos",settings().getSupporterKeyPrice()+"€"));
        infoLabel.setWrapText(true);
        BorderPane.setMargin(infoLabel, new Insets(20 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920, 20 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920));

        mainPane.setTop(infoLabel);
    }

    public String getSupporterKey() {
        return supporterKey;
    }
}

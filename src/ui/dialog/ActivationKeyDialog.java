package ui.dialog;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import ui.Main;

/**
 * Created by LM on 05/08/2016.
 */
public class ActivationKeyDialog extends Dialog{
    public static String BUY_ONE = "buy one";

    private String donationKey = "";
    private BorderPane mainPane;
    private TextField keyField = new TextField();

    public ActivationKeyDialog(){
        mainPane = new BorderPane();
        DialogPane dialogPane = new DialogPane();
        dialogPane.setContent(mainPane);
        dialogPane.setPrefWidth(Main.SCREEN_WIDTH*(1/3.0)*Main.SCREEN_WIDTH/1920);
        dialogPane.getStylesheets().add("res/flatterfx.css");
        dialogPane.getStyleClass().add("custom-choice-dialog");
        initStyle(StageStyle.UNDECORATED);
        initModality(Modality.APPLICATION_MODAL);
        setDialogPane(dialogPane);

        setTitle("Donation key");
        keyField.setPromptText(Main.RESSOURCE_BUNDLE.getString("donation_key_visit_gameroom_me"));
        keyField.textProperty().addListener((observable, oldValue, newValue) -> {
            donationKey = newValue;
        });
        BorderPane.setMargin(keyField, new Insets(20 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920, 20 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920));

        initTopPane();
        mainPane.setCenter(keyField);

        ButtonType buyButton = new ButtonType(ui.Main.RESSOURCE_BUNDLE.getString("donation_key_buy_one")+"!", ButtonBar.ButtonData.LEFT);
        ButtonType cancelButton = new ButtonType(ui.Main.RESSOURCE_BUNDLE.getString("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType activateButton = new ButtonType(Main.RESSOURCE_BUNDLE.getString("activate"), ButtonBar.ButtonData.OK_DONE);

        dialogPane.getButtonTypes().addAll(buyButton,cancelButton,activateButton);
    }

    private void initTopPane() {
        Label infoLabel = new Label(Main.RESSOURCE_BUNDLE.getString("donation_key_infos"));
        infoLabel.setWrapText(true);
        BorderPane.setMargin(infoLabel, new Insets(20 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920, 20 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920));

        mainPane.setTop(infoLabel);
    }

    public String getDonationKey() {
        return donationKey;
    }
}

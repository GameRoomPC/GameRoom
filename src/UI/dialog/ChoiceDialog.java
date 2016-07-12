package UI.dialog;

import UI.Main;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.StageStyle;


/**
 * Created by LM on 05/07/2016.
 */
public class ChoiceDialog extends Dialog<ButtonType> {
    private BorderPane mainPane;

    public ChoiceDialog(ChoiceDialogButton... choiceDialogButtons) {
        super();
        DialogPane dialogPane = new DialogPane();
        mainPane = new BorderPane();
        dialogPane.setContent(mainPane);
        dialogPane.getStylesheets().add("res/flatterfx.css");
        dialogPane.getStyleClass().add("custom-choice-dialog");
        initStyle(StageStyle.UNDECORATED);
        initModality(Modality.APPLICATION_MODAL);
        setDialogPane(dialogPane);

        VBox choicesBox = new VBox();
        choicesBox.setFillWidth(true);
        choicesBox.setSpacing(10*Main.SCREEN_HEIGHT /1080);
        choicesBox.getChildren().addAll(choiceDialogButtons);
        choicesBox.getStyleClass().add("vbox");
        for(ChoiceDialogButton db : choiceDialogButtons){
            db.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<Event>() {
                @Override
                public void handle(Event event) {
                    ChoiceDialog.this.setResult(db.getButtonType());
                    ChoiceDialog.this.close();
                }
            });
        }

        mainPane.setCenter(choicesBox);
        mainPane.getStyleClass().add("container");
        ButtonType cancelButton = new ButtonType(Main.RESSOURCE_BUNDLE.getString("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().addAll(cancelButton);

    }
    public void setHeader(String text){
        Label header = new Label(text);
        header.setStyle("-fx-font-size: 26.0px;");
        mainPane.setTop(header);
        BorderPane.setMargin(header, new Insets(20*Main.SCREEN_WIDTH /1920, 20*Main.SCREEN_HEIGHT /1080,40*Main.SCREEN_WIDTH /1920, 20*Main.SCREEN_HEIGHT /1080));
    }


    public static class ChoiceDialogButton extends BorderPane{
        private Label titleLabel;
        private Label descriptionLabel;

        public ChoiceDialogButton(String title, String description){
            super();
            titleLabel = new Label(title);
            titleLabel.setStyle("-fx-font-family: 'Helvetica Neue';\n" +
                    "    -fx-font-size: 24.0px;\n" +
                    "    -fx-font-weight: 600;");
            descriptionLabel = new Label(description);
            descriptionLabel.setStyle("-fx-font-family: 'Helvetica Neue';\n" +
                    "    -fx-font-size: 16.0px;\n" +
                    "    -fx-font-weight: 600;");
            descriptionLabel.setWrapText(true);
            BorderPane.setAlignment(descriptionLabel, Pos.CENTER_LEFT);
            getStyleClass().addAll(new String[]{"choice-button"});
            setTop(titleLabel);
            setCenter(descriptionLabel);
        }

        public ButtonType getButtonType() {
            return new ButtonType(titleLabel.getText(), ButtonBar.ButtonData.OK_DONE);
        }
    }
}

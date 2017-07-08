package ui.dialog;

import javafx.application.Platform;
import javafx.beans.NamedArg;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import system.application.settings.PredefinedSetting;
import system.device.GameController;
import ui.Main;
import ui.theme.ThemeUtils;

import java.util.Optional;

import static system.application.settings.GeneralSettings.settings;
import static ui.Main.LOGGER;
import static ui.Main.MAIN_SCENE;
import static ui.Main.gameController;

/**
 * Created by LM on 06/08/2016.
 */
public class GameRoomAlert extends Alert {
    public GameRoomAlert(AlertType alertType) {
        this(alertType, "");
    }

    public GameRoomAlert(@NamedArg("alertType") AlertType alertType, @NamedArg("contentText") String contentText, ButtonType... buttons) {
        super(alertType, contentText, buttons);

        setHeaderText(null);
        initStyle(StageStyle.UNDECORATED);
        ThemeUtils.applyCurrentTheme(this);
        getDialogPane().setStyle("-fx-font-size: " + Double.toString(settings().getUIScale().getFontSize()) + "px;");

        initOwner(MAIN_SCENE.getParentStage());
        initModality(Modality.WINDOW_MODAL);

        getButtonTypes().forEach(buttonType -> {
            getDialogPane().lookupButton(buttonType).focusedProperty().addListener((observable, oldValue, newValue) -> {
                WindowFocusManager.dialogFocusChanged(GameRoomDialog.isDialogFocused(this));
            });
        });

        EventHandler<KeyEvent> fireOnEnter = event -> {
            if (KeyCode.ENTER.equals(event.getCode())
                    && event.getTarget() instanceof Button) {
                ((Button) event.getTarget()).fire();
            }
        };

        getButtonTypes().stream()
                .map(getDialogPane()::lookupButton)
                .forEach(button ->
                        button.addEventHandler(
                                KeyEvent.KEY_PRESSED,
                                fireOnEnter
                        )
                );

    }

    public static ButtonType warning(String s) {
        return displayAlert(AlertType.WARNING, s);
    }

    public static ButtonType confirmation(String s) {
        return displayAlert(AlertType.CONFIRMATION, s);
    }

    public static ButtonType info(String s) {
        return displayAlert(AlertType.INFORMATION, s);
    }

    public static ButtonType error(String s) {
        return displayAlert(AlertType.ERROR, s);
    }

    public static ButtonType errorIGDB() {
        return error(Main.getString("error_igdb"));
    }

    private static ButtonType displayAlert(AlertType type, String s) {
        ButtonType[] buttonChosen = new ButtonType[1];
        Main.runAndWait(() -> {
            GameRoomAlert alert = new GameRoomAlert(type, s);
            Optional<ButtonType> result = alert.showAndWait();

            result.ifPresent(buttonType -> {
                buttonChosen[0] = buttonType;
            });
        });
        return buttonChosen[0];
    }

}

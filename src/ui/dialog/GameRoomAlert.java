package ui.dialog;

import javafx.application.Platform;
import javafx.beans.NamedArg;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogEvent;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import ui.Main;
import ui.theme.ThemeUtils;

import java.util.Optional;

import static ui.Main.GENERAL_SETTINGS;
import static ui.Main.MAIN_SCENE;

/**
 * Created by LM on 06/08/2016.
 */
public class GameRoomAlert extends Alert {


    public GameRoomAlert(AlertType alertType){
        this(alertType,"");
    }
    public GameRoomAlert(@NamedArg("alertType") AlertType alertType, @NamedArg("contentText") String contentText, ButtonType... buttons) {
        super(alertType, contentText, buttons);

        setHeaderText(null);
        initStyle(StageStyle.UNDECORATED);
        ThemeUtils.applyCurrentTheme(this);
        getDialogPane().setStyle("-fx-font-size: "+Double.toString(GENERAL_SETTINGS.getUIScale().getFontSize())+"px;");

        initOwner(MAIN_SCENE.getParentStage());
        initModality(Modality.WINDOW_MODAL);
    }

    public static ButtonType warning(String s){
        return displayAlert(AlertType.WARNING, s);
    }

    public static ButtonType confirmation(String s){
        return displayAlert(AlertType.CONFIRMATION, s);
    }

    public static ButtonType info(String s){
        return displayAlert(AlertType.INFORMATION,s);
    }

    public static ButtonType error(String s){
        return displayAlert(AlertType.ERROR,s);
    }

    public static ButtonType errorIGDB(){
        return error(Main.getString("error_igdb"));
    }

    private static ButtonType displayAlert(AlertType type, String s){
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

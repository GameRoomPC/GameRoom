package ui.dialog;

import javafx.beans.NamedArg;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.StageStyle;

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
        getDialogPane().getStylesheets().add("res/flatterfx.css");
        initModality(Modality.APPLICATION_MODAL);
    }
}

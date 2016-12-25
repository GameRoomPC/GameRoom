package ui.dialog;

import javafx.beans.NamedArg;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogEvent;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import ui.theme.ThemeUtils;

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
}

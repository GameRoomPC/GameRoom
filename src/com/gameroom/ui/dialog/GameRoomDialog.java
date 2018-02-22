package com.gameroom.ui.dialog;

import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import com.gameroom.ui.theme.ThemeUtils;

import static com.gameroom.system.application.settings.GeneralSettings.settings;
import static com.gameroom.ui.Main.MAIN_SCENE;

/**
 * Created by LM on 06/08/2016.
 */
public abstract class GameRoomDialog<T> extends Dialog<T> {
    protected StackPane rootStackPane;
    protected BorderPane mainPane;

    protected GameRoomDialog(){
        this(Modality.WINDOW_MODAL);
    }

    protected GameRoomDialog(Modality modality){
        super();
        DialogPane dialogPane = new DialogPane();
        mainPane = new BorderPane();
        rootStackPane = new StackPane();
        rootStackPane.getChildren().add(mainPane);
        if(!modality.equals(Modality.NONE)) {
            if (MAIN_SCENE != null && MAIN_SCENE.getParentStage() != null){
                initOwner(MAIN_SCENE.getParentStage());
            }
        }
        dialogPane.setContent(rootStackPane);
        dialogPane.getStyleClass().add("dialog-pane");
        setDialogPane(dialogPane);
        ThemeUtils.applyCurrentTheme(dialogPane);
        dialogPane.setStyle("-fx-font-size: "+Double.toString(settings().getUIScale().getFontSize())+"px;");
        initStyle(StageStyle.UNDECORATED);
        initModality(modality);

        getDialogPane().getButtonTypes().forEach(buttonType -> {
            getDialogPane().lookupButton(buttonType).focusedProperty().addListener((observable, oldValue, newValue) -> {
                WindowFocusManager.dialogFocusChanged(isDialogFocused(this));
            });
        });
    }


    /** Checks if a given {@link Dialog} is focused, by checking if any of its composing {@link javafx.scene.Node} is focused.
     *
     * @param dialog dialog to check
     * @return true if the dialog is considered focused, false otherwise
     */
    static boolean isDialogFocused(Dialog dialog){
        if(dialog == null){
            return false;
        }
        if(dialog.getDialogPane() != null && dialog.getDialogPane().isFocused()){
            return true;
        }
        if(dialog.getGraphic()!=null && dialog.getGraphic().isFocused()){
            return true;
        }
        boolean focused[] = {false};
        if(dialog.getDialogPane() != null){
            dialog.getDialogPane().getButtonTypes().forEach(buttonType -> {
                focused[0] = focused[0] || dialog.getDialogPane().lookupButton(buttonType).isFocused();
            });
        }
        return focused[0];
    }

    public BorderPane getMainPane() {
        return mainPane;
    }

}

package ui.dialog;

import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import ui.theme.ThemeUtils;

import static system.application.settings.GeneralSettings.settings;
import static ui.Main.MAIN_SCENE;

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
    }
}

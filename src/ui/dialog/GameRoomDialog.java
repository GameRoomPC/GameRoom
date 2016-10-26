package ui.dialog;

import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import ui.theme.ThemeUtils;

import static ui.Main.MAIN_SCENE;

/**
 * Created by LM on 06/08/2016.
 */
abstract class GameRoomDialog<T> extends Dialog<T> {
    StackPane rootStackPane;
    BorderPane mainPane;

    GameRoomDialog(){
        super();
        DialogPane dialogPane = new DialogPane();
        mainPane = new BorderPane();
        rootStackPane = new StackPane();
        rootStackPane.getChildren().add(mainPane);
        if(MAIN_SCENE!=null && MAIN_SCENE.getParentStage()!=null)
            initOwner(MAIN_SCENE.getParentStage());
        dialogPane.setContent(rootStackPane);
        ThemeUtils.applyCurrentTheme(dialogPane);
        initStyle(StageStyle.UNDECORATED);
        initModality(Modality.WINDOW_MODAL);
        setDialogPane(dialogPane);
    }
}

package com.gameroom.ui.control.textfield;

import com.gameroom.ui.UIValues;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import com.gameroom.ui.Main;
import com.gameroom.ui.control.button.ImageButton;
import com.gameroom.ui.dialog.GameRoomAlert;
import com.gameroom.ui.dialog.selector.AppSelectorDialog;

import java.io.File;
import java.util.Optional;

import static com.gameroom.ui.Main.SCREEN_WIDTH;

/**
 * Created by LM on 05/01/2017.
 */
public class AppPathField extends PathTextField {
    public AppPathField(String initialPath, Window ownerWindow, int fileChooserCode, String fileChooserTitle, String[] extensions) {
        super(initialPath, ownerWindow, fileChooserCode, fileChooserTitle,extensions);

        double imgSize = 30 * SCREEN_WIDTH / 1920;
        ImageButton searchButton = new ImageButton("app-path-field-search-button", imgSize, imgSize);
        searchButton.setFocusTraversable(false);

        buttonsBox.getChildren().add(0, searchButton);

        searchButton.setOnAction(event -> {
            if(getTextField().getText() == null){
                GameRoomAlert.error(Main.getString("invalid_gamesFolder_exist"));
                return;
            }
            File file = new File(getTextField().getText());
            if (!file.exists()) {
                GameRoomAlert.error(Main.getString("invalid_gamesFolder_exist"));
            } else if (file.isDirectory()) {
                try {
                    AppSelectorDialog selector = new AppSelectorDialog(file,getExtensions());
                    selector.searchApps();
                    Optional<ButtonType> ignoredOptionnal = selector.showAndWait();
                    ignoredOptionnal.ifPresent(pairs -> {
                        if (pairs.getButtonData().equals(ButtonBar.ButtonData.OK_DONE) && selector.getSelectedFile() != null) {
                            getTextField().setText(selector.getSelectedFile().getAbsolutePath());
                        }
                    });
                } catch (IllegalArgumentException e) {
                    GameRoomAlert.warning(Main.getString("invalid_gamesFolder_is_no_folder"));
                }
            } else {
                GameRoomAlert.warning(Main.getString("invalid_gamesFolder_is_no_folder"));
            }
        });
    }
}

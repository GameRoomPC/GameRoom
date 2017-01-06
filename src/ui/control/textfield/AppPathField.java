package ui.control.textfield;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import ui.Main;
import ui.control.button.ImageButton;
import ui.dialog.GameRoomAlert;
import ui.dialog.selector.AppSelectorDialog;

import java.io.File;
import java.util.Optional;

import static ui.Main.SCREEN_WIDTH;

/**
 * Created by LM on 05/01/2017.
 */
public class AppPathField extends PathTextField {
    public AppPathField(String initialPath, Window ownerWindow, int fileChooserCode, String fileChooserTitle) {
        super(initialPath, ownerWindow, fileChooserCode, fileChooserTitle);

        double imgSize = 50 * SCREEN_WIDTH / 1920;
        ImageButton searchButton = new ImageButton("search-button", imgSize, imgSize);
        searchButton.setFocusTraversable(false);

        buttonsBox.getChildren().add(0, searchButton);

        searchButton.setOnAction(event -> {
            File file = new File(getTextField().getText());
            if (!file.exists()) {
                GameRoomAlert alert = new GameRoomAlert(Alert.AlertType.ERROR, Main.getString("invalid_gamesFolder_exist"));
                alert.showAndWait();
            } else if (file.isDirectory()) {
                try {
                    AppSelectorDialog selector = new AppSelectorDialog(file);
                    Optional<ButtonType> ignoredOptionnal = selector.showAndWait();
                    ignoredOptionnal.ifPresent(pairs -> {
                        if (pairs.getButtonData().equals(ButtonBar.ButtonData.OK_DONE)) {
                            getTextField().setText(selector.getSelectedFile().getAbsolutePath());
                        }
                    });
                } catch (IllegalArgumentException e) {
                    GameRoomAlert alert = new GameRoomAlert(Alert.AlertType.WARNING, Main.getString("invalid_gamesFolder_is_no_folder"));
                    alert.showAndWait();
                }
            } else {
                GameRoomAlert alert = new GameRoomAlert(Alert.AlertType.WARNING, Main.getString("invalid_gamesFolder_is_no_folder"));
                alert.showAndWait();
            }
        });
    }
}

package ui.control.textfield;

import ui.control.button.ImageButton;
import ui.scene.BaseScene;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.StageStyle;

import java.io.File;
import java.nio.file.Path;

import static ui.Main.*;

/**
 * Created by LM on 14/07/2016.
 */
public class PathTextField extends StackPane {
    private TextField field;
    private ImageButton button;
    private BaseScene parentScene;
    private Path initialPath;

    public PathTextField(Path initialPath, BaseScene parentScene){
        super();
        this.initialPath = initialPath;
        field = new TextField(initialPath.toString());
        Image folderImage = new Image("res/ui/folderButton.png", 50*SCREEN_WIDTH/1920, 50*SCREEN_HEIGHT/1080, false, true);
        button = new ImageButton(folderImage);
        button.setFocusTraversable(false);
        this.parentScene = parentScene;
        getChildren().addAll(field,button);
        StackPane.setAlignment(button, Pos.CENTER_RIGHT);
        //StackPane.setMargin(button, new Insets(0,field.getHeight()-0.9*field.getHeight(),0,0));

        button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setInitialDirectory(initialPath.getParent().toFile());
                fileChooser.setTitle(RESSOURCE_BUNDLE.getString("select_picture"));
                fileChooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("EXE", "*.exe"),
                        new FileChooser.ExtensionFilter("JAR", "*.jar"),
                        new FileChooser.ExtensionFilter("URL", "*.url")
                );
                try {
                    File selectedFile = fileChooser.showOpenDialog(parentScene.getParentStage());
                    if (selectedFile != null) {
                        field.setText(selectedFile.getAbsolutePath());
                    }
                }catch (NullPointerException ne){
                    ne.printStackTrace();
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setHeaderText(null);
                    alert.initStyle(StageStyle.UNDECORATED);
                    alert.getDialogPane().getStylesheets().add("res/flatterfx.css");
                    alert.initModality(Modality.APPLICATION_MODAL);
                    alert.setContentText(RESSOURCE_BUNDLE.getString("warning_internet_shortcut"));
                    alert.showAndWait();
                }
            }
        });
    }
    public void setText(String text){
        field.setText(text);
    }

    public TextField getTextField() {
        return field;
    }
}

package ui.control.textfield;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.stage.DirectoryChooser;
import ui.Main;
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
import java.util.ArrayList;
import java.util.Arrays;

import static ui.Main.*;

/**
 * Created by LM on 14/07/2016.
 */
public class PathTextField extends StackPane {
    public final static int FILE_CHOOSER_APPS = 0;
    public final static int FILE_CHOOSER_FOLDER = 1;

    private TextField field;
    private ImageButton button;
    private BaseScene parentScene;
    private String initialPath;

    public PathTextField(String initialPath, BaseScene parentScene, int fileChooserCode, String fileChooserTitle){
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
                try {
                    File initialDir = new File(initialPath);
                    switch (fileChooserCode){
                    case FILE_CHOOSER_APPS:
                        FileChooser fileChooser = new FileChooser();

                        if(initialPath.equals("")){
                            initialDir = new File(System.getProperty("user.home"));
                        }else{
                            initialDir = initialDir.getParentFile();
                        }

                        fileChooser.setInitialDirectory(initialDir);
                        fileChooser.setTitle(fileChooserTitle);
                        fileChooser.getExtensionFilters().addAll(
                                new FileChooser.ExtensionFilter("EXE", "*.exe"),
                                new FileChooser.ExtensionFilter("JAR", "*.jar")
                        );
                        File selectedFile = fileChooser.showOpenDialog(parentScene.getParentStage());
                        if (selectedFile != null) {
                            field.setText(selectedFile.getAbsolutePath());
                        }
                        break;
                    case FILE_CHOOSER_FOLDER:
                        DirectoryChooser folderChooser = new DirectoryChooser();
                        folderChooser.setTitle(fileChooserTitle);

                        if(initialPath.equals("")){
                            initialDir = new File(System.getProperty("user.home"));
                        }
                        folderChooser.setInitialDirectory(initialDir);
                        File selectedFolder = folderChooser.showDialog(parentScene.getParentStage());
                        if (selectedFolder != null) {
                            field.setText(selectedFolder.getAbsolutePath());
                        }
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

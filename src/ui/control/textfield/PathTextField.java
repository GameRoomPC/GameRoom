package ui.control.textfield;

import data.game.entry.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import ui.Main;
import ui.control.button.ImageButton;
import ui.dialog.GameRoomAlert;

import java.io.File;

import static ui.Main.SCREEN_WIDTH;

/**
 * Created by LM on 14/07/2016.
 */
public class PathTextField extends StackPane {
    private static Image DEFAULT_FOLDER_IMAGE;

    public final static int FILE_CHOOSER_APPS = 0;
    public final static int FILE_CHOOSER_FOLDER = 1;

    private TextField field;
    private ImageButton folderButton;
    protected HBox buttonsBox;
    private String initialPath;
    private String[] extensions;

    public PathTextField(String initialPath, Window ownerWindow, int fileChooserCode, String fileChooserTitle) {
        this(initialPath,ownerWindow,fileChooserCode,fileChooserTitle, Platform.PC.getSupportedExtensions());
    }

    public PathTextField(String initialPath, Window ownerWindow, int fileChooserCode, String fileChooserTitle, String[] extensions) {
        super();
        this.initialPath = initialPath;
        this.extensions = extensions;
        field = new TextField(initialPath);
        buttonsBox = new HBox(5 * Main.SCREEN_WIDTH / 1920);
        buttonsBox.setFocusTraversable(false);
        buttonsBox.setAlignment(Pos.CENTER_RIGHT);

        double imgSize = 50 * SCREEN_WIDTH / 1920;
        folderButton = new ImageButton("folder-button", imgSize, imgSize);
        folderButton.setFocusTraversable(false);
        buttonsBox.setPickOnBounds(false);
        buttonsBox.getChildren().add(folderButton);

        getChildren().addAll(field, buttonsBox);

        folderButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    String path = field.getText();
                    if(path == null || path.isEmpty()){
                        path = System.getProperty("user.home");
                    }
                    File initialDir = new File(path);
                    switch (fileChooserCode) {
                        case FILE_CHOOSER_APPS:
                            FileChooser fileChooser = new FileChooser();

                            while (!initialDir.exists() || !initialDir.isDirectory()) {
                                initialDir = initialDir.getParentFile();
                                if (initialDir == null) {
                                    initialDir = new File(System.getProperty("user.home"));
                                }
                            }

                            fileChooser.setInitialDirectory(initialDir);
                            fileChooser.setTitle(fileChooserTitle);
                            if(PathTextField.this.extensions != null){
                                for(String ext : PathTextField.this.extensions){
                                    if(ext != null && !ext.isEmpty()) {
                                        fileChooser.getExtensionFilters().add(0, new FileChooser.ExtensionFilter(ext.replace("*.","").toUpperCase(), ext.toLowerCase()));
                                    }
                                }
                            }
                            fileChooser.getExtensionFilters().add(0,new FileChooser.ExtensionFilter(Main.getString("supported_files"),PathTextField.this.extensions));
                            fileChooser.getExtensionFilters().add(1,new FileChooser.ExtensionFilter(Main.getString("all_files"),"*.*"));


                            File selectedFile = fileChooser.showOpenDialog(ownerWindow);

                            if (selectedFile != null) {
                                field.setText(selectedFile.getAbsolutePath());
                            }
                            break;
                        case FILE_CHOOSER_FOLDER:
                            DirectoryChooser folderChooser = new DirectoryChooser();
                            folderChooser.setTitle(fileChooserTitle);

                            if (initialPath == null || initialPath.equals("")) {
                                initialDir = new File(System.getProperty("user.home"));
                            }
                            folderChooser.setInitialDirectory(initialDir);

                            File selectedFolder = folderChooser.showDialog(ownerWindow);

                            if (selectedFolder != null) {
                                field.setText(selectedFolder.getAbsolutePath());
                            }
                    }


                } catch (NullPointerException ne) {
                    ne.printStackTrace();
                    GameRoomAlert.warning(Main.getString("warning_internet_shortcut"));
                }
            }
        });
    }

    public void setText(String text) {
        field.setText(text);
    }

    public TextField getTextField() {
        return field;
    }

    public ImageButton getFolderButton() {
        return folderButton;
    }

    public String[] getExtensions() {
        return extensions;
    }

    public void setExtensions(String[] extensions) {
        this.extensions = extensions;
    }
}

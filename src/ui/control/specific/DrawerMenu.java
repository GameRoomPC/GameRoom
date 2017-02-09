package ui.control.specific;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import ui.Main;
import ui.dialog.ChoiceDialog;
import ui.dialog.GameRoomAlert;
import ui.scene.GameEditScene;
import ui.scene.SettingsScene;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static ui.Main.*;

/**
 * Created by LM on 09/02/2017.
 */
public class DrawerMenu extends VBox {
    public static final double WIDTH_RATIO = 0.025;

    public DrawerMenu() {
        super();
        setMaxWidth(GENERAL_SETTINGS.getWindowWidth() * WIDTH_RATIO);
        setPrefWidth(GENERAL_SETTINGS.getWindowWidth() * WIDTH_RATIO);
        setFocusTraversable(false);
        //setEffect(new InnerShadow());
        setId("menu-bar");

        setOnMouseEntered(event -> {
            if (translateXProperty().getValue() != 0) {
                //TODO replace by smooth anim of translation
                translateXProperty().setValue(0);
                MAIN_SCENE.setTranslateBackgroundView(getWidth(), 0);
                setManaged(true);
            }
        });

        setOnMouseExited(event -> {
            if(event.getX() > 5){
                //TODO replace by smooth anim of translation
                translateXProperty().setValue( - (getWidth() - 5));
                MAIN_SCENE.setTranslateBackgroundView(0, 0);
                setManaged(false);
            }
        });
        init();
    }

    private void init() {
        initAddButton();
        initSortButton();

        initGroupButton();
        //initScaleSlider();
        initSettingsButton();
    }

    private void initAddButton() {
        DrawerButton addButton = new DrawerButton("main-add-button", this);
        addButton.setFocusTraversable(false);

        addButton.setOnAction(event -> {
            MAIN_SCENE.getRootStackPane().setMouseTransparent(true);
            ChoiceDialog choiceDialog = new ChoiceDialog(
                    new ChoiceDialog.ChoiceDialogButton(Main.getString("Add_exe"), Main.getString("add_exe_long")),
                    new ChoiceDialog.ChoiceDialogButton(Main.getString("Add_folder"), Main.getString("add_symlink_long"))
            );
            choiceDialog.setTitle(Main.getString("add_a_game"));
            choiceDialog.setHeader(Main.getString("choose_action"));

            Optional<ButtonType> result = choiceDialog.showAndWait();
            result.ifPresent(letter -> {
                if (letter.getText().equals(Main.getString("Add_exe"))) {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle(Main.getString("select_program"));
                    fileChooser.setInitialDirectory(
                            new File(System.getProperty("user.home"))
                    );
                    //TODO fix internet shorcuts problem (bug submitted)
                    fileChooser.getExtensionFilters().addAll(
                            new FileChooser.ExtensionFilter("EXE", "*.exe"),
                            new FileChooser.ExtensionFilter("JAR", "*.jar")
                    );
                    try {
                        File selectedFile = fileChooser.showOpenDialog(MAIN_SCENE.getParentStage());
                        if (selectedFile != null) {
                            MAIN_SCENE.fadeTransitionTo(new GameEditScene(MAIN_SCENE, selectedFile), MAIN_SCENE.getParentStage());
                        }
                    } catch (NullPointerException ne) {
                        ne.printStackTrace();
                        GameRoomAlert alert = new GameRoomAlert(Alert.AlertType.WARNING);
                        alert.setContentText(Main.getString("warning_internet_shortcut"));
                        alert.showAndWait();
                    }
                } else if (letter.getText().equals(Main.getString("Add_folder"))) {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle(Main.getString("Select_folder_ink"));
                    directoryChooser.setInitialDirectory(
                            new File(System.getProperty("user.home"))
                    );
                    File selectedFolder = directoryChooser.showDialog(MAIN_SCENE.getParentStage());
                    if (selectedFolder != null) {
                        ArrayList<File> files = new ArrayList<File>();
                        files.addAll(Arrays.asList(selectedFolder.listFiles()));
                        if (files.size() != 0) {
                            MAIN_SCENE.batchAddFolderEntries(files, 0).run();
                            //startMultiAddScenes(files);
                        }
                    }
                }
            });
            MAIN_SCENE.getRootStackPane().setMouseTransparent(false);
        });

        getChildren().add(addButton);
    }

    private void initSortButton() {
        DrawerButton sortButton = new DrawerButton("main-sort-button", this);
        sortButton.setFocusTraversable(false);

        getChildren().add(sortButton);
    }

    private void initGroupButton() {
        DrawerButton groupButton = new DrawerButton("main-group-button", this);
        groupButton.setFocusTraversable(false);

        getChildren().add(groupButton);
    }

    private void initSettingsButton() {
        DrawerButton settingsButton = new DrawerButton("main-settings-button", this);
        settingsButton.setFocusTraversable(false);
        settingsButton.setOnAction(event -> {
            long start = System.currentTimeMillis();
            SettingsScene settingsScene = new SettingsScene(new StackPane(), MAIN_SCENE.getParentStage(), MAIN_SCENE);
            LOGGER.debug("SettingsScene : init = " + (System.currentTimeMillis() - start) + "ms");
            MAIN_SCENE.fadeTransitionTo(settingsScene, MAIN_SCENE.getParentStage(), true);
        });

        getChildren().add(settingsButton);
    }


}

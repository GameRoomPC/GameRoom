package ui.control.specific;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import ui.Main;
import ui.dialog.ChoiceDialog;
import ui.dialog.GameRoomAlert;
import ui.scene.GameEditScene;
import ui.scene.MainScene;
import ui.scene.SettingsScene;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Executor;

import static ui.Main.*;

/**
 * Created by LM on 09/02/2017.
 */
public class DrawerMenu extends AnchorPane {
    public static double ANIMATION_TIME = 0.04;
    public static final double WIDTH_RATIO = 0.025;
    private Timeline openAnim;
    private Timeline closeAnim;
    private VBox topButtonsBox = new VBox();
    private VBox bottomButtonsBox = new VBox();


    public DrawerMenu(MainScene mainScene) {
        super();
        setMaxWidth(GENERAL_SETTINGS.getWindowWidth() * WIDTH_RATIO);
        setPrefWidth(GENERAL_SETTINGS.getWindowWidth() * WIDTH_RATIO);
        setFocusTraversable(false);
        //setEffect(new InnerShadow());
        setId("menu-bar");

        setOnMouseEntered(event -> {
            if (translateXProperty().getValue() != 0) {
                open(mainScene);
            }
        });

        setOnMouseExited(event -> {
            if (event.getX() > getWidth()) {
                close(mainScene);
            }
        });
        setCache(true);
        init(mainScene);
    }

    public void open(MainScene mainScene){
        setManaged(true);
        if(closeAnim != null) {
            closeAnim.stop();
        }
        openAnim = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(translateXProperty(), translateXProperty().getValue(), Interpolator.EASE_OUT)),
                new KeyFrame(Duration.seconds(ANIMATION_TIME),
                        new KeyValue(translateXProperty(), 0, Interpolator.EASE_OUT)
                ));
        openAnim.setCycleCount(1);
        openAnim.setAutoReverse(false);
        openAnim.play();
    }

    public void close(MainScene mainScene){
        if(openAnim != null){
            openAnim.stop();
        }
        closeAnim = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(translateXProperty(), translateXProperty().getValue(), Interpolator.EASE_OUT),
                        new KeyValue(mainScene.getScrollPane().translateXProperty(), mainScene.getBackgroundView().getTranslateX(), Interpolator.EASE_OUT),
                        new KeyValue(mainScene.getBackgroundView().translateXProperty(), mainScene.getBackgroundView().getTranslateX(), Interpolator.EASE_OUT)),
                new KeyFrame(Duration.seconds(ANIMATION_TIME),
                        new KeyValue(translateXProperty(), -getWidth() + 2, Interpolator.EASE_OUT),
                        new KeyValue(mainScene.getScrollPane().translateXProperty(), 0, Interpolator.EASE_OUT),
                        new KeyValue(mainScene.getBackgroundView().translateXProperty(), 0, Interpolator.EASE_OUT)
                ));
        closeAnim.setCycleCount(1);
        closeAnim.setAutoReverse(false);
        closeAnim.setOnFinished(event -> {
            setManaged(false);
        });
        closeAnim.play();
    }

    private void init(MainScene mainScene) {
        initAddButton(mainScene);
        initScanButton(mainScene);
        initSortButton(mainScene);

        initGroupButton(mainScene);
        //initScaleSlider();
        initSettingsButton(mainScene);

        setTopAnchor(topButtonsBox,20.0 * Main.SCREEN_HEIGHT / 1080);
        setBottomAnchor(bottomButtonsBox,20.0 * Main.SCREEN_HEIGHT / 1080);

        getChildren().addAll(topButtonsBox,bottomButtonsBox);
    }

    private void initAddButton(MainScene mainScene) {
        DrawerButton addButton = new DrawerButton("main-add-button", this);
        addButton.setFocusTraversable(false);

        addButton.setOnAction(event -> {
            mainScene.getRootStackPane().setMouseTransparent(true);
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
                        File selectedFile = fileChooser.showOpenDialog(mainScene.getParentStage());
                        if (selectedFile != null) {
                            mainScene.fadeTransitionTo(new GameEditScene(mainScene, selectedFile), mainScene.getParentStage());
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
                    File selectedFolder = directoryChooser.showDialog(mainScene.getParentStage());
                    if (selectedFolder != null) {
                        ArrayList<File> files = new ArrayList<File>();
                        files.addAll(Arrays.asList(selectedFolder.listFiles()));
                        if (files.size() != 0) {
                            mainScene.batchAddFolderEntries(files, 0).run();
                            //startMultiAddScenes(files);
                        }
                    }
                }
            });
            mainScene.getRootStackPane().setMouseTransparent(false);
        });

        topButtonsBox.getChildren().add(addButton);
    }

    private void initScanButton(MainScene mainScene) {
        ScanButton b = new ScanButton(this);

        topButtonsBox.getChildren().add(b);
    }

    private void initSortButton(MainScene mainScene) {
        DrawerButton sortButton = new DrawerButton("main-sort-button", this);
        sortButton.setFocusTraversable(false);

        topButtonsBox.getChildren().add(sortButton);
    }

    private void initGroupButton(MainScene mainScene) {
        DrawerButton groupButton = new DrawerButton("main-group-button", this);
        groupButton.setFocusTraversable(false);

        topButtonsBox.getChildren().add(groupButton);
    }

    private void initSettingsButton(MainScene mainScene) {
        DrawerButton settingsButton = new DrawerButton("main-settings-button", this);
        settingsButton.setFocusTraversable(false);
        settingsButton.setOnAction(event -> {
            long start = System.currentTimeMillis();
            SettingsScene settingsScene = new SettingsScene(new StackPane(), mainScene.getParentStage(), mainScene);
            LOGGER.debug("SettingsScene : init = " + (System.currentTimeMillis() - start) + "ms");
            mainScene.fadeTransitionTo(settingsScene, mainScene.getParentStage(), true);
        });

        bottomButtonsBox.getChildren().add(settingsButton);
    }


}

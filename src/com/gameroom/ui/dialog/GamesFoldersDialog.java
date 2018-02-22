package com.gameroom.ui.dialog;

import com.gameroom.data.game.GameFolderManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import com.gameroom.ui.Main;

import java.io.File;

import static com.gameroom.system.application.settings.GeneralSettings.settings;

/**
 * @author LM. Garret (admin@gameroom.me)
 * @date 19/07/2017.
 */
public class GamesFoldersDialog extends GameRoomDialog {
    private ListView<File> listView;
    private Button addButton;
    private Button deleteButton;

    public GamesFoldersDialog() {
        initTop();
        initCenter();
        initRight();
    }

    private void initCenter() {
        VBox vBox = new VBox();
        vBox.setSpacing(5*Main.SCREEN_HEIGHT/1080);


        listView = new ListView<File>();
        listView.setEditable(false);
        listView.setCellFactory(param -> new ListCell<File>() {
            @Override
            public void updateItem(File file, boolean empty) {
                super.updateItem(file, empty);
                if (empty || file == null) {
                    setText(null);
                } else {
                    setText(file.getAbsolutePath());
                }
            }
        });
        listView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                deleteButton.setDisable(true);
            } else {
                deleteButton.setDisable(false);
            }
        });
        listView.getItems().addAll(GameFolderManager.getPCFolders());
        //TODO fix empty window being white in basic theme

        mainPane.setPrefWidth(Main.SCREEN_WIDTH * 1 / 3 * Main.SCREEN_WIDTH / 1920);
        mainPane.setPrefHeight(Main.SCREEN_HEIGHT * 1 / 2 * Main.SCREEN_HEIGHT / 1080);

        Label descriptionLabel = new Label(Main.getString("games_folders_explanation"));
        descriptionLabel.setPrefHeight(200);
        descriptionLabel.setWrapText(true);
        vBox.getChildren().addAll(listView,descriptionLabel);

        mainPane.setCenter(vBox);
    }

    private void initTop(){
        StackPane topPane = new StackPane();
        topPane.getStyleClass().add("header");
        topPane.getStyleClass().add("small-header");

        Label titleLabel = new Label(Main.getString("games_folders"));
        titleLabel.getStyleClass().add("small-title-label");

        ImageView iconView = new ImageView();
        double width = 35 * Main.SCREEN_WIDTH / 1920 * settings().getUIScale().getScale();
        double height = 35 * Main.SCREEN_HEIGHT / 1080 * settings().getUIScale().getScale();
        iconView.setSmooth(true);
        iconView.setPreserveRatio(true);
        iconView.setFitWidth(width);
        iconView.setFitHeight(height);
        iconView.setId("folder-button");

        HBox box = new HBox();
        box.setSpacing(15 * Main.SCREEN_WIDTH / 1920);
        box.getChildren().addAll(iconView, titleLabel);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("title-box");
        box.setPickOnBounds(false);

        topPane.getChildren().add(box);
        StackPane.setAlignment(box, Pos.CENTER);
        StackPane.setMargin(box, new Insets(10 * Main.SCREEN_HEIGHT / 1080
                , 10 * Main.SCREEN_WIDTH / 1920
                , 25 * Main.SCREEN_HEIGHT / 1080
                , 10 * Main.SCREEN_WIDTH / 1920));
        mainPane.setTop(topPane);
    }

    private void initRight(){
        VBox buttonBox = new VBox();
        buttonBox.setSpacing(5*Main.SCREEN_HEIGHT/1080);
        buttonBox.setPadding(new Insets(10 * Main.SCREEN_WIDTH / 1920,
                0 * Main.SCREEN_HEIGHT / 1080,
                10 * Main.SCREEN_WIDTH / 1920,
                20 * Main.SCREEN_HEIGHT / 1080
        ));
        addButton = new Button(Main.getString("add"));
        addButton.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            File chosen = chooser.showDialog(getOwner());
            if(GameFolderManager.isFolderExcluded(chosen)){
                GameRoomAlert.error(Main.getString("sorry_cannot_add_games_folders",chosen.getAbsolutePath()));
                return;
            }
            if (chosen != null && GameFolderManager.addPCFolder(chosen)) {
                listView.getItems().add(chosen);
            } else {
                GameRoomAlert.error(Main.getString("error_could_not_add_games_folder"));
            }
        });
        deleteButton = new Button(Main.getString("delete"));
        deleteButton.setOnAction(event -> {
            if (listView.getSelectionModel().getSelectedItem() != null
                    && GameFolderManager.deletePCFolder(listView.getSelectionModel().getSelectedItem())) {
                listView.getItems().remove(listView.getSelectionModel().getSelectedIndex());
            } else {
                GameRoomAlert.error(Main.getString("error_could_not_remove_games_folder"));
            }
        });
        buttonBox.getChildren().addAll(addButton, deleteButton);

        mainPane.setRight(buttonBox);

        getDialogPane().getButtonTypes().addAll(new ButtonType(Main.getString("ok"), ButtonBar.ButtonData.OK_DONE));
    }
}

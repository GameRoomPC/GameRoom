package ui.dialog;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import ui.Main;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Optional;


/**
 * Created by LM on 11/08/2016.
 */
public class ConsoleOutputDialog extends GameRoomDialog {
    private TextArea textArea = new TextArea("");
    private boolean showing = false;

    public ConsoleOutputDialog(){
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPadding(new Insets(10 * Main.SCREEN_HEIGHT / 1080
                , 10 * Main.SCREEN_WIDTH / 1920
                , 10 * Main.SCREEN_HEIGHT / 1080
                , 10 * Main.SCREEN_WIDTH / 1920));
        /*textArea.setStyle("-fx-font-family: 'Helvetica Neue';\n" +
                "    -fx-background-color: derive(-dark, 20%);\n"+
                "    -fx-font-size: 16.0px;\n" +
                "    -fx-font-size: 16.0px;\n" +
                "    -fx-text-fill: -fx-text-base-color;\n"+
                "    -fx-font-weight: 600;");*/
        textArea.setId("console-text-area");
        mainPane.setTop(new javafx.scene.control.Label("Console"));
        ScrollPane pane = new ScrollPane();
        pane.setFitToWidth(true);
        pane.setFitToHeight(true);
        pane.setContent(textArea);
        mainPane.setPadding(new Insets(30 * Main.SCREEN_HEIGHT / 1080
                , 30 * Main.SCREEN_WIDTH / 1920
                , 20 * Main.SCREEN_HEIGHT / 1080
                , 30 * Main.SCREEN_WIDTH / 1920));
        BorderPane.setAlignment(mainPane.getTop(), Pos.CENTER);
        BorderPane.setMargin(mainPane.getTop(),new Insets(10 * Main.SCREEN_HEIGHT / 1080
                , 10 * Main.SCREEN_WIDTH / 1920
                , 10 * Main.SCREEN_HEIGHT / 1080
                , 10 * Main.SCREEN_WIDTH / 1920));

        mainPane.setPrefWidth(Main.SCREEN_WIDTH * 1 / 3 * Main.SCREEN_WIDTH / 1920);
        mainPane.setPrefHeight(Main.SCREEN_HEIGHT * 2 / 3 * Main.SCREEN_HEIGHT / 1080);

        mainPane.setCenter(pane);

        ButtonType openLog = new ButtonType(Main.RESSOURCE_BUNDLE.getString("open_log"), ButtonBar.ButtonData.LEFT);
        getDialogPane().getButtonTypes().addAll(new ButtonType(Main.RESSOURCE_BUNDLE.getString("ok"), ButtonBar.ButtonData.OK_DONE));
        getDialogPane().getButtonTypes().addAll(openLog);
        setOnHiding(event -> {
            textArea.setText("");
            showing = false;
        });
    }
    public void appendLine(String line){
        textArea.setText(textArea.getText()+"\n"+line);
    }
    public void showConsole(){
        if(!showing){
            showing = true;
            Optional<ButtonType> result = showAndWait();
            result.ifPresent(buttonType -> {
                if(buttonType.getText().equals(Main.RESSOURCE_BUNDLE.getString("open_log"))){
                    try {
                        Desktop.getDesktop().open(new File("log"+File.separator+"GameRoom.log"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}

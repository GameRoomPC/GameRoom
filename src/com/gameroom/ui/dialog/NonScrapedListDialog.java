package com.gameroom.ui.dialog;

import com.gameroom.ui.Main;
import com.gameroom.ui.UIValues;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

/**
 * Created by LM on 11/08/2016.
 */
public class NonScrapedListDialog extends GameRoomDialog<ButtonType> {
    private TextArea textArea = new TextArea("");

    public NonScrapedListDialog() {
        textArea.setEditable(false);
        //textArea.setWrapText(true);
        textArea.setPadding(UIValues.CONTROL_SMALL.insets());
        textArea.setId("console-text-area");

        mainPane.setTop(new javafx.scene.control.Label("The following games could not be scraped, please try again later."));
        ScrollPane pane = new ScrollPane();
        pane.setFitToWidth(true);
        pane.setFitToHeight(true);
        pane.setContent(textArea);
        mainPane.setPadding(new Insets(30 * Main.SCREEN_HEIGHT / 1080
                , 30 * Main.SCREEN_WIDTH / 1920
                , 20 * Main.SCREEN_HEIGHT / 1080
                , 30 * Main.SCREEN_WIDTH / 1920));
        BorderPane.setAlignment(mainPane.getTop(), Pos.CENTER);
        BorderPane.setMargin(mainPane.getTop(), new Insets(10 * Main.SCREEN_HEIGHT / 1080
                , 10 * Main.SCREEN_WIDTH / 1920
                , 10 * Main.SCREEN_HEIGHT / 1080
                , 10 * Main.SCREEN_WIDTH / 1920));

        mainPane.setPrefWidth(Main.SCREEN_WIDTH * 1 / 3 * Main.SCREEN_WIDTH / 1920);
        mainPane.setPrefHeight(Main.SCREEN_HEIGHT * 2 / 3 * Main.SCREEN_HEIGHT / 1080);

        mainPane.setCenter(pane);

        getDialogPane().getButtonTypes().addAll(new ButtonType(Main.getString("ok"), ButtonBar.ButtonData.OK_DONE));
        setOnHiding(event -> textArea.setText(""));
    }

    public void appendLine(String line) {
        textArea.setText(textArea.getText() + "\n" + line);
    }
}
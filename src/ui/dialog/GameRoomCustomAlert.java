package ui.dialog;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import ui.Main;

/**
 * Created by LM on 15/09/2016.
 */
public class GameRoomCustomAlert extends GameRoomDialog {
    public void setBottom(Node content){
        mainPane.setBottom(content);
    }
    public void setCenter(Node text){
        mainPane.setCenter(text);
    }
    public void setPrefWidth(double height) {
        mainPane.setPrefWidth(height);
    }

        public void setPrefHeight(double height){
        mainPane.setPrefHeight(height);
    }
}

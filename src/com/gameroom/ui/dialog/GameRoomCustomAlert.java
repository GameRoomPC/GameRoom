package com.gameroom.ui.dialog;

import javafx.scene.Node;

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

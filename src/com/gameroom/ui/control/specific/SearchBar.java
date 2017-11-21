package com.gameroom.ui.control.specific;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import com.gameroom.ui.control.button.ImageButton;

import static com.gameroom.ui.Main.SCREEN_WIDTH;

/**
 * @author LM. Garret (admin@gameroom.me)
 * @date 09/06/2017.
 */
public class SearchBar extends HBox {
    private TextField searchField;
    private ImageButton searchButton;
    private double imgSize;

    public SearchBar(ChangeListener<String> changeListener){
        imgSize =20*SCREEN_WIDTH/1080;
        searchButton = new ImageButton("search-button", imgSize, imgSize);
        searchButton.setFocusTraversable(false);
        //searchButton.setPadding(new Insets(20*SCREEN_HEIGHT/1080,20*SCREEN_WIDTH/1080,20*SCREEN_HEIGHT/1080,20*SCREEN_WIDTH/1080));

        searchField = new TextField();
        searchField.setFocusTraversable(false);
        searchField.textProperty().addListener(changeListener);
        //searchField.setPadding(new Insets(10*SCREEN_HEIGHT/1080,10*SCREEN_WIDTH/1080,10*SCREEN_HEIGHT/1080,10*SCREEN_WIDTH/1080));

        getChildren().addAll(searchButton,searchField);

        setAlignment(Pos.BASELINE_CENTER);
        setFocusTraversable(false);
        setPickOnBounds(false);

        setHgrow(searchField, Priority.ALWAYS);
        setHgrow(searchButton,Priority.NEVER);
    }

    public void clearSearchField() {
        searchField.clear();
    }

    public TextField getSearchField() {
        return searchField;
    }

    public void show(){
        setVisible(true);
        setMouseTransparent(false);
    }

    public void hide(){
        setVisible(false);
        setMouseTransparent(true);
    }

    public double getImgSize() {
        return imgSize;
    }
}

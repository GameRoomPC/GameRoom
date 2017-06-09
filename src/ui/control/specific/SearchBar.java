package ui.control.specific;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import ui.control.button.ImageButton;

import static ui.Main.SCREEN_WIDTH;

/**
 * @author LM. Garret (admin@gameroom.me)
 * @date 09/06/2017.
 */
public class SearchBar extends HBox {
    private TextField searchField;
    private ImageButton searchButton;
    private double imgSize;

    public SearchBar(ChangeListener<String> changeListener){
        imgSize = SCREEN_WIDTH / 30;
        searchButton = new ImageButton("search-button", imgSize, imgSize);
        searchButton.setFocusTraversable(false);

        searchField = new TextField();
        searchField.setFocusTraversable(false);
        searchField.textProperty().addListener(changeListener);

        getChildren().addAll(searchButton,searchField);

        setAlignment(Pos.BASELINE_CENTER);
        setFocusTraversable(false);
        setPickOnBounds(false);
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

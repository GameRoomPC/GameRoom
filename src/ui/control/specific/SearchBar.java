package ui.control.specific;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import ui.control.button.ImageButton;

import static ui.Main.SCREEN_WIDTH;

/**
 * Created by LM on 10/02/2017.
 */
public class SearchBar extends HBox {
    private TextField searchField;
    private ImageButton searchButton;
    private ImageButton closeButton;

    public SearchBar(ChangeListener<String> changeListener){
        double imgSize = SCREEN_WIDTH / 30;
        searchButton = new ImageButton("search-button", imgSize, imgSize);
        searchButton.setFocusTraversable(false);

        closeButton = new ImageButton("toaddtile-ignore-button", imgSize/2,imgSize/2);
        closeButton.setFocusTraversable(false);
        closeButton.setOnAction(event -> {
            hide();
            clearSearchField();
        });

        searchField = new TextField();
        searchField.setFocusTraversable(false);
        searchField.textProperty().addListener(changeListener);

        getChildren().addAll(searchButton,searchField,closeButton);

        setId("search-bar");
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
}

package com.gameroom.ui.control.specific;

import com.gameroom.ui.UIValues;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import com.gameroom.ui.control.button.ImageButton;

import static com.gameroom.ui.Main.SCREEN_HEIGHT;
import static com.gameroom.ui.Main.SCREEN_WIDTH;

/**
 * Created by LM on 10/02/2017.
 */
public class FloatingSearchBar extends HBox {
    private SearchBar searchBar;
    private ImageButton closeButton;

    public FloatingSearchBar(ChangeListener<String> changeListener){
        searchBar = new SearchBar(changeListener);

        closeButton = new ImageButton("toaddtile-ignore-button", searchBar.getImgSize()/1.5, searchBar.getImgSize()/1.5);
        closeButton.setFocusTraversable(false);
        closeButton.setPadding(UIValues.CONTROL_SMALL.insets());
        closeButton.setOnAction(event -> {
            hide();
            clearSearchField();
        });

        getChildren().addAll(searchBar,closeButton);

        setId("search-bar");
        setAlignment(Pos.BASELINE_CENTER);
        setFocusTraversable(false);
        setPickOnBounds(false);

        searchBar.setPadding(UIValues.CONTROL_XSMALL.insets());

        //setPadding(new Insets(10*SCREEN_HEIGHT/1080,0*SCREEN_WIDTH/1920,10*SCREEN_HEIGHT/1080,0*SCREEN_WIDTH/1920));

    }

    public void clearSearchField() {
        searchBar.clearSearchField();
    }

    public TextField getSearchField() {
        return searchBar.getSearchField();
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

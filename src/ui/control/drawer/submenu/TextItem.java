package ui.control.drawer.submenu;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import ui.Main;

/**
 * Created by LM on 10/02/2017.
 */
public class TextItem extends Button {
    private static PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");
    private BooleanProperty selected = new SimpleBooleanProperty(false);

    public TextItem(String textId){
        super(Main.getString(textId));
        getStyleClass().remove("button");
        getStyleClass().add("text-item");
        selected.addListener(e -> pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, selected.get()));
    }


    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }
}

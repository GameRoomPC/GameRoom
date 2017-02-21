package ui.control.drawer;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.scene.control.Button;
import ui.control.button.ImageButton;

/**
 * Created by LM on 09/02/2017.
 */
public class DrawerButton extends ImageButton {
    private boolean selectionable = false;
    private static PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("submenu-opened");
    private BooleanProperty selected = new SimpleBooleanProperty(false);

    public DrawerButton(String cssId, DrawerMenu parentMenu) {
        super(cssId, parentMenu.getPrefWidth(), parentMenu.getPrefWidth());
        getStyleClass().add("drawer-button");
        setFocusTraversable(false);
        selected.addListener(e -> pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, selected.get()));

        parentMenu.prefWidthProperty().addListener((observable, oldValue, newValue) -> {
            setFitWidth(newValue.doubleValue());
            setFitHeight(newValue.doubleValue());
        });
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    public boolean isSelectionable() {
        return selectionable;
    }

    public void setSelectionable(boolean selectionable) {
        this.selectionable = selectionable;
    }
}

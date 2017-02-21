package ui.control.drawer.submenu;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;

/**
 * Created by LM on 10/02/2017.
 */
public interface SelectableItem {

    public void setSelected(boolean b);
    public boolean isSelected();
}

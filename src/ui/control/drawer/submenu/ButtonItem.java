package ui.control.drawer.submenu;

import javafx.scene.control.Button;
import ui.Main;

/**
 * Created by LM on 10/02/2017.
 */
public class ButtonItem extends Button {

    public ButtonItem(String textId){
        super(Main.getString(textId));
        getStyleClass().add("button-drawer-item");
        setFocusTraversable(false);
    }
}

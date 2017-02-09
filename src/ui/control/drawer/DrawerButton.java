package ui.control.drawer;

import ui.control.button.ImageButton;

/**
 * Created by LM on 09/02/2017.
 */
public class DrawerButton extends ImageButton {

    public DrawerButton(String cssId, DrawerMenu parentMenu) {
        super(cssId, parentMenu.getPrefWidth(), parentMenu.getPrefWidth());
    }
}

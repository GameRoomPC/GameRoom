package ui.control.drawer.submenu;

import javafx.scene.control.CheckBox;
import ui.Main;

/**
 * Created by LM on 10/02/2017.
 */
public class CheckBoxItem extends CheckBox{

    public CheckBoxItem(String textId){
        super(Main.getString(textId));
        //getStyleClass().remove("check-box");
        getStyleClass().add("checkbox-item");
        setFocusTraversable(false);
    }
}

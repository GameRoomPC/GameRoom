package com.gameroom.ui.control.drawer.submenu;

import javafx.scene.control.CheckBox;
import com.gameroom.ui.Main;

/**
 * Created by LM on 10/02/2017.
 */
public class CheckBoxItem extends CheckBox{

    public CheckBoxItem(String text, boolean isStringId){
        super(isStringId ? Main.getString(text) : text);
        //getStyleClass().remove("check-box");
        getStyleClass().add("checkbox-drawer-item");
        setFocusTraversable(false);
    }
}

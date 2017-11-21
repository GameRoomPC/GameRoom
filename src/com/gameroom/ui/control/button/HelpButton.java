package com.gameroom.ui.control.button;

import javafx.scene.control.Tooltip;
import com.gameroom.ui.dialog.GameRoomAlert;

import static com.gameroom.system.application.settings.GeneralSettings.settings;

/** This displays a question mark icon that holds a given tooltip
 *
 * @author LM. Garret (admin@gameroom.me)
 * @date 09/06/2017.
 */
public class HelpButton extends ImageButton {
    public HelpButton(String explanation) {
        this(explanation,false);
    }

    public HelpButton(String explanation, boolean inDialog){
        super("help-icon", 22*settings().getUIScale().getScale(), 22*settings().getUIScale().getScale());
        if(inDialog){
            setOnAction(event -> GameRoomAlert.info(explanation));
        }else{
            setTooltip(new Tooltip(explanation));
        }
    }
}

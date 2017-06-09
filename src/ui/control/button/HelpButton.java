package ui.control.button;

import javafx.scene.control.Tooltip;

import static system.application.settings.GeneralSettings.settings;

/** This displays a question mark icon that holds a given tooltip
 *
 * @author LM. Garret (admin@gameroom.me)
 * @date 09/06/2017.
 */
public class HelpButton extends ImageButton {
    public HelpButton(String explanation) {
        super("help-icon", 22*settings().getUIScale().getScale(), 22*settings().getUIScale().getScale());
        setTooltip(new Tooltip(explanation));
    }
}

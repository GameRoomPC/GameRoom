package system.device;

import net.java.games.input.Event;

/**
 * Created by LM on 26/07/2016.
 */
public interface ControllerButtonListener {
    public void onButtonPressed(String buttonId);
    public void onButtonReleased(String buttonId);
}

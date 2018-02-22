package com.gameroom.system.device;

/**
 * Created by LM on 26/07/2016.
 */
public interface ControllerButtonListener {
    void onButtonPressed(String buttonId);
    void onButtonReleased(String buttonId);
}

package system.device;

import ui.Main;

/**
 * Created by LM on 26/07/2016.
 */
public class XboxControllerTest {
    public static void main(String[] args) {
        XboxController c = new XboxController(new ControllerButtonListener() {
            @Override
            public void onButtonPressed(String buttonId) {
                switch (buttonId) {
                    case XboxController.BUTTON_A:
                        Main.logger.debug("A pressed");
                        break;
                    case XboxController.BUTTON_B:
                        Main.logger.debug("B pressed");
                        break;
                    case XboxController.BUTTON_X:
                        Main.logger.debug("X pressed");
                        break;
                    case XboxController.BUTTON_Y:
                        Main.logger.debug("Y pressed");
                        break;
                    case XboxController.BUTTON_DPAD_UP:
                        Main.logger.debug("DPAD up pressed");
                        break;
                    case XboxController.BUTTON_DPAD_LEFt:
                        Main.logger.debug("DPAD left pressed");
                        break;
                    case XboxController.BUTTON_DPAD_DOWN:
                        Main.logger.debug("DPAD down pressed");
                        break;
                    case XboxController.BUTTON_DPAD_RIGHT:
                        Main.logger.debug("DPAD right pressed");
                        break;
                    default:
                        break;
                }

            }

            @Override
            public void onButtonReleased(String buttonId) {

            }
        });
        while (true){
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

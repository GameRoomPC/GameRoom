package system.device;

import ui.Main;

/**
 * Created by LM on 26/07/2016.
 */
class XboxControllerTest {
    public static void main(String[] args) {
        GameController c = new GameController(new ControllerButtonListener() {
            @Override
            public void onButtonPressed(String buttonId) {
                switch (buttonId) {
                    case GameController.BUTTON_A:
                        Main.LOGGER.debug("A pressed");
                        break;
                    case GameController.BUTTON_B:
                        Main.LOGGER.debug("B pressed");
                        break;
                    case GameController.BUTTON_X:
                        Main.LOGGER.debug("X pressed");
                        break;
                    case GameController.BUTTON_Y:
                        Main.LOGGER.debug("Y pressed");
                        break;
                    case GameController.BUTTON_DPAD_UP:
                        Main.LOGGER.debug("DPAD up pressed");
                        break;
                    case GameController.BUTTON_DPAD_LEFT:
                        Main.LOGGER.debug("DPAD left pressed");
                        break;
                    case GameController.BUTTON_DPAD_DOWN:
                        Main.LOGGER.debug("DPAD down pressed");
                        break;
                    case GameController.BUTTON_DPAD_RIGHT:
                        Main.LOGGER.debug("DPAD right pressed");
                        break;
                    default:
                        break;
                }

            }

            @Override
            public void onButtonReleased(String buttonId) {

            }
        });
        //noinspection InfiniteLoopStatement
        while (true){
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

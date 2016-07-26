package system.device;

import net.java.games.input.*;
import ui.Main;

/**
 * Created by LM on 26/07/2016.
 */
public class XboxController {
    public final static String BUTTON_A = "0";
    public final static String BUTTON_B = "1";
    public final static String BUTTON_X = "2";
    public final static String BUTTON_Y = "3";
    public final static String BUTTON_L1 = "4";
    public final static String BUTTON_R1 = "5";
    public final static String BUTTON_SELECT = "6";
    public final static String BUTTON_L3 = "8";
    public final static String BUTTON_R3 = "9";
    public final static String BUTTON_DPAD_UP = "pov0.25";
    public final static String BUTTON_DPAD_RIGHT = "pov0.5";
    public final static String BUTTON_DPAD_DOWN = "pov0.75";
    public final static String BUTTON_DPAD_LEFt = "pov1.0";

    private Controller controller;
    private Component[] components;
    private ControllerButtonListener controllerButtonListener;


    public XboxController(ControllerButtonListener controllerButtonListener) {
        ControllerEnvironment controllerEnvironment = ControllerEnvironment.getDefaultEnvironment();
        Controller[] controllers = controllerEnvironment.getControllers();
        for (Controller controller : controllers) {
            if (controller.getType().equals(Controller.Type.GAMEPAD) && controller.getName().contains("XBOX 360")) {
                this.controller = controller;
                this.components = controller.getComponents();
                Main.logger.debug("Found gamepad [" + controller.getName() + "]");
                break;
            }
        }
        if(controller==null){
            return;
        }

        if (controller.poll()) {
            Main.logger.debug("Components");
            for (Component component : components) {
                Main.logger.debug("\t name=" + component.getName());
            }
        }
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                while (controller.poll()) {

                    EventQueue queue = controller.getEventQueue();
                    Event event = new Event();
                    while (queue.getNextEvent(event)) {
                        if (!event.getComponent().getName().contains("Rotation") && !event.getComponent().getName().contains("Axe")) {
                            Component comp = event.getComponent();
                            float value = event.getValue();
                            if(value > 0){
                                String id = comp.getIdentifier().toString();
                                if(id.equals("pov")){
                                    id += value;
                                }
                                controllerButtonListener.onButtonPressed(id);
                            }else{
                                controllerButtonListener.onButtonReleased(comp.getIdentifier().toString());
                            }
                        }

                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }

                }
            }
        });
        th.setDaemon(true);
        th.start();
    }
}
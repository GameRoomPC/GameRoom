package system.device;

import net.java.games.input.*;
import ui.Main;

import java.security.AccessController;
import java.security.PrivilegedAction;

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

    private Runnable pollingTask;
    private Runnable controllerDiscoverTask;

    private volatile boolean runThreads = false;

    public XboxController(ControllerButtonListener controllerButtonListener) {
        pollingTask = new Runnable() {
            @Override
            public void run() {
                while (controller.poll() && runThreads) {

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
                            e.printStackTrace();
                        }
                    }

                }
            }
        };

        //TODO call this thread again when gamepad disconnected
        controllerDiscoverTask = new Runnable() {
            @Override
            public void run() {

                Controller foundController = null;
                Component[] foundComponents = null;

                while (foundController == null && runThreads) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    ControllerEnvironment controllerEnvironment = ControllerEnvironment.getDefaultEnvironment();
                    Controller[] controllers = controllerEnvironment.getControllers();


                    for (Controller controller : controllers) {
                        if (controller.getType().equals(Controller.Type.GAMEPAD) && controller.getName().contains("XBOX 360")) {
                            foundController = controller;
                            foundComponents = controller.getComponents();
                            Main.logger.debug("Found gamepad [" + controller.getName() + "]");
                            break;
                        }
                    }
                }
                setController(foundController);
                setComponents(foundComponents);

                Thread th = new Thread(pollingTask);
                th.setDaemon(true);
                th.start();
            }
        };
    }
    private void setController(Controller controller){
        this.controller = controller;
    }
    private void setComponents(Component[] components){
        this.components = components;
    }

    /**
     * Fix windows 8 warnings by defining a working plugin
     */
    static {

        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                String os = System.getProperty("os.name", "").trim();
                if (os.startsWith("Windows 8") || os.startsWith("Windows 10")) {  // 8, 8.1 etc.

                    // disable default plugin lookup
                    System.setProperty("jinput.useDefaultPlugin", "false");

                    // set to same as windows 7 (tested for windows 8 and 8.1)
                    System.setProperty("net.java.games.input.plugins", "net.java.games.input.DirectAndRawInputEnvironmentPlugin");

                }
                return null;
            }
        });

    }
    public void stopThreads(){
        runThreads = false;
        Main.logger.debug("Stopping xbox controller threads");
    }
    public void startThreads(){
        runThreads = true;
        Main.logger.debug("Restarting xbox controller threads");
        Thread th = new Thread(controllerDiscoverTask);
        th.setDaemon(true);
        th.start();
    }

}
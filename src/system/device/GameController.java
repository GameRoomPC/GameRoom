package system.device;

import net.java.games.input.*;
import ui.GeneralToast;
import ui.Main;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.concurrent.*;

import static ui.Main.LOGGER;
import static ui.Main.MAIN_SCENE;

/**
 * Created by LM on 26/07/2016.
 */
public class GameController {
    private final static int POLL_RATE = 40;
    private final static int DISCOVER_RATE = 1000;
    private final static float AXIS_THRESHOLD = 0.99f;
    private final static long FIRST_NAV_DELAY = 400;

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
    public final static String BUTTON_DPAD_LEFT = "pov1.0";

    private volatile Controller controller;
    private volatile Component[] components;
    private volatile ControllerButtonListener controllerButtonListener;

    private Runnable pollingTask;
    private Runnable controllerDiscoverTask;

    private ScheduledFuture<?> pollingFuture;
    private ScheduledFuture<?> discoverFuture;

    private volatile boolean runThreads = true;

    private volatile float previousXValue = 0.0f;
    private volatile float previousYValue = 0.0f;
    private volatile boolean navKeyConsumed = false;
    private volatile long firstNavKeyEvent = 0;
    private volatile long lastNavKeyEvent = 0;

    private ScheduledThreadPoolExecutor threadPool = new ScheduledThreadPoolExecutor(1);

    public GameController(ControllerButtonListener controllerButtonListener) {
        this.controllerButtonListener = controllerButtonListener;
        threadPool.setRemoveOnCancelPolicy(true);

        pollingTask = () -> {
            //Main.LOGGER.debug("Starting polling task");
            boolean connected = false;
            if (getController() != null && (connected = getController().poll()) && runThreads && Main.KEEP_THREADS_RUNNING) {
                navKeyConsumed = false;
                EventQueue queue = getController().getEventQueue();
                Event event = new Event();
                while (queue.getNextEvent(event)) {
                    onDataPolled(event.getComponent(), event.getValue());
                }


                Arrays.stream(getController().getComponents())
                        .filter(component -> component.getName().contains("Axe") || component.getIdentifier().toString().contains("pov"))
                        .forEach(component -> {
                            float value = component.getPollData();
                            if ((System.currentTimeMillis() - lastNavKeyEvent > FIRST_NAV_DELAY) && !navKeyConsumed) {
                                onDataPolled(component, value);
                            }
                        });

            }
            if (!connected) {
                //means controller is disconnected and should look for an other
                LOGGER.debug("Controller disconnected: " + getController().getName());
                if (MAIN_SCENE != null) {
                    GeneralToast.displayToast(controller.getName() + " " + Main.getString("disconnected"), MAIN_SCENE.getParentStage());
                }
                setController(null);
                discoverFuture = threadPool.scheduleAtFixedRate(controllerDiscoverTask, 0, DISCOVER_RATE, TimeUnit.MILLISECONDS);
                if (pollingFuture != null) {
                    pollingFuture.cancel(true);
                }
            }
        };

        controllerDiscoverTask = () -> {
            if (controller == null && runThreads && Main.KEEP_THREADS_RUNNING) {
                ControllerEnvironment controllerEnvironment = new DirectAndRawInputEnvironmentPlugin();
                Controller[] controllers = controllerEnvironment.getControllers();

                //Main.LOGGER.info("Searching controllers...");

                for (Controller controller : controllers) {
                    //Main.LOGGER.info("Found controller : " + controller.getName());
                    //TODO let people choose, not have the first one working chosen
                    if (!controller.getName().equals("Keyboard")
                            && controller.getType().equals(Controller.Type.GAMEPAD)
                            && controller.poll()) {
                        LOGGER.info("Using controller : " + controller.getName());
                        if (MAIN_SCENE != null) {
                            GeneralToast.displayToast(controller.getName() + " " + Main.getString("connected"), MAIN_SCENE.getParentStage());
                        }
                        setController(controller);
                        setComponents(controller.getComponents());

                        if (runThreads && Main.KEEP_THREADS_RUNNING) {
                            pollingFuture = threadPool.scheduleAtFixedRate(pollingTask, 0, POLL_RATE, TimeUnit.MILLISECONDS);
                        }
                        if (discoverFuture != null) {
                            discoverFuture.cancel(true);
                        }
                        return;
                    }
                }
            }
        };
    }

    private void setController(Controller controller) {
        this.controller = controller;
    }

    private void setComponents(Component[] components) {
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

    public void pause() {
        if (pollingFuture != null) {
            pollingFuture.cancel(true);
        }
        if (discoverFuture != null) {
            discoverFuture.cancel(true);
        }
        runThreads = false;
        LOGGER.debug("Pausing controller service");
    }

    public void resume() {
        emptyQueue();
        LOGGER.debug("Resuming controller service");
        runThreads = true;
        if (controller != null) {
            //we have already found a controller
            pollingFuture = threadPool.scheduleAtFixedRate(pollingTask, 0, POLL_RATE, TimeUnit.MILLISECONDS);
        } else {
            discoverFuture = threadPool.scheduleAtFixedRate(controllerDiscoverTask, 0, DISCOVER_RATE, TimeUnit.MILLISECONDS);
        }
    }

    public void shutdown() {
        threadPool.shutdownNow();
    }

    private void emptyQueue() {
        if (controller != null) {
            controller.setEventQueueSize(0);
            controller.setEventQueueSize(5);
        }
    }

    public Controller getController() {
        return controller;
    }

    private void onDataPolled(Component component, float value) {
        String name = component.getName();
        String id = component.getIdentifier().toString();
        if (name.equals("Axe X") && !navKeyConsumed) {
            if (value > AXIS_THRESHOLD) {
                navKeyConsumed = true;
                lastNavKeyEvent = System.currentTimeMillis();
                controllerButtonListener.onButtonPressed(BUTTON_DPAD_RIGHT);
            } else if (value < -AXIS_THRESHOLD) {
                navKeyConsumed = true;
                lastNavKeyEvent = System.currentTimeMillis();
                controllerButtonListener.onButtonPressed(BUTTON_DPAD_LEFT);
            } else {
                controllerButtonListener.onButtonReleased("pov");
            }
        } else if (name.equals("Axe Y") && !navKeyConsumed) {
            if (value > AXIS_THRESHOLD) {
                navKeyConsumed = true;
                lastNavKeyEvent = System.currentTimeMillis();
                controllerButtonListener.onButtonPressed(BUTTON_DPAD_DOWN);
            } else if (value < -AXIS_THRESHOLD) {
                navKeyConsumed = true;
                lastNavKeyEvent = System.currentTimeMillis();
                controllerButtonListener.onButtonPressed(BUTTON_DPAD_UP);
            } else {
                controllerButtonListener.onButtonReleased("pov");
            }
        } else if (id.startsWith("pov") && !navKeyConsumed) {
            if (value > 0) {
                id += value;
                controllerButtonListener.onButtonPressed(id);
                navKeyConsumed = true;
                lastNavKeyEvent = System.currentTimeMillis();
            } else {
                controllerButtonListener.onButtonReleased(id);
            }
        } else if (!name.contains("Rotation")) {
            if (value > 0) {
                controllerButtonListener.onButtonPressed(id);
            } else {
                controllerButtonListener.onButtonReleased(id);
            }
        }
    }
}
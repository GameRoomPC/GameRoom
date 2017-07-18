package system.device;

import javafx.beans.property.*;
import javafx.concurrent.Task;
import net.java.games.input.*;
import system.SchedulableTask;
import system.application.settings.PredefinedSetting;
import ui.GeneralToast;
import ui.Main;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static system.application.settings.GeneralSettings.settings;
import static ui.Main.LOGGER;
import static ui.Main.MAIN_SCENE;

/**
 * Is a manager of the Controllers that can be used to interact with GameRoom. Basically, a first task is scheduled, the
 * {@link #controllerDiscoverTask}, which detects which compatible controllers can be used. It then selects the first one
 * that is compatible //TODO be able to select a controller.
 * After that is started a scheduled {@link #pollingTask} to read input from the controller and perform actions accordingly.
 * The user can hold a navigation key (which are either the navigation pad or the joystick axis) and the action will
 * be repeated.
 * <p>
 * As polling is used, it is advised to call {@link #pause()} whenever the app is not on the foreground, and then resume
 * calling {@link #resume()}.
 *
 * @author LM. Garret (admin@gameroom.me)
 * @date 26/07/2016.
 */
public class GameController {
    /**
     * The poll rate of the device. Higher values induce higher CPU usage
     */
    private final static int POLL_RATE = 40;

    /**
     * Poll rate to scan for connected controllers
     */
    private final static int DISCOVER_RATE = 1000;

    /**
     * Treshold after which a joystick is considered being used
     */
    private final static float AXIS_THRESHOLD = 0.99f;

    /**
     * Delay AND poll rate for the first navigation speed. Not really a poll rate, but defines after which amount of
     * time we should repeat the action associated to a button if the user is holding it.
     */
    private final static long FIRST_NAV_DELAY = 150;

    /**
     * Delay after which we use the second poll rate for the navigation.
     */
    private final static long SECOND_NAV_DELAY = 2400;

    /**
     * Second poll rate, thus second navigation speed.
     */
    private final static long SECOND_NAV_POLL_RATE = 2 * POLL_RATE;

    /**
     * Identifiers of the buttons on the controller
     */
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


    /**
     * Controller being used
     */
    private volatile Controller controller;

    /**
     * Controllers found when scanning
     */
    private SimpleObjectProperty<Controller[]> controllersProperty = new SimpleObjectProperty<>();

    /**
     * Listener which associates actions to the buttons of the controller
     */
    private volatile ControllerButtonListener controllerButtonListener;

    /**
     * Task scheduled every {@link #POLL_RATE}ms to detect input changes on the controller
     */
    private SchedulableTask<Void> pollingTask;

    /**
     * Task scheduled every {@link #DISCOVER_RATE}ms to detect connected controllers
     */
    private SchedulableTask<Controller[]> controllerDiscoverTask;


    /**
     * Used to stop polling tasks. More of a security, but tasks should be cancelled correctly when paused.
     */
    private volatile boolean paused = true;
    /**
     * Defines in the {@link #pollingTask} if we have already performed the action associated to a navigation key.
     */
    private volatile boolean navKeyConsumed = false;

    /**
     * Defines the very first time when {@link #continuousMode} has started
     */
    private volatile long firstNavKeyEvent = 0;

    /**
     * Defines the last time an action associated to a navigation key was performed
     */
    private volatile long lastNavKeyEvent = 0;

    /**
     * Defines if we are in continuous mode, which is basically when the user is holding a navigation key.
     */
    private volatile boolean continuousMode = false;

    /**
     * User holding joystick along the X axis
     */
    private volatile boolean continuousX = false;

    /**
     * User holding joystick along the Y axis
     */
    private volatile boolean continuousY = false;

    /**
     * User holding a nav pad key
     */
    private volatile boolean continuousPad = false;

    private ScheduledThreadPoolExecutor threadPool = new ScheduledThreadPoolExecutor(1);

    public GameController(ControllerButtonListener controllerButtonListener) {
        this.controllerButtonListener = controllerButtonListener;
        threadPool.setRemoveOnCancelPolicy(true);

        pollingTask = new SchedulableTask<Void>(0, POLL_RATE) {
            @Override
            protected Void execute() throws Exception {
                boolean connected = false;
                if (getController() != null && (connected = getController().poll()) && Main.KEEP_THREADS_RUNNING && !paused) {
                    navKeyConsumed = false;
                    EventQueue queue = getController().getEventQueue();
                    Event event = new Event();

                    /*********EVENT MODE *********/
                    //First we treat the events
                    while (queue.getNextEvent(event)) {
                        onDataPolled(event.getComponent(), event.getValue(), false);
                    }

                    /*****CONTINOUS MODE *******/
                    //Here we treat the continuous usage of buttons, i.e. if there are being held
                    Arrays.stream(getController().getComponents())
                            //we filter to get only joysticks and nav pad
                            .filter(component -> component.getName().contains("Axe") || component.getIdentifier().toString().contains("pov"))
                            .forEach(component -> {
                                float value = component.getPollData();

                                if (continuousMode && (System.currentTimeMillis() - firstNavKeyEvent > SECOND_NAV_DELAY)) {
                                    //second navigation speed here
                                    if ((System.currentTimeMillis() - lastNavKeyEvent > SECOND_NAV_POLL_RATE)) {
                                        onDataPolled(component, value, true);
                                        //is continous if a joystick or the nav pad is held
                                        continuousMode = continuousX || continuousY || continuousPad;
                                    }
                                } else if ((System.currentTimeMillis() - lastNavKeyEvent > FIRST_NAV_DELAY)) {
                                    //first speed navigation here
                                    boolean wasContinuous = continuousMode;
                                    onDataPolled(component, value, true);
                                    //is continous if a joystick or the nav pad is held
                                    continuousMode = continuousX || continuousY || continuousPad;
                                    if (!wasContinuous && continuousMode) {
                                        //first time we are in continous mode, record the current time
                                        firstNavKeyEvent = System.currentTimeMillis();
                                    }
                                }
                            });

                }
                if (!connected) {
                    //means controller is disconnected and should look for an other
                    LOGGER.debug("Controller disconnected: " + getController().getName());
                    if (MAIN_SCENE != null) {
                        GeneralToast.displayToast(controller.getName() + " " + Main.getString("disconnected"), MAIN_SCENE.getParentStage());
                    }
                    setController((Controller) null);
                }
                return null;
            }
        };

        controllerDiscoverTask = new SchedulableTask<Controller[]>(0, DISCOVER_RATE) {
            @Override
            protected Controller[] execute() throws Exception {
                if (Main.KEEP_THREADS_RUNNING && !paused) {
                    //LOGGER.info("Scanning controllers");
                    ControllerEnvironment controllerEnvironment = new DirectAndRawInputEnvironmentPlugin();
                    ArrayList<Controller> controllers = new ArrayList<>();
                    Arrays.stream(controllerEnvironment.getControllers())
                            .filter(controller1 -> !controller1.getName().equals("Keyboard")
                                    && controller1.getType().equals(Controller.Type.GAMEPAD)
                                    && controller1.poll()).forEach(controllers::add);
                    return controllers.toArray(new Controller[0]);
                }
                return new Controller[0];
            }
        };
        controllerDiscoverTask.setOnSucceeded(() -> {
            if(controllersProperty.get() != controllerDiscoverTask.getValue()) {
                controllersProperty.setValue(controllerDiscoverTask.getValue());
            }

            if (getController() != null && getController().poll()) {
                //no need to change of controller
                return;
            }
            String chosenController = settings().getString(PredefinedSetting.CHOSEN_CONTROLLER);

            int i = 0;
            int usedIndex = 0;
            for (Controller controller : controllersProperty.get()) {
                if (toPseudoUid(controller, i).equals(chosenController)) {
                    usedIndex = i;
                }
                i++;
            }
            if (controllersProperty.get().length > 0) {
                setController(controllersProperty.get()[usedIndex]);
                LOGGER.info("Using controller : " + controller.getName());
                if (MAIN_SCENE != null) {
                    GeneralToast.displayToast(controller.getName() + " " + Main.getString("connected"), MAIN_SCENE.getParentStage());
                }
            }
        });
    }

    /**
     * Computes some String tags for every Controller detected, and returns them
     *
     * @return an array containing pseudo ids of {@link Controller}
     */
    public String[] getControllers() {
        if (controllersProperty.get() == null) {
            return new String[0];
        }
        String[] ids = new String[controllersProperty.get().length];
        for (int i = 0; i < controllersProperty.get().length; i++) {
            ids[i] = toPseudoUid(controllersProperty.get()[i], i);
        }
        return ids;
    }

    public ReadOnlyObjectProperty<Controller[]> getControllersProperty() {
        return controllersProperty;
    }

    private static String toPseudoUid(Controller controller, int index) {
        if (controller == null) {
            return "";
        }
        return controller.getName() + " (" + index + ")";
    }

    private static Controller findFromPseudoId(String pseudoId, Controller[] controllers) {
        if (controllers == null || controllers.length == 0 || pseudoId == null || pseudoId.isEmpty()) {
            return null;
        }
        for (int i = 0; i < controllers.length; i++) {
            if (toPseudoUid(controllers[i], i).equals(pseudoId)) {
                return controllers[i];
            }
        }
        return null;
    }

    public void setController(String pseudoId) {
        setController(findFromPseudoId(pseudoId, controllersProperty.get()));
    }

    private void setController(Controller controller) {
        this.controller = controller;

        if (controller != null) {
            pollingTask.stop();
            if (Main.KEEP_THREADS_RUNNING) {
                pollingTask.scheduleOn(threadPool);
            }
        } else {
            pollingTask.stop();
        }
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

    /**
     * Pauses polling tasks used to interact with GameRoom using a controller. Decreases hugely the CPU usage,
     * should be called whenever the window loses focus, see {@link ui.dialog.WindowFocusManager#windowFocused}
     */
    public void pause() {
        pollingTask.stop();
        controllerDiscoverTask.stop();
        //LOGGER.debug("Pausing controller service");
        paused = true;
    }

    /**
     * Resumes polling tasks used to interact with GameRoom using a controller. Increases hugely the CPU usage,
     * should be called whenever the window gains focus, see {@link ui.dialog.WindowFocusManager#windowFocused}
     */
    public void resume() {
        emptyQueue();
        //LOGGER.debug("Resuming controller service");
        if (controller != null) {
            //we have already found a controller
            pollingTask.scheduleOn(threadPool);
        }
        controllerDiscoverTask.stop();
        controllerDiscoverTask.scheduleOn(threadPool);
        paused = false;
    }

    /**
     * Shuts down the threadPool used. Should be called when terminating the app.
     */
    public void shutdown() {
        threadPool.shutdownNow();
    }

    /**
     * Removes events from the event queue, so that there are no pending events to be treated.
     */
    private void emptyQueue() {
        if (controller != null) {
            controller.setEventQueueSize(0);
            controller.setEventQueueSize(5);
        }
    }

    /**
     * @return the controller being used
     */
    private Controller getController() {
        return controller;
    }

    /**
     * Called when polling occurs, this will use data from the {@link Component} of the {@link #controller} to perform
     * actions defined by the {@link #controllerButtonListener}.
     *
     * @param component  the component from where the data comes from
     * @param value      the value of the state of the component.
     * @param continuous if we are event based this is false, whereas if the user is maintaining the button (usually nav pad or
     *                   joysticks), this is true
     */
    private void onDataPolled(Component component, float value, boolean continuous) {
        String name = component.getName();
        String id = component.getIdentifier().toString();
        if (name.equals("Axe X")) {
            if (value > AXIS_THRESHOLD && !navKeyConsumed) {
                navKeyConsumed = true;
                continuousX = continuousX || continuous;
                lastNavKeyEvent = System.currentTimeMillis();
                controllerButtonListener.onButtonPressed(BUTTON_DPAD_RIGHT);
            } else if (value < -AXIS_THRESHOLD && !navKeyConsumed) {
                navKeyConsumed = true;
                continuousX = continuousX || continuous;
                lastNavKeyEvent = System.currentTimeMillis();
                controllerButtonListener.onButtonPressed(BUTTON_DPAD_LEFT);
            } else if (value >= -AXIS_THRESHOLD && value <= AXIS_THRESHOLD) {
                continuousX = false;
                controllerButtonListener.onButtonReleased("pov");
            }
        } else if (name.equals("Axe Y")) {
            if (value > AXIS_THRESHOLD && !navKeyConsumed) {
                navKeyConsumed = true;
                continuousY = continuousY || continuous;
                lastNavKeyEvent = System.currentTimeMillis();
                controllerButtonListener.onButtonPressed(BUTTON_DPAD_DOWN);
            } else if (value < -AXIS_THRESHOLD && !navKeyConsumed) {
                navKeyConsumed = true;
                continuousY = continuousY || continuous;
                lastNavKeyEvent = System.currentTimeMillis();
                controllerButtonListener.onButtonPressed(BUTTON_DPAD_UP);
            } else if (value >= -AXIS_THRESHOLD && value <= AXIS_THRESHOLD) {
                continuousY = false;
                controllerButtonListener.onButtonReleased("pov");
            }
        } else if (id.startsWith("pov")) {
            if (value > 0 && !navKeyConsumed) {
                id += value;
                controllerButtonListener.onButtonPressed(id);
                navKeyConsumed = true;
                continuousPad = continuousPad || continuous;
                lastNavKeyEvent = System.currentTimeMillis();
            } else if (value <= 0) {
                continuousPad = false;
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
package ui.control.specific;

import javafx.scene.control.Tooltip;
import javafx.stage.Window;
import ui.Main;
import ui.scene.BaseScene;
import ui.theme.ThemeUtils;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by LM on 05/01/2017.
 */
public class GeneralToast extends Tooltip {
    public final static int DURATION_LONG = 4000;
    public final static int DURATION_SHORT = 1000;

    private static volatile boolean ENABLED = true;
    private static volatile boolean WAITING_FOR_TOASTS = false;

    private final static LinkedBlockingQueue<GeneralToast> TOAST_QUEUE = new LinkedBlockingQueue<>();
    private static Thread DISPLAY_THREAD;
    private static volatile boolean CAN_INTERRUPT_TOAST = false;

    private int duration;
    private boolean interruptible = false;

    private GeneralToast(String text, Window window) {
        this(text, DURATION_LONG,window);
    }

    private GeneralToast(String text, int duration, Window window) {
        super(text);
        this.duration = duration;
        setAutoHide(false);
        setHideOnEscape(false);

        widthProperty().addListener((observable, oldValue, newValue) -> {
            if(window!=null){
                setAnchorX(window.getWidth() / 2 - (newValue.doubleValue() / 2.0));
            }
        });
        heightProperty().addListener((observable, oldValue, newValue) -> {
            if(window!=null) {
                setAnchorY(window.getHeight() - 2 * newValue.doubleValue());
            }
        });
    }

    public static void displayToast(String text, Window window) {
        displayToast(text, window, DURATION_LONG);
    }

    public static void displayToast(String text, Window window, int duration) {
        displayToast(text, window, duration, false);
    }

    public static void displayToast(String text, Window window, int duration, boolean interruptible) {
        GeneralToast toast = new GeneralToast(text, duration,window);
        toast.interruptible = interruptible;
        try {
            TOAST_QUEUE.put(toast);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (DISPLAY_THREAD == null) {
            DISPLAY_THREAD = new Thread(() -> {
                while (true) {
                    GeneralToast toast1;
                    while ((toast1 = TOAST_QUEUE.poll()) != null && ENABLED) { // does not block on empty list but returns null instead
                        toast1.showTimed(window);
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ignored) {

                        }
                    }
                    TOAST_QUEUE.clear();

                    try {
                        WAITING_FOR_TOASTS = true;
                        Thread.sleep(5 * 60 * 1000);
                    } catch (InterruptedException e) {
                        //toast has been added to the queue!
                    }
                    WAITING_FOR_TOASTS = false;
                }
            });

            DISPLAY_THREAD.setDaemon(true);
            DISPLAY_THREAD.setPriority(Thread.MIN_PRIORITY);
            DISPLAY_THREAD.start();
        }
        if (DISPLAY_THREAD.getState().equals(Thread.State.TIMED_WAITING) && CAN_INTERRUPT_TOAST) {
            DISPLAY_THREAD.interrupt();
        }
    }

    private void showTimed(Window window) {
        if (ENABLED) {
            CAN_INTERRUPT_TOAST = interruptible;
            Main.runAndWait(() -> {
                show(window);
            });
            try {
                Thread.sleep(duration);
            } catch (InterruptedException ignored) {

            }
            Main.runAndWait(this::hide);
            CAN_INTERRUPT_TOAST = true;
        }
    }

    public static void enableToasts(boolean enabled) {
        ENABLED = enabled;
        if((!enabled || WAITING_FOR_TOASTS) && DISPLAY_THREAD != null){
            DISPLAY_THREAD.interrupt();
        }
    }
}

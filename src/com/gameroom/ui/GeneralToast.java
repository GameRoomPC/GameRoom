package com.gameroom.ui;

import javafx.scene.control.Tooltip;
import javafx.stage.Stage;
import javafx.stage.Window;
import com.gameroom.system.application.settings.PredefinedSetting;

import java.util.concurrent.LinkedBlockingQueue;

import static com.gameroom.system.application.settings.GeneralSettings.settings;

/**
 * Created by LM on 05/01/2017.
 */
public class GeneralToast extends Tooltip {
    public final static int DURATION_LONG = 4000;
    public final static int DURATION_SHORT = 1000;

    private static volatile boolean ENABLED = false;
    private static volatile boolean WAITING_FOR_TOASTS = false;

    private static volatile LinkedBlockingQueue<GeneralToast> TOAST_QUEUE = new LinkedBlockingQueue<>();
    private static Thread DISPLAY_THREAD;

    private int duration;

    private GeneralToast(String text, int duration, Window window) {
        super(text);
        this.duration = duration;
        setAutoHide(false);
        setHideOnEscape(false);

        widthProperty().addListener((observable, oldValue, newValue) -> {
            if(window!=null){
                setAnchorX(window.getX() + window.getWidth() / 2 - (newValue.doubleValue() / 2.0));
            }
        });
        heightProperty().addListener((observable, oldValue, newValue) -> {
            if(window!=null) {
                setAnchorY(window.getY() + window.getHeight() - 2 * newValue.doubleValue());
            }
        });
    }

    public static void displayToast(String text, Stage window) {
        displayToast(text, window, DURATION_LONG);
    }


    public static void displayToast(String text, Stage window, int duration) {
        GeneralToast toast = new GeneralToast(text, duration,window);
        try {
            TOAST_QUEUE.put(toast);
        } catch (InterruptedException ignored) {
        }

        if (DISPLAY_THREAD == null) {
            DISPLAY_THREAD = new Thread(() -> {
                while (Main.KEEP_THREADS_RUNNING) {
                    GeneralToast toast1;
                    while ((toast1 = TOAST_QUEUE.poll()) != null && ENABLED) { // does not block on empty list but returns null instead
                        toast1.showTimed(window);
                        try {
                            Thread.sleep(50);
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
        if (DISPLAY_THREAD.getState().equals(Thread.State.TIMED_WAITING) ) {
            DISPLAY_THREAD.interrupt();
        }
    }

    private void showTimed(Window window) {
        if(!window.isShowing() || !window.isFocused()){
            return;
        }
        if (ENABLED && !settings().getBoolean(PredefinedSetting.NO_TOASTS)) {
            Main.runAndWait(() -> {
                show(window);
            });
            try {
                Thread.sleep(duration);
            } catch (InterruptedException ignored) {

            }
            Main.runAndWait(this::hide);
        }
    }

    public static void enableToasts(boolean enabled) {
        ENABLED = enabled;
        if((!enabled || WAITING_FOR_TOASTS) && DISPLAY_THREAD != null){
            DISPLAY_THREAD.interrupt();
        }
    }
}

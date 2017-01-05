package ui.control.specific;

import javafx.application.Platform;
import javafx.scene.control.Tooltip;
import ui.Main;
import ui.scene.BaseScene;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by LM on 05/01/2017.
 */
public class GeneralToast extends Tooltip {
    private final static int TOAST_DURATION = 3000;
    private final static LinkedBlockingQueue<GeneralToast> TOAST_QUEUE = new LinkedBlockingQueue<>();    private static Thread DISPLAY_THREAD;
    private static volatile boolean DISPLAYING_A_TOAST = false;

    private int duration;
    private boolean alreadyDisplayed = false;

    private GeneralToast(String text) {
        this(text, TOAST_DURATION);
    }

    private GeneralToast(String text, int duration) {
        super(text);
        this.duration = duration;
        setAutoHide(false);
        widthProperty().addListener((observable, oldValue, newValue) -> {
            setAnchorX(Main.SCREEN_WIDTH / 2 - (newValue.doubleValue() / 2.0));
        });
        heightProperty().addListener((observable, oldValue, newValue) -> {
            setAnchorY(Main.SCREEN_HEIGHT - 2 * newValue.doubleValue());
        });
    }

    public static void displayToast(String text, BaseScene scene) {
        GeneralToast toast = new GeneralToast(text);
        try {
            TOAST_QUEUE.put(toast);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(DISPLAY_THREAD == null){
            DISPLAY_THREAD = new Thread(() -> {
                while(true) {
                    Iterator<GeneralToast> iterator = TOAST_QUEUE.iterator();

                    GeneralToast toast1;
                    while ((toast1 = TOAST_QUEUE.poll()) != null) { // does not block on empty list but returns null instead
                        toast1.showTimed(scene);
                    }
                    TOAST_QUEUE.clear();

                    try {
                        Thread.sleep(5 * 60 * 1000);
                    } catch (InterruptedException e) {
                        //toast has been added to the queue!
                    }
                }
            });
            DISPLAY_THREAD.setDaemon(true);
            DISPLAY_THREAD.setPriority(Thread.MIN_PRIORITY);
            DISPLAY_THREAD.start();
        }
        if(DISPLAY_THREAD.getState().equals(Thread.State.TIMED_WAITING) && !DISPLAYING_A_TOAST){
            DISPLAY_THREAD.interrupt();
        }
    }

    private void showTimed(BaseScene scene) {
        if (scene.getParentStage().isFocused()) {
            DISPLAYING_A_TOAST = true;
            Main.runAndWait(() -> {
                show(scene.getParentStage());
            });
            try {
                Thread.sleep(duration);
            } catch (InterruptedException e) {

            }
            Main.runAndWait(() -> {
                hide();
            });
            alreadyDisplayed = true;
            DISPLAYING_A_TOAST = false;
        }
    }
}

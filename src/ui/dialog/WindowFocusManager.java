package ui.dialog;

import javafx.scene.control.Dialog;
import javafx.stage.Stage;
import system.application.settings.PredefinedSetting;
import ui.GeneralToast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static system.application.settings.GeneralSettings.settings;
import static ui.Main.LOGGER;
import static ui.Main.gameController;

/** This class is a helper to determine whether the GameRoom app is being focused or not. This solves the issue encountered
 * and described here https://stackoverflow.com/questions/44973129/javafx-alert-and-stage-focus
 *
 * Basically we compare the {@link Stage#focusedProperty()} against the focus computed for {@link javafx.scene.control.Dialog}
 * and {@link javafx.scene.control.Alert} using the {@link GameRoomDialog#isDialogFocused(Dialog)}.
 *
 * Thus the app/window is considered to be focused if there is a {@link Dialog} focused or if the {@link Stage} is focused.
 *
 * If any of them is focused, then the overall application is focused !
 *
 * It includes methods to execute whenever focus is lost/gained, see {@link #onFocusChanged()}.
 *
 * @author LM. Garret (admin@gameroom.me)
 * @date 08/07/2017.
 */
public class WindowFocusManager {
    private static volatile boolean stageFocused = false; //represents the focused property of the stage
    private static volatile boolean aDialogFocused = false; //represents the focused property of any dialog

    private static volatile boolean oldWindowFocused = false; //how the app was focused before it changed
    private static volatile boolean windowFocused = false; //current focus status of the app

    private static ExecutorService service = Executors.newCachedThreadPool(); //to hold delayed tasks

    /** To be called when the {@link Stage} changes of focus. Updates the {@link #stageFocused} property and then computes
     *  a new version of the {@link #windowFocused}.
     *
     * @param newValue the focus status of the stage
     */
    public static void stageFocusChanged(boolean newValue) {
        stageFocused = newValue;

        service.submit(() -> {
            try {
                //here we wait 300ms as when the stage loses focus it could be only to give it to a new dialog
                TimeUnit.MILLISECONDS.sleep(300);
                computeWindowFocus();
            } catch (InterruptedException e) {
                //do nothing here
            }
        });
    }

    /** To be called when a {@link Dialog} changes of focus. Updates the {@link #aDialogFocused} property and then computes
     *  a new version of the {@link #windowFocused}.
     *
     * @param newValue the focus status of the dialog displayed
     */
    public static void dialogFocusChanged(boolean newValue) {
        aDialogFocused = newValue;

        service.submit(() -> {
            try {
                //here we wait 300ms as when a dialog loses focus it could be only to give it back to the stage
                TimeUnit.MILLISECONDS.sleep(300);
                computeWindowFocus();
            } catch (InterruptedException e) {
                //do nothing here
            }
        });
    }

    /**
     *  Computes the current status of the app's window focus
     */
    private static void computeWindowFocus() {
        oldWindowFocused = windowFocused;
        windowFocused = stageFocused || aDialogFocused;
        onFocusChanged();
    }

    /** Getter of{@link #windowFocused}
     *
     * @return true if the app/window overall is focused, false otherwise
     */
    public static boolean isWindowFocused() {
        return windowFocused;
    }

    /**
     *  Shuts down the executor service used, should be done when window is closed.
     */
    public static void shutdown() {
        service.shutdownNow();
    }

    /**
     *  Executes actions to be done whenever the app/window focus status changes
     */
    private static void onFocusChanged() {
        if (!oldWindowFocused && windowFocused) {
            /*************************** FOCUS GAINED ***************************/
            LOGGER.debug("WindowFocus : gained");

            if (settings().getBoolean(PredefinedSetting.ENABLE_GAME_CONTROLLER_SUPPORT)) {
                gameController.startThreads();
            }
            GeneralToast.enableToasts(true);
        } else if (oldWindowFocused && !windowFocused) {
            /*************************** FOCUS LOST ***************************/
            LOGGER.debug("WindowFocus : lost");

            if (settings().getBoolean(PredefinedSetting.ENABLE_GAME_CONTROLLER_SUPPORT)) {
                gameController.stopThreads();
            }
            GeneralToast.enableToasts(false);
        }
    }
}

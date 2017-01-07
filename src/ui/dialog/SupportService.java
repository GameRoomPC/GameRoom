package ui.dialog;

import data.http.key.KeyChecker;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import system.application.Monitor;
import system.application.settings.PredefinedSetting;

import java.util.Date;

import static ui.Main.GENERAL_SETTINGS;
import static ui.Main.LOGGER;

/**
 * Created by LM on 07/01/2017.
 */
public class SupportService {
    private static SupportService INSTANCE;
    private final static int CHECK_FREQ = 2 * 60 * 60 * 1000;
    private final static int DISPLAY_FREQUENCY = 30 * 24 * 60 * 60 * 1000;
    private Thread thread;

    private SupportService(){
        thread = new Thread(() ->{
            while(!KeyChecker.assumeSupporterMode()){
                if(GENERAL_SETTINGS != null){
                    Date lastMessageDate = GENERAL_SETTINGS.getDate(PredefinedSetting.LAST_SUPPORT_MESSAGE);
                    Date currentDate = new Date();

                    long elapsedTime = currentDate.getTime() - lastMessageDate.getTime();

                    if(elapsedTime >= DISPLAY_FREQUENCY){
                        Platform.runLater(this::displayAlert);
                        GENERAL_SETTINGS.setSettingValue(PredefinedSetting.LAST_SUPPORT_MESSAGE,new Date());
                    }
                }

                try {
                    Thread.sleep(CHECK_FREQ);
                } catch (InterruptedException e) {
                    LOGGER.info("Checking if have to display support dialog");
                }
            }
        });
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
    }

    private static SupportService getInstance(){
        if(INSTANCE == null){
            INSTANCE = new SupportService();
        }
        return INSTANCE;
    }

    public void startOrResume(){
        switch (thread.getState()){
            case NEW:
            case RUNNABLE:
                thread.start();
                break;
            case TIMED_WAITING:
                thread.interrupt();
                break;
            default:break;
        }
    }

    public static void start(){
        getInstance().startOrResume();
    }

    private void displayAlert(){
        //TODO display a true alert
        GameRoomAlert alert = new GameRoomAlert(Alert.AlertType.INFORMATION,"Support us");
        alert.showAndWait();
    }
}

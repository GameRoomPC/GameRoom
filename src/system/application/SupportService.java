package system.application;

import data.game.entry.GameEntryUtils;
import data.game.entry.GameEntry;
import data.game.scraper.SteamOnlineScraper;
import data.http.key.KeyChecker;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import system.application.settings.PredefinedSetting;
import ui.Main;
import ui.GeneralToast;
import ui.dialog.GameRoomAlert;
import ui.dialog.WebBrowser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static ui.Main.*;

/**
 * Created by LM on 07/01/2017.
 */
public class SupportService {
    private static SupportService INSTANCE;
    private final static long RUN_FREQ = TimeUnit.MINUTES.toMillis(DEV_MODE ? 2 : 15);

    private final static long UPDATE_CHECK_FREQ = TimeUnit.MINUTES.toMillis(DEV_MODE ? 2 : 60);
    private final static long SUPPORT_ALERT_FREQ = TimeUnit.DAYS.toMillis(15);

    private Thread thread;
    private static volatile boolean DISPLAYING_SUPPORT_ALERT = false;

    private SupportService(){
        thread = new Thread(() ->{
            while(Main.KEEP_THREADS_RUNNING){
                long start = System.currentTimeMillis();

                checkAndDisplaySupportAlert();
                scanSteamGamesTime();
                checkForUpdates();

                long elapsedTime = System.currentTimeMillis() - start;
                if(elapsedTime < RUN_FREQ){
                    try {
                        Thread.sleep(RUN_FREQ - elapsedTime);
                    } catch (InterruptedException ignored) {
                    }
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

    private void checkAndDisplaySupportAlert(){
        if(GENERAL_SETTINGS != null){
            if(!KeyChecker.assumeSupporterMode()){
                LOGGER.info("Checking if have to display support dialog");
                Date lastMessageDate = GENERAL_SETTINGS.getDate(PredefinedSetting.LAST_SUPPORT_MESSAGE);
                Date currentDate = new Date();

                long elapsedTime = currentDate.getTime() - lastMessageDate.getTime();

                if(elapsedTime >= SUPPORT_ALERT_FREQ){
                    Platform.runLater(() -> displaySupportAlert());
                    GENERAL_SETTINGS.setSettingValue(PredefinedSetting.LAST_SUPPORT_MESSAGE,new Date());
                }
            }
        }
    }

    private static void displaySupportAlert(){
        if(DISPLAYING_SUPPORT_ALERT){
            return;
        }
        DISPLAYING_SUPPORT_ALERT = true;

        GameRoomAlert alert = new GameRoomAlert(Alert.AlertType.INFORMATION
                , Main.getString("support_alert_message"));
        alert.getButtonTypes().add(new ButtonType(Main.getString("more_infos")));
        Optional<ButtonType> result = alert.showAndWait();
        result.ifPresent(letter -> {
            if(letter.getText().equals(Main.getString("more_infos"))) {
                WebBrowser.openSupporterKeyBuyBrowser();
            }
        });
        DISPLAYING_SUPPORT_ALERT = false;
    }

    private void scanSteamGamesTime() {
        try {
            ArrayList<GameEntry> ownedSteamApps = SteamOnlineScraper.getOwnedSteamGames();
            if(MAIN_SCENE!=null){
                GeneralToast.displayToast(Main.getString("scanning_steam_play_time"),MAIN_SCENE.getParentStage(),GeneralToast.DURATION_SHORT,true);
            }
            LOGGER.info("Scanning Steam playtimes online");
            for (GameEntry ownedEntry : ownedSteamApps) {
                if (ownedEntry.getPlayTimeSeconds() != 0) {
                    for (GameEntry storedEntry : GameEntryUtils.ENTRIES_LIST) {
                        if (ownedEntry.getPlatformGameID() == storedEntry.getPlatformGameID() && ownedEntry.getPlayTimeSeconds() != storedEntry.getPlayTimeSeconds()) {
                            storedEntry.setPlayTimeSeconds(ownedEntry.getPlayTimeSeconds());
                            Platform.runLater(() -> {
                                Main.MAIN_SCENE.updateGame(storedEntry);
                            });
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkForUpdates(){
        if(GENERAL_SETTINGS == null){
            return;
        }
        if(DEV_MODE){
            return;
        }
        Date lastCheck = GENERAL_SETTINGS.getDate(PredefinedSetting.LAST_UPDATE_CHECK);
        long elapsed = System.currentTimeMillis() - lastCheck.getTime();
        if(elapsed >= UPDATE_CHECK_FREQ){
            if(!GameRoomUpdater.getInstance().isStarted()){
                GameRoomUpdater.getInstance().setOnUpdatePressedListener((observable, oldValue, newValue) -> {
                    GameRoomAlert.info(Main.getString("update_downloaded_in_background"));
                });
                GameRoomUpdater.getInstance().start();
            }
        }
    }
}

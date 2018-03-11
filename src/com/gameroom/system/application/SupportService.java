package com.gameroom.system.application;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.gameroom.data.game.entry.GameEntryUtils;
import com.gameroom.data.game.entry.GameEntry;
import com.gameroom.data.game.scraper.IGDBScraper;
import com.gameroom.data.game.scraper.SteamOnlineScraper;
import com.gameroom.data.http.key.KeyChecker;
import com.gameroom.data.http.key.CipherUtils;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.json.JSONObject;
import com.gameroom.system.application.settings.PredefinedSetting;
import com.gameroom.system.device.StatsUtils;
import com.gameroom.system.os.WinReg;
import com.gameroom.ui.Main;
import com.gameroom.ui.GeneralToast;
import com.gameroom.ui.dialog.GameRoomAlert;
import com.gameroom.ui.dialog.WebBrowser;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.gameroom.system.application.settings.GeneralSettings.settings;
import static com.gameroom.ui.Main.*;

/**
 * Created by LM on 07/01/2017.
 */
public class SupportService {
    private static SupportService INSTANCE;
    private final static long RUN_FREQ = TimeUnit.MINUTES.toMillis(DEV_MODE ? 2 : 15);

    private final static long UPDATE_CHECK_FREQ = TimeUnit.MINUTES.toMillis(DEV_MODE ? 2 : 60);
    private final static long SUPPORT_ALERT_FREQ = TimeUnit.DAYS.toMillis(15);
    private final static long MIN_INSTALL_PING_TIME = TimeUnit.DAYS.toMillis(3);
    private final static long PING_FREQ = TimeUnit.DAYS.toMillis(1);

    private final static String GAMEROOM_API_URL = IGDBScraper.API_URL;

    private Thread thread;
    private static volatile boolean DISPLAYING_SUPPORT_ALERT = false;

    private SupportService() {
        thread = new Thread(() -> {
            while (Main.KEEP_THREADS_RUNNING) {
                if (DEV_MODE) {
                    //LOGGER.info("SupportService Running");
                }
                long start = System.currentTimeMillis();

                checkAndDisplaySupportAlert();
                scanSteamGamesTime();
                checkForUpdates();
                checkFilesForEntries();
                ping();

                long elapsedTime = System.currentTimeMillis() - start;
                if (elapsedTime < RUN_FREQ) {
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

    private void checkFilesForEntries() {
        if(MAIN_SCENE != null){
            LOGGER.info("SupportService: starting file verification for entries.");
            MAIN_SCENE.checkFilesForEntries();
        }
    }

    private static SupportService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SupportService();
        }
        return INSTANCE;
    }

    public void startOrResume() {
        switch (thread.getState()) {
            case NEW:
            case RUNNABLE:
                thread.start();
                break;
            case TIMED_WAITING:
                thread.interrupt();
                break;
            default:
                break;
        }
    }

    public static void start() {
        getInstance().startOrResume();
    }

    private void checkAndDisplaySupportAlert() {
        if (settings() != null) {
            if (!KeyChecker.assumeSupporterMode()) {
                LOGGER.info("Checking if have to display support dialog");
                Date lastMessageDate = settings().getDate(PredefinedSetting.LAST_SUPPORT_MESSAGE);
                Date currentDate = new Date();

                long elapsedTime = currentDate.getTime() - lastMessageDate.getTime();

                if (elapsedTime >= SUPPORT_ALERT_FREQ) {
                    Platform.runLater(() -> displaySupportAlert());
                    settings().setSettingValue(PredefinedSetting.LAST_SUPPORT_MESSAGE, new Date());
                }
            }
        }
    }

    private static void displaySupportAlert() {
        if (DISPLAYING_SUPPORT_ALERT) {
            return;
        }
        DISPLAYING_SUPPORT_ALERT = true;

        GameRoomAlert alert = new GameRoomAlert(Alert.AlertType.INFORMATION
                , Main.getString("support_alert_message",settings().getSupporterKeyPrice()+"€"));
        alert.getButtonTypes().add(new ButtonType(Main.getString("more_infos")));
        Optional<ButtonType> result = alert.showAndWait();
        result.ifPresent(letter -> {
            if (letter.getText().equals(Main.getString("more_infos"))) {
                WebBrowser.openSupporterKeyBuyBrowser();
            }
        });
        DISPLAYING_SUPPORT_ALERT = false;
    }

    private void scanSteamGamesTime() {
        if (settings().getBoolean(PredefinedSetting.SYNC_STEAM_PLAYTIMES)) {
            try {
                ArrayList<GameEntry> ownedSteamApps = SteamOnlineScraper.getOwnedSteamGames();
                if (MAIN_SCENE != null) {
                    GeneralToast.displayToast(Main.getString("scanning_steam_play_time"), MAIN_SCENE.getParentStage(), GeneralToast.DURATION_SHORT);
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
    }

    private void checkForUpdates() {
        if (settings() == null) {
            return;
        }
        if (DEV_MODE) {
            return;
        }
        Date lastCheck = settings().getDate(PredefinedSetting.LAST_UPDATE_CHECK);
        long elapsed = System.currentTimeMillis() - lastCheck.getTime();
        if (elapsed >= UPDATE_CHECK_FREQ) {
            if (!GameRoomUpdater.getInstance().isStarted()) {
                GameRoomUpdater.getInstance().setOnUpdatePressedListener((observable, oldValue, newValue) -> {
                    GameRoomAlert.info(Main.getString("update_downloaded_in_background"));
                });
                GameRoomUpdater.getInstance().start();
            }
        }
    }

    /**
     * Basically pings GameRoom's servers with some com.gameroom.data to perform some stats
     */
    private void ping() {
        try {

            if (settings() == null) {
                return;
            }
        /*if(DEV_MODE){
            return;
        }*/
            Date installDate = settings().getDate(PredefinedSetting.INSTALL_DATE);
            Date lastPingDate = settings().getDate(PredefinedSetting.LAST_PING_DATE);
            long sinceInstall = System.currentTimeMillis() - installDate.getTime();
            long lastPing = System.currentTimeMillis() - lastPingDate.getTime();

            if (sinceInstall >= MIN_INSTALL_PING_TIME  && lastPing >= PING_FREQ) {
                HttpResponse<JsonNode> response = null;
                try {
                    if (settings().getBoolean(PredefinedSetting.ALLOW_COLLECT_SYSTEM_INFO)) {

                        SecretKey keyAES = CipherUtils.generateAES128Key();
                        PublicKey keyRSA = CipherUtils.loadPublicKey();

                        JSONObject obj = new JSONObject();
                        obj.put("NbGames", GameEntryUtils.ENTRIES_LIST.size())
                                .put("TotalPlaytime", StatsUtils.getTotalPlaytime())
                                .put("IsSupporter", KeyChecker.assumeSupporterMode() ? 1 : 0)
                                .put("ThemeUsed", settings().getTheme().getName())
                                .put("GPUs", StatsUtils.getGPUNames())
                                .put("CPUs", StatsUtils.getCPUNames())
                                .put("RAMAmount", StatsUtils.getRAMAmount())
                                .put("WinKey", WinReg.readHWGUID())
                                .put("OSInfo", StatsUtils.getOSInfo());

                        response = Unirest.post(GAMEROOM_API_URL + "/Stats/DailyPing")
                                .header("Accept", "application/json")
                                .field("aes",CipherUtils.cipherAESKeyWithRSA(keyAES,keyRSA))
                                .field("ping_data", CipherUtils.cipherAES(obj, keyAES))
                                .asJson();
                    } else {
                        response = Unirest.post(GAMEROOM_API_URL + "/Stats/DailyPing")
                                .header("Accept", "application/json")
                                .asJson();
                    }

                    if (response != null && response.getBody().getObject().getJSONObject("status").getInt("code") == 200) {
                        settings().setSettingValue(PredefinedSetting.LAST_PING_DATE, new Date());
                    }
                } catch (Exception e) {
                    if (DEV_MODE) {
                        e.printStackTrace();
                        if(response!=null){
                            LOGGER.error(response.getStatusText());
                        }
                    }
                }
            }
        } catch (Exception e) {
            //Here we catch any uncatched exception that could have occured
            if (DEV_MODE) {
                e.printStackTrace();
            }
        }
    }
}

package system.application;

import data.game.entry.GameEntry;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import system.application.settings.PredefinedSetting;
import system.os.PowerMode;
import ui.Main;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;

import static ui.Main.GENERAL_SETTINGS;
import static ui.Main.MAIN_SCENE;


/**
 * Created by LM on 14/07/2016.
 */
public class GameStarter {
    private static String STEAM_PREFIX = "steam";
    private GameEntry entry;
    private PowerMode originalPowerMode;


    public GameStarter(GameEntry entry){
        this.entry = entry;
    }
    public void start(){
        originalPowerMode = PowerMode.getActivePowerMode();
        if(GENERAL_SETTINGS.getBoolean(PredefinedSetting.ENABLE_GAMING_POWER_MODE) && !entry.isAlreadyStartedInGameRoom()){
            GENERAL_SETTINGS.getPowerMode(PredefinedSetting.GAMING_POWER_MODE).activate();
        }
        if(entry.getPath().startsWith(STEAM_PREFIX)){
            try {
                Desktop.getDesktop().browse(new URI(entry.getPath()));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }else{
            File gameLog = new File("log"+File.separator+entry.getProcessName()+".log");
            ProcessBuilder builder = new ProcessBuilder('"'+entry.getPath()+'"').inheritIO();
            builder.directory(new File(new File(entry.getPath()).getParent()));
            builder.redirectError(gameLog);
            try {
                Process process = builder.start();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        Task<Long> monitor = new Task() {
            @Override
            protected Object call() throws Exception {
                if(GENERAL_SETTINGS.getOnLaunchAction(PredefinedSetting.ON_GAME_LAUNCH_ACTION).equals(OnLaunchAction.CLOSE)){
                    Main.forceStop(MAIN_SCENE.getParentStage());
                }else if(GENERAL_SETTINGS.getOnLaunchAction(PredefinedSetting.ON_GAME_LAUNCH_ACTION).equals(OnLaunchAction.HIDE)){
                    Main.LOGGER.debug("Hiding");
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            MAIN_SCENE.getParentStage().hide();
                        }
                    });
                }
                //This condition means that some thread is already monitoring and waiting for the game to be restarted, no need to monitor
                if(!entry.isAlreadyStartedInGameRoom()){
                    entry.setAlreadyStartedInGameRoom(true);
                    Monitor timeMonitor = new Monitor(GameStarter.this);
                    return timeMonitor.start();
                }
                return new Long(-1);
            }
        };
        monitor.valueProperty().addListener(new ChangeListener<Long>() {
            @Override
            public void changed(ObservableValue<? extends Long> observable, Long oldValue, Long newValue) {
                if(!newValue.equals(new Long(-1))){
                    Main.LOGGER.debug("Adding "+Math.round(newValue/1000.0)+"s to game "+entry.getName());
                    entry.setAlreadyStartedInGameRoom(false);
                    entry.setSavedLocaly(true);
                    entry.addPlayTimeSeconds(Math.round(newValue/1000.0));
                    entry.setSavedLocaly(false);
                    if(!GENERAL_SETTINGS.getBoolean(PredefinedSetting.NO_NOTIFICATIONS)) {
                        Main.TRAY_ICON.displayMessage("GameRoom"
                                , GameEntry.getPlayTimeFormatted(Math.round(newValue/1000.0),GameEntry.TIME_FORMAT_SHORT_HMS) + " "
                                        + Main.RESSOURCE_BUNDLE.getString("tray_icon_time_recorded") + " "
                                        + entry.getName(), TrayIcon.MessageType.INFO);
                    }
                }else{
                    //No need to add playtime as if we are here, it means that some thread is already monitoring play time
                }
            }
        });
        Thread th = new Thread(monitor);
        th.setDaemon(true);

        th.start();
    }
    public void onStop(){
        if(GENERAL_SETTINGS.getBoolean(PredefinedSetting.ENABLE_GAMING_POWER_MODE))
            originalPowerMode.activate();
    }

    public GameEntry getGameEntry() {
        return entry;
    }
}

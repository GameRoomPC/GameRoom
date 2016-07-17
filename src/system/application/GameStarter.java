package system.application;

import data.game.GameEntry;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import sun.misc.Launcher;
import system.os.PowerMode;
import ui.Main;

import java.awt.*;
import java.io.*;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import static ui.Main.GENERAL_SETTINGS;
import static ui.Main.MAIN_SCENE;


/**
 * Created by LM on 14/07/2016.
 */
public class GameStarter {
    private GameEntry entry;
    private static final int MIN_MONITOR_TIME = 20000;
    private static final int MONITOR_REFRESH = 1000;

    public GameStarter(GameEntry entry){
        this.entry = entry;
    }
    public void start(){
        PowerMode originalPowerMode = PowerMode.getActivePowerMode();
        if(GENERAL_SETTINGS.isEnablePowerGamingMode() && !entry.isAlreadyStartedInGameRoom()){
            GENERAL_SETTINGS.getGamingPowerMode().activate();
        }
        Task<Long> monitor = new Task() {
            @Override
            protected Object call() throws Exception {
                File gameLog = new File("log"+File.separator+entry.getProcessName()+".log");
                ProcessBuilder builder = new ProcessBuilder('"'+entry.getPath()+'"').inheritIO();
                builder.directory(new File(new File(entry.getPath()).getParent()));
                builder.redirectError(gameLog);
                Process process = builder.start();

                if(GENERAL_SETTINGS.getOnLaunchAction().equals(OnLaunchAction.CLOSE)){
                    Main.forceStop(MAIN_SCENE.getParentStage());
                }else if(GENERAL_SETTINGS.getOnLaunchAction().equals(OnLaunchAction.HIDE)){
                    Main.logger.debug("Hiding");
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
                    return  monitorProcess(entry,originalPowerMode);
                }
                return new Long(-1);
            }
        };
        monitor.valueProperty().addListener(new ChangeListener<Long>() {
            @Override
            public void changed(ObservableValue<? extends Long> observable, Long oldValue, Long newValue) {
                if(!newValue.equals(new Long(-1))){
                    Main.logger.debug("Adding "+Math.round(newValue/1000.0)+"s to game "+entry.getName());
                    entry.setAlreadyStartedInGameRoom(false);
                    entry.setSavedLocaly(true);
                    entry.addPlayTimeSeconds(Math.round(newValue/1000.0));
                    entry.setSavedLocaly(false);
                    if(!GENERAL_SETTINGS.isDisableAllNotifications()) {
                        Main.TRAY_ICON.displayMessage("GameRoom"
                                , GameEntry.getPlayTimeFormatted(Math.round(newValue / 1000.0), true) + " "
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
    private long monitorProcess(GameEntry entry, PowerMode originalPowerMode){
        boolean keepRunning = isProcessRunning(entry.getProcessName());

        if(!keepRunning){
            while(!keepRunning){
                keepRunning = isProcessRunning(entry.getProcessName());
                try {
                    Thread.sleep(MONITOR_REFRESH);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        long startTime = System.currentTimeMillis();
        Main.logger.info("Monitoring "+ entry.getProcessName());
        while (keepRunning) {

            keepRunning = isProcessRunning(entry.getProcessName());
            if(!keepRunning){
                Main.logger.info(entry.getProcessName()+" killed");
            }
            try {
                Thread.sleep(MONITOR_REFRESH);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        long stopTime = System.currentTimeMillis();
        long result = stopTime - startTime;
        if(result < MIN_MONITOR_TIME){
            final FutureTask<Long> query = new FutureTask(new Callable() {
                @Override
                public Long call() throws Exception {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setHeaderText(null);
                    alert.initStyle(StageStyle.UNDECORATED);
                    alert.getDialogPane().getStylesheets().add("res/flatterfx.css");
                    alert.initModality(Modality.WINDOW_MODAL);
                    alert.setContentText(entry.getName()+" "
                            +Main.RESSOURCE_BUNDLE.getString("monitor_wait_dialog_1")+" "
                            +(stopTime-startTime)/1000+"s"
                            +Main.RESSOURCE_BUNDLE.getString("monitor_wait_dialog_2"));

                    Optional<ButtonType> dialogResult = alert.showAndWait();
                    if (dialogResult.get() == ButtonType.OK) {
                        Main.logger.info(entry.getProcessName()+" : waiting for until next launch to count playtime.");
                        return new Long(-1);
                    }
                    if(GENERAL_SETTINGS.isEnablePowerGamingMode())
                        originalPowerMode.activate();
                    return result;
                }
            });
            Platform.runLater(query);

            try {
                if(query.get().equals(new Long(-1))){
                    FutureTask<Long> monitor = new Task() {
                        @Override
                        protected Object call() throws Exception {
                            return  monitorProcess(entry,originalPowerMode);
                        }
                    };
                    Thread th = new Thread(monitor);
                    th.setDaemon(true);
                    th.start();
                    return monitor.get();
                }else {
                    return query.get();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
                return result;
            }
        }else{
            if(GENERAL_SETTINGS.isEnablePowerGamingMode())
                originalPowerMode.activate();
            return result;
        }
        return result;
    }
    public static boolean isProcessRunning(String process) {
        boolean found = false;
        try {
            File file = File.createTempFile("process_watcher",".vbs");
            file.deleteOnExit();
            FileWriter fw = new java.io.FileWriter(file);

            String vbs = "Set WshShell = WScript.CreateObject(\"WScript.Shell\")\n"
                    + "Set locator = CreateObject(\"WbemScripting.SWbemLocator\")\n"
                    + "Set service = locator.ConnectServer()\n"
                    + "Set processes = service.ExecQuery _\n"
                    + " (\"select * from Win32_Process where name='" + process +"'\")\n"
                    + "For Each process in processes\n"
                    + "wscript.echo process.Name \n"
                    + "Next\n"
                    + "Set WSHShell = Nothing\n";

            fw.write(vbs);
            fw.close();
            Process p = Runtime.getRuntime().exec("cscript //NoLogo " + file.getPath());
            BufferedReader input =
                    new BufferedReader
                            (new InputStreamReader(p.getInputStream()));
            String line;
            line = input.readLine();
            if (line != null) {
                if (line.equals(process)) {
                    found = true;
                }
            }
            input.close();

        }
        catch(Exception e){
            e.printStackTrace();
        }
        return found;
    }

}

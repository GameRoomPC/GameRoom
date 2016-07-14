package system.application;

import data.game.GameEntry;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import system.os.PowerMode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;

import static ui.Main.GENERAL_SETTINGS;
import static ui.Main.logger;

/**
 * Created by LM on 14/07/2016.
 */
public class GameStarter {
    private GameEntry entry;

    public GameStarter(GameEntry entry){
        this.entry = entry;
    }
    public void start(){
        PowerMode originalPowerMode = PowerMode.getActivePowerMode();
        if(GENERAL_SETTINGS.isEnablePowerGamingMode()){
            GENERAL_SETTINGS.getGamingPowerMode().activate();
        }
        Task<Long> monitor = new Task() {
            @Override
            protected Object call() throws Exception {
                Process process = new ProcessBuilder(entry.getPath()).start();
                long startTime = System.currentTimeMillis();

                boolean keepRunning = true;
                logger.info("Started "+ entry.getProcessName());
                while (keepRunning) {

                    keepRunning = isProcessRunning(entry.getProcessName());
                    if(!keepRunning){
                        logger.info(entry.getProcessName()+" killed");
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if(GENERAL_SETTINGS.isEnablePowerGamingMode()) {
                    originalPowerMode.activate();
                }
                long stopTime = System.currentTimeMillis();
                return stopTime - startTime;
            }
        };
        monitor.valueProperty().addListener(new ChangeListener<Long>() {
            @Override
            public void changed(ObservableValue<? extends Long> observable, Long oldValue, Long newValue) {
                logger.debug("Adding "+Math.round(newValue/1000.0)+"s to game "+entry.getName());
                entry.setSavedLocaly(true);
                entry.addPlayTimeSeconds(Math.round(newValue/1000.0));
                entry.setSavedLocaly(false);
            }
        });
        Thread th = new Thread(monitor);
        th.setDaemon(true);

        th.start();
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

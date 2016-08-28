package system.application;

import data.game.entry.GameEntry;
import data.game.scrapper.SteamLocalScrapper;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import ui.Main;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * Created by LM on 24/07/2016.
 */
public class Monitor {
    private static String TIME_TAG = "$$time$$";

    private static int MONITOR_REFRESH = 2500;
    private static final int MIN_MONITOR_TIME = 20000;

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH);
    public static final DateFormat DEBUG_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    private GameStarter gameStarter;
    private File vbsWatcher;
    private String timeWatcherCmd;

    private ArrayList<StandbyInterval> standbyIntervals = new ArrayList<>();

    private Date creationDate = null;

    protected Monitor(GameStarter starter) throws IOException {
        this.gameStarter = starter;
        if(!gameStarter.getGameEntry().isSteamGame()){
            DATE_FORMAT.setTimeZone(Calendar.getInstance().getTimeZone());

            vbsWatcher = File.createTempFile(gameStarter.getGameEntry().getProcessName() + "_watcher", ".vbs");
            vbsWatcher.deleteOnExit();
            FileWriter fw = new java.io.FileWriter(vbsWatcher);

            String vbs = "Set WshShell = WScript.CreateObject(\"WScript.Shell\")\n"
                    + "Set locator = CreateObject(\"WbemScripting.SWbemLocator\")\n"
                    + "Set service = locator.ConnectServer()\n"
                    + "Set processes = service.ExecQuery _\n"
                    + " (\"select * from Win32_Process where name='" + gameStarter.getGameEntry().getProcessName() + "'\")\n"
                    + "For Each process in processes\n"
                    + "wscript.echo process.Name \n"
                    + "wscript.echo \"" + TIME_TAG + "\" & process.CreationDate \n"
                    + "Next\n"
                    + "Set WSHShell = Nothing\n";

            fw.write(vbs);
            fw.close();

            timeWatcherCmd = "cscript //NoLogo " + vbsWatcher.getPath();
        }
    }

    public long start(Date initialDate) throws IOException {
        if(!gameStarter.getGameEntry().isSteamGame()){
            long originalPlayTime = gameStarter.getGameEntry().getPlayTimeSeconds();

            while (creationDate == null || creationDate.equals(initialDate)) {
                creationDate = computeCreationDate();
                try {
                    Thread.sleep(MONITOR_REFRESH);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Main.LOGGER.info("Monitoring " + gameStarter.getGameEntry().getProcessName());
            while (isProcessRunning()) {
                long result = computeTrueRunningTime();
                gameStarter.getGameEntry().setSavedLocaly(true);
                gameStarter.getGameEntry().setPlayTimeSeconds(originalPlayTime+Math.round(result/1000.0));
                gameStarter.getGameEntry().setSavedLocaly(false);
                try {
                    Thread.sleep(MONITOR_REFRESH);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Main.LOGGER.info(gameStarter.getGameEntry().getProcessName() + " killed");

            long result = computeTrueRunningTime();
            Main.LOGGER.debug("\tComputed playtime : "+ GameEntry.getPlayTimeFormatted(Math.round(result/1000),GameEntry.TIME_FORMAT_FULL_DOUBLEDOTS));

            if (result < MIN_MONITOR_TIME) {
                final FutureTask<Long> query = new FutureTask(new Callable() {
                    @Override
                    public Long call() throws Exception {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setHeaderText(null);
                        alert.initStyle(StageStyle.UNDECORATED);
                        alert.getDialogPane().getStylesheets().add("res/flatterfx.css");
                        alert.initModality(Modality.WINDOW_MODAL);
                        alert.setContentText(gameStarter.getGameEntry().getName() + " "
                                + Main.RESSOURCE_BUNDLE.getString("monitor_wait_dialog_1") + " "
                                + GameEntry.getPlayTimeFormatted(Math.round(result/1000.0),GameEntry.TIME_FORMAT_ROUNDED_HMS)
                                + Main.RESSOURCE_BUNDLE.getString("monitor_wait_dialog_2"));

                        Optional<ButtonType> dialogResult = alert.showAndWait();
                        if (dialogResult.get() == ButtonType.OK) {
                            Main.LOGGER.info(gameStarter.getGameEntry().getProcessName() + " : waiting for until next launch to count playtime.");
                            return new Long(-1);
                        }
                        gameStarter.onStop();

                        return result;
                    }
                });
                Platform.runLater(query);

                try {
                    if (query.get().equals(new Long(-1))) {
                        //we wait for next game launch
                        FutureTask<Long> monitor = new Task() {
                            @Override
                            protected Object call() throws Exception {
                                return start(creationDate);
                            }
                        };
                        Thread th = new Thread(monitor);
                        th.setDaemon(true);
                        th.start();
                        return monitor.get();
                    } else {
                        return query.get();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    return result;
                }
            } else {
                gameStarter.onStop();
                return result;
            }
            return result;
        }else{
            Main.LOGGER.info("Not monitoring time for " + gameStarter.getGameEntry().getName()+", is steam game");
            if(SteamLocalScrapper.isSteamGameInstalled(gameStarter.getGameEntry().getSteam_id())){
                //waiting for the game to start
                while (!SteamLocalScrapper.isSteamGameRunning(gameStarter.getGameEntry().getSteam_id())
                        && !SteamLocalScrapper.isSteamGameLaunching(gameStarter.getGameEntry().getSteam_id())) {
                    try {
                        Thread.sleep(MONITOR_REFRESH);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            while (SteamLocalScrapper.isSteamGameRunning(gameStarter.getGameEntry().getSteam_id())
                    || SteamLocalScrapper.isSteamGameLaunching(gameStarter.getGameEntry().getSteam_id())) {
                try {
                    Thread.sleep(MONITOR_REFRESH);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Main.LOGGER.info(gameStarter.getGameEntry().getProcessName() + " killed");
            gameStarter.onStop();
            return 0;
        }
    }
    protected long computeTrueRunningTime() throws IOException {
        long currentTime = System.currentTimeMillis();

        while(creationDate == null){
            creationDate = computeCreationDate();
            try {
                Thread.sleep(MONITOR_REFRESH);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        long result = currentTime-creationDate.getTime();

        String request = "strComputer = \".\"\n" +
                "Set objWMIService = GetObject(\"winmgmts:\" _\n" +
                "    & \"{impersonationLevel=impersonate}!\\\\\" _\n" +
                "    & strComputer & \"\\root\\cimv2\")\n" +
                "Set colLoggedEvents = objWMIService.ExecQuery _\n" +
                "    (\"Select * from Win32_NTLogEvent \" _\n" +
                "        & \"Where Logfile = 'System' AND (EventCode='42' OR EventCode='1') AND TimeWritten >= \'"+ DATE_FORMAT.format(creationDate.getTime()-3600000*2)+".000000-000\'\")\n" +
                "For Each objEvent in colLoggedEvents\n" +
                "    Wscript.Echo objEvent.EventCode & \""+TIME_TAG+"\" & objEvent.TimeWritten\n"+
                "Next";
        //TODO fix incorrect timezone in uppper script
        //Main.LOGGER.debug("\tOriginal date : "+ DATE_FORMAT.format(creationDate.getTime()));
        //Main.LOGGER.debug("\tCorrected date : "+ DATE_FORMAT.format(creationDate.getTime()-3600000*2));

        File standbyWatcher = File.createTempFile("event_watcher", ".vbs");
        standbyWatcher.deleteOnExit();
        FileWriter fw = new java.io.FileWriter(standbyWatcher);
        fw.write(request);
        fw.close();

        Process p = Runtime.getRuntime().exec("cscript //NoLogo " + standbyWatcher.getPath());
        BufferedReader input =
                new BufferedReader
                        (new InputStreamReader(p.getInputStream()));
        String line;

        Date event1Date=null;
        while ((line = input.readLine()) != null) {
            if(line.startsWith("1")){
                try {
                    event1Date = DATE_FORMAT.parse(line.substring(line.indexOf(TIME_TAG)+TIME_TAG.length(), line.indexOf('.')));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }else if(line.startsWith("42")){
                String dateString = line.substring(line.indexOf(TIME_TAG)+TIME_TAG.length(), line.indexOf('.'));
                try {
                    Date standbyDate = DATE_FORMAT.parse(dateString);
                    StandbyInterval si = new StandbyInterval(standbyDate,event1Date);

                    boolean alreadyExists = false;
                    for(StandbyInterval standbyInterval : standbyIntervals){
                        if(standbyInterval.startDate.compareTo(standbyDate) == 0){
                            alreadyExists = true;
                        }
                    }
                    if(!alreadyExists){
                        standbyIntervals.add(si);
                        result -= si.getStandbyTime();
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }

            }
        }
        input.close();

        BufferedReader errors =
                new BufferedReader
                        (new InputStreamReader(p.getErrorStream()));
        while ((line = errors.readLine()) != null) {
            Main.LOGGER.error(line);
        }
        errors.close();

        //Main.LOGGER.debug("\tComputed result : "+ GameEntry.getPlayTimeFormatted(result/1000,GameEntry.TIME_FORMAT_FULL_DOUBLEDOTS));

        return result;
    }

    private Date computeCreationDate() throws IOException {
        Date resultDate = null;

        Process p = Runtime.getRuntime().exec(timeWatcherCmd);
        BufferedReader input =
                new BufferedReader
                        (new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = input.readLine()) != null) {
            if (line.contains(TIME_TAG)) {
                String dateString = line.substring(TIME_TAG.length(), line.indexOf('.'));
                try {
                    resultDate = DATE_FORMAT.parse(dateString);
                    Main.LOGGER.debug("Found creation date of process : "+ DEBUG_DATE_FORMAT.format(resultDate));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
        input.close();
        return resultDate;
    }

    private boolean isProcessRunning() throws IOException {
        boolean found = false;
        Process p = Runtime.getRuntime().exec(timeWatcherCmd);
        BufferedReader input =
                new BufferedReader
                        (new InputStreamReader(p.getInputStream()));
        String line;
        line = input.readLine();
        if (line != null) {
            if (line.equals(gameStarter.getGameEntry().getProcessName())) {
                found = true;
            }
        }
        input.close();
        return found;
    }
    public static class StandbyInterval{
        private Date startDate;
        private Date endDate;

        StandbyInterval(Date startDate, Date endDate){
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public long getStandbyTime(){
            return endDate.getTime() - startDate.getTime();
        }
    }
}

package com.gameroom.system.application;

import com.gameroom.data.game.entry.Emulator;
import com.gameroom.data.game.entry.GameEntry;
import com.gameroom.data.game.scraper.SteamLocalScraper;
import com.gameroom.data.io.FileUtils;
import com.gameroom.system.SchedulableTask;
import com.gameroom.system.application.settings.PredefinedSetting;
import com.gameroom.system.os.Terminal;
import com.gameroom.ui.GeneralToast;
import com.gameroom.ui.Main;
import com.gameroom.ui.dialog.GameRoomAlert;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

import static com.gameroom.system.application.settings.GeneralSettings.settings;
import static com.gameroom.ui.Main.*;

/**
 * Created by LM on 24/07/2016.
 */
public class Monitor {
    private final static String TIME_TAG = "$$time$$";

    private final static String EXCEPTION_NOT_RUNNING = "Process not running";
    private final static long MONITOR_AGAIN = -1;

    private final static long MONITOR_REFRESH = TimeUnit.SECONDS.toMillis(1);
    private final static long MAX_AWAIT_CREATION_TIME = TimeUnit.MINUTES.toMillis(2);
    private final static long MAX_MONITOR_GAP_TIME = TimeUnit.SECONDS.toMillis(15);
    private final static long MIN_MONITOR_TIME = TimeUnit.SECONDS.toMillis(20);

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH);
    private static final DateFormat DEBUG_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    private GameStarter gameStarter;
    private File vbsWatcher;
    private String timeWatcherCmd;
    private boolean awaitingRestart = false;

    private volatile boolean stopMonitor = false;

    private long timer = 0;

    private ArrayList<StandbyInterval> standbyIntervals = new ArrayList<>();

    private Date creationDate = null;
    private String processName;
    long originalPlayTime;


    private SchedulableTask<Long> monitorTask;
    private SchedulableTask<Date> waitCreationTask;
    private long creationAwaitedTime;
    private long monitorGapTime;

    Monitor(GameStarter starter) throws IOException {
        this.gameStarter = starter;
        originalPlayTime = getGameEntry().getPlayTimeSeconds();
        getGameEntry().monitoredProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                stopMonitor = true;
            }
        });
        if (getGameEntry().getPlatform().isPCLauncher()) {
            processName = getGameEntry().getProcessName();
        } else {
            Emulator e = Emulator.getChosenEmulator(getGameEntry().getPlatform());
            if (e == null) {
                GameRoomAlert.error(Main.getString("error_no_emu_configured") + " " + getGameEntry().getPlatform());
            } else {
                processName = e.getProcessName();
            }
        }
        if (!isSteamGame()) {
            DATE_FORMAT.setTimeZone(Calendar.getInstance().getTimeZone());

            vbsWatcher = FileUtils.newTempFile(processName + "_watcher.vbs");
            vbsWatcher.deleteOnExit();
            FileWriter fw = new java.io.FileWriter(vbsWatcher);

            String vbs = "Set WshShell = WScript.CreateObject(\"WScript.Shell\")\n"
                    + "Set locator = CreateObject(\"WbemScripting.SWbemLocator\")\n"
                    + "Set service = locator.ConnectServer()\n"
                    + "Set processes = service.ExecQuery _\n"
                    + " (\"select * from Win32_Process where name='" + processName + "'\")\n"
                    + "For Each process in processes\n"
                    + "wscript.echo process.Name \n"
                    + "wscript.echo \"" + TIME_TAG + "\" & process.CreationDate \n"
                    + "Next\n"
                    + "Set WSHShell = Nothing\n";

            fw.write(vbs);
            fw.close();

            timeWatcherCmd = "cscript //NoLogo " + vbsWatcher.getPath();
        }

        monitorTask = new SchedulableTask<Long>(MONITOR_REFRESH,MONITOR_REFRESH) {
            @Override
            protected Long execute() throws Exception {
                if (!KEEP_THREADS_RUNNING || stopMonitor) {
                    cancel();
                    return 0L;
                }
                if (isProcessRunning()) {
                    timer += MONITOR_REFRESH;
                    return computeTrueRunningTime();
                } else {
                    throw new IllegalStateException(EXCEPTION_NOT_RUNNING);
                }
            }
        };

        monitorTask.setOnFailed(() -> {
            if(monitorTask.getException().getMessage().equals(EXCEPTION_NOT_RUNNING)) {
                monitorGapTime += MONITOR_REFRESH;
                if (monitorGapTime > MAX_MONITOR_GAP_TIME) {
                    info(processName + " killed");
                    long result = 0;
                    try {
                        result = computeTrueRunningTime() - monitorGapTime;
                        debug("Computed playtime : " + GameEntry.getPlayTimeFormatted(Math.round(result / 1000), GameEntry.TIME_FORMAT_FULL_DOUBLEDOTS));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    monitorTask.stop();
                    gameStarter.onStop(result);
                }
            } else{
                debug("monitorTask error, impossible to monitor games");
                monitorTask.getException().printStackTrace();
                monitorTask.stop();
                gameStarter.onStop(0);
                GeneralToast.displayToast(Main.getString("error_cannot_monitor") + getGameEntry().getName(),MAIN_SCENE.getParentStage());
            }
        });

        monitorTask.setOnSucceeded(() -> {
            getGameEntry().setSavedLocally(true);
            getGameEntry().setPlayTimeSeconds(originalPlayTime + Math.round(monitorTask.getValue() / 1000.0));
            getGameEntry().setSavedLocally(false);
        });

        monitorTask.setOnCancelled(() -> {
            waitCreationTask.stop();

            long result = 0;
            try {
                result = computeTrueRunningTime() - monitorGapTime;
                debug("Computed playtime : " + GameEntry.getPlayTimeFormatted(Math.round(result / 1000), GameEntry.TIME_FORMAT_FULL_DOUBLEDOTS));
            } catch (IOException e) {
                e.printStackTrace();
            }
            gameStarter.onStop(result);
        });

        waitCreationTask = new SchedulableTask<Date>(0,MONITOR_REFRESH) {
            @Override
            protected Date execute() throws Exception {
                debug("waitCreationTask running");
                if (!KEEP_THREADS_RUNNING || stopMonitor) {
                    cancel();
                    return null;
                }
                if (isSteamGame()) {
                    if (SteamLocalScraper.isSteamGameRunning(getGameEntry().getPlatformGameID())) {
                        return new Date();
                    }
                    throw new IllegalStateException(EXCEPTION_NOT_RUNNING);
                }
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
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }
                input.close();

                if (resultDate == null) {
                    throw new IllegalStateException(EXCEPTION_NOT_RUNNING);
                }
                return resultDate;

            }
        };

        waitCreationTask.setOnSucceeded(() -> {
            creationDate = waitCreationTask.getValue();
            waitCreationTask.stop();

            debug("Found creation date of process : " + DEBUG_DATE_FORMAT.format(creationDate));
            info("Monitoring " + processName);

            monitorTask.scheduleAtFixedRateOn(Main.getScheduledExecutor());
        });

        waitCreationTask.setOnCancelled(() -> {
            monitorTask.stop();
            debug("waitCreationTask cancelled.");
            gameStarter.onStop(0);
        });

        waitCreationTask.setOnFailed(() -> {
            if(waitCreationTask.getException().getMessage().equals(EXCEPTION_NOT_RUNNING)){
                creationAwaitedTime += MONITOR_REFRESH;
                if (creationAwaitedTime > MAX_AWAIT_CREATION_TIME ) {
                    debug("waitCreationTask error finding creation date of process " + processName);
                    gameStarter.onStop(0);
                    GeneralToast.displayToast(Main.getString("error_app_did_not_start",processName),MAIN_SCENE.getParentStage());
                }
            }else{
                debug("waitCreationTask error, impossible to monitor games");
                waitCreationTask.getException().printStackTrace();
                waitCreationTask.stop();
                gameStarter.onStop(0);
                GeneralToast.displayToast(Main.getString("error_cannot_monitor") + getGameEntry().getName(),MAIN_SCENE.getParentStage());
            }
        });
    }

    public void start(){
        if(getGameEntry().isMonitored()){
            debug("entry already monitored");
            //game is already being monitored !
            return;
        }else{
            getGameEntry().setMonitored(true);
        }
        waitCreationTask.scheduleAtFixedRateOn(Main.getScheduledExecutor());
    }

    private long computeTrueRunningTime() throws IOException {
        long currentTime = System.currentTimeMillis();

        while (creationDate == null && KEEP_THREADS_RUNNING && !stopMonitor) {
            creationDate = computeCreationDate();
            try {
                Thread.sleep(MONITOR_REFRESH);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        long result = currentTime - creationDate.getTime();

        String request = "strComputer = \".\"\n" +
                "Set objWMIService = GetObject(\"winmgmts:\" _\n" +
                "    & \"{impersonationLevel=impersonate}!\\\\\" _\n" +
                "    & strComputer & \"\\root\\cimv2\")\n" +
                "Set colLoggedEvents = objWMIService.ExecQuery _\n" +
                "    (\"Select * from Win32_NTLogEvent \" _\n" +
                "        & \"Where Logfile = 'System' AND (EventCode='42' OR EventCode='1') AND TimeWritten >= \'" + DATE_FORMAT.format(creationDate.getTime() - 3600000 * 2) + ".000000-000\'\")\n" +
                "For Each objEvent in colLoggedEvents\n" +
                "    Wscript.Echo objEvent.EventCode & \"" + TIME_TAG + "\" & objEvent.TimeWritten\n" +
                "Next";
        //TODO fix incorrect timezone in uppper script
        //Main.LOGGER.debug("\tOriginal date : "+ DATE_FORMAT.format(creationDate.getTime()));
        //Main.LOGGER.debug("\tCorrected date : "+ DATE_FORMAT.format(creationDate.getTime()-3600000*2));
        String tempFileName = processName+"_counter.vbs";
        File standbyWatcher = FileUtils.newTempFile(tempFileName);
        standbyWatcher.deleteOnExit();
        FileWriter fw = new java.io.FileWriter(standbyWatcher);
        fw.write(request);
        fw.close();

        Process p = Runtime.getRuntime().exec("cscript //NoLogo " + standbyWatcher.getPath());
        BufferedReader input =
                new BufferedReader
                        (new InputStreamReader(p.getInputStream()));
        String line;

        Date event1Date = null;
        while ((line = input.readLine()) != null) {
            if (line.startsWith("1")) {
                try {
                    event1Date = DATE_FORMAT.parse(line.substring(line.indexOf(TIME_TAG) + TIME_TAG.length(), line.indexOf('.')));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } else if (line.startsWith("42")) {
                String dateString = line.substring(line.indexOf(TIME_TAG) + TIME_TAG.length(), line.indexOf('.'));
                try {
                    Date standbyDate = DATE_FORMAT.parse(dateString);
                    StandbyInterval si = new StandbyInterval(standbyDate, event1Date);

                    boolean alreadyExists = false;
                    for (StandbyInterval standbyInterval : standbyIntervals) {
                        if (standbyInterval.startDate.compareTo(standbyDate) == 0) {
                            alreadyExists = true;
                        }
                    }
                    if (!alreadyExists) {
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


        if (isSteamGame() || !KEEP_THREADS_RUNNING) {
            result = timer;
        }

        return result;
    }

    private Date computeCreationDate() throws IOException {
        if (isSteamGame()) {
            if (SteamLocalScraper.isSteamGameRunning(getGameEntry().getPlatformGameID())) {
                return new Date();
            }
            return null;
        }
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
                    if (settings().getBoolean(PredefinedSetting.DEBUG_MODE)) {
                        Main.LOGGER.debug("Found creation date of process : " + DEBUG_DATE_FORMAT.format(resultDate));
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
        input.close();
        return resultDate;
    }

    private boolean isProcessRunning() throws IOException {
        if (isSteamGame()) {
            return SteamLocalScraper.isSteamGameRunning(getGameEntry().getPlatformGameID());
        } else {
            return isProcessRunning(processName);
        }
    }

    public static boolean isProcessRunning(String processName) {
        Terminal terminal = new Terminal();

        String[] output = null;
        try {
            output = terminal.execute("tasklist", "/FI", "\"IMAGENAME eq " + processName + "\"");
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            return false;
        }
        if (output != null) {
            for (String outputLine : output) {
                if (outputLine.toLowerCase().contains(processName.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSteamGame() {
        //TODO implement same mechanism to monitor playtime for MS Store games
        return getGameEntry().isSteamGame();
    }

    public static class StandbyInterval {
        private Date startDate;
        private Date endDate;

        StandbyInterval(Date startDate, Date endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public long getStandbyTime() {
            return endDate.getTime() - startDate.getTime();
        }
    }

    private GameEntry getGameEntry() {
        return gameStarter.getGameEntry();
    }

    private void debug(String msg){
        if (settings().getBoolean(PredefinedSetting.DEBUG_MODE)) {
            Main.LOGGER.debug(getLogTag()+ msg);
        }
    }

    private void info(String msg){
        Main.LOGGER.info(getLogTag()+msg);
    }

    private String getLogTag(){
        return "Monitor("+getGameEntry().getName()+"): ";
    }
}

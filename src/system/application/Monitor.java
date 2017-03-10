package system.application;

import data.game.entry.GameEntry;
import data.game.scraper.SteamLocalScraper;
import data.io.FileUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ButtonType;
import system.application.settings.PredefinedSetting;
import system.os.Terminal;
import ui.GeneralToast;
import ui.Main;
import ui.dialog.GameRoomAlert;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import static ui.Main.*;

/**
 * Created by LM on 24/07/2016.
 */
public class Monitor {
    private static String TIME_TAG = "$$time$$";

    private final static long MONITOR_AGAIN = -1;

    private static int MONITOR_REFRESH = 1000;
    private static final int MIN_MONITOR_TIME = 20000;

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH);
    private static final DateFormat DEBUG_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    private GameStarter gameStarter;
    private File vbsWatcher;
    private String timeWatcherCmd;
    private boolean awaitingRestart = false;

    private static long timer = 0;

    private ArrayList<StandbyInterval> standbyIntervals = new ArrayList<>();

    private Date creationDate = null;

    Monitor(GameStarter starter) throws IOException {
        this.gameStarter = starter;
        if (!isSteamGame()) {
            DATE_FORMAT.setTimeZone(Calendar.getInstance().getTimeZone());

            vbsWatcher = FileUtils.newTempFile(getGameEntry().getProcessName() + "_watcher.vbs");
            vbsWatcher.deleteOnExit();
            FileWriter fw = new java.io.FileWriter(vbsWatcher);

            String vbs = "Set WshShell = WScript.CreateObject(\"WScript.Shell\")\n"
                    + "Set locator = CreateObject(\"WbemScripting.SWbemLocator\")\n"
                    + "Set service = locator.ConnectServer()\n"
                    + "Set processes = service.ExecQuery _\n"
                    + " (\"select * from Win32_Process where name='" + getGameEntry().getProcessName() + "'\")\n"
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
        if (isSteamGame() && !SteamLocalScraper.isSteamGameInstalled(getGameEntry().getSteam_id())) {
            return 0;
        }
        timer = 0;

        long originalPlayTime = getGameEntry().getPlayTimeSeconds();

        while ((creationDate == null || creationDate.equals(initialDate)) && KEEP_THREADS_RUNNING) {
            creationDate = computeCreationDate();
            try {
                Thread.sleep(MONITOR_REFRESH);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (!KEEP_THREADS_RUNNING) {
            return 0;
        }
        Main.LOGGER.info("Monitoring " + getGameEntry().getProcessName());
        if (awaitingRestart) {
            awaitingRestart = false;
            if (MAIN_SCENE != null) {
                GeneralToast.displayToast(getGameEntry().getName()
                        + Main.getString("restarted"), MAIN_SCENE.getParentStage());
            }
        }
        while (isProcessRunning() && KEEP_THREADS_RUNNING) {
            long startTimeRec = System.currentTimeMillis();

            timer += MONITOR_REFRESH;
            long result = computeTrueRunningTime();
            getGameEntry().setSavedLocally(true);
            getGameEntry().setPlayTimeSeconds(originalPlayTime + Math.round(result / 1000.0));
            getGameEntry().setSavedLocally(false);

            try {
                Thread.sleep(MONITOR_REFRESH - (System.currentTimeMillis() - startTimeRec));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (!KEEP_THREADS_RUNNING) {
            return 0;
        }
        Main.LOGGER.info(getGameEntry().getProcessName() + " killed");

        long result = computeTrueRunningTime();
        Main.LOGGER.debug("\tComputed playtime : " + GameEntry.getPlayTimeFormatted(Math.round(result / 1000), GameEntry.TIME_FORMAT_FULL_DOUBLEDOTS));

        if (result < MIN_MONITOR_TIME) {
            String time = GameEntry.getPlayTimeFormatted(Math.round(result / 1000.0), GameEntry.TIME_FORMAT_ROUNDED_HMS);
            String text = Main.getString("monitor_wait_dialog", getGameEntry().getName(), time);
            final FutureTask<Long> query = new FutureTask(new Callable() {
                @Override
                public Long call() throws Exception {
                    ButtonType buttonResult = GameRoomAlert.confirmation(text);
                    if (buttonResult.getButtonData().isDefaultButton()) {
                        if (MAIN_SCENE != null) {
                            GeneralToast.displayToast(Main.getString("waiting_until")
                                    + getGameEntry().getName()
                                    + Main.getString("restarts"), MAIN_SCENE.getParentStage());
                        }
                        Main.LOGGER.info(getGameEntry().getProcessName() + " : waiting until next launch to count playtime.");
                        return MONITOR_AGAIN;
                    }
                    gameStarter.onStop();

                    return result;
                }
            });
            Platform.runLater(query);

            try {
                long queryResult = query.get();
                if (queryResult == MONITOR_AGAIN) {
                    //we wait for next game launch
                    FutureTask<Long> monitor = new Task() {
                        @Override
                        protected Object call() throws Exception {
                            return start(creationDate);
                        }
                    };
                    Thread th = new Thread(monitor);
                    th.setPriority(Thread.MIN_PRIORITY);
                    th.setDaemon(true);
                    th.start();
                    return monitor.get();
                } else {
                    return queryResult;
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                return result;
            }
        } else {
            gameStarter.onStop();
            return result;
        }
    }

    private long computeTrueRunningTime() throws IOException {
        long currentTime = System.currentTimeMillis();

        while (creationDate == null && KEEP_THREADS_RUNNING) {
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
            if (SteamLocalScraper.isSteamGameRunning(getGameEntry().getSteam_id())) {
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
                    if (GENERAL_SETTINGS.getBoolean(PredefinedSetting.DEBUG_MODE)) {
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
            return SteamLocalScraper.isSteamGameRunning(getGameEntry().getSteam_id());
        } else {
            return isProcessRunning(getGameEntry().getProcessName());
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
                if(outputLine.toLowerCase().contains(processName.toLowerCase())){
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSteamGame() {
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
}

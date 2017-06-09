package system.application;

import data.game.entry.Emulator;
import data.io.FileUtils;
import data.game.entry.GameEntry;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import system.application.settings.PredefinedSetting;
import system.os.PowerMode;
import system.os.Terminal;
import ui.Main;
import ui.GeneralToast;
import ui.dialog.GameRoomAlert;
import ui.dialog.PlatformSettingsDialog;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

import static system.application.settings.GeneralSettings.settings;
import static ui.Main.*;


/**
 * Created by LM on 14/07/2016.
 */
public class GameStarter {
    private static String STEAM_PREFIX = "steam";
    private GameEntry entry;
    private PowerMode originalPowerMode;
    private static String LOG_FOLDER;


    public GameStarter(GameEntry entry) {
        this.entry = entry;
        if (LOG_FOLDER == null) {
            LOG_FOLDER = FILES_MAP.get("games_log").getAbsolutePath() + File.separator;
        }
    }

    public void start() throws IOException {
        Main.LOGGER.info("Starting game : " + entry.getName());

        onPreGameLaunch();
        try {
            startGame();
        } catch (IllegalStateException e) {
            if (e.getMessage().equals("no_emu_configured")) {
                onPostGameLaunch(0);
                GameRoomAlert.error("There is no emulator configured for platform " + entry.getPlatform());
                PlatformSettingsDialog dialog = new PlatformSettingsDialog(entry.getPlatform());
                dialog.showAndWait();
                start();
                return;
            } else {
                e.printStackTrace();
            }
        }

        Task<Long> monitor = new Task() {
            @Override
            protected Object call() throws Exception {
                if (settings().getOnLaunchAction(PredefinedSetting.ON_GAME_LAUNCH_ACTION).equals(OnLaunchAction.CLOSE)) {
                    Main.forceStop(MAIN_SCENE.getParentStage(), "launchAction = OnLaunchAction.CLOSE");
                } else if (settings().getOnLaunchAction(PredefinedSetting.ON_GAME_LAUNCH_ACTION).equals(OnLaunchAction.HIDE)) {
                    Main.LOGGER.debug("Hiding");
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            MAIN_SCENE.getParentStage().hide();
                        }
                    });
                }
                //This condition means that some thread is already monitoring and waiting for the game to be restarted, no need to monitor
                if (!entry.isMonitored()) {
                    entry.setMonitored(true);
                    Monitor timeMonitor = new Monitor(GameStarter.this);
                    return timeMonitor.start(null);
                }
                return new Long(-1);
            }
        };
        monitor.valueProperty().addListener(new ChangeListener<Long>() {
            @Override
            public void changed(ObservableValue<? extends Long> observable, Long oldValue, Long newValue) {
                if (!newValue.equals(new Long(-1))) {
                    onPostGameLaunch(newValue);
                } else {
                    //No need to add playtime as if we are here, it means that some thread is already monitoring play time
                }
            }
        });
        Thread th = new Thread(monitor);
        th.setDaemon(true);

        th.start();
    }

    private void startGame() throws IOException, IllegalStateException {
        File preLog = FileUtils.initOrCreateFile(LOG_FOLDER + "pre_" + entry.getName() + ".log");

        Terminal terminal = new Terminal();
        String cmdBefore = entry.getCmd(GameEntry.CMD_BEFORE_START);
        String commandsBeforeString = settings().getStrings(PredefinedSetting.CMD)[GameEntry.CMD_BEFORE_START] + (cmdBefore != null ? "\n" + cmdBefore : "");
        String[] commandsBefore = commandsBeforeString.split("\n");

        if (entry.isSteamGame() || entry.getPath().startsWith(STEAM_PREFIX)) {
            terminal.execute(commandsBefore, preLog);
            try {
                String steamUri = entry.getPath();
                Desktop.getDesktop().browse(new URI(steamUri));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        } else {
            terminal.execute(commandsBefore, preLog, getGameParentFolder());

            File gameLog = new File(LOG_FOLDER + entry.getProcessName() + ".log");
            ProcessBuilder gameProcessBuilder = new ProcessBuilder(getStartGameCMD()).inheritIO();

            gameProcessBuilder.redirectOutput(gameLog);
            gameProcessBuilder.redirectError(gameLog);
            gameProcessBuilder.directory(new File(new File(entry.getPath()).getParent()));

            if (entry.getOnGameLaunched() != null) {
                entry.getOnGameLaunched().run();
            }

            if (MAIN_SCENE != null) {
                GeneralToast.displayToast(entry.getName() + Main.getString("launched"), MAIN_SCENE.getParentStage());
            }

            Process gameProcess = gameProcessBuilder.start();
        }
    }

    private java.util.List<String> getStartGameCMD() throws IllegalStateException {
        String[] args = entry.getArgs().split(" ");
        ArrayList<String> commands = new ArrayList<>();
        if (entry.mustRunAsAdmin()) {
            commands.add("powershell.exe");
            commands.add("Start-Process");
        }
        if (entry.getPlatform().isPC()) {
            commands.add('"' + entry.getPath() + '"');
        } else {
            Emulator e = Emulator.getChosenEmulator(entry.getPlatform());
            if (e == null) {
                //TODO replace hardcoded text
                throw new IllegalStateException("no_emu_configured");
            } else {
                commands.addAll(e.getCommandsToExecute(entry));
            }
        }
        Collections.addAll(commands, args);
        if (entry.mustRunAsAdmin()) {
            commands.add("-verb");
            commands.add("RunAs");
        }
        return commands;
    }

    private void onPreGameLaunch(){
        originalPowerMode = PowerMode.getActivePowerMode();
        if (settings().getBoolean(PredefinedSetting.ENABLE_GAMING_POWER_MODE) && !entry.isMonitored() && entry.isInstalled()) {
            settings().getPowerMode(PredefinedSetting.GAMING_POWER_MODE).activate();
        }
        entry.setSavedLocally(true);
        entry.setLastPlayedDate(LocalDateTime.now());
        entry.setSavedLocally(false);
    }

    private void onPostGameLaunch(long playtime){
        Main.LOGGER.debug("Adding " + Math.round(playtime / 1000.0) + "s to game " + entry.getName());

        if (entry.getOnGameStopped() != null) {
            entry.getOnGameStopped().run();
        }
        if (MAIN_SCENE != null) {
            GeneralToast.displayToast(entry.getName() + Main.getString("stopped"), MAIN_SCENE.getParentStage());
        }

        String cmdAfter = entry.getCmd(GameEntry.CMD_AFTER_END);
        String commandsAfterString = settings().getStrings(PredefinedSetting.CMD)[GameEntry.CMD_AFTER_END] + (cmdAfter != null ? "\n" + cmdAfter : "");
        LOGGER.debug("commandsAfter : \"" + commandsAfterString + "\"");
        String[] commandsAfter = commandsAfterString.split("\n");
        File postLog = new File(LOG_FOLDER + "post_" + entry.getName() + ".log");
        try {
            Terminal terminal = new Terminal();
            File path = new File(entry.getPath());
            if (!path.exists()) {
                //this is a steam game
                terminal.execute(commandsAfter, postLog);
            } else {
                File dir = new File(path.getParent());
                if (dir.exists() && dir.isDirectory()) {
                    terminal.execute(commandsAfter, postLog, dir);
                } else {
                    terminal.execute(commandsAfter, postLog);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        entry.setMonitored(false);
        MAIN_SCENE.updateGame(entry);
        if(playtime > 0) {
            String notificationText = GameEntry.getPlayTimeFormatted(Math.round(playtime / 1000.0), GameEntry.TIME_FORMAT_HMS_CASUAL) + " "
                    + Main.getString("tray_icon_time_recorded") + " "
                    + entry.getName();
            if (!settings().getBoolean(PredefinedSetting.NO_NOTIFICATIONS) && playtime != 0) {
                Main.TRAY_ICON.displayMessage("GameRoom", notificationText, TrayIcon.MessageType.INFO);
            }
            GeneralToast.displayToast(notificationText, MAIN_SCENE.getParentStage());
        }
    }


    void onStop() {
        if (settings().getBoolean(PredefinedSetting.ENABLE_GAMING_POWER_MODE)) {
            originalPowerMode.activate();
        }
        if (MAIN_SCENE != null) {
            MAIN_SCENE.updateGame(entry);
        }
    }



    private File getGameParentFolder() {
        return new File(new File(entry.getPath()).getParent());
    }

    public GameEntry getGameEntry() {
        return entry;
    }
}

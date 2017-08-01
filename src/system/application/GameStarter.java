package system.application;

import data.game.entry.Emulator;
import data.io.FileUtils;
import data.game.entry.GameEntry;
import javafx.application.Platform;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import system.application.settings.PredefinedSetting;
import system.os.PowerMode;
import system.os.Terminal;
import ui.Main;
import ui.GeneralToast;
import ui.dialog.GameRoomAlert;
import ui.dialog.EmulationDialog;
import ui.scene.SettingsScene;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static system.application.settings.GeneralSettings.settings;
import static ui.Main.*;


/**
 * Created by LM on 14/07/2016.
 */
public class GameStarter {
    private final static String ERR_NO_EMU = "no_emu_configured";
    private final static String ERR_NOT_SUPPORTER = "not_supporter";
    private final static String STEAM_PREFIX = "steam";

    private final static long VALUE_ALREADY_MONITORED = -1;

    private GameEntry entry;
    private PowerMode originalPowerMode;
    private static String LOG_FOLDER;


    public GameStarter(GameEntry entry) {
        this.entry = entry;
        if (LOG_FOLDER == null) {
            LOG_FOLDER = FILES_MAP.get("games_log").getAbsolutePath() + File.separator;
        }
    }

    public void start() {
        Main.LOGGER.info("Starting game : " + entry.getName());

        try {
            onPreGameLaunch();
            startGame();
            onPostGameLaunched();
        } catch (IllegalStateException | IOException e) {
            onStop(0);
            switch (e.getMessage()) {
                case ERR_NO_EMU:
                    GameRoomAlert.error("There is no emulator configured for platform " + entry.getPlatform());
                    ButtonType okButton = new ButtonType(Main.getString("start_game"), ButtonBar.ButtonData.OK_DONE);

                    EmulationDialog dialog = new EmulationDialog(entry.getPlatform(), okButton);
                    dialog.showAndWait().ifPresent(buttonType -> {
                        if (buttonType.getButtonData().equals(ButtonBar.ButtonData.OK_DONE)) {
                            start();
                        }
                    });
                    break;
                case ERR_NOT_SUPPORTER:
                    SettingsScene.checkAndDisplayRegisterDialog();
                    break;
                default:
                    e.printStackTrace();
                    GameRoomAlert.error(e.getMessage());
                    break;
            }
            return;
        }

        try {
            Monitor timeMonitor = new Monitor(this);
            timeMonitor.start();
        } catch (IOException e) {
            GeneralToast.displayToast(Main.getString("error_cannot_monitor") + getGameEntry().getName(),MAIN_SCENE.getParentStage());
            e.printStackTrace();
        }

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
            List<String> commands = getStartGameCMD();
            ProcessBuilder gameProcessBuilder = new ProcessBuilder(commands).inheritIO();

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
        ArrayList<String> commands = new ArrayList<>();
        commands.add("powershell.exe");
        commands.add("-Command");
        if (entry.getPlatform().isPCLauncher()) {
            commands.add(getPowerShellAdminCMD(entry.getPath(), Terminal.splitCMDLine(entry.getArgs()), entry.mustRunAsAdmin()));
        } else {
            if (SUPPORTER_MODE) {
                Emulator e = Emulator.getChosenEmulator(entry.getPlatform());
                if (e == null) {
                    throw new IllegalStateException(ERR_NO_EMU);
                } else {
                    commands.add(getPowerShellAdminCMD(e.getPath().getAbsolutePath(), e.getCommandArguments(entry), entry.mustRunAsAdmin()));
                }
            } else {
                throw new IllegalStateException(ERR_NOT_SUPPORTER);
            }
        }
        return commands;
    }

    private static String getPowerShellAdminCMD(String path, List<String> args, boolean asAdmin) {
        StringBuilder powerShellCmd = new StringBuilder("Start-Process -FilePath ");
        powerShellCmd.append("\'\\\""); //PS syntax + blank escaping
        powerShellCmd.append(path);
        powerShellCmd.append("\\\"\'"); //PS syntax + blank escaping
        if (args != null && !args.isEmpty()) {
            powerShellCmd.append(" -ArgumentList ");

            for (String s : args) {
                if(!s.trim().isEmpty()) {
                    powerShellCmd.append("\'\\\""); //PS syntax + blank escaping
                    powerShellCmd.append(s);
                    powerShellCmd.append("\\\"\'"); //PS syntax + blank escaping
                    powerShellCmd.append(',');
                }
            }
            powerShellCmd.delete(powerShellCmd.length() - 1, powerShellCmd.length()); //removes the last ','
        }
        if (asAdmin) {
            powerShellCmd.append(" -Verb runAs");
        }
        return powerShellCmd.toString();
    }

    private void onPreGameLaunch() {
        originalPowerMode = PowerMode.getActivePowerMode();
        if (settings().getBoolean(PredefinedSetting.ENABLE_GAMING_POWER_MODE) && !entry.isMonitored() && entry.isInstalled()) {
            settings().getPowerMode(PredefinedSetting.GAMING_POWER_MODE).activate();
        }
        entry.setSavedLocally(true);
        entry.setLastPlayedDate(LocalDateTime.now());
        entry.setSavedLocally(false);
    }

    private void onPostGameLaunched(){
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
    }

    private void onPostGameStopped(long playtime) {
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
        if (playtime > 0) {
            String notificationText = GameEntry.getPlayTimeFormatted(Math.round(playtime / 1000.0), GameEntry.TIME_FORMAT_HMS_CASUAL) + " "
                    + Main.getString("tray_icon_time_recorded") + " "
                    + entry.getName();
            if (!settings().getBoolean(PredefinedSetting.NO_NOTIFICATIONS) && playtime != 0) {
                Main.TRAY_ICON.displayMessage("GameRoom", notificationText, TrayIcon.MessageType.NONE);
            }
            GeneralToast.displayToast(notificationText, MAIN_SCENE.getParentStage());
        }
    }


    void onStop(long playtime) {
        onPostGameStopped(playtime);

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

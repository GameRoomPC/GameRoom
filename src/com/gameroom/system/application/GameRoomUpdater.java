package com.gameroom.system.application;

import com.gameroom.data.http.URLTools;
import de.dimaki.refuel.updater.boundary.Updater;
import de.dimaki.refuel.updater.entity.ApplicationStatus;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import com.gameroom.system.SchedulableTask;
import com.gameroom.system.application.settings.PredefinedSetting;
import com.gameroom.system.internet.FileDownloader;
import com.gameroom.ui.Main;
import com.gameroom.ui.dialog.GameRoomAlert;
import com.gameroom.ui.dialog.UpdateDialog;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static com.gameroom.system.application.settings.GeneralSettings.settings;
import static com.gameroom.ui.Main.*;

/**
 * Created by LM on 26/12/2016.
 */
public class GameRoomUpdater {
    public final static String HTTPS_HOST = "gameroom.me";
    private final static String HTTP_HOST = "s639232867.onlinehome.fr";
    private final static String URL_VERSION_XML_SUFFIX = DEV_MODE ? "/software/test_version.xml" : "/software/version.xml";
    private final static String URL_CHANGELOG_MD_SUFFIX = "/software/changelog.md";

    private String currentVersion;
    private URL updateUrl;
    private URL changelogUrl;
    private File workingDir;
    private ChangeListener<Number> changeListener;
    private ChangeListener<? super EventHandler<WorkerStateEvent>> failedProperty;
    private ChangeListener<? super EventHandler<WorkerStateEvent>> succeedProperty;
    private ChangeListener<? super EventHandler<WorkerStateEvent>> noUpdateListener;
    private ChangeListener<? super EventHandler<WorkerStateEvent>> cancelledListener;
    private ChangeListener<? super EventHandler<WorkerStateEvent>> onUpdatePressedListener;

    private boolean started = false;
    private volatile boolean isReminding = false;
    private SchedulableTask<Void> remindTask;
    private Future<?> pingFuture;
    private FileDownloader fdl;
    private final static GameRoomUpdater updaterInstance = new GameRoomUpdater();


    private GameRoomUpdater() {
        this.currentVersion = Main.getVersion();
        Task t = new Task() {
            @Override
            protected Object call() throws Exception {
                String domain = getDomain();
                try {
                    updateUrl = new URL(domain + URL_VERSION_XML_SUFFIX);
                    changelogUrl = new URL(domain + URL_CHANGELOG_MD_SUFFIX);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        pingFuture = Main.getExecutorService().submit(t);

        this.workingDir = Main.FILES_MAP.get("cache");
        this.changeListener = changeListener;
    }

    public static GameRoomUpdater getInstance() {
        return updaterInstance;
    }

    public void start() {
        try {
            pingFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        started = true;
        if (settings() != null) {
            settings().setSettingValue(PredefinedSetting.LAST_UPDATE_CHECK, new Date());
        }
        Updater updater = new Updater();
        LOGGER.info("Starting updater");

        ApplicationStatus status = updater.getApplicationStatus(currentVersion, updateUrl);
        if (status == null || status.getInfo() == null || status.getInfo().toLowerCase().contains("timeout")) {
            noUpdate();
            return;
        }

        if (status.getInfo().startsWith("Unknown Host")) {
            noUpdate();
            return;
        }
        if (status.getInfo().startsWith("No update available")) {
            noUpdate();
            return;
        }

        Main.LOGGER.info("Received info : " + status.getInfo());
        openUpdateDialog(status);
    }

    private void openUpdateDialog(ApplicationStatus status) {
        Platform.runLater(() -> {
            try {
                UpdateDialog updateDialog = new UpdateDialog(currentVersion, status.getInfo(), changelogUrl);
                Optional<ButtonType> result = updateDialog.showAndWait();
                result.ifPresent(letter -> {
                    if (letter.getText().equals(Main.getString("update"))) {
                        if (onUpdatePressedListener != null) {
                            onUpdatePressedListener.changed(null, null, null);
                        }
                        downloadUpdate(status);
                    } else {
                        updateDialog.close();
                        cancel();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                GameRoomAlert.error(Main.getString("error_check_updates"));
                cancel();
            }
        });
    }

    private void cancel() {
        started = false;
        if (cancelledListener != null) {
            cancelledListener.changed(null, null, null);
        }
    }

    private void noUpdate() {
        Main.LOGGER.info("No update found.");
        if (noUpdateListener != null) {
            noUpdateListener.changed(null, null, null);
        }
        started = false;
    }

    public boolean isStarted() {
        return started;
    }

    private void downloadUpdate(ApplicationStatus status) {
        fdl = new FileDownloader(status.getAppcast().getLatestEnclosure().getUrl(), workingDir.toPath(), false);
        if (changeListener != null) {
            fdl.progressProperty().addListener(changeListener);
        }
        if (failedProperty != null) {
            fdl.onFailedProperty().addListener(failedProperty);
        }
        if (succeedProperty != null) {
            fdl.onSucceededProperty().addListener(succeedProperty);
        }

        fdl.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                onDownloadFinished();
            }
        });
        fdl.startDownload();
    }

    private void onDownloadFinished() {
        remindTask = new SchedulableTask<Void>(0, TimeUnit.MINUTES.toMillis(1)) {
            @Override
            protected Void execute() throws Exception {
                Main.runAndWait(() -> {
                    if (!isReminding) {
                        isReminding = true;
                        GameRoomAlert alert = new GameRoomAlert(Alert.AlertType.INFORMATION
                                , Main.getString("gameroom_will_close_updater"));
                        alert.getButtonTypes().add(new ButtonType(Main.getString("remind_me_in_1mn")));
                        Optional<ButtonType> result = alert.showAndWait();
                        result.ifPresent(letter -> {
                            if (letter.getButtonData().equals(ButtonBar.ButtonData.OK_DONE)) {
                                isReminding = false;
                                remindTask.cancel();
                                try {
                                    Process process = new ProcessBuilder()
                                            .command(fdl.getOutputFile().getAbsolutePath())
                                            .redirectErrorStream(true)
                                            .start();
                                    started = false;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                Main.forceStop(MAIN_SCENE.getParentStage(), "Applying update");
                            } else if (letter.getText().equals(Main.getString("remind_me_in_1mn"))) {
                                isReminding = false;
                            }
                        });
                    }
                });
                return null;
            }
        };
        remindTask.scheduleAtFixedDelayOn(Main.getScheduledExecutor());

    }

    private static String getDomain() {
        boolean httpsOnline = URLTools.pingHttps(HTTPS_HOST, 2000);
        return httpsOnline ? URLTools.HTTPS_PREFIX + HTTPS_HOST : URLTools.HTTP_PREFIX + HTTP_HOST;
    }

    public void setSucceedPropertyListener(ChangeListener<? super EventHandler<WorkerStateEvent>> succeedProperty) {
        if (fdl != null) {
            if (this.succeedProperty != null) {
                fdl.onSucceededProperty().removeListener(this.succeedProperty);
            }
            if (succeedProperty != null) {
                fdl.onSucceededProperty().addListener(succeedProperty);
            }
        }
        this.succeedProperty = succeedProperty;
    }

    public void setFailedPropertyListener(ChangeListener<? super EventHandler<WorkerStateEvent>> failedProperty) {
        if (fdl != null) {
            if (this.failedProperty != null) {
                fdl.onFailedProperty().removeListener(this.failedProperty);
            }
            if (failedProperty != null) {
                fdl.onFailedProperty().addListener(failedProperty);
            }
        }
        this.failedProperty = failedProperty;

    }

    public void setNoUpdateListener(ChangeListener<? super EventHandler<WorkerStateEvent>> noUpdateListener) {
        this.noUpdateListener = noUpdateListener;
    }

    public void setCancelledListener(ChangeListener<? super EventHandler<WorkerStateEvent>> cancelledListener) {
        if (fdl != null) {
            if (this.cancelledListener != null) {
                fdl.onCancelledProperty().removeListener(this.cancelledListener);
            }
            if (cancelledListener != null) {
                fdl.onCancelledProperty().addListener(cancelledListener);
            }
        }
        this.cancelledListener = cancelledListener;
    }

    public void setChangeListener(ChangeListener<Number> changeListener) {
        if (fdl != null) {
            if (this.changeListener != null) {
                fdl.progressProperty().removeListener(this.changeListener);
            }
            if (changeListener != null) {
                fdl.progressProperty().addListener(changeListener);
            }
        }
        this.changeListener = changeListener;
    }

    public void setOnUpdatePressedListener(ChangeListener<? super EventHandler<WorkerStateEvent>> onUpdatePressedListener) {
        this.onUpdatePressedListener = onUpdatePressedListener;
    }
}

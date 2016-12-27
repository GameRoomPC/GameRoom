package system.application;

import data.http.URLTools;
import de.dimaki.refuel.updater.boundary.Updater;
import de.dimaki.refuel.updater.entity.ApplicationStatus;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import system.internet.FileDownloader;
import system.os.Terminal;
import ui.Main;
import ui.dialog.GameRoomAlert;
import ui.dialog.UpdateDialog;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import static ui.Main.LOGGER;
import static ui.Main.MAIN_SCENE;
import static ui.Main.RESSOURCE_BUNDLE;

/**
 * Created by LM on 26/12/2016.
 */
public class GameRoomUpdater {
    private final static String HTTPS_HOST = "gameroom.me";
    private final static String HTTP_HOST = "s639232867.onlinehome.fr";
    private final static String URL_VERSION_XML_SUFFIX = "/software/version.xml";
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
    private FileDownloader fdl;
    private final static GameRoomUpdater updaterInstance = new GameRoomUpdater();


    private GameRoomUpdater() {
        this.currentVersion = Main.getVersion();
        String domain = getDomain();
        try {
            this.updateUrl = new URL(domain + URL_VERSION_XML_SUFFIX);
            this.changelogUrl = new URL(domain + URL_CHANGELOG_MD_SUFFIX);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        this.workingDir = Main.FILES_MAP.get("cache");
        this.changeListener = changeListener;
    }

    public static GameRoomUpdater getInstance() {
        return updaterInstance;
    }

    public void start() {
        started = true;
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

        UpdateDialog updateDialog = new UpdateDialog(currentVersion, status.getInfo(), changelogUrl);
        Optional<ButtonType> result = updateDialog.showAndWait();
        result.ifPresent(letter -> {
            if (letter.getText().equals(RESSOURCE_BUNDLE.getString("update"))) {
                if (onUpdatePressedListener != null) {
                    onUpdatePressedListener.changed(null, null, null);
                }
                downloadUpdate(status);
            } else {
                updateDialog.close();
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
                onDownloadfinished();
            }
        });
        fdl.startDownload();
    }

    private void onDownloadfinished(){
        GameRoomAlert alert = new GameRoomAlert(Alert.AlertType.INFORMATION
                , Main.RESSOURCE_BUNDLE.getString("gameroom_will_close_updater"));
        alert.getButtonTypes().add(new ButtonType(Main.RESSOURCE_BUNDLE.getString("remind_me_in_1mn")));
        Optional<ButtonType> result = alert.showAndWait();
        result.ifPresent(letter -> {
            if (letter.getButtonData().equals(ButtonBar.ButtonData.OK_DONE)) {
                try {
                    Process process = new ProcessBuilder()
                            .command(workingDir.getAbsolutePath() + File.separator + fdl.getOutputFile().getName())
                            .redirectErrorStream(true)
                            .start();
                    started = false;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Main.forceStop(MAIN_SCENE.getParentStage(), "Applying update");
            } else if(letter.getText().equals(Main.RESSOURCE_BUNDLE.getString("remind_me_in_1mn"))) {
                Thread reminderThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(60000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                onDownloadfinished();
                            }
                        });
                    }
                });
                reminderThread.setDaemon(true);
                reminderThread.start();
            }
        });
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

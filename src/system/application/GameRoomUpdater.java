package system.application;

import data.http.URLTools;
import de.dimaki.refuel.updater.boundary.Updater;
import de.dimaki.refuel.updater.entity.ApplicationStatus;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
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

    public GameRoomUpdater(ChangeListener<Number> changeListener) throws MalformedURLException {
        this.currentVersion = Main.getVersion();
        String domain = getDomain();
        this.updateUrl = new URL(domain + URL_VERSION_XML_SUFFIX);
        this.changelogUrl = new URL(domain + URL_CHANGELOG_MD_SUFFIX);
        this.workingDir = Main.FILES_MAP.get("cache");
        this.changeListener = changeListener;
    }

    public void start() {
        Updater updater = new Updater();
        LOGGER.info("Starting updater");

        ApplicationStatus status = updater.getApplicationStatus(currentVersion, updateUrl);
        if(status == null || status.getInfo() == null || status.getInfo().toLowerCase().contains("timeout")){
            noUpdate();
            return;
        }

        if(status.getInfo().startsWith("Unknown Host")){
            noUpdate();
            return;
        }
        if(status.getInfo().startsWith("No update available")){
            noUpdate();
            return;
        }

        Main.LOGGER.info("Received info : " + status.getInfo());

        UpdateDialog updateDialog = new UpdateDialog(currentVersion, status.getInfo(), changelogUrl);
        Optional<ButtonType> result = updateDialog.showAndWait();
        result.ifPresent(letter -> {
            if (letter.getText().equals(RESSOURCE_BUNDLE.getString("update"))) {
                downloadUpdate(status);
            } else {
                updateDialog.close();
                cancel();
            }
        });
    }
    private void cancel(){
        cancelledListener.changed(null,null,null);
    }

    private void noUpdate(){
        Main.LOGGER.info("No update found.");
        noUpdateListener.changed(null,null,null);
    }

    private void downloadUpdate(ApplicationStatus status) {
        FileDownloader fdl = new FileDownloader(status.getAppcast().getLatestEnclosure().getUrl(),workingDir.toPath(),false);
        fdl.progressProperty().addListener(changeListener);
        fdl.onFailedProperty().addListener(failedProperty);
        fdl.onSucceededProperty().addListener(succeedProperty);

        fdl.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                try {
                    GameRoomAlert alert = new GameRoomAlert(Alert.AlertType.INFORMATION
                            ,Main.RESSOURCE_BUNDLE.getString("gameroom_will_close_updater"));
                    alert.showAndWait();
                    Process process = new ProcessBuilder()
                            .command(workingDir.getAbsolutePath()+File.separator+fdl.getOutputFile().getName())
                            .redirectErrorStream(true)
                            .start();
                    Main.forceStop(MAIN_SCENE.getParentStage(),"Applying update");
                }catch (IOException e) {
                    e.printStackTrace();
                    fdl.errorDownload();
                }
            }
        });
        fdl.startDownload();
    }

    private static String getDomain() {
        boolean httpsOnline = URLTools.pingHttps(HTTPS_HOST, 2000);
        return httpsOnline ? URLTools.HTTPS_PREFIX + HTTPS_HOST : URLTools.HTTP_PREFIX + HTTP_HOST;
    }

    public void setSucceedPropertyListener(ChangeListener<? super EventHandler<WorkerStateEvent>> succeedProperty) {
        this.succeedProperty = succeedProperty;
    }

    public void setFailedPropertyListener(ChangeListener<? super EventHandler<WorkerStateEvent>> failedProperty) {
        this.failedProperty = failedProperty;
    }

    public void setNoUpdateListener(ChangeListener<? super EventHandler<WorkerStateEvent>> noUpdateListener) {
        this.noUpdateListener = noUpdateListener;
    }

    public void setCancelledListener(ChangeListener<? super EventHandler<WorkerStateEvent>> cancelledListener) {
        this.cancelledListener = cancelledListener;
    }
}

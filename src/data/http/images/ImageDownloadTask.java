package data.http.images;

import data.game.scraper.OnDLDoneHandler;
import data.http.HTTPDownloader;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import ui.Main;

import java.io.File;

/**
 * Created by LM on 08/01/2017.
 */
public class ImageDownloadTask extends Task {
    private String url;
    private File outputFile;
    private OnDLDoneHandler handler;

    public ImageDownloadTask(String url, File outputFile, OnDLDoneHandler handler){
        this.url = url;
        this.outputFile = outputFile;
        this.handler = handler;

        setOnSucceeded(event -> {
            if (handler != null) {
                handler.run(outputFile);
            }
        });
    }

    @Override
    protected Object call() throws Exception {
        if (outputFile.exists()) {
            //this ensures that if the cached image is corrupted, we re-dl it
            Image img = new Image("file:///" + outputFile.getAbsolutePath());
            if (img.getException() != null) {
                handler.run(outputFile);
            }
        }

        try {
            Main.LOGGER.debug("Downloading " + url + " to " + outputFile);
            HTTPDownloader.downloadFile(url, Main.FILES_MAP.get("cache").getAbsolutePath(), outputFile.getName());
            Main.LOGGER.debug(outputFile + " downloaded");
        } catch (Exception e) {
            Main.LOGGER.error(e.toString());
            throw e;
        }
        return null;
    }

    public String getUrl() {
        return url;
    }
}

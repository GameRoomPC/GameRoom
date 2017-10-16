package data.http.images;

import data.game.scraper.OnDLDoneHandler;
import data.http.HTTPDownloader;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import ui.Main;

import java.io.File;
import java.io.IOException;

/**
 * Tasks objects used to submit download tasks to some {@link java.util.concurrent.ExecutorService} or similar. This is
 * dedicated to downloading images, and allow to set alternative URLs to download our image from, as for example downscaled
 * IGDB images.
 *
 * @author LM. Garret (admin@gameroom.me)
 * @date 08/01/2017.
 */
public class ImageDownloadTask extends Task {
    private final static int ORIGINAL_URL_INDEX = -1;
    private String url;
    private File outputFile;
    private OnDLDoneHandler handler;
    private String[] alternativeURLs = new String[0];
    private int urlIndex = ORIGINAL_URL_INDEX;

    public ImageDownloadTask(String url, File outputFile, OnDLDoneHandler handler) {
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
            while (!tryDownloadImage(urlIndex++)) ;
            //we try to download our image or alternatives if it fails
        } catch (IllegalStateException ignored) {
            //we have no more alternatives :(
        }

        return null;
    }

    /**
     * Attempts to download the image pointed by the index. Basically, when index is {@link #ORIGINAL_URL_INDEX}, we try
     * to download the original url, and others are for possible {@link #alternativeURLs}.
     * Returns true if the image was downloaded, false otherwise
     *
     * @param urlIndex the index of the image url to use
     * @return true if the image was downloaded, false otherwise (an exception occurred)
     * @throws IllegalStateException if the urlIndex is out of bound of {@link #alternativeURLs}
     */
    private boolean tryDownloadImage(int urlIndex) throws IllegalStateException {
        if (urlIndex == ORIGINAL_URL_INDEX) {
            try {
                Main.LOGGER.debug("Downloading " + url + " to " + outputFile);
                boolean downloaded = HTTPDownloader.downloadFile(url, Main.FILES_MAP.get("cache").getAbsolutePath(), outputFile.getName());
                if (downloaded) {
                    Main.LOGGER.debug(outputFile + " downloaded");
                }
                return downloaded;
            } catch (IOException e) {
                Main.LOGGER.error("Error downloading image " + url);
                Main.LOGGER.error(e.toString());
                return false;
            }
        } else {
            if (urlIndex >= alternativeURLs.length) {
                throw new IllegalStateException("No alternatives anymore!");
            }
            try {
                Main.LOGGER.debug("Downloading alternative " + alternativeURLs[urlIndex] + " to " + outputFile);
                boolean downloaded = HTTPDownloader.downloadFile(alternativeURLs[urlIndex], Main.FILES_MAP.get("cache").getAbsolutePath(), outputFile.getName());
                if (downloaded) {
                    Main.LOGGER.debug(outputFile + " alternative downloaded");
                }
                return downloaded;
            } catch (Exception e2) {
                Main.LOGGER.error("Error downloading alternative image " + alternativeURLs[urlIndex]);
                Main.LOGGER.error(e2.toString());
                return false;
            }
        }
    }

    public String getUrl() {
        return url;
    }

    /**
     * Sets alternative URLs to use in case the first download fails. Should be order by preference
     *
     * @param urls alternative urls to use
     */
    public void setAlternativeURLs(String... urls) {
        alternativeURLs = urls.clone();
    }
}

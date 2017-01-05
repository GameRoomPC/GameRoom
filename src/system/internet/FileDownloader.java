package system.internet;

/**
 * Created by LM on 30/07/2016.
 */

import javafx.concurrent.Task;
import ui.Main;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

import static system.internet.DownloadStatus.ERROR;
import static ui.Main.LOGGER;


// This class downloads a file from a URL.
public class FileDownloader extends Task<Path> {
    private final static long SPEED_MEASURE_FREQUENCY = 500;
    private static final int BUFFER_SIZE = 4096;
    private URL url; // download URL
    private Path downloadPath;
    private boolean deleteOnExit = false;
    private File outputFile;
    private int size; // size of download in bytes
    private int downloaded; // number of bytes downloaded

    private volatile DownloadStatus status;

    public FileDownloader(String url, Path downloadPath, boolean deleteOnExit) {
        this.downloadPath = downloadPath;
        this.deleteOnExit = deleteOnExit;
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            errorDownload();
        }
        size = -1;
        downloaded = 0;
        status = DownloadStatus.DOWNLOADING;
    }


    // Get this download's size.
    public int getSize() {
        return size;
    }

    @Override
    protected Path call() throws Exception {
        outputFile = new File(downloadPath + File.separator + getFileName(url));
        if (deleteOnExit) {
            outputFile.deleteOnExit();
        }

        try {
            boolean isHttps = url.toString().startsWith("https");
            HttpURLConnection httpConn = isHttps ? (HttpsURLConnection) url.openConnection() : (HttpURLConnection) url.openConnection();
            httpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
            size = httpConn.getContentLength();

            LOGGER.info("Size of download " + size);

            int responseCode = httpConn.getResponseCode();

            // always check HTTP response code first
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // opens input stream from the HTTP connection
                InputStream inputStream = httpConn.getInputStream();

                // opens an output stream to save into file
                FileOutputStream outputStream = new FileOutputStream(outputFile);

                int bytesRead = -1;
                byte[] buffer = new byte[BUFFER_SIZE];
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    downloaded += bytesRead;
                    updateProgress(downloaded, size);
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.close();
                inputStream.close();
            } else {
                System.out.println("No file to download. Server replied HTTP code: " + responseCode);
            }
            httpConn.disconnect();
            succeeded();
        } catch (IOException e) {
            LOGGER.error("Error while trying to download the file.");
            e.printStackTrace();
            errorDownload();
        }

        if (status.equals(DownloadStatus.CANCELLED)) {
            LOGGER.info("Download cancelled");
            failed();
        } else if (status.equals(DownloadStatus.ERROR)) {
            LOGGER.error("An error occured during the download");
            failed();
        }
        succeeded();
        LOGGER.info("File downloaded!");
        return outputFile.toPath();
    }

    public void startDownload() {
        download();
    }

    private void download() {
        Thread th = new Thread(this);
        th.setDaemon(true);
        th.start();
    }

    // Mark this download as having an error.
    public void errorDownload() {
        status = ERROR;
        updateMessage(Main.getString("error"));
        failed();
    }

    // Get file name portion of URL.
    private String getFileName(URL url) {
        String fileName = url.getFile();
        return fileName.substring(fileName.lastIndexOf('/') + 1);
    }

    private String getSizeReadable(boolean si, long size) {
        int unit = si ? 1000 : 1024;
        if (size < unit) return size + " B";
        int exp = (int) (Math.log(size) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", size / Math.pow(unit, exp), pre);
    }

    public File getOutputFile() {
        return outputFile;
    }
}
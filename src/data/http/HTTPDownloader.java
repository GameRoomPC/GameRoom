package data.http;

/**
 * Created by LM on 13/07/2016.
 */

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A utility that downloads a file from a URL.
 *
 * @author www.codejava.net
 */
public class HTTPDownloader {
    private static final int BUFFER_SIZE = 4096;
    private final static String HTTPS = "https";

    /**
     * Downloads a file from a URL
     *
     * @param fileURL HTTP URL of the file to be downloaded
     * @param saveDir path of the directory to save the file
     * @throws IOException
     */
    public static boolean downloadFile(String fileURL, String saveDir, String fileName)
            throws IOException {

        URL url = new URL(fileURL);

        boolean isHttps = fileURL.startsWith(HTTPS);
        HttpURLConnection httpConn = isHttps ? (HttpsURLConnection) url.openConnection() : (HttpURLConnection) url.openConnection();
        httpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
        int responseCode = httpConn.getResponseCode();

        // always check HTTP response code first
        if (responseCode == HttpURLConnection.HTTP_OK) {

            String disposition = httpConn.getHeaderField("Content-Disposition");
            String contentType = httpConn.getContentType();
            int contentLength = httpConn.getContentLength();

            /*System.out.println("Content-Type = " + contentType);
            System.out.println("Content-Disposition = " + disposition);
            System.out.println("Content-Length = " + contentLength);
            System.out.println("fileName = " + fileName);*/

            // opens input stream from the HTTP connection
            InputStream inputStream = httpConn.getInputStream();
            String saveFilePath = saveDir + File.separator + fileName;

            // opens an output stream to save into file
            FileOutputStream outputStream = new FileOutputStream(saveFilePath);

            int bytesRead = -1;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();
            httpConn.disconnect();
            //System.out.println("File downloaded");
            return true;
        } else {
            httpConn.disconnect();
            System.out.println("No file to download. Server replied HTTP code: " + responseCode);
            return false;
        }
    }

    public static void main(String[] args) {
        String imageURL = "https://images.igdb.com/igdb/image/upload/t_cover_big_2x/vdzfsbissgp55fvfxccp.jpg";
        File output = new File("cache/");
        try {
            output.mkdir();
            downloadFile(imageURL, output.getAbsolutePath(), "1020_cover_big_2x.jpg");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
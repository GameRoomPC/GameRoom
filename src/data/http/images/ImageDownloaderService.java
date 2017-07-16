package data.http.images;

/**
 * Created by LM on 08/01/2017.
 */
public class ImageDownloaderService {
    private final static int MAX_THREADS = 8;

    private static ImageDownloaderService service;

    public static ImageDownloaderService getInstance() {
        if (service == null) {
            service = new ImageDownloaderService();
        }
        return service;
    }

    public void addTask(ImageDownloadTask task) {
        ImageUtils.getExecutorService().submit(task);
    }

    public void shutDownNow() {
        ImageUtils.getExecutorService().shutdownNow();
    }
}

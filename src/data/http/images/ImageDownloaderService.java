package data.http.images;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by LM on 08/01/2017.
 */
public class ImageDownloaderService {
    private final static int MAX_THREADS = 8;

    private final ExecutorService executorService;

    private static ImageDownloaderService service;

    private ImageDownloaderService(){
        executorService =Executors.newFixedThreadPool(MAX_THREADS);
    }
    public static ImageDownloaderService getInstance(){
        if(service == null){
            service = new ImageDownloaderService();
        }
        return service;
    }

    public void addTask(ImageDownloadTask task){
        executorService.submit(task);
    }

    public void shutDownNow(){
        executorService.shutdownNow();
    }
}

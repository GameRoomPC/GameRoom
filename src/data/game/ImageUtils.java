package data.game;

import data.io.HTTPDownloader;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import ui.Main;

import java.io.File;

import static ui.scene.BaseScene.FADE_IN_OUT_TIME;

/**
 * Created by LM on 06/08/2016.
 */
public class ImageUtils {

    /********************IGDB**************************/
    /* ABOUT FORMAT AND SIZE
    cover_small :Fit to 90 x 128
    screenshot_med : Lfill to 569 x 320 (Center gravity)
    cover_big : Fit to 227 x 320
    logo_med : Fit to 284 x 160
    screenshot_big : Lfill to 889 x 500 (Center gravity)
    screenshot_huge : Lfill to 1280 x 720 (Center gravity)
    thumb : Thumb to 90 x 90 (Center gravity)
    micro : Thumb to 35 x 35 (Center gravity)
     */
    public final static String IGDB_TYPE_COVER = "cover";
    public final static String IGDB_TYPE_SCREENSHOT = "screenshot";

    public final static String IGDB_SIZE_BIG_2X = "_big_2x";
    public final static String IGDB_SIZE_SMALL = "_small";
    public final static String IGDB_SIZE_MED = "_med";
    private final static String IGDB_IMAGE_URL_PREFIX = "https://res.cloudinary.com/igdb/image/upload/t_";

    /*******************STEAM ***************************/
    private final static String STEAM_IMAGE_URL_PREFIX = "http://cdn.akamai.steamstatic.com/steam/apps/";
    public final static String STEAM_TYPE_CAPSULE = "capsule";
    public final static String STEAM_TYPE_HEADER = "header";

    public final static String STEAM_SIZE_SMALL = "_sm_120";
    public final static String STEAM_SIZE_MEDIUM = "_616x353";

    /**
     * Simple queue implementation, so that image are downloaded as fast as possible but actions are done in order
     */
    //private static final ArrayList<Thread> threadsList = new ArrayList<>();

    public static File downloadSteamImageToCache(int steam_id,String type,String size,OnDLDoneHandler dlDoneHandler){
        String imageURL = STEAM_IMAGE_URL_PREFIX + steam_id +"/"+ type + (type.equals(STEAM_TYPE_HEADER)? "":size)+".jpg";
        String imageFileName = steam_id + "_" + type + (type.equals(STEAM_TYPE_HEADER)? "":size) + ".jpg";
        return downloadImgToCache(imageURL,imageFileName,dlDoneHandler);
    }

    public static File downloadIGDBImageToCache(int igdb_id, String imageHash, String type, String size, OnDLDoneHandler dlDoneHandler) {
        String imageURL = IGDB_IMAGE_URL_PREFIX + type + size + "/" + imageHash + ".jpg";
        String imageFileName = igdb_id + "_" + type + size + "_" + imageHash + ".jpg";
        return downloadImgToCache(imageURL,imageFileName,dlDoneHandler);
    }
    private static File downloadImgToCache(String url, String filenameOutput, OnDLDoneHandler dlDoneHandler){
        File imageOutputFile = new File(Main.CACHE_FOLDER + File.separator + filenameOutput);
        imageOutputFile.deleteOnExit();
        if (!imageOutputFile.exists()) {
            Task<String> imageDownloadTask = new Task<String>() {
                @Override
                protected String call() throws Exception {
                    try {
                        Main.LOGGER.debug("Downloading " + url + " to " + imageOutputFile);
                        HTTPDownloader.downloadFile(url, Main.CACHE_FOLDER.getAbsolutePath(), filenameOutput);
                        Main.LOGGER.debug(filenameOutput + " downloaded");
                    }catch (Exception e){
                        Main.LOGGER.error(e.toString());
                        throw e;
                    }
                    return null;
                }
            };
            Thread th = new Thread(imageDownloadTask);

            imageDownloadTask.setOnSucceeded(event -> {
                /*if (threadsList.size() > 0) {
                    Thread nextThread;
                    while (!(nextThread = threadsList.get(0)).equals(th)) {
                        try {
                            nextThread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                synchronized (threadsList) {
                    threadsList.remove(th);
                }*/
                if(dlDoneHandler!=null) {
                    dlDoneHandler.run(imageOutputFile);
                }
            });
            /*synchronized (threadsList){
                threadsList.add(th);
            }*/
            th.setDaemon(true);
            th.start();
        } else {
            dlDoneHandler.run(imageOutputFile);
        }
        return imageOutputFile;
    }
    public static void transitionToImage(Image image2, ImageView imageView, double finalOpaycity){
        Platform.runLater(() -> {

            Timeline fadeOutTimeline = new Timeline(
                    new KeyFrame(Duration.seconds(0),
                            new KeyValue(imageView.opacityProperty(), imageView.opacityProperty().getValue(), Interpolator.EASE_IN)),
                    new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                            new KeyValue(imageView.opacityProperty(), 0, Interpolator.EASE_OUT)
                    ));
            fadeOutTimeline.setCycleCount(1);
            fadeOutTimeline.setAutoReverse(false);
            fadeOutTimeline.setOnFinished(new EventHandler<javafx.event.ActionEvent>() {
                @Override
                public void handle(javafx.event.ActionEvent event) {
                    imageView.setImage(image2);
                    Timeline fadeInTimeline = new Timeline(
                            new KeyFrame(Duration.seconds(0),
                                    new KeyValue(imageView.opacityProperty(), 0, Interpolator.EASE_IN)),
                            new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                                    new KeyValue(imageView.opacityProperty(), finalOpaycity, Interpolator.EASE_OUT)
                            ));
                    fadeInTimeline.setCycleCount(1);
                    fadeInTimeline.setAutoReverse(false);
                    fadeInTimeline.play();
                }
            });
            fadeOutTimeline.play();
        });
    }

    public static void transitionToImage(Image image2, ImageView imageView){
            transitionToImage(image2,imageView,1);
    }

}

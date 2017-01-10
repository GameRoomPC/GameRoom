package data.http.images;

import data.game.scraper.OnDLDoneHandler;
import data.http.HTTPDownloader;
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
import ui.scene.BaseScene;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import static ui.Main.GENERAL_SETTINGS;
import static ui.scene.BaseScene.FADE_IN_OUT_TIME;

/**
 * Created by LM on 06/08/2016.
 */
public class ImageUtils {

    /********************
     * IGDB
     **************************/
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
    public final static String IGDB_SIZE_BIG = "_big";
    public final static String IGDB_SIZE_SMALL = "_small";
    public final static String IGDB_SIZE_MED = "_med";
    private final static String IGDB_IMAGE_URL_PREFIX = "https://images.igdb.com/igdb/image/upload/t_";

    /*******************
     * STEAM
     ***************************/
    private final static String STEAM_IMAGE_URL_PREFIX = "http://cdn.akamai.steamstatic.com/steam/apps/";
    public final static String STEAM_TYPE_CAPSULE = "capsule";
    private final static String STEAM_TYPE_HEADER = "header";

    public final static String STEAM_SIZE_SMALL = "_sm_120";
    public final static String STEAM_SIZE_MEDIUM = "_616x353";

    /**
     * Simple queue implementation, so that image are downloaded as fast as possible but actions are done in order
     */
    //private static final ArrayList<Thread> threadsList = new ArrayList<>();
    public static Task downloadSteamImageToCache(int steam_id, String type, String size, OnDLDoneHandler dlDoneHandler) {
        String imageURL = STEAM_IMAGE_URL_PREFIX + steam_id + "/" + type + (type.equals(STEAM_TYPE_HEADER) ? "" : size) + ".jpg";
        String imageFileName = steam_id + "_" + type + (type.equals(STEAM_TYPE_HEADER) ? "" : size) + ".jpg";
        return downloadImgToCache(imageURL, imageFileName, dlDoneHandler);
    }

    private static File getIGDBImageCacheFileOutput(int igdb_id, String imageHash, String type, String size) {
        return getOutputImageCacheFile(igdb_id + "_" + type + size + "_" + imageHash + ".jpg");
    }

    private static File getOutputImageCacheFile(String fileName) {
        return new File(Main.FILES_MAP.get("cache") + File.separator + fileName);
    }

    public static Task downloadIGDBImageToCache(ExecutorService executor, int igdb_id, String imageHash, String type, String size, OnDLDoneHandler dlDoneHandler) {
        String imageURL = IGDB_IMAGE_URL_PREFIX + type + size + "/" + imageHash + ".jpg";
        return downloadImgToCache(imageURL, getIGDBImageCacheFileOutput(igdb_id, imageHash, type, size), dlDoneHandler, executor);
    }

    public static Task downloadIGDBImageToCache(int igdb_id, String imageHash, String type, String size, OnDLDoneHandler dlDoneHandler) {
        return downloadIGDBImageToCache(null, igdb_id, imageHash,type,size, dlDoneHandler);
    }

    private static Task downloadImgToCache(String url, File fileOutput, OnDLDoneHandler dlDoneHandler, ExecutorService executor) {
        fileOutput.deleteOnExit();
        ImageDownloadTask task = new ImageDownloadTask(url, fileOutput, dlDoneHandler);
        if (executor != null && executor.isShutdown()) {
            executor.submit(task);
        } else {
            ImageDownloaderService.getInstance().addTask(task);
        }
        return task;
    }

    private static Task downloadImgToCache(String url, String filenameOutput, OnDLDoneHandler dlDoneHandler) {
        return downloadImgToCache(url, getOutputImageCacheFile(filenameOutput), dlDoneHandler,null);
    }

    public static void transitionToWindowBackground(Image img, ImageView imageView) {
        if (img != null) {
            double widthScale = (double) GENERAL_SETTINGS.getWindowWidth() / img.getWidth();
            double heightScale = (double) GENERAL_SETTINGS.getWindowHeight() / img.getHeight();

            /*if (imageView.getScaleX() != widthScale) {
                imageView.setScaleX(widthScale);
            }
            if (imageView.getScaleY() != heightScale) {
                imageView.setScaleY(heightScale);
            }*/
        }
        ImageUtils.transitionToImage(img, imageView, BaseScene.BACKGROUND_IMAGE_MAX_OPACITY);

    }

    public static void transitionToImage(Image image2, ImageView imageView, double finalOpacity) {
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
                                    new KeyValue(imageView.opacityProperty(), finalOpacity, Interpolator.EASE_OUT)
                            ));
                    fadeInTimeline.setCycleCount(1);
                    fadeInTimeline.setAutoReverse(false);
                    fadeInTimeline.play();
                }
            });
            fadeOutTimeline.play();
        });
    }

    public static void transitionToImage(Image image2, ImageView imageView) {
        transitionToImage(image2, imageView, 1);
    }

    public static boolean imagesEquals(Image img1, Image img2) {
        if (img1 == null || img2 == null) {
            return false;
        }
        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
            return false;
        }
        for (int i = 0; i < img1.getWidth(); i++) {
            for (int j = 0; j < img1.getHeight(); j++) {
                if (img1.getPixelReader().getArgb(i, j) != img2.getPixelReader().getArgb(i, j)) return false;
            }
        }
        return true;
    }

}

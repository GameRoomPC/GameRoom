package data.http.images;

import data.game.scraper.OnDLDoneHandler;
import data.http.SimpleImageInfo;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import system.application.settings.PredefinedSetting;
import ui.Main;
import ui.control.button.gamebutton.GameButton;
import ui.scene.BaseScene;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static system.application.settings.GeneralSettings.settings;
import static ui.Main.LOGGER;
import static ui.scene.BaseScene.BACKGROUND_IMAGE_LOAD_RATIO;
import static ui.scene.BaseScene.FADE_IN_OUT_TIME;

/**
 * A utils class containing many useful methods used throughout GameRoom's different Scenes, to compare, download, move,
 * or set images. It complete what the {@link Image} API of JavaFX may lack of.
 *
 * @author LM. Garret (admin@gameroom.me)
 * @date 06/08/2016.
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

    public static Task downloadIGDBImageToCache(int igdb_id, String imageHash, String type, String size, OnDLDoneHandler dlDoneHandler) {
        String imageURL = IGDB_IMAGE_URL_PREFIX + type + size + "/" + imageHash + ".jpg";
        return downloadImgToCache(imageURL, getIGDBImageCacheFileOutput(igdb_id, imageHash, type, size), dlDoneHandler);
    }

    private static Task downloadImgToCache(String url, File fileOutput, OnDLDoneHandler dlDoneHandler) {
        fileOutput.deleteOnExit();
        ImageDownloadTask task = new ImageDownloadTask(url, fileOutput, dlDoneHandler);
        getExecutorService().submit(task);
        return task;
    }

    private static Task downloadImgToCache(String url, String filenameOutput, OnDLDoneHandler dlDoneHandler) {
        return downloadImgToCache(url, getOutputImageCacheFile(filenameOutput), dlDoneHandler);
    }

    /**
     * Basically does the same as {@link #transitionToImage(Image, ImageView, double)}, but with the predefined opacity
     * of {@link BaseScene#BACKGROUND_IMAGE_MAX_OPACITY}.
     *
     * @param img       the background image to load
     * @param imageView and where to place it
     */
    public static void transitionToWindowBackground(Image img, ImageView imageView) {
        ImageUtils.transitionToImage(img, imageView, BaseScene.BACKGROUND_IMAGE_MAX_OPACITY);
    }

    /**
     * Basically does the same as {@link #transitionToWindowBackground(Image, ImageView)}, but creates the new Image
     * with the Window's size and stretched ratio.
     *
     * @param imgFile   the file to load the background image from
     * @param imageView and where to place it
     */
    public static void transitionToWindowBackground(File imgFile, ImageView imageView) {
        if (imgFile == null) {
            ImageUtils.transitionToWindowBackground((Image) null, imageView);
        } else {
            ImageUtils.transitionToWindowBackground(new Image("file:" + File.separator + File.separator + File.separator + imgFile.getAbsolutePath(),
                            settings().getWindowWidth() * BACKGROUND_IMAGE_LOAD_RATIO,
                            settings().getWindowHeight() * BACKGROUND_IMAGE_LOAD_RATIO,
                            false,
                            true),
                    imageView);
        }
    }


    /**
     * Makes a smooth transition between the given image and the current image of the imageView. It does a simple
     * linear fade-in/out of those images.
     *
     * @param image2       the image to load
     * @param imageView    where to load the image
     * @param finalOpacity the final opacity that we want for our imageView
     */
    public static void transitionToImage(Image image2, ImageView imageView, double finalOpacity) {
        if (!ImageUtils.imagesEquals(image2, imageView.getImage())) {
            Platform.runLater(() -> {

                Timeline fadeOutTimeline = new Timeline(
                        new KeyFrame(Duration.seconds(0),
                                new KeyValue(imageView.opacityProperty(), imageView.opacityProperty().getValue(), Interpolator.EASE_IN)),
                        new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                                new KeyValue(imageView.opacityProperty(), 0, Interpolator.EASE_OUT)
                        ));
                fadeOutTimeline.setCycleCount(1);
                fadeOutTimeline.setAutoReverse(false);
                fadeOutTimeline.setOnFinished(event -> {
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
                });
                fadeOutTimeline.play();
            });
        }
    }

    /**
     * See {@link #transitionToImage(Image, ImageView, double)}
     *
     * @param image2    the image to load
     * @param imageView where to set the image
     */
    public static void transitionToImage(Image image2, ImageView imageView) {
        transitionToImage(image2, imageView, 1);
    }

    /**
     * Does basically the same as {@link #transitionToImage(Image, ImageView)}, but also checks if the image should keep
     * its ratio and loads it accordingly
     *
     * @param imgFile         the file where the image is stored
     * @param requestedWidth  the wanted width for our image
     * @param requestedHeight the wanted height for our image
     * @param imageView       the imageView to change the image from
     */
    public static void transitionToCover(File imgFile, double requestedWidth, double requestedHeight, ImageView imageView) {
        boolean preserveRatio = shouldKeepImageRatio(imgFile);
        imageView.setPreserveRatio(preserveRatio);
        transitionToImage(new Image("file:" + File.separator + File.separator + File.separator + imgFile.getAbsolutePath(),
                        requestedWidth,
                        requestedHeight,
                        preserveRatio,
                        true),
                imageView,
                1);
    }

    public static void transitionToCover(String imgPath, double requestedWidth, double requestedHeight, ImageView imageView) {
        transitionToCover(new File(imgPath), requestedWidth, requestedHeight, imageView);
    }

    public static void transitionToCover(Image img, ImageView imageView) {
        boolean preserveRatio = shouldKeepImageRatio(img);
        imageView.setPreserveRatio(preserveRatio);
        transitionToImage(img,
                imageView,
                1);
    }

    /**
     * Compares two images and returns whether they are considered equals, not in memory term but rather pixel per pixel.
     *
     * @param img1 the first image to compare
     * @param img2 the second image to compare
     * @return wheter both images are the same
     */
    public static boolean imagesEquals(Image img1, Image img2) {
        if (img1 == null || img2 == null) {
            return false; //even if they are both null, we prefer to set them as not equal
        }
        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
            return false; //different sizes is considered not equal
        }
        for (int i = 0; i < img1.getWidth(); i++) {
            for (int j = 0; j < img1.getHeight(); j++) {
                //per pixel comparison
                if (img1.getPixelReader().getArgb(i, j) != img2.getPixelReader().getArgb(i, j)) return false;
            }
        }
        return true;
    }

    /**
     * Given an image File, checks whether it should keep its ratio, only if the option keep ratio is checked and
     * if its cover ratio is too far from the standard cover ratio
     *
     * @param imgFile the file to check
     * @return true if it should keep its cover ratio, false otherwise
     */
    public static boolean shouldKeepImageRatio(File imgFile) {
        try {
            SimpleImageInfo imageInfo = new SimpleImageInfo(new File(imgFile.getAbsolutePath()));
            return Math.abs(((double) imageInfo.getHeight() / imageInfo.getWidth()) - GameButton.COVER_HEIGHT_WIDTH_RATIO) > 0.2
                    && settings().getBoolean(PredefinedSetting.KEEP_COVER_RATIO);
        } catch (IOException e) {
            LOGGER.error("Could not check image keep ratio for file : \"" + imgFile.getAbsolutePath() + "\"");
            e.printStackTrace();
        }
        return false;
    }

    /**
     * See {@link #shouldKeepImageRatio(File)}, does the same but for an already loaded image.
     *
     * @param img the img to check
     * @return true if it should keep it ratio, false otherwise.
     */
    public static boolean shouldKeepImageRatio(Image img) {
        return Math.abs((img.getHeight() / img.getWidth()) - GameButton.COVER_HEIGHT_WIDTH_RATIO) > 0.2
                && settings().getBoolean(PredefinedSetting.KEEP_COVER_RATIO);
    }

    public static ExecutorService getExecutorService() {
        return Main.getExecutorService();
    }


}

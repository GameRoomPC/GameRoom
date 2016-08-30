package ui.control.specific;

import data.game.entry.GameEntry;
import data.http.YoutubeSoundtrackScrapper;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import ui.Main;
import ui.control.button.DualImageButton;
import ui.control.button.OnActionHandler;

import java.net.MalformedURLException;

/**
 * Created by LM on 07/08/2016.
 */
public class YoutubePlayerAndButton {
    private WebView webView;
    private DualImageButton soundMuteButton;

    public YoutubePlayerAndButton(GameEntry entry) throws MalformedURLException {
        super();
        webView = new WebView();
        webView.setPrefWidth(200);
        webView.setPrefHeight(180);
        webView.setOpacity(100);
        webView.setVisible(false);
        webView.setMouseTransparent(true);
        webView.setFocusTraversable(false);

        Image muteImage = new Image("res/ui/muteIcon.png"
                , Main.GENERAL_SETTINGS.getWindowWidth() / 18
                , Main.GENERAL_SETTINGS.getWindowHeight() / 18
                , true
                , true);
        Image soundImage = new Image("res/ui/soundIcon.png"
                , Main.GENERAL_SETTINGS.getWindowWidth() / 18
                , Main.GENERAL_SETTINGS.getWindowHeight() / 18
                , true
                , true);
        soundMuteButton = new DualImageButton(muteImage, soundImage, "mute", "sound");
        soundMuteButton.setMouseTransparent(true);
        soundMuteButton.setOnDualAction(new OnActionHandler() {
            @Override
            public void handle(ActionEvent me) {
                if (soundMuteButton.inFirstState()) {
                    webView.getEngine().executeScript("player.pauseVideo()");
                } else {
                    webView.getEngine().executeScript("player.playVideo()");
                }
            }
        });

        Task playVideoTask = new Task() {
            @Override
            protected Object call() throws Exception {
                try {
                    String hash = getHash(entry);
                    YoutubePlayerHTML html = new YoutubePlayerHTML(hash);

                    Platform.runLater(() -> {
                        webView.getEngine().loadContent(html.getHTMLCode());
                        JSObject win
                                = (JSObject) webView.getEngine().executeScript("window");
                        win.setMember("buttonToggler", new ButtonToggler(soundMuteButton));
                        //soundMuteButton.toggleState();
                    });
                } catch (Exception e) {
                    Main.LOGGER.error(e.toString());
                }
                //JSObject jsobj = (JSObject) webView.getEngine().executeScript("window");
                //jsobj.setMember("button", soundMuteButton);
                return null;
            }
        };
        Thread th = new Thread(playVideoTask);
        th.setDaemon(true);
        th.start();
        //getChildren().add(webView);
        StackPane.setAlignment(webView, Pos.TOP_RIGHT);
        StackPane.setAlignment(soundMuteButton, Pos.TOP_RIGHT);

    }
    public DualImageButton getSoundMuteButton(){
        return soundMuteButton;
    }
    public void quitYoutube(){
        webView.getEngine().load("about:blank");
// Delete cookies
        java.net.CookieHandler.setDefault(new java.net.CookieManager());
    }
    private String getHash(GameEntry entry) {
        try {
            if(entry.getYoutubeSoundtrackHash().equals("")) {
                String hash = YoutubeSoundtrackScrapper.getThemeYoutubeHash(entry);
                entry.setYoutubeSoundtrackHash(hash);
            }
            return entry.getYoutubeSoundtrackHash();
        }  catch (Exception e) {
            e.printStackTrace();
            try {
                Thread.currentThread().wait(100);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            return getHash(entry);
        }
        }
        // JavaScript interface object
        public class ButtonToggler {
            private DualImageButton mutesoundButton;
            ButtonToggler(DualImageButton muteSoundButton){
                this.mutesoundButton = muteSoundButton;
            }
            public void muteAndDisable() {
                Platform.runLater(() -> {
                    mutesoundButton.forceState("mute");
                    mutesoundButton.setMouseTransparent(true);
                });
            }
            public void unmuteAndEnable() {
                Platform.runLater(() -> {
                    mutesoundButton.forceState("sound");
                    mutesoundButton.setMouseTransparent(false);
                });
            }
        }
        private class YoutubePlayerHTML {
            private String hash;

            YoutubePlayerHTML(String hash) {
                this.hash = hash;
            }

            public String getHTMLCode() {
                return "<!DOCTYPE html>\n" +
                        "<html>\n" +
                        "  <body>\n" +
                        "    <!-- 1. The <iframe> (and video player) will replace this <div> tag. -->\n" +
                        "    <div id=\"player\"></div>\n" +
                        "\n" +
                        "    <script>\n" +
                        "      // 2. This code loads the IFrame Player API code asynchronously.\n" +
                        "      var tag = document.createElement('script');\n" +
                        "\n" +
                        "      tag.src = \"https://www.youtube.com/iframe_api\";\n" +
                        "      var firstScriptTag = document.getElementsByTagName('script')[0];\n" +
                        "      firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);\n" +
                        "\n" +
                        "      // 3. This function creates an <iframe> (and YouTube player)\n" +
                        "      //    after the API code downloads.\n" +
                        "      var player;\n" +
                        "      function onYouTubeIframeAPIReady() {\n" +
                        "        player = new YT.Player('player', {\n" +
                        "          height: '180',\n" +
                        "          width: '300',\n" +
                        "          videoId: '" + hash + "',\n" +
                        "          events: {\n" +
                        "            'onReady': onPlayerReady,\n" +
                        "            'onStateChange': onPlayerStateChange\n" +
                        "          }\n" +
                        "        });\n" +
                        "      }\n" +
                        "\n" +
                        "      // 4. The API will call this function when the video player is ready.\n" +
                        "      function onPlayerReady(event) {\n" +
                        "        buttonToggler.unmuteAndEnable();\n" +
                        "        event.target.playVideo();\n" +
                        "      }\n" +
                        "\n" +
                        "      // 5. The API calls this function when the player's state changes.\n" +
                        "      //    The function indicates that when playing a video (state=1),\n" +
                        "      //    the player should play for six seconds and then stop.\n" +
                        "      var done = false;\n" +
                        "      function onPlayerStateChange(event) {" +
                        "           if(event.data === 0) {          \n" +
                        "               buttonToggler.muteAndDisable();\n" +
                        "            }"+
                        "      }\n" +
                        "      function stopVideo() {\n" +
                        "        player.stopVideo();\n" +
                        "      }\n" +
                        "    </script>\n" +
                        "  </body>\n" +
                        "</html>";

            }
        }
    }

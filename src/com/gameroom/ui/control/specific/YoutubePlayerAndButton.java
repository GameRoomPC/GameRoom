package com.gameroom.ui.control.specific;

import com.gameroom.data.game.entry.GameEntry;
import com.gameroom.data.http.YoutubeSoundtrackScrapper;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import com.gameroom.ui.Main;
import com.gameroom.ui.control.button.DualImageButton;
import com.gameroom.ui.control.button.OnActionHandler;
import com.gameroom.ui.scene.BaseScene;

import java.net.MalformedURLException;

import static com.gameroom.system.application.settings.GeneralSettings.settings;
import static com.gameroom.ui.Main.LOGGER;

/**
 * Created by LM on 07/08/2016.
 */
public class YoutubePlayerAndButton {
    private static WebView WEB_VIEW;
    private final BaseScene scene;
    private DualImageButton soundMuteButton;
    private volatile boolean stopThread = false;
    private boolean wasPlaying = true;

    public YoutubePlayerAndButton(GameEntry entry, BaseScene scene) throws MalformedURLException {
        super();
        this.scene = scene;

        double imgSize = settings().getWindowWidth() / 35;

        //TODO inverse state (see link for code), and check that buttons enable itself when music starts https://github.com/n0xew/GameRoom/blob/c3cce2ce90225dc8c963269d47e7778c98a9e1f0/src/ui/control/specific/YoutubePlayerAndButton.java
        soundMuteButton = new DualImageButton("sound-button","mute-button", imgSize, imgSize);
        //soundMuteButton.setMouseTransparent(true);
        soundMuteButton.setOnDualAction(new OnActionHandler() {
            @Override
            public void handle(ActionEvent me) {
                if(me != null) {
                    //this means that it was not forced but mouse actioned
                    wasPlaying = soundMuteButton.inFirstState();
                }
                if (soundMuteButton.inFirstState()) {
                    WEB_VIEW.getEngine().executeScript("player.playVideo()");
                } else {
                    WEB_VIEW.getEngine().executeScript("player.pauseVideo()");
                }
            }
        });
        if (WEB_VIEW == null) {
            WEB_VIEW = new WebView();
        }

        WEB_VIEW.setPrefWidth(200);
        WEB_VIEW.setPrefHeight(180);
        WEB_VIEW.setMouseTransparent(true);
        WEB_VIEW.setFocusTraversable(false);

        Task playVideoTask = new Task() {
            @Override
            protected Object call() throws Exception {
                try {
                    String hash = getHash(entry);
                    YoutubePlayerHTML html = new YoutubePlayerHTML(hash);

                    if(stopThread){
                        return null;
                    }
                    Platform.runLater(() -> {
                        if(!stopThread) {
                            WEB_VIEW.getEngine().loadContent(html.getHTMLCode());
                            JSObject win
                                    = (JSObject) WEB_VIEW.getEngine().executeScript("window");
                            win.setMember("buttonToggler", new ButtonToggler(soundMuteButton));
                        }//soundMuteButton.toggleState();
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


        //getChildren().add(WEB_VIEW);
        StackPane.setAlignment(WEB_VIEW, Pos.TOP_RIGHT);
        StackPane.setAlignment(soundMuteButton, Pos.TOP_RIGHT);

    }

    public DualImageButton getSoundMuteButton() {
        return soundMuteButton;
    }

    public void quitYoutube() {
        stopThread = true;
        WEB_VIEW.getEngine().load("about:blank");
// Delete cookies
        java.net.CookieHandler.setDefault(new java.net.CookieManager());
    }

    private void play() {
        if (soundMuteButton != null) {
            soundMuteButton.forceState("sound-button", true);
            soundMuteButton.setMouseTransparent(false);
        }
    }

    private void pause() {
        if (soundMuteButton != null) {
            soundMuteButton.forceState("mute-button", true);
            soundMuteButton.setMouseTransparent(false);
        }
    }

    public void automaticPause(){
        pause();
    }

    public void automaticPlay(){
        if(wasPlaying){
            play();
        }
    }

    private String getHash(GameEntry entry) {
        try {
            if (entry.getYoutubeSoundtrackHash().equals("")) {
                String hash = YoutubeSoundtrackScrapper.getThemeYoutubeHash(entry,scene);

                entry.setSavedLocally(true);
                entry.setYoutubeSoundtrackHash(hash);
                entry.setSavedLocally(false);
            }
            return entry.getYoutubeSoundtrackHash();
        } catch (Exception e) {
            if (e.toString().contains("java.net.UnknownHostException: www.youtube.com")) {
                LOGGER.error("Could not connect to youtube");
            } else {
                e.printStackTrace();
            }
            try {
                Thread.currentThread().wait(100);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            return getHash(entry);

        }
    }


    public WebView getWebView() {
        return WEB_VIEW;
    }

    // JavaScript interface object
    public class ButtonToggler {
        private DualImageButton mutesoundButton;

        ButtonToggler(DualImageButton muteSoundButton) {
            this.mutesoundButton = muteSoundButton;
        }

        public void muteAndDisable() {
            wasPlaying = false;
            pause();
            mutesoundButton.setMouseTransparent(true);
        }

        public void unmuteAndEnable() {
            wasPlaying = true;
            play();
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
                    "        waitForElement();\n" +
                    "        event.target.playVideo();\n" +
                    "        buttonToggler.unmuteAndEnable();\n" +
                    "      }\n" +
                    "\n" +
                    "      // 5. The API calls this function when the player's state changes.\n" +
                    "      //    The function indicates that when playing a video (state=1),\n" +
                    "      //    the player should play for six seconds and then stop.\n" +
                    "      var done = false;\n" +
                    "      function onPlayerStateChange(event) {" +
                    "           if(event.com.gameroom.data === 0) {          \n" +
                    "               buttonToggler.muteAndDisable();\n" +
                    "            }" +
                    "      }\n" +
                    "      function stopVideo() {\n" +
                    "        player.stopVideo();\n" +
                    "      }\n" +
                    "      function waitForElement(){\n" +
                    "           if(typeof buttonToggler !== \"undefined\"){\n" +
                    "               //variable exists, do what you want\n" +
                    "           }\n" +
                    "           else{\n" +
                    "               setTimeout(waitForElement, 250);\n" +
                    "           }\n" +
                    "       }" +
                    "    </script>\n" +
                    "  </body>\n" +
                    "</html>";

        }
    }
}

package ui.dialog;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import ui.Main;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import static ui.Main.LOGGER;

/**
 * Created by LM on 09/01/2017.
 */
public class WebBrowser extends GameRoomDialog<ButtonType> {
    private String initialUrl;
    private WebView webView;
    private Label urlLabel = new Label();
    private Button forwardButton = new Button("\uD83E\uDC62");
    private Button backButton = new Button("\uD83E\uDC68");

    public WebBrowser(String url) {
        super();
        this.initialUrl = url;
        mainPane.getStyleClass().add("container");
        ButtonType okButton = new ButtonType(Main.getString("close"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType(Main.getString("open_in_browser"), ButtonBar.ButtonData.LEFT);
        getDialogPane().getButtonTypes().addAll(cancelButton, okButton);

        mainPane.setCenter(createCenterPane());
        mainPane.setTop(createTopPane());

        setOnHiding(event -> {
            onClose();
        });
    }

    private void onClose() {
        webView.getEngine().load("about:blank");
        webView = null;
        // Delete cookies
        java.net.CookieHandler.setDefault(new java.net.CookieManager());
    }

    private Pane createTopPane() {
        urlLabel.textProperty().bind(webView.getEngine().locationProperty());
        backButton.setOnAction(e -> webView.getEngine().getHistory().go(-1));
        forwardButton.setOnAction(e -> webView.getEngine().getHistory().go(1));
        double padding = 10 * Main.SCREEN_WIDTH / 1920;
        HBox box = new HBox(padding);
        box.setFocusTraversable(false);
        box.getChildren().addAll(backButton, forwardButton, urlLabel);
        box.setPadding(new Insets(padding,padding,padding,padding));
        return box;
    }

    private Pane createCenterPane() {
        webView = new WebView();
        webView.getEngine().setOnError(event -> {
            LOGGER.error("WebView error : " + event.getMessage());
        });
        webView.getEngine().load(initialUrl);
        StackPane contentPane = new StackPane();
        contentPane.getChildren().add(webView);
        return contentPane;
    }

    public static void openSupporterKeyBuyBrowser(){
        WebBrowser browser = new WebBrowser("https://gameroom.me/downloads/key");
        Optional<ButtonType> webResult = browser.showAndWait();
        webResult.ifPresent(buttonType -> {
            if(buttonType.getText().equals(Main.getString("open_in_browser"))){
                try {
                    Desktop.getDesktop().browse(new URI("https://gameroom.me/downloads/key"));
                } catch (IOException | URISyntaxException e1) {
                    LOGGER.error(e1.getMessage());
                    GameRoomAlert.error(Main.getString("error")+" "+e1.getMessage());
                }
            }else if(buttonType.getText().equals(Main.getString("close"))){

            }
        });
    }
}

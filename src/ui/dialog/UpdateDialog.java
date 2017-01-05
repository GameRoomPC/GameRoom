package ui.dialog;

import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import org.pegdown.PegDownProcessor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import ui.Main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Created by LM on 27/07/2016.
 */
public class UpdateDialog extends GameRoomDialog<ButtonType> {
    private static final String CSS =
            "body {"
                    + "    background-color: #43474c; "
                    + "    font-family: Arial, Helvetica Neue, san-serif;"
                    + "    color: #ffffff;"
                    + "    font-size: 110%;"
                    + "    font-weight: 600;"
                    + "    line-height: 150%;"
                    + "}";

    public UpdateDialog(String currentVersion, String newVersion, URL changelogUrl) {
        super(Modality.NONE);
        setTitle("GameRoom Updater");

        mainPane.getStyleClass().add("container");
        ButtonType okButton = new ButtonType(Main.getString("update"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType(Main.getString("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(cancelButton, okButton);

        mainPane.setTop(createTopPane(currentVersion, newVersion));
        mainPane.setCenter(createChangelogArea(changelogUrl));
    }

    public Pane createTopPane(String currentVersion, String newVersion) {
        HBox pane = new HBox();

        Label label1 = new Label(Main.getString("current_version") + ": ");
        label1.setStyle("-fx-font-weight: bolder;");

        Label label2 = new Label(currentVersion + ", ");

        Label label3 = new Label(Main.getString("new_version") + ": ");
        label3.setStyle("-fx-font-weight: bolder;");

        Label label4 = new Label(newVersion);

        pane.getChildren().addAll(label1, label2, label3, label4);

        BorderPane.setAlignment(pane, Pos.CENTER_LEFT);
        BorderPane.setMargin(pane, new Insets(20 * Main.SCREEN_WIDTH / 1920, 20 * Main.SCREEN_HEIGHT / 1080, 40 * Main.SCREEN_WIDTH / 1920, 20 * Main.SCREEN_HEIGHT / 1080));
        return pane;
    }

    public Pane createChangelogArea(URL changelogURL) {
        StackPane pane = new StackPane();

        try {
            PegDownProcessor pegProc = new PegDownProcessor();
            StringBuilder htmlString = new StringBuilder();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(changelogURL.openStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null)
                htmlString.append(inputLine).append("\n");
            in.close();

            WebView webview = new WebView();
            webview.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    Document doc = webview.getEngine().getDocument();
                    Element styleNode = doc.createElement("style");
                    Text styleContent = doc.createTextNode(CSS);
                    styleNode.appendChild(styleContent);
                    doc.getDocumentElement().getElementsByTagName("head").item(0).appendChild(styleNode);
                }
            });

            webview.getEngine().loadContent(pegProc.markdownToHtml(htmlString.toString()).replaceAll("\\s*<a.*</a>\\s*", ""));

            pane.getChildren().add(webview);

        } catch (IOException e) {
            e.printStackTrace();
            Label label = new Label(e.toString());
            label.setWrapText(true);
            pane.getChildren().add(label);
        }
        pane.getStyleClass().add("changelog-scrollpane");
        pane.setPrefWidth(400 * Main.SCREEN_WIDTH / 1920);
        pane.setPrefHeight(500 * Main.SCREEN_HEIGHT / 1080);
        initModality(Modality.WINDOW_MODAL);
        BorderPane.setMargin(pane, new Insets(0 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920, 0 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920));
        return pane;
    }
}

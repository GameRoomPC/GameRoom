package ui.dialog;

import data.game.ImageUtils;
import data.game.OnDLDoneHandler;
import ui.Main;
import ui.control.button.ImageButton;
import ui.control.button.gamebutton.GameButton;
import ui.scene.GameEditScene;
import data.game.GameEntry;
import data.game.GameScrapper;
import data.http.SimpleImageInfo;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;

import static ui.Main.SCREEN_WIDTH;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;

/**
 * Created by LM on 12/07/2016.
 */
public class SearchDialog extends GameRoomDialog<GameEntry> {
    private ArrayList<SearchResultRow> resultsList = new ArrayList<>();
    private HBox topBox;
    private VBox resultsPane;
    private TextField searchField;
    private Label statusLabel;

    private JSONArray gamesDataArray;

    final ToggleGroup toggleGroup = new ToggleGroup();
    private int selectedID = -1;

    public SearchDialog() {
        super();
        getDialogPane().getStyleClass().add("search-dialog");

        statusLabel = new Label(Main.RESSOURCE_BUNDLE.getString("search_a_game"));
        statusLabel.setWrapText(true);
        statusLabel.setMouseTransparent(true);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        searchField = new TextField();
        searchField.setPromptText(Main.RESSOURCE_BUNDLE.getString("example_games"));
        searchField.setPrefColumnCount(20);
        Image searchImage = new Image("res/ui/searchButton.png", SCREEN_WIDTH / 28, SCREEN_WIDTH / 28, true, true);
        ImageButton searchButton = new ImageButton(searchImage);

        mainPane.getStyleClass().add("container");

        resultsPane = new VBox();

        resultsPane.setFillWidth(true);
        resultsPane.setSpacing(10 * Main.SCREEN_HEIGHT / 1080);
        resultsPane.getStyleClass().add("vbox");

        searchButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                selectedID = -1;
                resultsPane.getChildren().clear();
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        statusLabel.setText(Main.RESSOURCE_BUNDLE.getString("searching") + "...");
                    }
                });
                try {
                    JSONArray resultArray = GameScrapper.searchGame(searchField.getText());
                    ArrayList<Integer> ids = new ArrayList<Integer>();
                    for (Object obj : resultArray) {
                        JSONObject jsob = ((JSONObject) obj);
                        ids.add(jsob.getInt("id"));
                    }
                    if (ids.size() == 0) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                statusLabel.setText(Main.RESSOURCE_BUNDLE.getString("no_result"));
                            }
                        });
                    } else {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                statusLabel.setText(Main.RESSOURCE_BUNDLE.getString("loading") + "...");
                            }
                        });
                        Task<String> scrapping = new Task<String>() {
                            @Override
                            protected String call() throws Exception {
                                gamesDataArray = GameScrapper.getGamesData(ids);
                                String gameList = "SearchResult : ";
                                for (Object obj : gamesDataArray)

                                {
                                    JSONObject jsob = ((JSONObject) obj);
                                    Platform.runLater(() -> addNewRow(jsob));
                                }

                                Main.LOGGER.debug(gameList.substring(0, gameList.length() - 3));
                                return null;
                            }
                        };
                        Thread th = new Thread(scrapping);
                        th.setDaemon(true);
                        th.start();
                    }
                } catch (ConnectTimeoutException cte) {
                    cte.printStackTrace();
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            statusLabel.setText(Main.RESSOURCE_BUNDLE.getString("no_internet"));
                        }
                    });
                }
            }
        });
        resultsPane.setPadding(new Insets(10 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920));

        topBox = new HBox();
        topBox.setAlignment(Pos.CENTER);
        topBox.setSpacing(15 * Main.SCREEN_WIDTH / 1920);
        topBox.getChildren().addAll(searchField, searchButton);

        resultsPane.setPrefHeight(Main.SCREEN_HEIGHT / 3);
        //resultsPane.setPrefWidth(ui.Main.SCREEN_WIDTH);

        StackPane centerPane = new StackPane();
        centerPane.setFocusTraversable(false);
        scrollPane.setContent(resultsPane);
        centerPane.getChildren().addAll(scrollPane, statusLabel);

        mainPane.setTop(topBox);
        mainPane.setCenter(centerPane);
        BorderPane.setMargin(topBox, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920, 20 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920));

        HBox buttonBox = new HBox();
        buttonBox.setSpacing(30 * SCREEN_WIDTH / 1920);

        //ButtonType cancelButton = new ButtonType(ui.Main.RESSOURCE_BUNDLE.getString("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType nextButton = new ButtonType(Main.RESSOURCE_BUNDLE.getString("next"), ButtonBar.ButtonData.OK_DONE);

        getDialogPane().getButtonTypes().addAll(nextButton);
    }

    protected void addNewRow(JSONObject jsob) {
        String coverHash = null;
        try{
            coverHash = GameScrapper.getCoverImageHash(jsob);
        }catch (JSONException je){
            Main.LOGGER.debug("No cover for game "+jsob.getString("name"));
            if(!je.toString().contains("cover")){
                je.printStackTrace();
            }
        }
        SearchResultRow row = new SearchResultRow(jsob.getString("name")
                , GameScrapper.getYear(jsob.getInt("id"), gamesDataArray)
                , jsob.getInt("id")
                , coverHash);
        row.setPrefWidth(topBox.getWidth() - topBox.getSpacing() * 2);
        statusLabel.setText("");
        resultsPane.getChildren().add(row);
        setOnShown(new EventHandler<DialogEvent>() {
            @Override
            public void handle(DialogEvent event) {
                //TODO fix this method not being called

                searchField.fireEvent(new MouseEvent(MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 0, false, false, false, false, false, false, false, false, false, false, null));
                searchField.requestFocus();
            }
        });

        row.radioButton.setToggleGroup(toggleGroup);
        row.radioButton.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    row.setStyle("-fx-background-color: derive(-flatter-red, -20.0%);");
                    selectedID = jsob.getInt("id");
                } else {
                    row.setStyle("-fx-background-color: derive(-dark, 20%);");
                }
            }
        });
        row.setOnMouseClicked(me -> {
            row.radioButton.setSelected(true);
            selectedID = jsob.getInt("id");
        });
        setOnHiding(ne -> {
            if (selectedID != -1) {
                JSONObject gameJson = this.gamesDataArray.getJSONObject(GameScrapper.indexOf(selectedID, this.gamesDataArray));
                setResult(GameScrapper.getEntry(gameJson));
            }
        });
    }

    static class SearchResultRow extends GridPane {
        private final static int COVER_WIDTH = 70;
        private StackPane coverPane = new StackPane();
        private ImageView coverView = new ImageView();

        protected RadioButton radioButton;

        public SearchResultRow(String gameName, String year, int id, String coverHash) {
            super();
            getStyleClass().addAll(new String[]{"search-result-row"});
            if(coverHash!=null) {
                ImageUtils.downloadImageToCache(id, coverHash, ImageUtils.TYPE_COVER, ImageUtils.SIZE_SMALL, new OnDLDoneHandler() {
                    @Override
                    public void run(File outputfile) {
                        boolean keepRatio = true;
                        try {
                            SimpleImageInfo imageInfo = new SimpleImageInfo(outputfile);
                            keepRatio = Math.abs(((double) imageInfo.getHeight() / imageInfo.getWidth()) - GameButton.COVER_HEIGHT_WIDTH_RATIO) > 0.2;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        boolean finalKeepRatio = keepRatio;
                        Platform.runLater(() -> {
                            coverView.setImage(new Image("file:" + File.separator + File.separator + File.separator + outputfile.getAbsolutePath(), COVER_WIDTH, COVER_WIDTH * GameButton.COVER_HEIGHT_WIDTH_RATIO, finalKeepRatio, true));
                        });
                    }
                });
            }

            setWidth(Double.MAX_VALUE);
            setAlignment(Pos.CENTER_RIGHT);
            radioButton = new RadioButton();
            add(radioButton, 0, 0);
            GridPane.setMargin(radioButton, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 10 * Main.SCREEN_WIDTH / 1920));

            coverPane.getChildren().add(new ImageView(new Image("res/defaultImages/cover.jpg", COVER_WIDTH, COVER_WIDTH * GameButton.COVER_HEIGHT_WIDTH_RATIO, false, true)));
            coverPane.getChildren().add(coverView);
            GridPane.setMargin(coverPane, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 10 * Main.SCREEN_WIDTH / 1920));
            add(coverPane, 1, 0);

            Label nameLabel = new Label(gameName);
            nameLabel.setWrapText(true);
            Label yearLabel = new Label(year);
            yearLabel.setWrapText(true);

            yearLabel.setStyle("-fx-font-family: 'Helvetica Neue';\n" +
                    "    -fx-font-size: 18.0px;\n" +
                    "    -fx-font-weight: 600;" +
                    "    -fx-font-style: italic;");
            VBox box = new VBox();
            box.getChildren().addAll(nameLabel, yearLabel);
            add(box, 2, 0);
            GridPane.setMargin(box, new Insets(20 * Main.SCREEN_HEIGHT / 1080, 30 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 30 * Main.SCREEN_WIDTH / 1920));
            setAlignment(Pos.CENTER_LEFT);
            setFocusTraversable(true);
        }

    }
}

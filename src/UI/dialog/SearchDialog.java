package UI.dialog;

import UI.Main;
import UI.button.ImageButton;
import UI.button.gamebutton.GameButton;
import UI.scene.GameEditScene;
import data.GameEntry;
import data.GameScrapper;
import data.SimpleImageInfo;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.util.Pair;
import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Iterator;

import static UI.Main.RESSOURCE_BUNDLE;
import static UI.Main.SCREEN_HEIGHT;
import static UI.Main.SCREEN_WIDTH;

/**
 * Created by LM on 12/07/2016.
 */
public class SearchDialog extends Dialog<GameEntry> {
    private BorderPane mainPane;
    private ArrayList<SearchResultRow> resultsList = new ArrayList<>();
    private HBox topBox;
    private VBox resultsPane;
    private TextField searchField;
    private Label statusLabel;

    private JSONArray gamesDataArray;

    final ToggleGroup toggleGroup = new ToggleGroup();
    private int selectedID=-1;

    public SearchDialog() {
        super();
        DialogPane dialogPane = new DialogPane();
        mainPane = new BorderPane();
        dialogPane.setContent(mainPane);
        dialogPane.getStylesheets().add("res/flatterfx.css");
        initStyle(StageStyle.UNDECORATED);
        initModality(Modality.APPLICATION_MODAL);
        setDialogPane(dialogPane);
        dialogPane.getStyleClass().add("search-dialog");

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
                                for (Object obj: gamesDataArray)

                                {
                                    JSONObject jsob = ((JSONObject) obj);

                                    try {
                                        URL imageURL = new URL(GameScrapper.getCoverImage(jsob.getInt("id"), "cover_small", gamesDataArray));
                                        String outputPath = Main.CACHE_FOLDER + File.separator + Integer.toString(jsob.getInt("id")) + "_cover_small." + GameEditScene.getExtension(imageURL.getPath());
                                        File imageFile = new File(outputPath);
                                        imageFile.deleteOnExit();

                                        if (!imageFile.exists()) {
                                            Task<String> task = new Task<String>() {
                                                @Override
                                                protected String call() throws Exception {
                                                    ReadableByteChannel rbc = Channels.newChannel(imageURL.openStream());
                                                    FileOutputStream fos = new FileOutputStream(outputPath);
                                                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                                                    fos.close();
                                                    addNewRow(jsob,imageFile);
                                                    return null;
                                                }
                                            };
                                            Thread th = new Thread(task);
                                            th.setDaemon(true);
                                            th.start();
                                        } else {
                                            addNewRow(jsob,imageFile);
                                        }
                                        gameList += jsob.getString("name") + ", ";

                                    } catch (MalformedURLException e) {
                                        e.printStackTrace();
                                    } catch (JSONException je) {
                                        if (je.toString().contains("cover")) {
                                            addNewRow(jsob,null);
                                        }
                                    }
                                }

                                Main.logger.debug(gameList.substring(0, gameList.length() - 3));
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
        //resultsPane.setPrefWidth(Main.SCREEN_WIDTH);

        StackPane centerPane = new StackPane();
        centerPane.setFocusTraversable(false);
        scrollPane.setContent(resultsPane);
        centerPane.getChildren().addAll(scrollPane, statusLabel);

        mainPane.setTop(topBox);
        mainPane.setCenter(centerPane);
        BorderPane.setMargin(topBox, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920, 20 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920));

        HBox buttonBox = new HBox();
        buttonBox.setSpacing(30* SCREEN_WIDTH /1920);

        //ButtonType cancelButton = new ButtonType(Main.RESSOURCE_BUNDLE.getString("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType nextButton = new ButtonType(Main.RESSOURCE_BUNDLE.getString("next"), ButtonBar.ButtonData.OK_DONE);

        dialogPane.getButtonTypes().addAll(nextButton);
    }

    protected void addNewRow(JSONObject jsob, File imageFile) {
        String path = null;
        if(imageFile != null){
            path = imageFile.getAbsolutePath();
        }
        SearchResultRow row = new SearchResultRow(jsob.getString("name")
                , GameScrapper.getYear(jsob.getInt("id"), gamesDataArray )
                , jsob.getInt("id")
                , path);
        row.setPrefWidth(topBox.getWidth()-topBox.getSpacing()*2);
        Platform.runLater(new

                                  Runnable() {
                                      @Override
                                      public void run() {
                                          Platform.runLater(new Runnable() {
                                              @Override
                                              public void run() {
                                                  statusLabel.setText("");
                                                  resultsPane.getChildren().add(row);
                                              }
                                          });
                                      }
                                  }

        );
        setOnShowing(new EventHandler<DialogEvent>() {
            @Override
            public void handle(DialogEvent event) {
                searchField.requestFocus();
            }
        });

        row.radioButton.setToggleGroup(toggleGroup);
        row.radioButton.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if(newValue){
                    row.setStyle("-fx-background-color: derive(-flatter-red, -20.0%);");
                    selectedID = jsob.getInt("id");
                }else{
                    row.setStyle("-fx-background-color: derive(-dark, 20%);");
                }
            }
        });
        row.setOnMouseClicked(me->{
            row.radioButton.setSelected(true);
            selectedID = jsob.getInt("id");
        });
        setOnHiding(ne->{
            if(selectedID != -1){
                JSONObject gameJson = this.gamesDataArray.getJSONObject(GameScrapper.indexOf(selectedID, this.gamesDataArray));
                setResult(GameScrapper.getEntry(gameJson));
            }
        });
    }

    static class SearchResultRow extends GridPane {
        private final static int COVER_WIDTH = 70;
        private StackPane coverPane = new StackPane();
        private ImageView coverView;
        private int id;
        protected RadioButton radioButton;

        public SearchResultRow(String gameName, String year, int id, String imageURL) {
            super();
            this.id = id;
            getStyleClass().addAll(new String[]{"search-result-row"});
            if (imageURL != null) {
                boolean keepRatio = true;
                try {
                    SimpleImageInfo imageInfo = new SimpleImageInfo(new File(imageURL));
                    keepRatio = Math.abs(((double) imageInfo.getHeight() / imageInfo.getWidth()) - GameButton.COVER_HEIGHT_WIDTH_RATIO) > 0.2;
                } catch (IOException e) {
                    e.printStackTrace();
                }

                coverView = new ImageView(new Image("file:" + File.separator + File.separator + File.separator + imageURL, COVER_WIDTH, COVER_WIDTH * GameButton.COVER_HEIGHT_WIDTH_RATIO, keepRatio, true));
            } else {
                coverView = new ImageView();
            }

            setWidth(Double.MAX_VALUE);
            setAlignment(Pos.CENTER_RIGHT);
            radioButton = new RadioButton();
            add(radioButton,0,0);
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

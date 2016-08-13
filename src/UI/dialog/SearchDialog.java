package ui.dialog;

import data.game.ImageUtils;
import data.game.OnDLDoneHandler;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import ui.Main;
import ui.control.button.ImageButton;
import ui.control.button.gamebutton.GameButton;
import ui.pane.SelectListPane;
import data.game.entry.GameEntry;
import data.game.scrapper.IGDBScrapper;
import data.http.SimpleImageInfo;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static ui.Main.SCREEN_WIDTH;

/**
 * Created by LM on 12/07/2016.
 */
public class SearchDialog extends GameRoomDialog<ButtonType> {
    private HBox topBox;
    private TextField searchField;
    private Label statusLabel;
    private GameEntry selectedEntry;

    private JSONArray gamesDataArray;

    private SearchList searchListPane;

    private boolean doNotDownloadCover = false;

    public SearchDialog() {
        super();
        getDialogPane().getStyleClass().add("search-dialog");

        statusLabel = new Label(Main.RESSOURCE_BUNDLE.getString("search_a_game"));
        statusLabel.setWrapText(true);
        statusLabel.setMouseTransparent(true);

        searchField = new TextField();
        searchField.setPromptText(Main.RESSOURCE_BUNDLE.getString("example_games"));
        searchField.setPrefColumnCount(20);
        Image searchImage = new Image("res/ui/searchButton.png", SCREEN_WIDTH / 28, SCREEN_WIDTH / 28, true, true);
        ImageButton searchButton = new ImageButton(searchImage);

        mainPane.getStyleClass().add("container");

        topBox = new HBox();
        topBox.setAlignment(Pos.CENTER);
        topBox.setSpacing(15 * Main.SCREEN_WIDTH / 1920);
        topBox.getChildren().addAll(searchField, searchButton);

        searchListPane = new SearchList(Main.SCREEN_HEIGHT / 2.5,topBox.widthProperty());

        searchButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                searchListPane.clearItems();
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        statusLabel.setText(Main.RESSOURCE_BUNDLE.getString("searching") + "...");
                    }
                });
                try {
                    JSONArray resultArray = IGDBScrapper.searchGame(searchField.getText());
                    ArrayList<Integer> ids = new ArrayList<Integer>();
                    if(resultArray == null){
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                statusLabel.setText(Main.RESSOURCE_BUNDLE.getString("no_result")+"/"+Main.RESSOURCE_BUNDLE.getString("no_internet"));
                            }
                        });
                    }else {
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
                                    gamesDataArray = IGDBScrapper.getGamesData(ids);
                                    String gameList = "SearchResult : ";
                                    searchListPane.setGamesDataArray(gamesDataArray);
                                    Platform.runLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            statusLabel.setText("");
                                        }
                                    });
                                    Platform.runLater(() -> searchListPane.addItems(gamesDataArray.iterator()));

                                    Main.LOGGER.debug(gameList.substring(0, gameList.length() - 3));
                                    return null;
                                }
                            };
                            Thread th = new Thread(scrapping);
                            th.setDaemon(true);
                            th.start();
                        }
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
        searchListPane.setPadding(new Insets(10 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920));

        //resultsPane.setPrefWidth(ui.Main.SCREEN_WIDTH);

        StackPane centerPane = new StackPane();
        centerPane.setFocusTraversable(false);

        centerPane.getChildren().addAll(searchListPane, statusLabel);

        CheckBox doNotDownloadCoverCheckBox = new CheckBox(Main.RESSOURCE_BUNDLE.getString("do_not_download_cover"));
        doNotDownloadCoverCheckBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                doNotDownloadCover = newValue;
            }
        });

        mainPane.setTop(topBox);
        mainPane.setCenter(centerPane);
        mainPane.setBottom(doNotDownloadCoverCheckBox);

        BorderPane.setMargin(topBox, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920, 20 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920));
        BorderPane.setMargin(doNotDownloadCoverCheckBox, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920, 0 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920));

        ButtonType cancelButton = new ButtonType(ui.Main.RESSOURCE_BUNDLE.getString("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType nextButton = new ButtonType(Main.RESSOURCE_BUNDLE.getString("next"), ButtonBar.ButtonData.OK_DONE);

        getDialogPane().getButtonTypes().addAll(cancelButton,nextButton);
        setOnHiding(event -> {
            if(searchListPane.getSelectedValue()!=null) {
                selectedEntry = IGDBScrapper.getEntry(searchListPane.getSelectedValue());
            }
        });
    }

    public GameEntry getSelectedEntry() {
        return selectedEntry;
    }

    public boolean doNotDownloadCover() {
        return doNotDownloadCover;
    }

    private static class SearchList extends SelectListPane<JSONObject> {
        private JSONArray gamesDataArray;
        private ReadOnlyDoubleProperty prefRowWidth;

        public SearchList(double prefHeight, ReadOnlyDoubleProperty prefRowWidth) {
            super(prefHeight);
            this.prefRowWidth = prefRowWidth;
        }

        public void setGamesDataArray(JSONArray gamesDataArray) {
            this.gamesDataArray = gamesDataArray;
        }

        @Override
        protected ListItem<JSONObject> createListItem(JSONObject value) {
            String coverHash = null;
            try {
                coverHash = IGDBScrapper.getCoverImageHash(value);
            } catch (JSONException je) {
                Main.LOGGER.debug("No cover for game " + value.getString("name"));
                if (!je.toString().contains("cover")) {
                    je.printStackTrace();
                }
            }
            SearchItem row = new SearchItem(value,this,value.getString("name")
                    , IGDBScrapper.getReleaseDate(value.getInt("id"), gamesDataArray)
                    , value.getInt("id")
                    , coverHash,prefRowWidth);
            row.prefWidthProperty().bind(prefRowWidth);
            return row;
        }
    }



    private static class SearchItem<JSONObject> extends SelectListPane.ListItem {
        private final static int COVER_WIDTH = 70;
        private StackPane coverPane = new StackPane();
        private ImageView coverView = new ImageView();
        private ReadOnlyDoubleProperty prefRowWidth;

        private String gameName;
        private String coverHash;
        private String date;
        private int id;
        private SearchItem(Object value, SelectListPane parentList, String gameName, Date date, int id, String coverHash, ReadOnlyDoubleProperty prefRowWidth) {
            super(value,parentList);
            this.gameName = gameName;
            this.date = date!=null ? new SimpleDateFormat("yyyy").format(date) : null;
            this.id = id;
            this.coverHash = coverHash;
            this.prefRowWidth = prefRowWidth;

            addContent();
        }

        @Override
        protected void addContent() {
            if (coverHash != null) {
                ImageUtils.downloadIGDBImageToCache(id, coverHash, ImageUtils.IGDB_TYPE_COVER, ImageUtils.IGDB_SIZE_SMALL, new OnDLDoneHandler() {
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
                            ImageUtils.transitionToImage(new Image("file:" + File.separator + File.separator + File.separator + outputfile.getAbsolutePath(), COVER_WIDTH, COVER_WIDTH * GameButton.COVER_HEIGHT_WIDTH_RATIO, finalKeepRatio, true),coverView);
                        });
                    }
                });
            }
            coverPane.getChildren().add(new ImageView(new Image("res/defaultImages/cover.jpg", COVER_WIDTH, COVER_WIDTH * GameButton.COVER_HEIGHT_WIDTH_RATIO, false, true)));
            coverPane.getChildren().add(coverView);
            GridPane.setMargin(coverPane, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 10 * Main.SCREEN_WIDTH / 1920));
            add(coverPane,columnCount++,0);

            Label nameLabel = new Label(gameName);
            nameLabel.setPrefWidth(Double.MAX_VALUE);
            nameLabel.setWrapText(true);
            nameLabel.setTooltip(new Tooltip(gameName));
            Label yearLabel = new Label(date);

            yearLabel.setWrapText(true);

            yearLabel.setStyle("-fx-font-family: 'Helvetica Neue';\n" +
                    "    -fx-font-size: 18.0px;\n" +
                    "    -fx-font-weight: 600;" +
                    "    -fx-font-style: italic;");
            VBox box = new VBox();
            box.prefWidthProperty().bind(prefRowWidth);
            box.getChildren().addAll(nameLabel, yearLabel);
            add(box,columnCount++,0);
            GridPane.setMargin(box, new Insets(20 * Main.SCREEN_HEIGHT / 1080, 30 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 30 * Main.SCREEN_WIDTH / 1920));;
        }
    }
}

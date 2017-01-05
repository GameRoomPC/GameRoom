package ui.dialog;

import data.ImageUtils;
import data.game.entry.GameEntry;
import data.game.scrapper.IGDBScrapper;
import data.game.scrapper.OnDLDoneHandler;
import data.http.SimpleImageInfo;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import org.apache.http.conn.ConnectTimeoutException;
import org.controlsfx.control.CheckComboBox;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import ui.Main;
import ui.control.button.ImageButton;
import ui.control.button.gamebutton.GameButton;
import ui.pane.SelectListPane;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;

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

    private HashMap<String, Boolean> doNotUpdateFieldsMap;

    public SearchDialog(HashMap<String, Boolean> doNotUpdateFieldsMap) {
        this(doNotUpdateFieldsMap, null);
    }

    public SearchDialog(HashMap<String, Boolean> doNotUpdateFieldsMap, String gameName) {
        super();
        this.doNotUpdateFieldsMap = doNotUpdateFieldsMap;
        getDialogPane().getStyleClass().add("search-dialog");

        statusLabel = new Label(Main.getString("search_a_game"));
        statusLabel.setWrapText(true);
        statusLabel.setMouseTransparent(true);
        statusLabel.setFocusTraversable(false);

        searchField = new TextField();
        searchField.setPromptText(Main.getString("example_games"));
        searchField.setPrefColumnCount(20);
        if (gameName != null) {
            searchField.setText(gameName);
        }

        showingProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue)
                    searchField.requestFocus();
            }
        });
        //Image searchImage = new Image("res/ui/searchButton.png", SCREEN_WIDTH / 28, SCREEN_WIDTH / 28, true, true);
        //ImageButton searchButton = new ImageButton(searchImage);
        double imgSize = SCREEN_WIDTH / 28;
        ImageButton searchButton = new ImageButton("search-button",imgSize,imgSize);

        mainPane.getStyleClass().add("container");

        topBox = new HBox();
        topBox.setAlignment(Pos.CENTER);
        topBox.setSpacing(15 * Main.SCREEN_WIDTH / 1920);
        topBox.getChildren().addAll(searchField, searchButton);

        mainPane.setPrefWidth(1.0 / 3.5 * Main.SCREEN_WIDTH);
        mainPane.setPrefHeight(2.0 / 3 * Main.SCREEN_HEIGHT);

        searchListPane = new SearchList(topBox.widthProperty());

        searchButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                searchListPane.clearItems();
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        statusLabel.setText(Main.getString("searching") + "...");
                    }
                });
                try {
                    JSONArray resultArray = IGDBScrapper.searchGame(searchField.getText());
                    ArrayList<Integer> ids = new ArrayList<Integer>();
                    if (resultArray == null) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                statusLabel.setText(Main.getString("no_result") + "/" + Main.getString("no_internet"));
                            }
                        });
                    } else {
                        for (Object obj : resultArray) {
                            JSONObject jsob = ((JSONObject) obj);
                            ids.add(jsob.getInt("id"));
                        }
                        if (ids.size() == 0) {
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    statusLabel.setText(Main.getString("no_result"));
                                }
                            });
                        } else {
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    statusLabel.setText(Main.getString("loading") + "...");
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
                            statusLabel.setText(Main.getString("no_internet"));
                        }
                    });
                }
            }
        });
        searchListPane.setPadding(new Insets(10 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920));

        try {
            remapEnterKey(getDialogPane(), searchButton, searchField);
        } catch (AWTException e) {
            e.printStackTrace();
        }
        //resultsPane.setPrefWidth(ui.Main.SCREEN_WIDTH);

        StackPane centerPane = new StackPane();
        centerPane.setFocusTraversable(false);

        centerPane.getChildren().addAll(searchListPane, statusLabel);

        ObservableList<String> fields = FXCollections.observableArrayList();
        for (String key : doNotUpdateFieldsMap.keySet()) {
            fields.add(key);
        }
        fields.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });
        // Create the CheckComboBox with the data
        final CheckComboBox<String> fieldsComboBox = new CheckComboBox<String>(fields);
        fieldsComboBox.setConverter(new StringConverter<String>() {
            @Override
            public String toString(String object) {
                return Main.getString(object);
            }

            @Override
            public String fromString(String string) {
                return null;
            }
        });
        for (String key : doNotUpdateFieldsMap.keySet()) {
            if (doNotUpdateFieldsMap.get(key))
                fieldsComboBox.getCheckModel().check(key);
        }
        fieldsComboBox.setPrefWidth(250 * Main.SCREEN_WIDTH / 1920);
        fieldsComboBox.getCheckModel().getCheckedItems().addListener(new ListChangeListener<String>() {
            @Override
            public void onChanged(Change<? extends String> c) {
                while (c.next()) {
                    for (String s : c.getAddedSubList()) {
                        SearchDialog.this.doNotUpdateFieldsMap.put(s, true);
                    }
                    for (String s : c.getRemoved()) {
                        SearchDialog.this.doNotUpdateFieldsMap.put(s, false);
                    }
                }
            }
        });
        Label doNotUpdateLabel = new Label(Main.getString("do_not_update") + ":");
        doNotUpdateLabel.setFocusTraversable(false);
        HBox notUpdateHbox = new HBox();
        notUpdateHbox.setAlignment(Pos.CENTER);
        notUpdateHbox.setSpacing(10 * Main.SCREEN_WIDTH / 1920);
        notUpdateHbox.getChildren().addAll(doNotUpdateLabel, fieldsComboBox);

        mainPane.setTop(topBox);
        mainPane.setCenter(centerPane);
        mainPane.setBottom(notUpdateHbox);

        BorderPane.setMargin(topBox, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920, 20 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920));
        BorderPane.setMargin(notUpdateHbox, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920, 0 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920));

        ButtonType cancelButtonType = new ButtonType(ui.Main.getString("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType nextButtonType = new ButtonType(Main.getString("next"), ButtonBar.ButtonData.OK_DONE);

        getDialogPane().getButtonTypes().addAll(cancelButtonType, nextButtonType);
        Button cancelButton = (Button) getDialogPane().lookupButton(cancelButtonType);
        Button nextButton = (Button) getDialogPane().lookupButton(nextButtonType);
        cancelButton.setDefaultButton(false);
        cancelButton.setFocusTraversable(false);
        nextButton.setDefaultButton(false);
        nextButton.setFocusTraversable(false);

        setOnHiding(event -> {
            if (searchListPane.getSelectedValue() != null) {
                selectedEntry = IGDBScrapper.getEntry(searchListPane.getSelectedValue());
                String coverHash = IGDBScrapper.getEntry(searchListPane.getSelectedValue()).getIgdb_imageHash(0);
            }
        });
        if(gameName!=null){
            searchButton.fireEvent(new ActionEvent());
        }
    }

    public GameEntry getSelectedEntry() {
        return selectedEntry;
    }

    public HashMap<String, Boolean> getDoNotUpdateFieldsMap() {
        return doNotUpdateFieldsMap;
    }

    private static class SearchList extends SelectListPane<JSONObject> {
        private JSONArray gamesDataArray;
        private ReadOnlyDoubleProperty prefRowWidth;

        public SearchList(ReadOnlyDoubleProperty prefRowWidth) {
            super();
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
            SearchItem row = new SearchItem(value, this, value.getString("name")
                    , IGDBScrapper.getReleaseDate(value.getInt("id"), gamesDataArray)
                    , value.getInt("id")
                    , coverHash, prefRowWidth);
            row.prefWidthProperty().bind(prefRowWidth);
            return row;
        }
    }

    private void remapEnterKey(Pane pane, Button searchButton, TextField searchField) throws AWTException {
        pane.addEventFilter(KeyEvent.ANY, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (!event.isShiftDown()) {
                    switch (event.getCode()) {
                        case ENTER:
                            if (searchField.isFocused() && !searchField.getText().equals("")) {
                                searchButton.fireEvent(new ActionEvent());
                            }
                            break;
                    }
                }
            }
        });
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
            super(value, parentList);
            this.gameName = gameName;
            this.date = date != null ? new SimpleDateFormat("yyyy").format(date) : null;
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
                            ImageUtils.transitionToImage(new Image("file:" + File.separator + File.separator + File.separator + outputfile.getAbsolutePath(), COVER_WIDTH, COVER_WIDTH * GameButton.COVER_HEIGHT_WIDTH_RATIO, finalKeepRatio, true), coverView);
                        });
                    }
                });
            }
            coverPane.getChildren().add(new ImageView(new Image("res/defaultImages/cover.jpg", COVER_WIDTH, COVER_WIDTH * GameButton.COVER_HEIGHT_WIDTH_RATIO, false, true)));
            coverPane.getChildren().add(coverView);
            GridPane.setMargin(coverPane, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 10 * Main.SCREEN_WIDTH / 1920));
            add(coverPane, columnCount++, 0);

            Label nameLabel = new Label(gameName);
            nameLabel.setPrefWidth(Double.MAX_VALUE);
            nameLabel.setWrapText(true);
            nameLabel.setTooltip(new Tooltip(gameName));
            Label yearLabel = new Label(date);

            yearLabel.setWrapText(true);

            /*yearLabel.setStyle("-fx-font-family: 'Helvetica Neue';\n" +
                    "    -fx-font-size: 18.0px;\n" +
                    "    -fx-font-weight: 600;" +
                    "    -fx-font-style: italic;");*/
            yearLabel.setId("search-result-year-label");
            VBox box = new VBox();
            box.prefWidthProperty().bind(prefRowWidth);
            box.getChildren().addAll(nameLabel, yearLabel);
            add(box, columnCount++, 0);
            GridPane.setMargin(box, new Insets(20 * Main.SCREEN_HEIGHT / 1080, 30 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 30 * Main.SCREEN_WIDTH / 1920));
        }
    }
}

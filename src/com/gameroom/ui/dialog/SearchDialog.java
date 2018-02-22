package com.gameroom.ui.dialog;

import com.mashape.unirest.http.exceptions.UnirestException;
import com.gameroom.data.game.entry.GameEntry;
import com.gameroom.data.game.entry.Platform;
import com.gameroom.data.game.scraper.IGDBScraper;
import com.gameroom.data.game.scraper.OnDLDoneHandler;
import com.gameroom.data.http.images.ImageUtils;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.gameroom.ui.Main;
import com.gameroom.ui.control.button.ImageButton;
import com.gameroom.ui.control.button.gamebutton.GameButton;
import com.gameroom.ui.pane.SelectListPane;

import java.awt.*;
import java.io.File;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.gameroom.system.application.settings.GeneralSettings.settings;
import static com.gameroom.ui.Main.LOGGER;
import static com.gameroom.ui.Main.SCREEN_WIDTH;
import static com.gameroom.ui.Main.SUPPORTER_MODE;

/**
 * Created by LM on 12/07/2016.
 */
public class SearchDialog extends GameRoomDialog<ButtonType> {
    private HBox topBox;
    private TextField searchField;
    private Label statusLabel;
    private GameEntry selectedEntry;
    private boolean updatePlatform = false; //if we should update the platform after this dialog is closed

    private SearchList searchListPane;

    private BooleanProperty allowDLCs = new SimpleBooleanProperty(false);
    private IntegerProperty platformIdToSearch = new SimpleIntegerProperty(-1);

    public SearchDialog(String gameName) {
        super();
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

        showingProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue)
                searchField.requestFocus();
        });
        double imgSize = 40 * SCREEN_WIDTH / 1920;
        ImageButton searchButton = new ImageButton("search-button", imgSize, imgSize);

        mainPane.getStyleClass().add("container");

        topBox = new HBox();
        topBox.setAlignment(Pos.CENTER);
        topBox.setSpacing(15 * Main.SCREEN_WIDTH / 1920);
        topBox.getStyleClass().add("header");
        topBox.getChildren().addAll(searchField, searchButton);

        mainPane.setPrefWidth(1.0 / 3.5 * Main.SCREEN_WIDTH);
        mainPane.setPrefHeight(2.0 / 3 * Main.SCREEN_HEIGHT);

        searchListPane = new SearchList(topBox.widthProperty());

        searchButton.setOnAction(event -> startResearch());

        searchListPane.setPadding(new Insets(10 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920));

        try {
            remapEnterKey(getDialogPane(), searchField);
        } catch (AWTException e) {
            e.printStackTrace();
        }
        //resultsPane.setPrefWidth(com.gameroom.ui.Main.SCREEN_WIDTH);

        StackPane centerPane = new StackPane();
        centerPane.setFocusTraversable(false);

        centerPane.getChildren().addAll(searchListPane, statusLabel);

        try {
            Platform.initWithDb();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        final ObservableList<Platform> platforms = FXCollections.observableArrayList(Platform.getEmulablePlatforms());
        platforms.add(0, Platform.ALL_PLATFORMS);
        platforms.add(1, Platform.PC);

        Label restrictPlatformLabel = new Label(Main.getString("platform") + ":");
        restrictPlatformLabel.setFocusTraversable(false);

        // Create the CheckComboBox with the com.gameroom.data
        final ComboBox<Platform> platformComboBox = new ComboBox<>(platforms);

        platformComboBox.setCellFactory(param -> new ListCell<Platform>() {
            private ImageView imageView = new ImageView();

            @Override
            public void updateItem(Platform platform, boolean empty) {
                super.updateItem(platform, empty);
                if (empty || platform == null) {
                    //Corresponds to the "all" choice !
                    imageView.setId("");
                    setText(Main.getString("all_platforms"));
                    setGraphic(null);
                } else {
                    double width = 25 * Main.SCREEN_WIDTH / 1920;
                    double height = 25 * Main.SCREEN_HEIGHT / 1080;

                    platform.setCSSIcon(imageView, settings().getTheme().useDarkPlatformIconsInList());
                    imageView.setFitWidth(width);
                    imageView.setFitHeight(height);
                    imageView.setSmooth(true);

                    setText(platform.getName());
                    setGraphic(imageView);
                }
            }
        });
        platformComboBox.setId("platform");
        platformComboBox.getSelectionModel().select(0);
        platformComboBox.maxWidthProperty().bind(searchField.widthProperty().subtract(restrictPlatformLabel.widthProperty()));

        platformComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            platformIdToSearch.setValue(newValue.getIGDBId());
            startResearch();
        });

        HBox restrictPlatformHbox = new HBox();
        restrictPlatformHbox.setAlignment(Pos.CENTER_LEFT);
        restrictPlatformHbox.setSpacing(10 * Main.SCREEN_WIDTH / 1920);
        restrictPlatformHbox.getChildren().addAll(restrictPlatformLabel, platformComboBox);

        CheckBox searchDLCCheckBox = new CheckBox();
        searchDLCCheckBox.setSelected(false);
        allowDLCs.bind(searchDLCCheckBox.selectedProperty());
        searchDLCCheckBox.selectedProperty().addListener(observable -> startResearch());
        Label searchDLCLabel = new Label(Main.getString("search_also_DLCs") + ":");
        searchDLCLabel.setFocusTraversable(false);
        HBox searchDLCHbox = new HBox();
        searchDLCHbox.setAlignment(Pos.CENTER_LEFT);
        searchDLCHbox.setSpacing(10 * Main.SCREEN_WIDTH / 1920);
        searchDLCHbox.getChildren().addAll(searchDLCLabel, searchDLCCheckBox);

        VBox bottomVbox = new VBox();
        bottomVbox.setAlignment(Pos.BASELINE_LEFT);
        bottomVbox.setSpacing(5 * Main.SCREEN_WIDTH / 1920);
        bottomVbox.getChildren().add(searchDLCHbox);
        if (SUPPORTER_MODE) {
            bottomVbox.getChildren().add(restrictPlatformHbox);
        }

        mainPane.setTop(topBox);
        mainPane.setCenter(centerPane);
        mainPane.setBottom(bottomVbox);

        BorderPane.setMargin(topBox, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920, 20 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920));
        BorderPane.setMargin(bottomVbox, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920, 0 * Main.SCREEN_HEIGHT / 1080, 20 * Main.SCREEN_WIDTH / 1920));

        ButtonType cancelButtonType = new ButtonType(com.gameroom.ui.Main.getString("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
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
                selectedEntry = IGDBScraper.getEntry(searchListPane.getSelectedValue());
                selectedEntry.setPlatform(Platform.getFromIGDBId(platformIdToSearch.get()));
                updatePlatform = platformIdToSearch.get() != Platform.ALL_PLATFORMS.getIGDBId();
            }
        });
        Main.getExecutorService().submit(() -> {
            if (gameName != null) {
                startResearch();
            }
        });
    }

    public boolean updatePlatformOnClose() {
        return updatePlatform;
    }

    private void startResearch() {
        searchListPane.clearItems();
        javafx.application.Platform.runLater(() -> statusLabel.setText(Main.getString("searching") + "..."));
        try {
            JSONArray resultArray = IGDBScraper.searchGame(searchField.getText(), allowDLCs.get(), platformIdToSearch.get());
            if (resultArray == null) {
                javafx.application.Platform.runLater(() -> statusLabel.setText(Main.getString("no_result") + "/" + Main.getString("no_internet")));
            } else {
                try {
                    if (resultArray.length() == 0) {
                        javafx.application.Platform.runLater(() -> statusLabel.setText(Main.getString("no_result")));
                    } else {
                        javafx.application.Platform.runLater(() -> statusLabel.setText(Main.getString("loading") + "..."));
                        Task scrapping = new Task() {
                            @Override
                            protected String call() throws Exception {
                                javafx.application.Platform.runLater(() -> statusLabel.setText(""));
                                javafx.application.Platform.runLater(() -> searchListPane.addItems(resultArray.iterator()));
                                return null;
                            }
                        };
                        Main.getExecutorService().submit(scrapping);
                    }
                } catch (JSONException e) {
                    GameRoomAlert.errorGameRoomAPI();
                }
            }
        } catch (UnirestException e) {
            LOGGER.error(e.getMessage());
            GameRoomAlert.errorGameRoomAPI();
            close();
        }
    }

    public GameEntry getSelectedEntry() {
        return selectedEntry;
    }

    private static class SearchList extends SelectListPane<JSONObject> {
        private ReadOnlyDoubleProperty prefRowWidth;

        SearchList(ReadOnlyDoubleProperty prefRowWidth) {
            super();
            this.prefRowWidth = prefRowWidth;
        }

        @Override
        protected ListItem createListItem(JSONObject value) {
            String coverHash = null;
            try {
                coverHash = IGDBScraper.extractCoverImageHash(value);
            } catch (JSONException je) {
                Main.LOGGER.debug("No cover for game " + value.getString("name"));
                if (!je.toString().contains("cover")) {
                    je.printStackTrace();
                }
            }
            Date release_date = null;
            if (value.has("release_date") && !value.isNull("release_date")) {
                release_date = new Date(value.getLong("release_date"));
            }

            SearchItem row = new SearchItem(
                    value,
                    this,
                    value.getString("name"),
                    release_date,
                    value.getInt("id"),
                    coverHash,
                    prefRowWidth,
                    IGDBScraper.extractPlatformIds(value)
            );
            row.prefWidthProperty().bind(prefRowWidth);
            return row;
        }
    }

    private void remapEnterKey(Pane pane, TextField searchField) throws AWTException {
        pane.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if (!event.isShiftDown()) {
                switch (event.getCode()) {
                    case ENTER:
                        if (searchField.isFocused() && !searchField.getText().equals("")) {
                            startResearch();
                            event.consume();
                        }
                        break;
                }
            }
        });
    }

    private static class SearchItem extends SelectListPane.ListItem {
        private final static int COVER_WIDTH = 70;
        private StackPane coverPane = new StackPane();
        private ImageView coverView = new ImageView();
        private ReadOnlyDoubleProperty prefRowWidth;

        private String gameName;
        private String coverHash;
        private String date;
        private int id;
        private int[] platformIds;

        private SearchItem(Object value, SelectListPane parentList, String gameName, Date date, int id, String coverHash, ReadOnlyDoubleProperty prefRowWidth, int[] platformIds) {
            super(value, parentList);
            this.gameName = gameName;
            this.date = date != null ? new SimpleDateFormat("yyyy").format(date) : null;
            this.id = id;
            this.coverHash = coverHash;
            this.prefRowWidth = prefRowWidth;
            this.platformIds = platformIds;

            addContent();
        }

        @Override
        protected void addContent() {
            if (coverHash != null) {
                ImageUtils.downloadIGDBImageToCache(id, coverHash, ImageUtils.IGDB_TYPE_COVER, ImageUtils.IGDB_SIZE_SMALL, new OnDLDoneHandler() {
                    @Override
                    public void run(File outputFile) {
                        boolean finalKeepRatio = ImageUtils.shouldKeepImageRatio(outputFile);
                        javafx.application.Platform.runLater(() -> {
                            ImageUtils.transitionToImage(new Image("file:" + File.separator + File.separator + File.separator + outputFile.getAbsolutePath(), COVER_WIDTH, COVER_WIDTH * GameButton.COVER_HEIGHT_WIDTH_RATIO, finalKeepRatio, true), coverView);
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

            HBox logoBox = new HBox(5 * Main.SCREEN_WIDTH / 1920);
            logoBox.prefWidthProperty().bind(prefRowWidth);
            double logoWidth = 25 * Main.SCREEN_WIDTH / 1920;
            double logoHeight = 25 * Main.SCREEN_HEIGHT / 1080;


            for (int platformId : platformIds) {
                Platform p = Platform.getFromIGDBId(platformId);
                if (p != null) {
                    ImageView temp = new ImageView();
                    temp.setSmooth(false);
                    temp.setPreserveRatio(true);
                    temp.setFitWidth(logoWidth);
                    temp.setFitHeight(logoHeight);
                    p.setCSSIcon(temp, false);
                    logoBox.getChildren().add(temp);
                }
            }

            VBox box = new VBox();
            box.prefWidthProperty().bind(prefRowWidth);
            box.getChildren().addAll(nameLabel, yearLabel, logoBox);
            add(box, columnCount++, 0);
            GridPane.setMargin(box, new Insets(20 * Main.SCREEN_HEIGHT / 1080, 30 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 30 * Main.SCREEN_WIDTH / 1920));
        }
    }
}

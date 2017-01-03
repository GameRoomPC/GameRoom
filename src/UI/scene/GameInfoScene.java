package ui.scene;

import data.game.entry.GameEntry;
import data.game.entry.GameGenre;
import data.game.entry.GameTheme;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import system.application.settings.PredefinedSetting;
import ui.Main;
import ui.control.button.ImageButton;
import ui.control.button.gamebutton.InfoGameButton;
import ui.control.specific.YoutubePlayerAndButton;
import ui.dialog.GameRoomAlert;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

import static ui.Main.*;

/**
 * Created by LM on 03/07/2016.
 */
public class GameInfoScene extends BaseScene {
    private BorderPane wrappingPane;
    private GameEntry entry;
    private GridPane propertiesPane = new GridPane();
    private InfoGameButton coverButton;
    private YoutubePlayerAndButton ytButton;

    private int row_count = 0;

    public GameInfoScene(StackPane stackPane, Stage parentStage, BaseScene previousScene, GameEntry entry) {
        super(stackPane, parentStage);
        this.entry = entry;
        this.previousScene = previousScene;
        initTop();
        initCenter();
        initBottom();
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            switch (event.getCode()){
                case SPACE:
                    if(ytButton!=null){
                        if(!ytButton.getSoundMuteButton().isMouseTransparent()){
                            ytButton.getSoundMuteButton().getOnAction().handle(new ActionEvent());
                        }
                    }
                    break;
                default:
                    break;
            }
        });
        if(previousScene instanceof MainScene && ((MainScene)previousScene).getInputMode() == MainScene.INPUT_MODE_KEYBOARD){
            coverButton.requestFocus();
        }
    }

    private void initBottom() {
        HBox hBox = new HBox();
        hBox.setSpacing(30 * SCREEN_WIDTH / 1920);
        Button editButton = new Button(RESSOURCE_BUNDLE.getString("edit"));
        editButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                fadeTransitionTo(new GameEditScene(GameInfoScene.this, entry,coverButton.getImage()), getParentStage());
            }
        });
        Button deleteButton = new Button(RESSOURCE_BUNDLE.getString("delete"));
        deleteButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                GameRoomAlert alert = new GameRoomAlert(Alert.AlertType.CONFIRMATION);
                alert.setContentText(RESSOURCE_BUNDLE.getString("delete_entry?"));

                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK) {
                    entry.deleteFiles();
                    MAIN_SCENE.removeGame(entry);
                    fadeTransitionTo(previousScene, getParentStage());
                }
            }
        });
        hBox.getChildren().addAll(deleteButton, editButton);
        hBox.setAlignment(Pos.BOTTOM_RIGHT);
        wrappingPane.setBottom(hBox);
        BorderPane.setMargin(hBox, new Insets(10 * SCREEN_WIDTH / 1920, 30 * SCREEN_WIDTH / 1920, 30 * SCREEN_WIDTH / 1920, 30 * SCREEN_WIDTH / 1920));

        if (entry.getImagePath(1) != null) {
            //Main.LOGGER.debug("Screenshot available : "+entry.getImagePath(1));
            Image screenshotImage = entry.getImage(1,
                    Main.GENERAL_SETTINGS.getWindowWidth()*BACKGROUND_IMAGE_LOAD_RATIO,
                    Main.GENERAL_SETTINGS.getWindowHeight()*BACKGROUND_IMAGE_LOAD_RATIO
                    , false, true);

            double widthScale = 1;
            double heightScale = 1;
            if(screenshotImage.getWidth() != GENERAL_SETTINGS.getWindowWidth()){
                widthScale = (double)GENERAL_SETTINGS.getWindowWidth()/screenshotImage.getWidth();
            }
            if(screenshotImage.getHeight() != GENERAL_SETTINGS.getWindowHeight()){
                heightScale = (double)GENERAL_SETTINGS.getWindowHeight()/screenshotImage.getHeight();
            }
            backgroundView.setScaleX(widthScale);
            backgroundView.setScaleY(heightScale);
            backgroundView.setImage(screenshotImage);
            GaussianBlur blur = new GaussianBlur(BACKGROUND_IMAGE_BLUR);

            backgroundView.setEffect(blur);
            backgroundView.setOpacity(BACKGROUND_IMAGE_MAX_OPACITY);
        }

    }

    private void initTop() {
        StackPane topStackPane = createTop(entry.getName());
        if(!GENERAL_SETTINGS.getBoolean(PredefinedSetting.DISABLE_GAME_MAIN_THEME)) {
            try {
                ytButton = new YoutubePlayerAndButton(entry);
                topStackPane.getChildren().add(ytButton.getSoundMuteButton());
                StackPane.setAlignment(ytButton.getSoundMuteButton(), Pos.CENTER_RIGHT);
                setOnSceneFadedOutAction(new Runnable() {
                    @Override
                    public void run() {
                        ytButton.quitYoutube();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        wrappingPane.setTop(topStackPane);
    }

    private void initCenter() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        propertiesPane.setVgap(20 * SCREEN_WIDTH / 1920);
        propertiesPane.setHgap(20 * SCREEN_WIDTH / 1920);

        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setPercentWidth(20);
        propertiesPane.getColumnConstraints().add(cc1);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setPercentWidth(70);
        propertiesPane.getColumnConstraints().add(cc2);

        propertiesPane.setAlignment(Pos.TOP_LEFT);
        addProperty("play_time", entry.getPlayTimeFormatted(GameEntry.TIME_FORMAT_HALF_FULL_HMS)).setId("basic-info-label");

        /***************************PATH******************************************/
        addProperty("game_path", entry.getPath());

        double imgSize = 50 * SCREEN_WIDTH / 1920;
        //Image folderImage = new Image("res/ui/folderButton.png", 50 * SCREEN_WIDTH / 1920, 50 * SCREEN_HEIGHT / 1080, false, true);
        ImageButton folderButton = new ImageButton("folder-button", imgSize,imgSize);
        folderButton.setOnAction(event -> {
            try {
                Desktop.getDesktop().open(new File(entry.getPath()).getParentFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        if(new File(entry.getPath()).exists()) {
            propertiesPane.add(folderButton, 2, row_count - 1);
        }
        /***************************END PATH******************************************/


        /***************************SEPARATORS******************************************/
        Separator s1 = new Separator();
        propertiesPane.add(s1, 0, row_count);
        row_count++;
        /****************************END SEPARATORS************************************/
        addProperty("release_date", entry.getReleaseDate() != null ? GameEntry.DATE_DISPLAY_FORMAT.format(entry.getReleaseDate()) : "");
        if(GENERAL_SETTINGS.getBoolean(PredefinedSetting.DEBUG_MODE)){
            addProperty("added_date", entry.getAddedDate() != null ? GameEntry.DATE_STORE_FORMAT.format(entry.getAddedDate()) : "").setId("advanced-setting-label");
        }
        addProperty("developer", entry.getDeveloper());
        addProperty("publisher", entry.getPublisher());
        addProperty("serie", entry.getSerie());
        if(entry.getGenres()!=null){
            String genres = "";
            for(GameGenre genre : entry.getGenres()){
                genres+=genre.getDisplayName()+", ";
            }
            addProperty("genre",!genres.equals("") ? genres.substring(0,genres.length()-2):"-");
        }else{
            addProperty("genre",null);
        }
        if(entry.getThemes()!=null){
            String themes = "";
            for(GameTheme theme : entry.getThemes()){
                themes+=theme.getDisplayName()+", ";
            }
            addProperty("theme",!themes.equals(", ") ? themes.substring(0,themes.length()-2) : "-");
        }else{
            addProperty("theme",null);
        }
        addProperty("description", entry.getDescription());

        GridPane coverAndPropertiesPane = new GridPane();

        coverAndPropertiesPane.setVgap(20 * SCREEN_WIDTH / 1920);
        coverAndPropertiesPane.setHgap(60 * SCREEN_WIDTH / 1920);

        coverButton = new InfoGameButton(entry, this, wrappingPane);
        coverAndPropertiesPane.add(coverButton, 0, 0);
        coverAndPropertiesPane.setPadding(new Insets(50 * SCREEN_HEIGHT / 1080, 50 * SCREEN_WIDTH / 1920, 20 * SCREEN_HEIGHT / 1080, 50 * SCREEN_WIDTH / 1920));

        propertiesPane.setPadding(new Insets(30 * SCREEN_HEIGHT / 1080, 30 * SCREEN_WIDTH / 1920, 30 * SCREEN_HEIGHT / 1080, 30 * SCREEN_WIDTH / 1920));

        scrollPane.setContent(propertiesPane);
        coverAndPropertiesPane.add(scrollPane, 1, 0);

        wrappingPane.setCenter(coverAndPropertiesPane);

    }

    private Label addProperty(String title, String value) {
        Label titleLabel = new Label(RESSOURCE_BUNDLE.getString(title) + " :");
        titleLabel.setAlignment(Pos.TOP_LEFT);
        titleLabel.setStyle("-fx-font-weight: lighter;");
        titleLabel.setTooltip(new Tooltip(RESSOURCE_BUNDLE.getString(title)));
        propertiesPane.add(titleLabel, 0, row_count);
        Label valueLabel = new Label(value);
        if (value == null || value.equals("")) {
            valueLabel.setText("-");
        }
        valueLabel.setStyle("-fx-font-weight: normal;");
        propertiesPane.add(valueLabel, 1, row_count);
        valueLabel.setWrapText(true);
        valueLabel.setId(title);
        row_count++;
        return valueLabel;
    }
    void updateWithEditedEntry(GameEntry editedEntry){
        updateProperty("play_time", editedEntry.getPlayTimeFormatted(GameEntry.TIME_FORMAT_HALF_FULL_HMS));
        updateProperty("game_path", editedEntry.getPath());
        updateProperty("release_date", editedEntry.getReleaseDate()!=null ? GameEntry.DATE_DISPLAY_FORMAT.format(editedEntry.getReleaseDate()) : "");
        updateProperty("developer", editedEntry.getDeveloper());
        updateProperty("publisher", editedEntry.getPublisher());
        updateProperty("serie", editedEntry.getSerie());
        if(entry.getGenres()!=null){
            String genres = "";
            for(GameGenre genre : entry.getGenres()){
                genres+=genre.getDisplayName()+", ";
            }
            updateProperty("genre", genres.length() > 2 ? genres.substring(0, genres.length() - 2) : "-");

        }else{
            updateProperty("genre",null);
        }
        if(entry.getThemes()!=null){
            String themes = "";
            for(GameTheme theme : entry.getThemes()){
                themes+=theme.getDisplayName()+", ";
            }
            updateProperty("theme",themes.length() > 2 ?themes.substring(0,themes.length()-2) : "-");
        }else{
            updateProperty("theme",null);
        }
        updateProperty("description", editedEntry.getDescription());
        Image backgroundImage = editedEntry.getImage(1,
                Main.GENERAL_SETTINGS.getWindowWidth(),
                Main.GENERAL_SETTINGS.getWindowHeight()
                , false, true);
        //no need to fade transition here as it is the "right" image and no actual change
        double widthScale = 1;
        double heightScale = 1;
        if(backgroundImage.getWidth() != GENERAL_SETTINGS.getWindowWidth()){
            widthScale = (double)GENERAL_SETTINGS.getWindowWidth()/backgroundImage.getWidth();
        }
        if(backgroundImage.getHeight() != GENERAL_SETTINGS.getWindowHeight()){
            heightScale = (double)GENERAL_SETTINGS.getWindowHeight()/backgroundImage.getHeight();
        }
        backgroundView.setScaleX(widthScale);
        backgroundView.setScaleY(heightScale);
        backgroundView.setImage(backgroundImage);
        GaussianBlur blur = new GaussianBlur(BACKGROUND_IMAGE_BLUR);

        backgroundView.setEffect(blur);
        backgroundView.setOpacity(BACKGROUND_IMAGE_MAX_OPACITY);
        coverButton.setImage(editedEntry.getImage(0
                ,coverButton.getWidth()
                ,coverButton.getHeight()
                ,false
                ,true));
    }
    private void updateProperty(String title, String value) {
        for (Node node : propertiesPane.getChildren()) {
            if (node != null && node instanceof Label && node.getId() != null && node.getId().equals(title)) {
                ((Label) node).setText(value);
            }
        }
    }

    @Override
    public Pane getWrappingPane() {
        return wrappingPane;
    }

    @Override
    void initAndAddWrappingPaneToRoot() {
        wrappingPane = new BorderPane();
        getRootStackPane().getChildren().add(wrappingPane);
    }
}

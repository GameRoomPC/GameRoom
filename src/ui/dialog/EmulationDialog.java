package ui.dialog;

import data.game.entry.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import ui.Main;
import ui.control.specific.SearchBar;
import ui.pane.platform.PlatformSettingsPane;

import static system.application.settings.GeneralSettings.settings;
import static ui.Main.SCREEN_HEIGHT;
import static ui.Main.SCREEN_WIDTH;

/**
 * @author LM. Garret (admin@gameroom.me)
 * @date 09/06/2017.
 */
public class EmulationDialog extends GameRoomDialog<ButtonType> {
    private PlatformSettingsPane currentCenterPane;

    public EmulationDialog() {
        this(null);
    }

    public EmulationDialog(Platform focusedPlatform){
        this(focusedPlatform,null);
    }

    public EmulationDialog(Platform focusedPlatform, ButtonType buttonType) {
        super();
        mainPane.getStyleClass().add("container");
        ButtonType okButton = new ButtonType(Main.getString("close"), ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().add(okButton);

        if(buttonType != null){
            getDialogPane().getButtonTypes().add(buttonType);
        }

        mainPane.setCenter(new EmulationPane(focusedPlatform,getOwner()));
    }

    public static class EmulationPane extends BorderPane{
        private PlatformSettingsPane currentCenterPane;

        public EmulationPane(Window window){
            this(null,window);
        }
        public EmulationPane(Platform focusedPlatform, Window window){
            super();
            getStyleClass().add("container");

            setLeft(createLeftPane(focusedPlatform,window));
        }

        private Node createLeftPane(Platform focusedPlatform, Window window) {

            ListView<Platform> listView = new ListView<Platform>();
            ObservableList<Platform> items = FXCollections.observableArrayList (Platform.getEmulablePlatforms());
            listView.setItems(items);

            listView.setCellFactory(param -> new ListCell<Platform>() {
                private ImageView imageView = new ImageView();
                @Override
                public void updateItem(Platform platform, boolean empty) {
                    super.updateItem(platform, empty);
                    if (empty || platform == null) {
                        imageView.setId("");
                        setText(null);
                        setGraphic(null);
                    } else {
                        double width = 30*Main.SCREEN_WIDTH/1920;
                        double height =  30*Main.SCREEN_HEIGHT/1080;

                        platform.setCSSIconDark(imageView);
                        imageView.setFitWidth(width);
                        imageView.setFitHeight(height);
                        imageView.setSmooth(true);

                        setText(platform.getName());
                        setGraphic(imageView);
                    }
                }
            });
            listView.setEditable(false);
            listView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if(newValue != null) {
                    currentCenterPane = new PlatformSettingsPane(newValue,window);
                    currentCenterPane.setMaxWidth(3 * settings().getWindowWidth() / 5.0);
                    currentCenterPane.setPrefWidth(2.5 * settings().getWindowWidth() / 5.0);
                    currentCenterPane.setPadding(new Insets(10 * Main.SCREEN_WIDTH / 1920,
                            20 * Main.SCREEN_HEIGHT / 1080,
                            10 * Main.SCREEN_WIDTH / 1920,
                            20 * Main.SCREEN_HEIGHT / 1080
                    ));
                    setCenter(currentCenterPane);
                }
            });
            listView.getSelectionModel().select(focusedPlatform == null ? 0 : items.indexOf(focusedPlatform));
            listView.setPrefWidth(1.1 * settings().getWindowWidth() / 5.0);
            listView.setPrefHeight(3 * settings().getWindowHeight() / 5.0);

            SearchBar bar = new SearchBar((observable, oldValue, newValue) -> {
                listView.setItems(
                        items.filtered(platform -> platform.getName().trim().toLowerCase().contains(newValue.trim().toLowerCase()))
                );
                listView.refresh();
            });
            bar.setId("search-bar-embedded");
            bar.prefWidthProperty().bind(listView.widthProperty());
            bar.setPadding(new Insets(10*SCREEN_HEIGHT/1080,0*SCREEN_WIDTH/1920,10*SCREEN_HEIGHT/1080,0*SCREEN_WIDTH/1920));


            VBox box = new VBox();
            box.setSpacing(5*Main.SCREEN_HEIGHT/1080);
            box.getChildren().addAll(bar,listView);
            VBox.setVgrow(listView, Priority.ALWAYS);
            VBox.setVgrow(bar, Priority.NEVER);
            box.setPadding(new Insets(10 * Main.SCREEN_WIDTH / 1920,
                    0 * Main.SCREEN_HEIGHT / 1080,
                    10 * Main.SCREEN_WIDTH / 1920,
                    20 * Main.SCREEN_HEIGHT / 1080
            ));
            return box;
        }
    }
}

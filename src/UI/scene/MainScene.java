package UI.scene;

import UI.button.ImageButton;
import UI.dialog.ChoiceDialog;
import UI.button.gamebutton.TileGameButton;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.*;
import data.GameEntry;
import UI.Main;

import java.io.File;
import java.util.*;

import static UI.Main.*;
import static UI.button.gamebutton.GameButton.COVER_HEIGHT_WIDTH_RATIO;

/**
 * Created by LM on 03/07/2016.
 */
public class MainScene extends BaseScene {

    public final static double MAX_SCALE_FACTOR = 0.9;
    public final static double MIN_SCALE_FACTOR = 0.1;

    private BorderPane wrappingPane;

    private TilePane tilePane = new TilePane();

    public MainScene(Stage parentStage) {
        super(new StackPane(), parentStage);
        setCursor(Cursor.DEFAULT);
        addEventHandler(MouseEvent.MOUSE_MOVED, new EventHandler<MouseEvent>(){

            @Override
            public void handle(MouseEvent event) {
                if(getCursor()==null){
                    setCursor(Cursor.DEFAULT);
                }
                if(getCursor().equals(Cursor.NONE)){
                    setCursor(Cursor.DEFAULT);
                }
            }
        });
        initCenter();
        initTop();
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

    private void initCenter() {
        tilePane.setHgap(50* SCREEN_WIDTH /1920);
        tilePane.setVgap(70* SCREEN_HEIGHT /1080);
        tilePane.setPrefTileWidth(SCREEN_WIDTH / 4);
        tilePane.setPrefTileHeight(tilePane.getPrefTileWidth() * COVER_HEIGHT_WIDTH_RATIO);

        for (UUID uuid : Main.ALL_GAMES_ENTRIES.readUUIDS()) {
            addGame(new GameEntry(uuid));
        }
        ScrollPane centerPane = new ScrollPane();
        remapArrowKeys(centerPane);
        centerPane.setFitToWidth(true);
        centerPane.setFitToHeight(true);
        //centerPane.setPrefViewportHeight(tilePane.getPrefHeight());
        centerPane.setFocusTraversable(false);
        centerPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        centerPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        //StackPane.setMargin(tilePane, new Insets(50* SCREEN_HEIGHT /1080, 25* SCREEN_WIDTH /1920, 60* SCREEN_HEIGHT /1080, 25* SCREEN_WIDTH /1920));
        tilePane.setPadding(new Insets(50* SCREEN_HEIGHT /1080, 30* SCREEN_WIDTH /1920, 0* SCREEN_HEIGHT /1080, 30* SCREEN_WIDTH /1920));

        centerPane.setContent(tilePane);
        //centerPane.getStylesheets().add("res/flatterfx.css");

        wrappingPane.setCenter(centerPane);

    }

    private void initTop() {
        Slider sizeSlider = new Slider();
        sizeSlider.setMin(MIN_SCALE_FACTOR);
        sizeSlider.setMax(MAX_SCALE_FACTOR);
        sizeSlider.setBlockIncrement(0.1);
        sizeSlider.setFocusTraversable(false);

        sizeSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                tilePane.setPrefTileWidth(Main.SCREEN_WIDTH / 4 * newValue.doubleValue());
                tilePane.setPrefTileHeight(Main.SCREEN_WIDTH / 4 * COVER_HEIGHT_WIDTH_RATIO * newValue.doubleValue());
            }
        });
        sizeSlider.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                Main.GENERAL_SETTINGS.setTileZoom(sizeSlider.getValue());
            }
        });
        sizeSlider.setValue(Main.GENERAL_SETTINGS.getTileZoom());

        sizeSlider.setPrefWidth(Main.SCREEN_WIDTH / 8);
        sizeSlider.setMaxWidth(Main.SCREEN_WIDTH / 8);
        sizeSlider.setPrefHeight(Main.SCREEN_WIDTH / 160);
        sizeSlider.setMaxHeight(Main.SCREEN_WIDTH / 160);

        /*sizeSlider.setScaleX(0.7);
        sizeSlider.setScaleY(0.7);*/

        Image settingsImage = new Image("res/ui/settingsButton.png", SCREEN_WIDTH / 40, SCREEN_WIDTH / 40, true, true);
        ImageButton settingsButton = new ImageButton(settingsImage);
        settingsButton.setFocusTraversable(false);
        settingsButton.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.isPrimaryButtonDown()) {
                    SettingsScene settingsScene = new SettingsScene(new StackPane(), (int) SCREEN_WIDTH, (int) SCREEN_HEIGHT, getParentStage(), MainScene.this);
                    fadeTransitionTo(settingsScene, getParentStage());
                }
            }
        });
        /*settingsButton.setScaleX(0.7);
        settingsButton.setScaleY(0.7);*/

        Image addImage = new Image("res/ui/addButton.png", SCREEN_WIDTH / 45, SCREEN_WIDTH / 45, true, true);
        ImageButton addButton = new ImageButton(addImage);
        addButton.setFocusTraversable(false);

        addButton.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.isPrimaryButtonDown()) {
                    UI.dialog.ChoiceDialog choiceDialog = new ChoiceDialog(
                            new ChoiceDialog.ChoiceDialogButton(RESSOURCE_BUNDLE.getString("Add_exe"), RESSOURCE_BUNDLE.getString("add_exe_long")),
                            new ChoiceDialog.ChoiceDialogButton(RESSOURCE_BUNDLE.getString("Add_folder_ink"), RESSOURCE_BUNDLE.getString("add_symlink_long"))
                    );
                    choiceDialog.setTitle(RESSOURCE_BUNDLE.getString("add_a_game"));
                    choiceDialog.setHeader(RESSOURCE_BUNDLE.getString("choose_action"));

                    Optional<ButtonType> result = choiceDialog.showAndWait();
                    result.ifPresent(letter -> {
                        if (letter.getText().equals(RESSOURCE_BUNDLE.getString("Add_exe"))) {
                            FileChooser fileChooser = new FileChooser();
                            fileChooser.setTitle(RESSOURCE_BUNDLE.getString("select_program"));
                            fileChooser.setInitialDirectory(
                                    new File(System.getProperty("user.home"))
                            );
                            //TODO fix internet shorcuts problem (bug submitted)
                            fileChooser.getExtensionFilters().addAll(
                                    new FileChooser.ExtensionFilter("EXE", "*.exe"),
                                    new FileChooser.ExtensionFilter("JAR", "*.jar"),
                                    new FileChooser.ExtensionFilter("URL", "*.url")
                            );
                            try {
                                File selectedFile = fileChooser.showOpenDialog(getParentStage());
                                if (selectedFile != null) {
                                    fadeTransitionTo(new GameEditScene(new StackPane(), (int) SCREEN_WIDTH, (int) SCREEN_HEIGHT, getParentStage(), MainScene.this, selectedFile), getParentStage());
                                }
                            }catch (NullPointerException ne){
                                ne.printStackTrace();
                                Alert alert = new Alert(Alert.AlertType.WARNING);
                                alert.setHeaderText(null);
                                alert.initStyle(StageStyle.UNDECORATED);
                                alert.getDialogPane().getStylesheets().add("res/flatterfx.css");
                                alert.initModality(Modality.APPLICATION_MODAL);
                                alert.setContentText(RESSOURCE_BUNDLE.getString("warning_internet_shortcut"));
                                alert.showAndWait();
                            }
                        } else if (letter.getText().equals(RESSOURCE_BUNDLE.getString("Add_folder_ink"))) {
                            DirectoryChooser directoryChooser = new DirectoryChooser();
                            directoryChooser.setTitle(RESSOURCE_BUNDLE.getString("Select_folder_ink"));
                            directoryChooser.setInitialDirectory(
                                    new File(System.getProperty("user.home"))
                            );
                            File selectedFile = directoryChooser.showDialog(getParentStage());
                            if (selectedFile != null) {
                                //TODO implement import of folder of links
                            }
                        }
                    });
                    //dialog.show();
                }
            }
        });

        HBox hbox = new HBox();
        hbox.setPadding(new Insets(15, 12, 15, 10));
        hbox.setSpacing(0);
        hbox.getChildren().addAll(addButton, settingsButton, sizeSlider);
        hbox.setAlignment(Pos.CENTER_LEFT);

        //HBox.setMargin(sizeSlider, new Insets(15, 12, 15, 12));

        wrappingPane.setTop(hbox);
    }
    private int indexOf(GameEntry entry){
        int index = 0;
        int i = 0;
        for(Node n : tilePane.getChildren()){

            if(((TileGameButton)n).getEntry().getUuid().equals(entry.getUuid())){
                index = i;
                break;
            }
            i++;
        }
        return index;
    }
    public void removeGame(GameEntry entry){
        Main.logger.debug("Removed game : "+entry.getName());
        tilePane.getChildren().remove(indexOf(entry));
    }
    public void updateGame(GameEntry entry) {
        Main.logger.debug("Updated game : "+entry.getName());
        tilePane.getChildren().set(indexOf(entry), new TileGameButton(entry, tilePane, this));
    }
    public void addGame(GameEntry entry) {
        Main.logger.debug("Added game : "+entry.getName());
        tilePane.getChildren().add(new TileGameButton(entry, tilePane, this));
        sortByName();
    }

    public void sortByName() {
        ObservableList<Node> nodes = FXCollections.observableArrayList(
                tilePane.getChildren()
        );

        nodes.sort(new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                String name1 = ((TileGameButton) o1).getEntry().getName();
                String name2 = ((TileGameButton) o2).getEntry().getName();
                return name1.compareToIgnoreCase(name2);
            }
        });
        tilePane.getChildren().setAll(nodes);
    }
    private void remapArrowKeys(ScrollPane scrollPane) {
        List<KeyEvent> mappedEvents = new ArrayList<>();
        scrollPane.addEventFilter(KeyEvent.ANY, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (mappedEvents.remove(event))
                    return;

                switch (event.getCode()) {
                    case UP:
                    case DOWN:
                    case LEFT:
                    case RIGHT:
                        setCursor(Cursor.NONE);
                        KeyEvent newEvent = remap(event);
                        mappedEvents.add(newEvent);
                        event.consume();
                        Event.fireEvent(event.getTarget(), newEvent);
                }
            }

            private KeyEvent remap(KeyEvent event) {
                KeyEvent newEvent = new KeyEvent(
                        event.getEventType(),
                        event.getCharacter(),
                        event.getText(),
                        event.getCode(),
                        !event.isShiftDown(),
                        event.isControlDown(),
                        event.isAltDown(),
                        event.isMetaDown()
                );

                return newEvent.copyFor(event.getSource(), event.getTarget());
            }
        });
    }

}

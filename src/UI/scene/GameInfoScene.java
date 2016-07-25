package ui.scene;

import javafx.geometry.HPos;
import javafx.scene.Node;
import sun.java2d.windows.GDIRenderer;
import ui.control.button.gamebutton.InfoGameButton;
import data.game.GameEntry;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Optional;

import static ui.Main.*;

/**
 * Created by LM on 03/07/2016.
 */
public class GameInfoScene extends BaseScene {
    private BorderPane wrappingPane;
    private GameEntry entry;
    private GridPane propertiesPane = new GridPane();

    private int row_count=0;

    public GameInfoScene(StackPane stackPane, Stage parentStage, BaseScene previousScene, GameEntry entry) {
        super(stackPane, parentStage);
        this.entry = entry;
        this.previousScene = previousScene;
        initTop();
        initCenter();
        initBottom();
    }
    private void initBottom(){
        HBox hBox = new HBox();
        hBox.setSpacing(30* SCREEN_WIDTH /1920);
        Button editButton = new Button(RESSOURCE_BUNDLE.getString("edit"));
        editButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                fadeTransitionTo(new GameEditScene(new StackPane(),(int) SCREEN_WIDTH,(int) SCREEN_HEIGHT,getParentStage(),GameInfoScene.this ,entry), getParentStage());
            }
        });
        Button deleteButton = new Button(RESSOURCE_BUNDLE.getString("delete"));
        deleteButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setHeaderText(null);
                alert.initStyle(StageStyle.UNDECORATED);
                alert.getDialogPane().getStylesheets().add("res/flatterfx.css");
                alert.initModality(Modality.APPLICATION_MODAL);
                alert.setContentText(RESSOURCE_BUNDLE.getString("delete_entry?"));

                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK){
                    entry.deleteFiles();
                    MAIN_SCENE.removeGame(entry);
                    fadeTransitionTo(previousScene, getParentStage());
                }
            }
        });
        hBox.getChildren().addAll(deleteButton, editButton);
        hBox.setAlignment(Pos.BOTTOM_RIGHT);
        wrappingPane.setBottom(hBox);
        BorderPane.setMargin(hBox, new Insets(10* SCREEN_WIDTH /1920,30* SCREEN_WIDTH /1920,30* SCREEN_WIDTH /1920,30* SCREEN_WIDTH /1920));

    }
    private void initTop(){
        wrappingPane.setTop(createTop(entry.getName()));
    }
    private void initCenter(){
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        propertiesPane.setVgap(20* SCREEN_WIDTH /1920);
        propertiesPane.setHgap(20* SCREEN_WIDTH /1920);

        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setPercentWidth(20);
        propertiesPane.getColumnConstraints().add(cc1);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setPercentWidth(80);
        propertiesPane.getColumnConstraints().add(cc2);

        propertiesPane.setAlignment(Pos.TOP_LEFT);
        addProperty("play_time", entry.getPlayTimeFormatted(GameEntry.TIME_FORMAT_HALF_FULL_HMS)).setStyle("-fx-font-size: 34.0px;");
        addProperty("game_path", entry.getPath());

        /***************************SEPARATORS******************************************/
        Separator s1 = new Separator();
        propertiesPane.add(s1,0,row_count);
        row_count++;
        /****************************END SEPARATORS************************************/

        addProperty("year", entry.getYear());
        addProperty("developer", entry.getDeveloper());
        addProperty("publisher", entry.getPublisher());
        addProperty("description", entry.getDescription());

        GridPane coverAndPropertiesPane = new GridPane();

        coverAndPropertiesPane.setVgap(20* SCREEN_WIDTH /1920);
        coverAndPropertiesPane.setHgap(60* SCREEN_WIDTH /1920);

        InfoGameButton button = new InfoGameButton(entry, this, wrappingPane);
        coverAndPropertiesPane.add(button,0,0);
        coverAndPropertiesPane.setPadding(new Insets(50* SCREEN_HEIGHT /1080,50* SCREEN_WIDTH /1920,20* SCREEN_HEIGHT /1080,50* SCREEN_WIDTH /1920));

        propertiesPane.setPadding(new Insets(30 * SCREEN_HEIGHT / 1080, 30 * SCREEN_WIDTH / 1920, 30 * SCREEN_HEIGHT / 1080, 30 * SCREEN_WIDTH / 1920));
        scrollPane.setContent(propertiesPane);
        coverAndPropertiesPane.add(scrollPane,1,0);

        wrappingPane.setCenter(coverAndPropertiesPane);

    }
    private Label addProperty(String title, String value){
        Label titleLabel = new Label(RESSOURCE_BUNDLE.getString(title)+" :");
        titleLabel.setAlignment(Pos.TOP_LEFT);
        titleLabel.setStyle("-fx-font-weight: lighter;");
        titleLabel.setTooltip(new Tooltip(RESSOURCE_BUNDLE.getString(title)));
        propertiesPane.add(titleLabel,0,row_count);
        Label valueLabel = new Label(value);
        if(value.equals("")){
            valueLabel.setText("-");
        }
        valueLabel.setStyle("-fx-font-weight: normal;");
        propertiesPane.add(valueLabel, 1,row_count);
        valueLabel.setWrapText(true);
        valueLabel.setId(title);
        row_count++;
        return valueLabel;
    }

    protected void updateProperty(String title, String value){
        for(Node node : propertiesPane.getChildren()){
            if(node!=null && node instanceof Label && node.getId()!=null && node.getId().equals(title)){
                ((Label)node).setText(value);
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

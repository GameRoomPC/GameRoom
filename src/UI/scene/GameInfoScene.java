package UI.scene;

import UI.Main;
import UI.button.gamebutton.InfoGameButton;
import UI.button.ImageButton;
import data.GameEntry;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Optional;

import static UI.Main.*;

/**
 * Created by LM on 03/07/2016.
 */
public class GameInfoScene extends BaseScene {
    private BorderPane wrappingPane;
    private BaseScene previousScene;
    private GameEntry entry;

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
        deleteButton.setStyle("-fx-background-color: -flatter-red;");
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
        Image leftArrowImage = new Image("res/ui/arrowLeft.png", SCREEN_WIDTH /45, SCREEN_WIDTH /45,true,true);
        ImageButton backButton = new ImageButton(leftArrowImage);
        backButton.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                fadeTransitionTo(previousScene,getParentStage());
            }
        });

        Label titleLabel = new Label(entry.getName());
        titleLabel.setScaleX(2.5);
        titleLabel.setScaleY(2.5);

        StackPane topPane = new StackPane();

        topPane.getChildren().addAll(backButton,titleLabel);
        StackPane.setAlignment(backButton, Pos.TOP_LEFT);
        StackPane.setAlignment(titleLabel,Pos.TOP_CENTER);
        StackPane.setMargin(titleLabel, new Insets(55* Main.SCREEN_HEIGHT /1080
                ,12* Main.SCREEN_WIDTH /1920
                , 15* Main.SCREEN_HEIGHT /1080
                , 15* Main.SCREEN_WIDTH /1920));
        wrappingPane.setTop(topPane);
    }
    private void initCenter(){
        ScrollPane centerPane = new ScrollPane();
        centerPane.setFitToWidth(true);
        centerPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        centerPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        GridPane propertiesPane = new GridPane();

        propertiesPane.setVgap(20* SCREEN_WIDTH /1920);
        propertiesPane.setHgap(20* SCREEN_WIDTH /1920);

        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setPercentWidth(15);
        propertiesPane.getColumnConstraints().add(cc1);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setPercentWidth(85);
        propertiesPane.getColumnConstraints().add(cc2);

        propertiesPane.setAlignment(Pos.TOP_LEFT);
        addProperty(RESSOURCE_BUNDLE.getString("play_time"), entry.getPlayTimeFormatted(false), propertiesPane).setStyle("-fx-font-size: 34.0px;");
        addProperty(RESSOURCE_BUNDLE.getString("game_path"), entry.getPath(),propertiesPane);
        addProperty(RESSOURCE_BUNDLE.getString("year"), entry.getYear(),propertiesPane);
        addProperty(RESSOURCE_BUNDLE.getString("developer"), entry.getDeveloper(),propertiesPane);
        addProperty(RESSOURCE_BUNDLE.getString("publisher"), entry.getPublisher(),propertiesPane);
        addProperty(RESSOURCE_BUNDLE.getString("description"), entry.getDescription(),propertiesPane);

        GridPane coverAndPropertiesPane = new GridPane();

        coverAndPropertiesPane.setVgap(20* SCREEN_WIDTH /1920);
        coverAndPropertiesPane.setHgap(60* SCREEN_WIDTH /1920);

        InfoGameButton button = new InfoGameButton(entry, this, wrappingPane);
        coverAndPropertiesPane.add(button,0,0);
        coverAndPropertiesPane.setPadding(new Insets(50* SCREEN_HEIGHT /1080,50* SCREEN_WIDTH /1920,20* SCREEN_HEIGHT /1080,50* SCREEN_WIDTH /1920));

        centerPane.setContent(propertiesPane);
        coverAndPropertiesPane.add(centerPane,1,0);

        wrappingPane.setCenter(coverAndPropertiesPane);

    }
    private Label addProperty(String title, String value, GridPane contentPane){
        Label titleLabel = new Label(title+" :");
        titleLabel.setAlignment(Pos.TOP_LEFT);
        titleLabel.setStyle("-fx-font-weight: lighter;");
        contentPane.add(titleLabel,0,row_count);
        Label valueLabel = new Label(value);
        if(value.equals("")){
            valueLabel.setText("-");
        }
        valueLabel.setStyle("-fx-font-weight: normal;");
        contentPane.add(valueLabel, 1,row_count);
        valueLabel.setWrapText(true);
        row_count++;
        return valueLabel;
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

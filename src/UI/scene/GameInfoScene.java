package UI.scene;

import UI.ImageButton;
import data.GameEntry;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import static UI.Main.HEIGHT;
import static UI.Main.WIDTH;

/**
 * Created by LM on 03/07/2016.
 */
public class GameInfoScene extends BaseScene {
    private BorderPane wrappingPane;
    private BaseScene previousScene;
    private GameEntry entry;

    public GameInfoScene(StackPane stackPane, int width, int height, Stage parentStage, BaseScene previousScene, GameEntry entry) {
        super(stackPane, width, height, parentStage);
        this.entry = entry;
        this.previousScene = previousScene;
        initTop();
        initLeft();
    }
    private void initLeft(){
        ImageView view = new ImageView(new Image(entry.getImagePath(0), HEIGHT*2/(3*MainScene.COVER_HEIGHT_WIDTH_RATIO), HEIGHT*2/3 , false, true));
        wrappingPane.setLeft(view);
        BorderPane.setMargin(view, new Insets(50,50,50,50));
    }
    private void initTop(){
        Image leftArrowImage = new Image("res/ui/arrowLeft.png",WIDTH/45,WIDTH/45,true,true);
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

        BorderPane topPane = new BorderPane();
        topPane.setPadding(new Insets(15, 12, 15, 10));
        topPane.setTop(backButton);
       // BorderPane.setMargin(backButton, new Insets(15,0,0,0));
        topPane.setCenter(titleLabel);
        BorderPane.setAlignment(backButton, Pos.TOP_LEFT);
        BorderPane.setAlignment(titleLabel, Pos.BOTTOM_CENTER);


        //HBox.setMargin(sizeSlider, new Insets(15, 12, 15, 12));

        wrappingPane.setTop(topPane);
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

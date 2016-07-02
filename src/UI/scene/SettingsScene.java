package UI.scene;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Created by LM on 03/07/2016.
 */
public class SettingsScene extends GameRoomScene {
    private GridPane wrappingPane;

    public SettingsScene(StackPane root,int width, int height, Stage parentStage){
        super(root, width, height, parentStage);
        getStylesheets().add("res/flatterfx.css");

        wrappingPane.add(new Button("I'm a test"), 0,0);
        wrappingPane.add(new TextField("Second test"), 1,1);
        wrappingPane.add(new CheckBox("Hello there"),2,2);
    }

    @Override
    public Pane getWrappingPane() {
        return wrappingPane;
    }

    @Override
    void initAndAddWrappingPaneToRoot() {
        wrappingPane = new GridPane();
        getRootStackPane().getChildren().add(wrappingPane);
    }
}

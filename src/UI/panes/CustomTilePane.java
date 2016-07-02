package UI.panes;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.TilePane;
import sample.Main;

/**
 * Created by LM on 02/07/2016.
 */
public class CustomTilePane extends TilePane {
    private final static int HGAP = 30;
    private final static int VGAP = 30;

    public CustomTilePane(){
        super();

        setHgap(30);
        setVgap(30);
        setPrefColumns(5);
        setPrefTileWidth(Main.WIDTH/4);
        setPrefTileHeight(getPrefTileWidth()*Main.COVER_HEIGHT_WIDTH_RATIO);

    }
    private void addKeyboardListener(){
        setOnKeyPressed(new EventHandler<KeyEvent>() {

            @Override
            public void handle(KeyEvent t) {
                if(t.getCode().equals(KeyCode.UP)) {

                }
            }
        });
    }
    public void add(Node n){
        getChildren().add(n);
    }
}

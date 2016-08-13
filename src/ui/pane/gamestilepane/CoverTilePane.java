package ui.pane.gamestilepane;

import data.game.entry.GameEntry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import ui.control.button.gamebutton.GameButton;
import ui.control.button.gamebutton.TileGameButton;
import ui.scene.BaseScene;
import ui.scene.MainScene;

import static ui.Main.SCREEN_HEIGHT;
import static ui.Main.SCREEN_WIDTH;
import static ui.control.button.gamebutton.GameButton.COVER_HEIGHT_WIDTH_RATIO;

/**
 * Created by LM on 14/08/2016.
 */
public class CoverTilePane extends GamesTilePane{
    public CoverTilePane(MainScene parentScene) {
        super(parentScene);
        tilePane.setPadding(new Insets(50 * SCREEN_HEIGHT / 1080, 30 * SCREEN_WIDTH / 1920, 30 * SCREEN_HEIGHT / 1080, 30 * SCREEN_WIDTH / 1920));
        tilePane.setHgap(50 * SCREEN_WIDTH / 1920);
        tilePane.setVgap(70 * SCREEN_HEIGHT / 1080);
        setPrefTileWidth(SCREEN_WIDTH / 4);
        setPrefTileHeight(tilePane.getPrefTileWidth() * COVER_HEIGHT_WIDTH_RATIO);
    }

    @Override
    protected Node getScrollPaneContent() {
        return tilePane;
    }

    @Override
    protected void removeTileFromTilePane(GameButton button) {
        tilePane.getChildren().remove(button);
    }

    @Override
    protected void addTileToTilePane(GameButton button) {
        tilePane.getChildren().add(button);
    }

    @Override
    protected GameButton createGameButton(GameEntry newEntry) {
        return new TileGameButton(newEntry, tilePane, parentScene);
    }

    @Override
    protected void sortByReleaseDate() {
        ObservableList<Node> nodes = FXCollections.observableArrayList(
                tilePane.getChildren()
        );
        sortByReleaseDate(nodes);
        replaceChildrensAfterSort(nodes, new Runnable() {
            @Override
            public void run() {
                for(Node button : nodes){
                    ((GameButton)button).hidePlaytime();
                    ((GameButton)button).hideRating();
                    ((GameButton)button).showReleaseDate();
                }
            }
        });
    }

    @Override
    protected void sortByRating() {
        ObservableList<Node> nodes = FXCollections.observableArrayList(
                tilePane.getChildren()
        );
        sortByRating(nodes);
        replaceChildrensAfterSort(nodes, new Runnable() {
            @Override
            public void run() {
                for(Node button : nodes){
                    ((GameButton)button).hidePlaytime();
                    ((GameButton)button).showRating();
                    ((GameButton)button).hideReleaseDate();
                }
            }
        });
    }

    @Override
    protected void sortByTimePlayed() {
        ObservableList<Node> nodes = FXCollections.observableArrayList(
                tilePane.getChildren()
        );
        sortByTimePlayed(nodes);
        replaceChildrensAfterSort(nodes, new Runnable() {
            @Override
            public void run() {
                for(Node button : nodes){
                    ((GameButton)button).showPlaytime();
                    ((GameButton)button).hideRating();
                    ((GameButton)button).hideReleaseDate();
                }
            }
        });
    }

    @Override
    protected void sortByName() {
        ObservableList<Node> nodes = FXCollections.observableArrayList(
                tilePane.getChildren()
        );
        sortByName(nodes);
        replaceChildrensAfterSort(nodes, new Runnable() {
            @Override
            public void run() {
                for(Node button : nodes){
                    ((GameButton)button).hidePlaytime();
                    ((GameButton)button).hideRating();
                }
            }
        });
    }
}

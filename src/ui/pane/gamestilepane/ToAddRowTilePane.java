package ui.pane.gamestilepane;

import data.game.entry.GameEntry;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import ui.Main;
import ui.control.button.ImageButton;
import ui.control.button.gamebutton.AddIgnoreGameButton;
import ui.control.button.gamebutton.GameButton;
import ui.scene.MainScene;

import java.util.ArrayList;

import static ui.Main.SCREEN_WIDTH;

/**
 * Created by LM on 17/08/2016.
 */
public abstract class ToAddRowTilePane extends RowCoverTilePane {
    public ToAddRowTilePane(MainScene parentScene) {
        super(parentScene, RowCoverTilePane.TYPE_RECENTLY_ADDED);
        setTitle(Main.RESSOURCE_BUNDLE.getString("to_add"));
        maxColumn = Integer.MAX_VALUE;
        automaticSort=false;

        ImageButton addAllButton = new ImageButton(new Image("res/ui/doubleValidIcon.png",SCREEN_WIDTH / 65,SCREEN_WIDTH / 65,true,true));
        addAllButton.setOnAction(event -> {
            ArrayList<GameEntry> entries = new ArrayList<GameEntry>();
            for(GameButton b : tilesList){
                entries.add(b.getEntry());
            }
            batchAddEntries(entries);
        });
        addAllButton.setFocusTraversable(false);
        addAllButton.setTooltip(new Tooltip(Main.RESSOURCE_BUNDLE.getString("add_all_games")));
        buttonsBox.getChildren().add(addAllButton);
    }
    protected abstract void batchAddEntries(ArrayList<GameEntry> entries);
    @Override
    protected GameButton createGameButton(GameEntry newEntry) {
        return new AddIgnoreGameButton(newEntry, parentScene, tilePane,this);

    }
    public GameButton getGameButton(GameEntry entry){
        if(indexOfTile(entry)!=-1){
            return getGameButtons().get(indexOfTile(entry));
        }
        return null;
    }

}

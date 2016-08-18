package ui.pane.gamestilepane;

import data.game.entry.GameEntry;
import ui.control.button.gamebutton.GameButton;
import ui.scene.MainScene;

/**
 * Created by LM on 18/08/2016.
 */
public abstract class GroupRowTilePane extends RowCoverTilePane {

    public GroupRowTilePane(MainScene parentScene) {
        super(parentScene, TYPE_NAME);
        maxColumn = Integer.MAX_VALUE;
    }
    @Override
    protected void addTile(GameButton button){
        if(fillsRequirement(button.getEntry())) {
            addTileToTilePane(button);
            tilesList.add(button);
        }
    }
    public abstract boolean fillsRequirement(GameEntry entry);
}

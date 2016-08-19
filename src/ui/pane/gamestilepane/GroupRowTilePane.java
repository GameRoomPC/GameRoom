package ui.pane.gamestilepane;

import data.game.entry.GameEntry;
import ui.control.button.gamebutton.GameButton;
import ui.scene.MainScene;

import java.util.ArrayList;

/**
 * Created by LM on 18/08/2016.
 */
public abstract class GroupRowTilePane extends RowCoverTilePane {
    private boolean autoAddMatchingEntries = true;

    public GroupRowTilePane(MainScene parentScene) {
        super(parentScene, TYPE_NAME);
        maxColumn = Integer.MAX_VALUE;
    }
    @Override
    public void addGame(GameEntry game){
        if(indexOfTile(game) == -1 && fillsRequirement(game)) {
            addTile(createGameButton(game));
            if (automaticSort)
                sort();
        }
    }
    @Override
    public void updateGame(GameEntry newEntry){
        int index=indexOfTile(newEntry);
        if(index!=-1){
            if(fillsRequirement(newEntry)){
                tilesList.get(index).reloadWith(newEntry);
                tilesList.set(index,tilesList.get(index));//to fire updated/replaced event
            }else{
                removeGame(newEntry);
            }
        }else{
            if(autoAddMatchingEntries && fillsRequirement(newEntry)){
                addGame(newEntry);
            }
        }
        if(automaticSort)
            sort();
    }
    public abstract boolean fillsRequirement(GameEntry entry);

    public boolean isAutoAddMatchingEntries() {
        return autoAddMatchingEntries;
    }

    public void setAutoAddMatchingEntries(boolean autoAddMatchingEntries) {
        this.autoAddMatchingEntries = autoAddMatchingEntries;
    }
}

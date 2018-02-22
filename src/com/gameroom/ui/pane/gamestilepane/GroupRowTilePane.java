package com.gameroom.ui.pane.gamestilepane;

import com.gameroom.data.game.entry.GameEntry;
import com.gameroom.ui.scene.MainScene;

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
    public boolean isValidToAdd(GameEntry entry) {
        return fillsRequirement(entry);
    }
    @Override
    protected void onNotFoundForUpdate(GameEntry newEntry){
        if(autoAddMatchingEntries && fillsRequirement(newEntry)){
            addGame(newEntry);
        }
    }
    public abstract boolean fillsRequirement(GameEntry entry);

    public boolean isAutoAddMatchingEntries() {
        return autoAddMatchingEntries;
    }

    public void setAutoAddMatchingEntries(boolean autoAddMatchingEntries) {
        this.autoAddMatchingEntries = autoAddMatchingEntries;
    }
}

package ui.pane.gamestilepane;

import data.game.entry.GameEntry;
import ui.Main;
import ui.control.button.gamebutton.AddIgnoreGameButton;
import ui.control.button.gamebutton.GameButton;
import ui.scene.MainScene;

/**
 * Created by LM on 17/08/2016.
 */
public class ToAddRowTilePane extends RowCoverTilePane {
    public ToAddRowTilePane(MainScene parentScene) {
        super(parentScene, RowCoverTilePane.TYPE_NAME);
        setTitle(Main.RESSOURCE_BUNDLE.getString("to_add"));
        maxColumn = Integer.MAX_VALUE;
    }
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

    @Override
    public void sort() {

    }
}

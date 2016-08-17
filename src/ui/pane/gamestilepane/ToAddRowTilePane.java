package ui.pane.gamestilepane;

import data.game.entry.GameEntry;
import ui.control.button.gamebutton.GameButton;
import ui.control.button.gamebutton.TileGameButton;
import ui.scene.MainScene;

/**
 * Created by LM on 17/08/2016.
 */
public class ToAddRowTilePane extends RowCoverTilePane {
    public ToAddRowTilePane(MainScene parentScene) {
        super(parentScene, RowCoverTilePane.TYPE_RECENTLY_ADDED);
        setTitle("To add");
        maxColumn = Integer.MAX_VALUE;
    }
    @Override
    protected GameButton createGameButton(GameEntry newEntry) {
        //TODO replace by a gameButton with and add and IGNORE BUTTON instead
        return new TileGameButton(newEntry, tilePane, parentScene);

    }
}

package data.game.scanner;

import data.game.entry.GameEntry;
import ui.control.button.gamebutton.GameButton;

import java.util.Collection;

/**
 * Created by LM on 17/08/2016.
 */
public interface OnScannerResultHandler {

    GameButton gameToAddFound(GameEntry entry);

    void onAllGamesFound(int gamesCount);
}

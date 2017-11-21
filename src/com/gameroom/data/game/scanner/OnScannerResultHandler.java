package com.gameroom.data.game.scanner;

import com.gameroom.data.game.entry.GameEntry;
import com.gameroom.ui.control.button.gamebutton.GameButton;

/**
 * Created by LM on 17/08/2016.
 */
public interface OnScannerResultHandler {

    GameButton gameToAddFound(GameEntry entry);

    void onAllGamesFound(int gamesCount);
}

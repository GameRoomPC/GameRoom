package com.gameroom.data.game.scanner;

import com.gameroom.data.game.entry.GameEntry;

/**
 * Created by LM on 06/01/2017.
 */
public interface OnGameFound {
    public void handle(GameEntry entry);
}

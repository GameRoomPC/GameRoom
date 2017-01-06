package data.game.scanner;

import data.game.entry.GameEntry;

/**
 * Created by LM on 06/01/2017.
 */
public interface OnGameFound {
    public void handle(GameEntry entry);
}

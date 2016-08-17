package data.game;

import data.game.entry.GameEntry;

/**
 * Created by LM on 17/08/2016.
 */
public interface OnGameFoundHandler {

    public void gameToAddFound(GameEntry entry);

    public void onAllGamesFound();
}

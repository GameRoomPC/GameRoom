package data.game.scanner;

import data.game.GameWatcher;
import data.game.entry.GameEntry;

import java.util.ArrayList;

/**
 * Created by LM on 29/08/2016.
 */
public abstract class OtherLaunchersScanner extends FolderGameScanner {
    public OtherLaunchersScanner(GameWatcher parentLooker) {
        super(parentLooker);
    }
    public abstract ArrayList<GameEntry> getEntriesInstalled();

    @Override
    protected ArrayList<GameEntry> getPotentialEntries(){
        return getEntriesInstalled();
    }
}

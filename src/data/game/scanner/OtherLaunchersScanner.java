package data.game.scanner;

import data.game.GameWatcher;
import data.game.entry.GameEntry;

import java.util.ArrayList;

import static ui.Main.LOGGER;

/**
 * Created by LM on 29/08/2016.
 */
public abstract class OtherLaunchersScanner extends FolderGameScanner {
    private ScannerProfile profile;

    public OtherLaunchersScanner(GameWatcher parentLooker, ScannerProfile profile) {
        super(parentLooker);
        this.profile = profile;
    }
    public abstract ArrayList<GameEntry> getEntriesInstalled();

    @Override
    protected ArrayList<GameEntry> getPotentialEntries(){
        if(!profile.isEnabled()){
            LOGGER.info(profile.toString()+" is disabled.");
            return new ArrayList<>();
        }
        return getEntriesInstalled();
    }
}

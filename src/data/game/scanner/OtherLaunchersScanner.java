package data.game.scanner;

import data.game.GameWatcher;
import data.game.entry.GameEntry;
import ui.Main;
import ui.control.specific.GeneralToast;
import ui.scene.MainScene;

import java.util.ArrayList;

import static ui.Main.LOGGER;
import static ui.Main.MAIN_SCENE;

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
        if(MAIN_SCENE!=null){
            GeneralToast.displayToast(Main.getString("scanning")+" "+profile.toString(),MAIN_SCENE.getParentStage(),GeneralToast.DURATION_SHORT,true);
        }

        return getEntriesInstalled();
    }
}

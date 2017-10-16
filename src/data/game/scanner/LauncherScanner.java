package data.game.scanner;

import data.game.GameWatcher;
import data.game.scraper.LauncherGameScraper;

import java.util.Collection;
import java.util.concurrent.Callable;

/**
 * Created by LM on 29/08/2016.
 */
public class LauncherScanner extends FolderGameScanner {

    public LauncherScanner(GameWatcher parentLooker, ScannerProfile profile) {
        super(parentLooker);
        this.profile = profile;
    }

    @Override
    public void scanAndAddGames() {
        LauncherGameScraper.scanInstalledGames(this);
    }

    @Override
    public String getScannerName(){
        return profile.name()+" scanner";
    }

}

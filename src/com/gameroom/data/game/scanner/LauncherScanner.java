package com.gameroom.data.game.scanner;

import com.gameroom.data.game.GameWatcher;
import com.gameroom.data.game.scraper.LauncherGameScraper;

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

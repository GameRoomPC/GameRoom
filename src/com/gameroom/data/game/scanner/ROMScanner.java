package com.gameroom.data.game.scanner;

import com.gameroom.data.game.GameWatcher;
import com.gameroom.data.game.entry.GameEntry;
import com.gameroom.data.game.entry.Platform;
import com.gameroom.data.http.key.KeyChecker;
import com.gameroom.data.io.FileUtils;
import com.gameroom.ui.GeneralToast;
import com.gameroom.ui.Main;

import java.io.File;

import static com.gameroom.data.game.GameWatcher.cleanNameForDisplay;
import static com.gameroom.ui.Main.MAIN_SCENE;

/**
 * @author LM. Garret (admin@gameroom.me)
 * @date 20/07/2017.
 */
public class ROMScanner extends FolderGameScanner {
    public ROMScanner(GameWatcher parentLooker) {
        super(parentLooker);
    }

    @Override
    public void scanAndAddGames() {
        if (!KeyChecker.assumeSupporterMode()) {
            return;
        }
        Platform.getNonPCPlatforms().forEach(platform -> {
            if (platform.getROMFolder() == null || platform.getROMFolder().isEmpty()) {
                return;
            }
            File ROMFolder = new File(platform.getROMFolder());
            if (!ROMFolder.exists() || !ROMFolder.isDirectory()) {
                return;
            }
            File[] children = ROMFolder.listFiles();
            if (children == null) {
                return;
            }
            for (File f : children) {
                ScanTask task = new ScanTask(this, () -> {
                    File file = FileUtils.tryResolveLnk(f);
                    GameEntry potentialEntry = new GameEntry(cleanNameForDisplay(
                            f.getName(),
                            platform.getSupportedExtensions()
                    )); //f because we prefer to use the .lnk name if its the case !

                    potentialEntry.setPath(file.getAbsolutePath());
                    if (checkValidToAdd(potentialEntry)) {
                        if (isPotentiallyAGame(file, platform.getSupportedExtensions())) {
                            potentialEntry.setInstalled(true);
                            potentialEntry.setPlatform(platform);
                            addGameEntryFound(potentialEntry);
                        }
                    }
                    return null;
                });
                GameWatcher.getInstance().submitTask(task);
            }
        });
    }

    @Override
    protected void displayStartToast() {
        if (MAIN_SCENE != null) {
            GeneralToast.displayToast(Main.getString("scanning") + " " + Main.getString("rom_folders"), MAIN_SCENE.getParentStage(), GeneralToast.DURATION_SHORT);
        }
    }

    @Override
    public String getScannerName(){
        return "ROMs scanner";
    }
}

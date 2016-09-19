package data.game.scanner;

import data.game.GameWatcher;
import data.game.entry.AllGameEntries;
import data.game.entry.GameEntry;
import data.game.scrapper.SteamLocalScrapper;
import data.game.scrapper.SteamOnlineScrapper;
import data.game.scrapper.SteamPreEntry;
import javafx.application.Platform;
import ui.Main;

import java.io.IOException;
import java.util.ArrayList;

import static ui.Main.GENERAL_SETTINGS;
import static ui.Main.MAIN_SCENE;

/**
 * Created by LM on 19/08/2016.
 */
public class SteamOnlineGameScanner extends GameScanner {
    private ArrayList<SteamPreEntry> ownedSteamApps = new ArrayList<>();
    private ArrayList<SteamPreEntry> installedSteamApps = new ArrayList<>();

    public SteamOnlineGameScanner(GameWatcher parentLooker) {
        super(parentLooker);
    }

    @Override
    public void scanForGames() {
        ArrayList<SteamPreEntry> steamEntriesToAdd = new ArrayList<SteamPreEntry>();
        try {
            initGameLists();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (SteamPreEntry steamEntry : ownedSteamApps) {
            if (!steamGameAlreadyInLibrary(steamEntry) && !steamGameIgnored(steamEntry) && !parentLooker.alreadyWaitingToBeAdded(steamEntry)) {
                steamEntriesToAdd.add(steamEntry);
            }
        }
        if (steamEntriesToAdd.size() != 0) {
            for (SteamPreEntry preEntryToAdd : steamEntriesToAdd) {
                GameEntry entryToAdd = automaticSteamScrap(preEntryToAdd);
                if (!parentLooker.alreadyWaitingToBeAdded(entryToAdd)) {
                    addGameEntryFound(entryToAdd);
                }
            }
        }
        if (foundGames.size() > 0) {
            Main.LOGGER.info(SteamOnlineGameScanner.class.getName() + " : total games found = " + foundGames.size());
        }
    }

    private GameEntry automaticSteamScrap(SteamPreEntry steamEntryFound) {
        GameEntry convertedEntry = new GameEntry(steamEntryFound.getName());
        convertedEntry.setSteam_id(steamEntryFound.getId());

        GameEntry fetchedEntry = null;
        try {
            fetchedEntry = SteamOnlineScrapper.getEntryForSteamId(steamEntryFound.getId(), installedSteamApps);
        } catch (Exception e) {
            if (e.toString().contains("UnknownHostException")) {
                Main.LOGGER.error("Could not connect to steam, toAdd entry will not be scrapped");
            }
        }
        return fetchedEntry != null ? fetchedEntry : convertedEntry;
    }

    private void initGameLists() throws IOException {
        ownedSteamApps.clear();
        ownedSteamApps.addAll(SteamOnlineScrapper.getOwnedSteamGamesPreEntry());
        installedSteamApps.clear();
        installedSteamApps.addAll(SteamLocalScrapper.getSteamAppsInstalledPreEntries());
        foundGames.clear();
    }

    private boolean steamGameIgnored(SteamPreEntry steamEntry) {
        boolean ignored = false;
        SteamPreEntry[] ignoredSteamApps = GENERAL_SETTINGS.getSteamAppsIgnored();
        for (SteamPreEntry ignoredEntry : ignoredSteamApps) {
            ignored = steamEntry.getId() == ignoredEntry.getId();
            if (ignored) {
                break;
            }
        }
        return ignored;
    }

    private boolean steamGameAlreadyInLibrary(SteamPreEntry steamEntry) {
        boolean alreadyAddedToLibrary = false;
        for (GameEntry entry : AllGameEntries.ENTRIES_LIST) {
            alreadyAddedToLibrary = steamEntry.getId() == entry.getSteam_id();
            if (alreadyAddedToLibrary) {
                for (SteamPreEntry installedEntry : installedSteamApps) {
                    if (installedEntry.getId() == entry.getSteam_id()) {
                        //games is installed!
                        if (entry.isNotInstalled()) {
                            entry.setNotInstalled(false);
                            Platform.runLater(() -> {
                                MAIN_SCENE.updateGame(entry);
                            });
                            break;
                        }
                    }
                }
                break;
            }
        }
        return alreadyAddedToLibrary;
    }
}

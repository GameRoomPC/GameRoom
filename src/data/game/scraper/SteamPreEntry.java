package data.game.scraper;

import data.game.entry.GameEntry;
import data.game.scanner.FolderGameScanner;
import ui.Main;

import java.util.Collection;

/**
 * Created by LM on 10/08/2016.
 */
public class SteamPreEntry {
    private String name;
    private int id;

    public SteamPreEntry(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public GameEntry toGameEntry() {
        GameEntry entry = new GameEntry(getName());
        entry.setSteam_id(getId());
        return entry;
    }

}

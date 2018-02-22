package com.gameroom.data.game.scraper;

import com.gameroom.data.game.entry.GameEntry;
import com.gameroom.data.game.entry.Platform;

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
        entry.setPlatform(Platform.STEAM_ID);
        entry.setPlatformGameId(getId());
        return entry;
    }

}

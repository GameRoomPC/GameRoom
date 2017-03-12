package data.game.scanner;

import data.game.entry.Platform;
import ui.Main;

/**
 * Created by LM on 04/01/2017.
 */
public enum ScannerProfile {
    STEAM(Platform.STEAM_ID, "steam_scanner_name", "steam-icon")
    , STEAM_ONLINE(Platform.STEAM_ONLINE_ID, "steam_online_scanner_name", "steam-icon")
    , GOG(Platform.GOG_ID,"gog_scanner_name","gog-icon")
    , ORIGIN(Platform.ORIGIN_ID,"origin_scanner_name","origin-icon")
    , UPLAY(Platform.UPLAY_ID,"uplay_scanner_name","uplay-icon")
    , BATTLE_NET(Platform.BATTLENET_ID,"battle-net_scanner_name","battlenet-icon");

    private int platformId;
    private String stringKey;
    private String iconCSSID;

    ScannerProfile(int platformId, String stringKey, String iconCSSId) {
        this.platformId = platformId;
        this.stringKey = stringKey;
        this.iconCSSID = iconCSSId;
    }

    @Override
    public String toString(){
        return Main.getString(stringKey);
    }

    public int getPlatformId() {
        return platformId;
    }

    public String getIconCSSID() {
        return iconCSSID;
    }

    public boolean isEnabled(){
        return Main.GENERAL_SETTINGS.isGameScannerEnabled(this);
    }
}

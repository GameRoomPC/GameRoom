package com.gameroom.data.game.scanner;

import com.gameroom.data.game.entry.Platform;
import com.gameroom.ui.Main;

import static com.gameroom.system.application.settings.GeneralSettings.settings;

/**
 * Created by LM on 04/01/2017.
 */
public enum ScannerProfile {
    STEAM(Platform.STEAM_ID, "steam_scanner_name")
    , STEAM_ONLINE(Platform.STEAM_ONLINE_ID, "steam_online_scanner_name")
    , GOG(Platform.GOG_ID,"gog_scanner_name")
    , ORIGIN(Platform.ORIGIN_ID,"origin_scanner_name")
    , UPLAY(Platform.UPLAY_ID,"uplay_scanner_name")
    , BATTLE_NET(Platform.BATTLENET_ID,"battle-net_scanner_name")
    , MICROSOFT_STORE(Platform.MICROSOFT_STORE_ID,"microsoft_store_scanner_name");

    private int platformId;
    private String stringKey;

    ScannerProfile(int platformId, String stringKey) {
        this.platformId = platformId;
        this.stringKey = stringKey;
    }

    @Override
    public String toString(){
        return Main.getString(stringKey);
    }

    public int getPlatformId() {
        return platformId;
    }

    public boolean isEnabled(){
        return settings().isGameScannerEnabled(this);
    }
}

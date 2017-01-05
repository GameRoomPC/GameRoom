package data.game.scanner;

import ui.Main;

/**
 * Created by LM on 04/01/2017.
 */
public enum ScannerProfile {
    STEAM(1, "steam_scanner_name", "steam-icon")
    , STEAM_ONLINE(2, "steam_online_scanner_name", "steam-icon")
    , GOG(3,"gog_scanner_name","gog-icon")
    , ORIGIN(4,"origin_scanner_name","origin-icon")
    , UPLAY(5,"uplay_scanner_name","uplay-icon")
    , BATTLE_NET(6,"battle-net_scanner_name","battlenet-icon");

    private int code;
    private String stringKey;
    private String iconCSSID;

    ScannerProfile(int code, String stringKey, String iconCSSId) {
        this.code = code;
        this.stringKey = stringKey;
        this.iconCSSID = iconCSSId;
    }

    @Override
    public String toString(){
        return Main.getString(stringKey);
    }

    public int getCode() {
        return code;
    }

    public String getIconCSSID() {
        return iconCSSID;
    }

    public boolean isEnabled(){
        return Main.GENERAL_SETTINGS.isGameScannerEnabled(this);
    }
}

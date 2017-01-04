package data.game.scanner;

import ui.Main;

/**
 * Created by LM on 04/01/2017.
 */
public enum ScannerProfile {
    STEAM(1, "steam", "steam-icon")
    , STEAM_ONLINE(2, "steam_online", "steam-icon")
    , GOG(3,"gog","gog-icon")
    , ORIGIN(4,"origin","origin-icon")
    , UPLAY(5,"uplay","uplay-icon")
    , BATTLE_NET(6,"battle-net","battlenet-icon");

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
        return Main.RESSOURCE_BUNDLE.getString(stringKey);
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

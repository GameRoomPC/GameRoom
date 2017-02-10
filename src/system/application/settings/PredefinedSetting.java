package system.application.settings;

import data.game.scanner.ScanPeriod;
import data.game.scanner.ScannerProfile;
import data.game.scraper.SteamPreEntry;
import system.application.OnLaunchAction;
import system.os.PowerMode;
import ui.Main;
import ui.theme.Theme;
import ui.theme.UIScale;

import java.io.File;
import java.util.Date;
import java.util.Locale;

import static system.application.settings.SettingValue.*;

/**
 * Created by LM on 08/08/2016.
 */
public enum PredefinedSetting {
    LOCALE("locale", new SettingValue(Locale.getDefault(),Locale.class,CATEGORY_GENERAL))
    ,TILE_ZOOM("tileZoom", new SettingValue(0.365, Double.class, CATEGORY_NONE))
    ,ON_GAME_LAUNCH_ACTION("onGameLaunchAction", new SettingValue<OnLaunchAction>(OnLaunchAction.DO_NOTHING,OnLaunchAction.class,CATEGORY_ON_GAME_START))
    ,FULL_SCREEN("fullScreen", new SettingValue(false,Boolean.class,CATEGORY_NONE))
    ,WINDOW_WIDTH("windowWidth", new SettingValue(1366,Integer.class,CATEGORY_NONE))
    ,WINDOW_HEIGHT("windowHeight", new SettingValue(768,Integer.class,CATEGORY_NONE))
    ,GAMING_POWER_MODE("gamingPowerMode", new SettingValue(PowerMode.getActivePowerMode(),PowerMode.class,CATEGORY_ON_GAME_START))
    ,ENABLE_GAMING_POWER_MODE("enableGamingPowerMode", new SettingValue(false,Boolean.class,CATEGORY_ON_GAME_START))
    , NO_NOTIFICATIONS("noNotifications", new SettingValue(false,Boolean.class, CATEGORY_UI))
    , NO_TOASTS("noToasts", new SettingValue(false,Boolean.class, CATEGORY_UI))
    ,START_MINIMIZED("startMinimized", new SettingValue(false,Boolean.class,CATEGORY_GENERAL))
    ,WINDOW_MAXIMIZED("windowMaximized", new SettingValue(true,Boolean.class,CATEGORY_NONE))
    ,HIDE_TOOLBAR("hideToolBar", new SettingValue(false,Boolean.class,CATEGORY_NONE))
    ,HIDE_TILES_ROWS("hideTilesRows", new SettingValue(false,Boolean.class,CATEGORY_NONE))
    ,START_WITH_WINDOWS("startWithWindows", new SettingValue(false,Boolean.class,CATEGORY_GENERAL))
    ,NO_MORE_ICON_TRAY_WARNING("noMoreIconTrayWarning", new SettingValue(false,Boolean.class,CATEGORY_NONE))
    ,ENABLE_XBOX_CONTROLLER_SUPPORT("enableXboxControllerSupport", new SettingValue(false,Boolean.class,CATEGORY_UI))
    ,GAMES_FOLDER("gamesFolder", new SettingValue("",String.class,CATEGORY_GENERAL))
    , SUPPORTER_KEY("supporterKey", new SettingValue("",String.class,CATEGORY_GENERAL))
    ,DISABLE_MAINSCENE_WALLPAPER("disableMainSceneWallpaper", new SettingValue(false,Boolean.class,CATEGORY_UI))
    ,DISABLE_SCROLLBAR_IN_FULLSCREEN("disableScrollbarFullScreen", new SettingValue(true,Boolean.class,CATEGORY_UI))
    ,IGNORED_STEAM_APPS("ignoredSteamApps",new SettingValue(new SteamPreEntry[]{},SteamPreEntry[].class,CATEGORY_GENERAL))
    ,IGNORED_GAME_FOLDERS("ignoredGameFolders",new SettingValue(new File[]{},File[].class,CATEGORY_GENERAL))
    , SCAN_PERIOD("scanPeriod", new SettingValue(ScanPeriod.HALF_HOUR,ScanPeriod.class,CATEGORY_GENERAL))
    , DISABLE_GAME_MAIN_THEME("disableGameMainTheme", new SettingValue(true,Boolean.class,CATEGORY_UI))
    , ADVANCED_MODE("advancedMode", new SettingValue(false,Boolean.class,CATEGORY_GENERAL))
    , DEBUG_MODE("debugMode", new SettingValue(false,Boolean.class,CATEGORY_GENERAL))
    , FOLDED_ROW_LAST_PLAYED("foldedRowLastPlay", new SettingValue(false,Boolean.class,CATEGORY_UI))
    , FOLDED_ROW_RECENTLY_ADDED("foldedRowRecentlyAdded", new SettingValue(false,Boolean.class,CATEGORY_UI))
    , FOLDED_TOADD_ROW("foldedToAddRow", new SettingValue(false,Boolean.class,CATEGORY_UI))
    ,CMD("cmd", new SettingValue(new String[]{"",""},String[].class,CATEGORY_ON_GAME_START))
    , DISPLAY_WELCOME_MESSAGE("displayWelcomeMessage", new SettingValue(true,Boolean.class,CATEGORY_NONE))
    , SCROLLBAR_VVALUE("scrollbarVValue", new SettingValue(0.0,Double.class,CATEGORY_NONE))
    , UI_SCALE("uiscale", new SettingValue(UIScale.NORMAL,UIScale.class,CATEGORY_UI))
    , THEME("theme", new SettingValue(Theme.DEFAULT_THEME,Theme.class,CATEGORY_UI))
    , ENABLED_GAME_SCANNERS("enabledGameScanners",new SettingValue(ScannerProfile.values(),ScannerProfile[].class,CATEGORY_GENERAL))
    , LAST_SUPPORT_MESSAGE("lastSupportMessage", new SettingValue(new Date(),Date.class,CATEGORY_NONE))
    , LAST_UPDATE_CHECK("lastUpdateCheck", new SettingValue(new Date(),Date.class,CATEGORY_NONE))
    ;


    private String key;
    private SettingValue defaultValue;

    PredefinedSetting(String key, SettingValue setting){
        this.key = key;
        this.defaultValue = setting;
    }
    public String getLabel(){
        String label = Main.getSettingsString(key +"_label");
        if(label == null){
            throw new NullPointerException("Forgot to set label for key"+key);
        }
        return label!=null ? label : key;
    }

    public String getTooltip(){
        String tooltip = Main.getSettingsString(key +"_tooltip");
        return tooltip!=null ? tooltip : getLabel();
    }

    public String getKey() {
        return key;
    }

    public void setDefaultValue(SettingValue defaultValue) {
        this.defaultValue = defaultValue;
    }

    public SettingValue getDefaultValue() {
        return defaultValue;
    }
    public boolean isClass(Class c){
        return c.equals(defaultValue.getValueClass());
    }

    @Override
    public String toString(){
        return key;
    }

    public String getCategory() {
        return defaultValue.getCategory();
    }
}

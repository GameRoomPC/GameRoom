package data.migration;

import data.game.scanner.ScanPeriod;
import data.game.scanner.ScannerProfile;
import data.game.scraper.SteamPreEntry;
import data.game.scraper.SteamProfile;
import javafx.beans.property.SimpleBooleanProperty;
import system.application.OnLaunchAction;
import system.os.PowerMode;
import ui.Main;
import ui.control.drawer.DrawerMenu;
import ui.theme.Theme;
import ui.theme.UIScale;

import java.io.File;
import java.util.Date;
import java.util.Locale;

import static system.application.settings.SettingValue.*;

/**
 * Created by LM on 08/08/2016.
 */
enum OldPredefinedSetting {
    LOCALE("locale", new OldSettingValue(Locale.getDefault(),Locale.class,CATEGORY_GENERAL))
    ,TILE_ZOOM("tileZoom", new OldSettingValue(0.365, Double.class, CATEGORY_NONE))
    ,ON_GAME_LAUNCH_ACTION("onGameLaunchAction", new OldSettingValue<OnLaunchAction>(OnLaunchAction.DO_NOTHING,OnLaunchAction.class,CATEGORY_ON_GAME_START))
    ,FULL_SCREEN("fullScreen", new OldSettingValue(new SimpleBooleanProperty(false),Boolean.class,CATEGORY_NONE))
    ,WINDOW_WIDTH("windowWidth", new OldSettingValue(1366,Integer.class,CATEGORY_NONE))
    ,WINDOW_HEIGHT("windowHeight", new OldSettingValue(768,Integer.class,CATEGORY_NONE))
    ,GAMING_POWER_MODE("gamingPowerMode", new OldSettingValue(PowerMode.getActivePowerMode(),PowerMode.class,CATEGORY_ON_GAME_START))
    ,ENABLE_GAMING_POWER_MODE("enableGamingPowerMode", new OldSettingValue(false,Boolean.class,CATEGORY_ON_GAME_START))
    , NO_NOTIFICATIONS("noNotifications", new OldSettingValue(false,Boolean.class, CATEGORY_UI))
    , NO_TOASTS("noToasts", new OldSettingValue(false,Boolean.class, CATEGORY_UI))
    ,START_MINIMIZED("startMinimized", new OldSettingValue(false,Boolean.class,CATEGORY_GENERAL))
    ,WINDOW_MAXIMIZED("windowMaximized", new OldSettingValue(true,Boolean.class,CATEGORY_NONE))
    ,HIDE_TOOLBAR("hideToolBar", new OldSettingValue(false,Boolean.class,CATEGORY_NONE))
    ,HIDE_TILES_ROWS("hideTilesRows", new OldSettingValue(false,Boolean.class,CATEGORY_NONE))
    ,ENABLE_STATIC_WALLPAPER("enableStaticWallpaper", new OldSettingValue(false,Boolean.class,CATEGORY_NONE))
    ,START_WITH_WINDOWS("startWithWindows", new OldSettingValue(false,Boolean.class,CATEGORY_GENERAL))
    ,NO_MORE_ICON_TRAY_WARNING("noMoreIconTrayWarning", new OldSettingValue(false,Boolean.class,CATEGORY_NONE))
    , ENABLE_GAME_CONTROLLER_SUPPORT("enableXboxControllerSupport", new OldSettingValue(false,Boolean.class,CATEGORY_UI))
    ,GAMES_FOLDER("gamesFolder", new OldSettingValue("",String.class,CATEGORY_SCAN))
    , SUPPORTER_KEY("supporterKey", new OldSettingValue("",String.class,CATEGORY_GENERAL))
    ,DISABLE_MAINSCENE_WALLPAPER("disableMainSceneWallpaper", new OldSettingValue(false,Boolean.class,CATEGORY_NONE))
    ,DISABLE_SCROLLBAR_IN_FULLSCREEN("disableScrollbarFullScreen", new OldSettingValue(true,Boolean.class,CATEGORY_UI))
    , STEAM_PROFILE("steamProfile", new OldSettingValue(null, SteamProfile.class,CATEGORY_SCAN))
    ,IGNORED_STEAM_APPS("ignoredSteamApps",new OldSettingValue(new SteamPreEntry[]{},SteamPreEntry[].class,CATEGORY_SCAN))
    ,IGNORED_GAME_FOLDERS("ignoredGameFolders",new OldSettingValue(new File[]{},File[].class,CATEGORY_SCAN)), SCAN_PERIOD("scanPeriod", new OldSettingValue(ScanPeriod.HALF_HOUR,ScanPeriod.class,CATEGORY_SCAN))
    , DISABLE_GAME_MAIN_THEME("disableGameMainTheme", new OldSettingValue(true,Boolean.class,CATEGORY_UI))
    , ADVANCED_MODE("advancedMode", new OldSettingValue(false,Boolean.class,CATEGORY_GENERAL))
    , DEBUG_MODE("debugMode", new OldSettingValue(false,Boolean.class,CATEGORY_GENERAL))
    , FOLDED_ROW_LAST_PLAYED("foldedRowLastPlay", new OldSettingValue(false,Boolean.class,CATEGORY_UI))
    , FOLDED_ROW_RECENTLY_ADDED("foldedRowRecentlyAdded", new OldSettingValue(false,Boolean.class,CATEGORY_UI))
    , FOLDED_TOADD_ROW("foldedToAddRow", new OldSettingValue(false,Boolean.class,CATEGORY_UI))
    ,CMD("cmd", new OldSettingValue(new String[]{"",""},String[].class,CATEGORY_ON_GAME_START))
    , DISPLAY_WELCOME_MESSAGE("displayWelcomeMessage", new OldSettingValue(true,Boolean.class,CATEGORY_NONE))
    , SCROLLBAR_VVALUE("scrollbarVValue", new OldSettingValue(0.0,Double.class,CATEGORY_NONE))
    , UI_SCALE("uiscale", new OldSettingValue(UIScale.NORMAL,UIScale.class,CATEGORY_UI))
    , THEME("theme", new OldSettingValue(Theme.DEFAULT_THEME,Theme.class,CATEGORY_UI))
    , ENABLED_GAME_SCANNERS("enabledGameScanners",new OldSettingValue(ScannerProfile.values(),ScannerProfile[].class,CATEGORY_SCAN))
    , LAST_SUPPORT_MESSAGE("lastSupportMessage", new OldSettingValue(new Date(),Date.class,CATEGORY_NONE))
    , LAST_UPDATE_CHECK("lastUpdateCheck", new OldSettingValue(new Date(),Date.class,CATEGORY_NONE))
    , DRAWER_MENU_WIDTH("drawerMenuWidth", new OldSettingValue(0,Double.class,CATEGORY_NONE))
    ;


    private String key;
    private OldSettingValue defaultValue;

    OldPredefinedSetting(String key, OldSettingValue setting){
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

    public OldSettingValue getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String toString(){
        return key;
    }
}

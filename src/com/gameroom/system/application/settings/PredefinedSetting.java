package com.gameroom.system.application.settings;

import com.gameroom.data.game.scanner.ScanPeriod;
import com.gameroom.data.game.scanner.ScannerProfile;
import com.gameroom.data.game.scraper.SteamPreEntry;
import com.gameroom.data.game.scraper.SteamProfile;
import com.gameroom.ui.control.drawer.GroupType;
import com.gameroom.ui.control.drawer.SortType;
import javafx.beans.property.SimpleBooleanProperty;
import com.gameroom.system.application.OnLaunchAction;
import com.gameroom.system.os.PowerMode;
import com.gameroom.ui.Main;
import com.gameroom.ui.theme.Theme;
import com.gameroom.ui.theme.UIScale;

import java.io.File;
import java.util.Date;
import java.util.Locale;

import static com.gameroom.system.application.settings.SettingValue.*;

/** This is the list of possible settings through the app.
 *  Each Setting has :
 *      - a {@link String} {@link #key} that uniquely identifies it (useful for DB and HashMap storing, see {@link GeneralSettings}.)
 *      - a default {@link SettingValue}, {@link #defaultValue}. It can be overriden (in this case it saved into the DB).
 *  See {@link SettingValue} to understand which restriction there are on this.
 *  Those {@link PredefinedSetting} can then be used along with the {@link GeneralSettings} instance to read and update
 *  the setting value.
 *
 * @author LM. Garret (admin@gameroom.me)
 * @date 08/08/2016
 */
public enum PredefinedSetting {
    LOCALE("locale", new SettingValue(Locale.getDefault(),Locale.class,CATEGORY_GENERAL))
    ,TILE_ZOOM("tileZoom", new SettingValue(0.365, Double.class, CATEGORY_NONE))
    ,ON_GAME_LAUNCH_ACTION("onGameLaunchAction", new SettingValue<OnLaunchAction>(OnLaunchAction.DO_NOTHING,OnLaunchAction.class,CATEGORY_ON_GAME_START))
    ,FULL_SCREEN("fullScreen", new SettingValue(new SimpleBooleanProperty(false),Boolean.class,CATEGORY_NONE))
    ,WINDOW_WIDTH("windowWidth", new SettingValue(1366,Integer.class,CATEGORY_NONE))
    ,WINDOW_HEIGHT("windowHeight", new SettingValue(768,Integer.class,CATEGORY_NONE))
    ,WINDOW_X("windowX", new SettingValue(0.0, Double.class, CATEGORY_NONE))
    ,WINDOW_Y("windowY", new SettingValue(0.0, Double.class, CATEGORY_NONE))
    ,GAMING_POWER_MODE("gamingPowerMode", new SettingValue(PowerMode.getActivePowerMode(),PowerMode.class,CATEGORY_ON_GAME_START))
    ,ENABLE_GAMING_POWER_MODE("enableGamingPowerMode", new SettingValue(false,Boolean.class,CATEGORY_ON_GAME_START))
    , NO_NOTIFICATIONS("noNotifications", new SettingValue(false,Boolean.class, CATEGORY_UI))
    , NO_TOASTS("noToasts", new SettingValue(false,Boolean.class, CATEGORY_UI))
    ,START_MINIMIZED("startMinimized", new SettingValue(false,Boolean.class,CATEGORY_GENERAL))
    ,WINDOW_MAXIMIZED("windowMaximized", new SettingValue(true,Boolean.class,CATEGORY_NONE))
    ,HIDE_TOOLBAR("hideToolBar", new SettingValue(false,Boolean.class,CATEGORY_NONE))
    ,HIDE_TILES_ROWS("hideTilesRows", new SettingValue(false,Boolean.class,CATEGORY_NONE))
    ,ENABLE_STATIC_WALLPAPER("enableStaticWallpaper", new SettingValue(false,Boolean.class,CATEGORY_NONE))
    ,START_WITH_WINDOWS("startWithWindows", new SettingValue(false,Boolean.class,CATEGORY_GENERAL))
    ,NO_MORE_ICON_TRAY_WARNING("noMoreIconTrayWarning", new SettingValue(false,Boolean.class,CATEGORY_NONE))
    , ENABLE_GAME_CONTROLLER_SUPPORT("enableXboxControllerSupport", new SettingValue(false,Boolean.class,CATEGORY_UI))
    , SUPPORTER_KEY("supporterKey", new SettingValue("",String.class,CATEGORY_GENERAL))
    ,DISABLE_MAINSCENE_WALLPAPER("disableMainSceneWallpaper", new SettingValue(false,Boolean.class,CATEGORY_NONE))
    ,DISABLE_SCROLLBAR_IN_FULLSCREEN("disableScrollbarFullScreen", new SettingValue(true,Boolean.class,CATEGORY_UI))
    , STEAM_PROFILE("steamProfile", new SettingValue(null, SteamProfile.class,CATEGORY_SCAN))
    ,IGNORED_STEAM_APPS("ignoredSteamApps",new SettingValue(new SteamPreEntry[]{},SteamPreEntry[].class,CATEGORY_SCAN))
    ,IGNORED_GAME_FOLDERS("ignoredGameFolders",new SettingValue(new File[]{},File[].class,CATEGORY_SCAN))
    , SCAN_PERIOD("scanPeriod", new SettingValue(ScanPeriod.HALF_HOUR,ScanPeriod.class,CATEGORY_SCAN))
    , SYNC_STEAM_PLAYTIMES("syncSteamPlaytime", new SettingValue(true,Boolean.class,CATEGORY_SCAN))
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
    , ENABLED_GAME_SCANNERS("enabledGameScanners",new SettingValue(ScannerProfile.values(),ScannerProfile[].class,CATEGORY_SCAN))
    , LAST_SUPPORT_MESSAGE("lastSupportMessage", new SettingValue(new Date(),Date.class,CATEGORY_NONE))
    , LAST_UPDATE_CHECK("lastUpdateCheck", new SettingValue(new Date(),Date.class,CATEGORY_NONE))
    , DRAWER_MENU_WIDTH("drawerMenuWidth", new SettingValue(0.0,Double.class,CATEGORY_NONE))
    ,NO_MORE_ADD_APP_WARNING("noMoreAddAppWarning", new SettingValue(false,Boolean.class,CATEGORY_NONE))
    ,NO_MORE_ADD_FOLDER_WARNING("noMoreAddAppWarning", new SettingValue(false,Boolean.class,CATEGORY_NONE))
    ,KEEP_COVER_RATIO("keepCoverRatio", new SettingValue(false,Boolean.class,CATEGORY_NONE))
    ,SUPPORTER_KEY_PRICE("supporterKeyPrice", new SettingValue("8",String.class,CATEGORY_NONE))
    ,CHOSEN_CONTROLLER("chosenController", new SettingValue(null,String.class,CATEGORY_UI))
    ,SHOW_PC_ICON("showPCIcon", new SettingValue(false,Boolean.class,CATEGORY_NONE))
    , INSTALL_DATE("installDate", new SettingValue(new Date(),Date.class,CATEGORY_NONE))
    , LAST_PING_DATE("lastPingDate", new SettingValue(new Date(),Date.class,CATEGORY_NONE))
    ,ALLOW_COLLECT_SYSTEM_INFO("allowCollectSystemInfo", new SettingValue(true,Boolean.class,CATEGORY_GENERAL))
    ,GROUP_BY("group_by", new SettingValue(GroupType.DEFAULT.getId(),String.class,CATEGORY_NONE))
    ,SORT_BY("sort_by", new SettingValue(SortType.NAME.getId(),String.class,CATEGORY_NONE))
    , REPORT_INVALID_GAMES("reportInvalidGames", new SettingValue(true,Boolean.class,CATEGORY_NONE))
    ;


    private String key;
    private SettingValue defaultValue;

    PredefinedSetting(String key, SettingValue setting){
        this.key = key;
        this.defaultValue = setting;
    }
    /** Gets the localized label used to name a setting.
     * Labels are stored along with tooltips in a other properties files than usual for usual strings;
     *
     * @return the corresponding label to this setting
     * @throws NullPointerException if the label was not set
     */
    public String getLabel(){
        String label = Main.getSettingsString(key +"_label");
        if(label == null){
            throw new NullPointerException("Forgot to set label for key"+key);
        }
        return label!=null ? label : key;
    }

    /** Gets the localized tooltip to be displayed when the user hovers over the label.
     * Tooltips are stored along with labels in a other properties files than usual for usual strings;
     *
     * @return the corresponding tooltip to this setting, or the label if it has not been set. (see {@link #getLabel()}
     */
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

    /** Given a key, it returns the corresponding {@link PredefinedSetting} from the enum above.
     *
     * @param key key used to identify the {@link PredefinedSetting}. If null, this returns null
     * @return the corresponding {@link PredefinedSetting}, null if the key is null or unknown.
     */
    public static PredefinedSetting getFromKey(String key){
        if(key == null){
            return null;
        }
        for(PredefinedSetting setting : values()){
            if(setting.getKey().equals(key)){
                return setting;
            }
        }
        return null;
    }
}

package system.application.settings;

import data.game.scanner.ScanPeriod;
import data.game.scanner.ScannerProfile;
import data.game.scraper.SteamPreEntry;
import data.game.scraper.SteamProfile;
import data.io.DataBase;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import system.application.OnLaunchAction;
import system.os.PowerMode;
import ui.Main;
import ui.scene.SettingsScene;
import ui.theme.Theme;
import ui.theme.UIScale;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;

import static system.application.settings.PredefinedSetting.STEAM_PROFILE;

/**
 * Created by LM on 03/07/2016.
 */
public class GeneralSettings {
    private HashMap<String, SettingValue> settingsMap = new HashMap<>();

    public GeneralSettings() {
        loadSettings();
    }

    private void loadSettings() {

        try {
            for (PredefinedSetting predefinedSetting : PredefinedSetting.values()) {
                SettingValue.loadSetting(settingsMap, predefinedSetting);
            }
        } finally {
            Main.LOGGER.info("Loaded settings : "
                    + "windowWidth=" + getWindowWidth()
                    + ", windowHeight=" + getWindowHeight()
                    + ", locale=" + getLocale(PredefinedSetting.LOCALE).getLanguage());
        }
    }

    public void saveSettings() throws SQLException {
        String sql = "INSERT OR REPLACE INTO Settings (id,value) VALUES ";
        String pair = "(?,?)";
        String comma = ", ";

        int settingNb = PredefinedSetting.values().length;
        for (int i = 0; i < settingNb; i++) {
            sql += pair;
            if (i != settingNb - 1) {
                sql+=comma;
            } else {
                sql+=";";
            }
        }

        Connection connection = DataBase.getUserConnection();
        PreparedStatement statement = connection.prepareStatement(sql);

        int i = 1;
        for(PredefinedSetting setting : PredefinedSetting.values()){
            statement.setString(i++, setting.getKey());
            statement.setString(i++, settingsMap.get(setting.getKey()).toString());
        }
        statement.execute();
        statement.close();
    }

    /***************************SETTERS AND GETTERS*******************************/

    public int getWindowWidth() {
        SettingValue setting = settingsMap.get(PredefinedSetting.WINDOW_WIDTH.getKey());
        return (int) setting.getSettingValue();
    }

    public int getWindowHeight() {
        SettingValue setting = settingsMap.get(PredefinedSetting.WINDOW_HEIGHT.getKey());
        return (int) setting.getSettingValue();
    }

    public Boolean getBoolean(PredefinedSetting key) {
        SettingValue setting = settingsMap.get(key.getKey());
        if (setting.getSettingValue() instanceof SimpleBooleanProperty) {
            return ((SimpleBooleanProperty) setting.getSettingValue()).getValue();
        }
        return (boolean) setting.getSettingValue();
    }

    public SimpleBooleanProperty getBooleanProperty(PredefinedSetting key) {
        SettingValue setting = settingsMap.get(key.getKey());
        return (SimpleBooleanProperty) setting.getSettingValue();
    }

    public Locale getLocale(PredefinedSetting key) {
        SettingValue setting = settingsMap.get(key.getKey());
        return (Locale) setting.getSettingValue();
    }

    public OnLaunchAction getOnLaunchAction(PredefinedSetting key) {
        SettingValue setting = settingsMap.get(key.getKey());
        return (OnLaunchAction) setting.getSettingValue();
    }

    public PowerMode getPowerMode(PredefinedSetting key) {
        SettingValue setting = settingsMap.get(key.getKey());
        return (PowerMode) setting.getSettingValue();
    }

    public int getInt(PredefinedSetting key) {
        SettingValue setting = settingsMap.get(key.getKey());
        return (int) setting.getSettingValue();
    }

    public double getDouble(PredefinedSetting key) {
        SettingValue setting = settingsMap.get(key.getKey());
        return (double) setting.getSettingValue();
    }

    public String[] getStrings(PredefinedSetting key) {
        SettingValue setting = settingsMap.get(key.getKey());
        return (String[]) setting.getSettingValue();
    }

    public String getString(PredefinedSetting key) {
        SettingValue setting = settingsMap.get(key.getKey());
        return (String) setting.getSettingValue();
    }

    public SteamProfile getSteamProfileToScan() {
        SettingValue setting = settingsMap.get(STEAM_PROFILE.getKey());
        return (SteamProfile) setting.getSettingValue();
    }

    public SteamPreEntry[] getSteamAppsIgnored() {
        SettingValue setting = settingsMap.get(PredefinedSetting.IGNORED_STEAM_APPS.getKey());
        return (SteamPreEntry[]) setting.getSettingValue();
    }

    public UIScale getUIScale() {
        SettingValue<UIScale> settingValue = settingsMap.get(PredefinedSetting.UI_SCALE.getKey());
        return settingValue.getSettingValue();
    }

    public ScanPeriod getScanPeriod() {
        SettingValue<ScanPeriod> setting = settingsMap.get(PredefinedSetting.SCAN_PERIOD.getKey());
        return setting.getSettingValue();
    }

    public Date getDate(PredefinedSetting predefSetting) {
        SettingValue<Date> setting = settingsMap.get(predefSetting.getKey());
        return setting.getSettingValue();
    }

    public Theme getTheme() {
        SettingValue<Theme> settingValue = settingsMap.get(PredefinedSetting.THEME.getKey());
        return settingValue.getSettingValue();
    }

    public boolean isGameScannerEnabled(ScannerProfile profile) {
        ScannerProfile[] enabledValues = (ScannerProfile[]) settingsMap.get(PredefinedSetting.ENABLED_GAME_SCANNERS.getKey()).getSettingValue();

        boolean enabled = false;
        for (ScannerProfile disabledProfile : enabledValues) {
            enabled = enabled || disabledProfile.equals(profile);
            if (enabled) {
                break;
            }
        }
        return enabled;
    }

    public void setGameScannersEnabled(ScannerProfile[] profiles) {
        setSettingValue(PredefinedSetting.ENABLED_GAME_SCANNERS, profiles);
    }

    /*public void setGameScannerEnabled(boolean enabled, ScannerProfile profile){
        ScannerProfile[] disabledProfiles = (ScannerProfile[]) settingsMap.get(PredefinedSetting.ENABLED_GAME_SCANNERS.getKey()).getSettingValue();
        if(enabled == isGameScannerEnabled(profile)){
            return;
        }
        ScannerProfile[] futureDisabledProfiles = new ScannerProfile[0];
        if(!enabled){
            futureDisabledProfiles = new ScannerProfile[disabledProfiles.length + 1];
            System.arraycopy(disabledProfiles, 0, futureDisabledProfiles, 0, disabledProfiles.length);
            futureDisabledProfiles[futureDisabledProfiles.length - 1] = profile;
        }else{
            futureDisabledProfiles = new ScannerProfile[disabledProfiles.length - 1];
            int offset = 0;
            for (int i = 0; i < futureDisabledProfiles.length; i++) {
                if(disabledProfiles[i].equals(profile)){
                    offset++;
                }
                futureDisabledProfiles[i] = disabledProfiles[i+offset];
            }
        }
        Main.GENERAL_SETTINGS.setSettingValue(PredefinedSetting.ENABLED_GAME_SCANNERS, futureDisabledProfiles);

    }*/

    public void setSettingValue(PredefinedSetting key, Object value) {
        SettingValue settingValue;

        if (key.getDefaultValue().getSettingValue() instanceof SimpleBooleanProperty && value instanceof Boolean) {
            SettingValue<SimpleBooleanProperty> oldValue = settingsMap.get(key.getKey());
            if (oldValue == null) {
                oldValue = new SettingValue(new SimpleBooleanProperty((Boolean) value), SimpleBooleanProperty.class, key.getDefaultValue().getCategory());
            }
            oldValue.getSettingValue().setValue((Boolean) value);
            settingValue = oldValue;
        } else {
            settingValue = new SettingValue(value, value.getClass(), key.getDefaultValue().getCategory());
        }

        settingsMap.put(key.getKey(), settingValue);
        try {
            saveSettings();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void onSupporterModeActivated() {

    }

    public void onSupporterModeDeactivated() {
        Main.GENERAL_SETTINGS.setSettingValue(PredefinedSetting.THEME, Theme.DEFAULT_THEME);
        Main.GENERAL_SETTINGS.setSettingValue(PredefinedSetting.ENABLE_STATIC_WALLPAPER, false);
        Platform.runLater(() -> {
            SettingsScene.displayRestartDialog();
            Main.restart(Main.MAIN_SCENE.getParentStage(), "Not in supporter mode anymore");
        });
    }
}

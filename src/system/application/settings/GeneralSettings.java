package system.application.settings;

import com.google.gson.JsonSyntaxException;
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import static system.application.settings.PredefinedSetting.STEAM_PROFILE;

/** This is the main interface to access settings.
 * @author LM. Garret (admin@gameroom.me)
 * @date 03/07/2016
 */
public class GeneralSettings {
    private static GeneralSettings INSTANCE;
    private HashMap<String, SettingValue> settingsMap = new HashMap<>();

    public static GeneralSettings settings(){
        if(INSTANCE == null){
            INSTANCE = new GeneralSettings();
        }
        return INSTANCE;
    }
    public GeneralSettings() {
        load();
    }

    private void load() {
        try {
            try {
                Connection connection = DataBase.getUserConnection();
                PreparedStatement statement = connection.prepareStatement("select * from Settings");
                ResultSet set = statement.executeQuery();
                while (set.next()) {
                    String value = set.getString("value");
                    String id = set.getString("id");
                    PredefinedSetting predefinedSetting = PredefinedSetting.getFromKey(id);
                    if (value != null && id != null && predefinedSetting != null) {
                        try {
                            SettingValue settingValue = SettingValue.getSettingValue(predefinedSetting, value);
                            settingsMap.put(predefinedSetting.getKey(), settingValue != null ? settingValue : predefinedSetting.getDefaultValue());
                        } catch (JsonSyntaxException jse) {
                            Main.LOGGER.error("Wrong JSON syntax for setting \"" + predefinedSetting.getKey() + "\", using value : " + predefinedSetting.getDefaultValue());
                        }
                    }
                }
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } finally {
            Main.LOGGER.info("Loaded settings : "
                    + "windowWidth=" + getWindowWidth()
                    + ", windowHeight=" + getWindowHeight()
                    + ", locale=" + getLocale(PredefinedSetting.LOCALE).getLanguage());
        }
    }

    public void save() throws SQLException {
        String sql = "INSERT OR REPLACE INTO Settings (id,value) VALUES ";
        String pair = "(?,?)";
        String comma = ", ";

        int settingNb = PredefinedSetting.values().length;
        for (int i = 0; i < settingNb; i++) {
            sql += pair;
            if (i != settingNb - 1) {
                sql += comma;
            } else {
                sql += ";";
            }
        }

        Connection connection = DataBase.getUserConnection();
        PreparedStatement statement = connection.prepareStatement(sql);

        int i = 1;
        for (PredefinedSetting setting : PredefinedSetting.values()) {
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
        settings().setSettingValue(PredefinedSetting.ENABLED_GAME_SCANNERS, futureDisabledProfiles);

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
            save();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void onSupporterModeActivated() {

    }

    public void onSupporterModeDeactivated() {
        settings().setSettingValue(PredefinedSetting.THEME, Theme.DEFAULT_THEME);
        settings().setSettingValue(PredefinedSetting.ENABLE_STATIC_WALLPAPER, false);
        Platform.runLater(() -> {
            SettingsScene.displayRestartDialog();
            Main.restart(Main.MAIN_SCENE.getParentStage(), "Not in supporter mode anymore");
        });
    }
}

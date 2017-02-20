package system.application.settings;

import data.game.scanner.ScanPeriod;
import data.game.scanner.ScannerProfile;
import data.game.scraper.SteamPreEntry;
import system.application.OnLaunchAction;
import system.os.PowerMode;
import ui.Main;
import ui.theme.Theme;
import ui.theme.UIScale;

import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;

/**
 * Created by LM on 03/07/2016.
 */
public class GeneralSettings {
    private HashMap<String, SettingValue> settingsMap = new HashMap<>();

    public GeneralSettings() {
        loadSettings();
    }

    private void loadSettings() {
        Properties prop = new Properties();
        InputStream input = null;

        try {
            input = new FileInputStream(Main.FILES_MAP.get("config.properties"));

            // load a properties file
            prop.load(input);

            for (PredefinedSetting predefinedSetting : PredefinedSetting.values()) {
                SettingValue.loadSetting(settingsMap, prop, predefinedSetting);
            }
        } catch (IOException ex) {
            ex.printStackTrace();

        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Main.LOGGER.info("Loaded settings : "
                    + "windowWidth=" + getWindowWidth()
                    + ", windowHeight=" + getWindowHeight()
                    + ", locale=" + getLocale(PredefinedSetting.LOCALE).getLanguage());
        }
    }

    public void saveSettings() {
        Properties prop = new Properties();
        OutputStream output = null;

        try {

            output = new FileOutputStream(Main.FILES_MAP.get("config.properties"));

            for (PredefinedSetting key : PredefinedSetting.values()) {
                prop.setProperty(key.toString(), settingsMap.get(key.getKey()).toString());
            }
            // save properties to project root folder
            prop.store(output, null);

        } catch (IOException io) {
            io.printStackTrace();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
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
        return (boolean) setting.getSettingValue();
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

    public File[] getFiles(PredefinedSetting key) {
        SettingValue setting = settingsMap.get(key.getKey());
        return (File[]) setting.getSettingValue();
    }

    public String[] getStrings(PredefinedSetting key) {
        SettingValue setting = settingsMap.get(key.getKey());
        return (String[]) setting.getSettingValue();
    }

    public String getString(PredefinedSetting key) {
        SettingValue setting = settingsMap.get(key.getKey());
        return (String) setting.getSettingValue();
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
        SettingValue settingValue = new SettingValue(value, value.getClass(), key.getDefaultValue().getCategory());
        settingsMap.put(key.getKey(), settingValue);
        saveSettings();
    }

    public void onSupporterModeActivated() {

    }

    public void onSupporterModeDeactivated() {
        Main.GENERAL_SETTINGS.setSettingValue(PredefinedSetting.THEME, Theme.DEFAULT_THEME);
    }
}

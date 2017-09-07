package system.application.settings;

import com.google.gson.JsonSyntaxException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import data.game.scanner.ScanPeriod;
import data.game.scanner.ScannerProfile;
import data.game.scraper.SteamProfile;
import data.io.DataBase;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import org.json.JSONArray;
import org.json.JSONException;
import system.application.OnLaunchAction;
import system.os.PowerMode;
import ui.Main;
import ui.scene.SettingsScene;
import ui.theme.Theme;
import ui.theme.ThemeUtils;
import ui.theme.UIScale;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import static system.application.settings.PredefinedSetting.*;

/**
 * This is the main interface to access settings. This singleton is responsible reading/writing settings from/into the
 * database.
 * <p>
 * Settings must be accessed only using {@link #settings()}.
 * <p>
 * This class uses three other classes to represent a setting, namely {@link PredefinedSetting} and {@link SettingValue}.
 * {@link PredefinedSetting} is an enum that offers the list of settings that can be read and updated. Each of them is
 * basically mapped into the {@link #settingsMap} to their current {@link SettingValue}. {@link #settingsMap} works like
 * a cache of what's inside the database.
 *
 * @author LM. Garret (admin@gameroom.me)
 * @date 03/07/2016
 */
public class GeneralSettings {
    private static GeneralSettings INSTANCE;
    private HashMap<String, SettingValue> settingsMap = new HashMap<>();

    public static GeneralSettings settings() {
        if (INSTANCE == null) {
            INSTANCE = new GeneralSettings();
        }
        return INSTANCE;
    }

    public GeneralSettings() {
        load();
    }

    /**
     * Loads all the settings from the DB. Settings in the DB are represented as a pair, <id,value>.
     * Once the {@link SettingValue} have been created, we store it into the {@link #settingsMap} by using the corresponding
     * {@link PredefinedSetting}'s key as the HashMap key.
     */
    public void load() {
        settingsMap.clear();
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

    /**
     * Saves settings into the DB. See {@link #load()} to understand how the mapping is done.
     *
     * @throws SQLException in case an error occurred while saving settings to the db.
     */
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
            SettingValue value = settingsMap.get(setting.getKey());
            if (value == null) {
                statement.setString(i++, setting.getDefaultValue().toString());
            } else {
                statement.setString(i++, value.toString());
            }
        }
        statement.execute();
        statement.close();
    }

    /***************************SETTERS AND GETTERS*******************************/

    public int getWindowWidth() {
        SettingValue setting = settingsMap.get(PredefinedSetting.WINDOW_WIDTH.getKey());
        if(setting == null){
            return (int)PredefinedSetting.WINDOW_WIDTH.getDefaultValue().getSettingValue();
        }
        return (int) setting.getSettingValue();
    }

    public int getWindowHeight() {
        SettingValue setting = settingsMap.get(PredefinedSetting.WINDOW_HEIGHT.getKey());
        if(setting == null){
            return (int) PredefinedSetting.WINDOW_HEIGHT.getDefaultValue().getSettingValue();
        }
        return (int) setting.getSettingValue();
    }

    public Boolean getBoolean(PredefinedSetting key) {
        SettingValue setting = settingsMap.get(key.getKey());
        if(setting == null){
            return (Boolean)key.getDefaultValue().getSettingValue();
        }
        if (setting.getSettingValue() instanceof SimpleBooleanProperty) {
            return ((SimpleBooleanProperty) setting.getSettingValue()).getValue();
        }
        return (boolean) setting.getSettingValue();
    }

    public SimpleBooleanProperty getBooleanProperty(PredefinedSetting key) {
        SettingValue setting = settingsMap.get(key.getKey());
        if(setting == null){
            return (SimpleBooleanProperty) key.getDefaultValue().getSettingValue();
        }
        return (SimpleBooleanProperty) setting.getSettingValue();
    }

    public Locale getLocale(PredefinedSetting key) {
        SettingValue setting = settingsMap.get(key.getKey());
        if(setting == null){
            return (Locale) key.getDefaultValue().getSettingValue();
        }
        return (Locale) setting.getSettingValue();
    }

    public OnLaunchAction getOnLaunchAction(PredefinedSetting key) {
        SettingValue setting = settingsMap.get(key.getKey());
        if(setting == null){
            return (OnLaunchAction) key.getDefaultValue().getSettingValue();
        }
        return (OnLaunchAction) setting.getSettingValue();
    }

    public PowerMode getPowerMode(PredefinedSetting key) {
        SettingValue setting = settingsMap.get(key.getKey());
        if(setting == null){
            return (PowerMode) key.getDefaultValue().getSettingValue();
        }
        return (PowerMode) setting.getSettingValue();
    }

    public int getInt(PredefinedSetting key) {
        SettingValue setting = settingsMap.get(key.getKey());
        if(setting == null){
            return (int)key.getDefaultValue().getSettingValue();
        }
        return (int) setting.getSettingValue();
    }

    public double getDouble(PredefinedSetting key) {
        SettingValue setting = settingsMap.get(key.getKey());
        if(setting == null){
            return (double)key.getDefaultValue().getSettingValue();
        }
        return (double) setting.getSettingValue();
    }

    public String[] getStrings(PredefinedSetting key) {
        SettingValue setting = settingsMap.get(key.getKey());
        if(setting == null){
            return (String[])key.getDefaultValue().getSettingValue();
        }
        return (String[]) setting.getSettingValue();
    }

    public String getString(PredefinedSetting key) {
        SettingValue setting = settingsMap.get(key.getKey());
        if(setting == null){
            return (String) key.getDefaultValue().getSettingValue();
        }
        return (String) setting.getSettingValue();
    }

    public SteamProfile getSteamProfileToScan() {
        SettingValue setting = settingsMap.get(STEAM_PROFILE.getKey());
        if(setting == null){
            return (SteamProfile) STEAM_PROFILE.getDefaultValue().getSettingValue();
        }
        return (SteamProfile) setting.getSettingValue();
    }

    public UIScale getUIScale() {
        SettingValue<UIScale> settingValue = settingsMap.get(PredefinedSetting.UI_SCALE.getKey());
        if(settingValue == null){
            return (UIScale) UI_SCALE.getDefaultValue().getSettingValue();
        }
        return settingValue.getSettingValue();
    }

    public ScanPeriod getScanPeriod() {
        SettingValue<ScanPeriod> setting = settingsMap.get(PredefinedSetting.SCAN_PERIOD.getKey());
        if(setting == null){
            return (ScanPeriod) SCAN_PERIOD.getDefaultValue().getSettingValue();
        }
        return setting.getSettingValue();
    }

    public Date getDate(PredefinedSetting predefSetting) {
        SettingValue<Date> setting = settingsMap.get(predefSetting.getKey());
        if(setting == null){
            return (Date) predefSetting.getDefaultValue().getSettingValue();
        }
        return setting.getSettingValue();
    }

    public Theme getTheme() {
        SettingValue<Theme> settingValue = settingsMap.get(PredefinedSetting.THEME.getKey());
        if(settingValue == null){
            return (Theme) THEME.getDefaultValue().getSettingValue();
        }
        return ThemeUtils.getThemeFromName(settingValue.getSettingValue().getName());
    }

    public String getSupporterKeyPrice() {
        String price = null;
        try {
            HttpResponse<JsonNode> response  = Unirest.get("https://gameroom.me/edd-api/products/?product=297")
                    .header("Accept", "application/json")
                    .asJson();
                try {
                    JSONArray array = response.getBody().getArray();
                    price= response.getBody().getObject().getJSONArray("products")
                            .getJSONObject(0)
                            .getJSONObject("pricing")
                            .getString("amount");
                } catch (JSONException jse) {
                    if (jse.toString().contains("not found")) {
                        //Main.LOGGER.error("Serie not found");
                    }
                }
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        if(price != null){
            setSettingValue(SUPPORTER_KEY_PRICE,price);
        }

        SettingValue<String> settingValue = settingsMap.get(PredefinedSetting.SUPPORTER_KEY_PRICE.getKey());
        if(settingValue == null){
            return (String) SUPPORTER_KEY_PRICE.getDefaultValue().getSettingValue();
        }
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

    /***************************SUPPORTER KEY (DE)ACTIVATION*******************************/

    /**
     * Put here all code that should be executed whenever the user activates his supporter key.
     */
    public void onSupporterModeActivated() {

    }

    /**
     * Put here all code that should be executed whenever the user revokes his supporter key.
     */
    public void onSupporterModeDeactivated() {
        settings().setSettingValue(PredefinedSetting.THEME, Theme.DEFAULT_THEME);
        settings().setSettingValue(PredefinedSetting.ENABLE_STATIC_WALLPAPER, false);
        Platform.runLater(() -> {
            SettingsScene.displayRestartDialog("Not in supporter mode anymore");
        });
    }
}

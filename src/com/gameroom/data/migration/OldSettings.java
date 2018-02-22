package com.gameroom.data.migration;

import com.gameroom.data.game.entry.Platform;
import com.gameroom.data.game.scraper.SteamPreEntry;
import com.gameroom.data.io.DataBase;
import com.gameroom.ui.Main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;

import static com.gameroom.ui.Main.FILES_MAP;

/**
 * Created by LM on 02/06/2017.
 */
public class OldSettings {
    protected static HashMap<String, OldSettingValue> settingsMap = new HashMap<>();

    public static void transferOldSettings() {
        File configFile = FILES_MAP.get("config.properties");
        if (!configFile.exists()) { //already migrated
            return;
        }
        Properties prop = new Properties();
        InputStream input = null;

        try {
            input = new FileInputStream(Main.FILES_MAP.get("config.properties"));

            // load a properties file
            prop.load(input);

            for (OldPredefinedSetting predefinedSetting : OldPredefinedSetting.values()) {
                OldSettingValue.loadSetting(settingsMap, prop, predefinedSetting);
            }
            for (OldPredefinedSetting predefinedSetting : OldPredefinedSetting.values()) {
                if (predefinedSetting.equals(OldPredefinedSetting.GAMES_FOLDER)) {
                    OldSettingValue value = settingsMap.get(predefinedSetting.getKey());
                    if(value != null && value.toString() != null && !value.toString().isEmpty()){
                        try {
                            Connection connection = DataBase.getUserConnection();
                            PreparedStatement statement = connection.prepareStatement("INSERT OR REPLACE INTO GameFolder (path,platform_id) VALUES (?,?)");
                            statement.setString(1, value.toString().replace("\"",""));
                            statement.setInt(2, Platform.PC_ID);
                            statement.execute();
                            statement.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    OldSettingValue.toDB(settingsMap, predefinedSetting);
                }
            }

        } catch (IOException | SQLException ex) {
            ex.printStackTrace();

        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected static File[] getIgnoredFiles() {
        OldSettingValue setting = settingsMap.get(OldPredefinedSetting.IGNORED_GAME_FOLDERS.getKey());
        if (setting == null || setting.getSettingValue() == null) {
            return new File[0];
        }
        return (File[]) setting.getSettingValue();
    }

    protected static SteamPreEntry[] getIgnoredSteamApps() {
        OldSettingValue setting = settingsMap.get(OldPredefinedSetting.IGNORED_STEAM_APPS.getKey());
        if (setting == null || setting.getSettingValue() == null) {
            return new SteamPreEntry[0];
        }
        return (SteamPreEntry[]) setting.getSettingValue();
    }
}

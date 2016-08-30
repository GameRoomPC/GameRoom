package system.application.settings;

import data.game.scrapper.SteamPreEntry;
import system.application.OnLaunchAction;
import system.os.PowerMode;
import ui.Main;

import java.io.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;

/**
 * Created by LM on 03/07/2016.
 */
public class GeneralSettings {
    private HashMap<String, SettingValue> settingsMap = new HashMap<>();

    public GeneralSettings(){
        loadSettings();
    }
    private void loadSettings(){
        Properties prop = new Properties();
        InputStream input = null;

        try {
            File configFile = new File("config.properties");
            if(!configFile.exists()){
                configFile.createNewFile();
                saveSettings();
            }

            input = new FileInputStream("config.properties");

            // load a properties file
            prop.load(input);

            for(PredefinedSetting predefinedSetting : PredefinedSetting.values()){
                SettingValue.loadSetting(settingsMap,prop, predefinedSetting);
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
                    +"windowWidth="+getWindowWidth()
                    +", windowHeight="+ getWindowHeight()
                    +", locale="+getLocale(PredefinedSetting.LOCALE).getLanguage());
        }
    }

    public void saveSettings(){
        Properties prop = new Properties();
        OutputStream output = null;

        try {

            output = new FileOutputStream("config.properties");

            for(PredefinedSetting key : PredefinedSetting.values()){
                prop.setProperty(key.toString(),settingsMap.get(key.getKey()).toString());
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
        return (int)setting.getSettingValue();
    }

    public int getWindowHeight() {
        SettingValue setting = settingsMap.get(PredefinedSetting.WINDOW_HEIGHT.getKey());
        return (int)setting.getSettingValue();
    }

    public Boolean getBoolean(PredefinedSetting key){
        SettingValue setting = settingsMap.get(key.getKey());
        return (boolean) setting.getSettingValue();
    }
    public Locale getLocale(PredefinedSetting key){
        SettingValue setting = settingsMap.get(key.getKey());
        return (Locale) setting.getSettingValue();
    }
    public OnLaunchAction getOnLaunchAction(PredefinedSetting key){
        SettingValue setting = settingsMap.get(key.getKey());
        return (OnLaunchAction) setting.getSettingValue();
    }
    public PowerMode getPowerMode(PredefinedSetting key){
        SettingValue setting = settingsMap.get(key.getKey());
        return (PowerMode) setting.getSettingValue();
    }
    public int getInt(PredefinedSetting key){
        SettingValue setting = settingsMap.get(key.getKey());
        return (int) setting.getSettingValue();
    }
    public double getDouble(PredefinedSetting key){
        SettingValue setting = settingsMap.get(key.getKey());
        return (double) setting.getSettingValue();
    }
    public File getFile(PredefinedSetting key){
        SettingValue setting = settingsMap.get(key.getKey());
        File file = new File(((File)setting.getSettingValue()).getPath());
        return file;
    }
    public File[] getFiles(PredefinedSetting key){
        SettingValue setting = settingsMap.get(key.getKey());
        return (File[]) setting.getSettingValue();
    }
    public String[] getStrings(PredefinedSetting key){
        SettingValue setting = settingsMap.get(key.getKey());
        return (String[]) setting.getSettingValue();
    }
    public String getString(PredefinedSetting key){
        SettingValue setting = settingsMap.get(key.getKey());
        return (String) setting.getSettingValue();
    }
    public SteamPreEntry[] getSteamAppsIgnored(){
        SettingValue setting = settingsMap.get(PredefinedSetting.IGNORED_STEAM_APPS.getKey());
        return (SteamPreEntry[]) setting.getSettingValue();
    }

    public void setSettingValue(PredefinedSetting key, Object value){
        SettingValue settingValue = new SettingValue(value,value.getClass(),key.getDefaultValue().getCategory());
        settingsMap.put(key.getKey(),settingValue);
        saveSettings();
    }
}

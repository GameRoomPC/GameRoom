package system.application.settings;

import system.application.OnLaunchAction;
import system.os.PowerMode;
import ui.Main;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;

/**
 * Created by LM on 03/07/2016.
 */
public class GeneralSettings {
    private HashMap<String, SettingValue> settingsMap = new HashMap<>();

    /*private Locale locale = Locale.getDefault();
    //private boolean closeOnLaunch = false;
    private OnLaunchAction onLaunchAction = OnLaunchAction.DO_NOTHING;
    private double tileZoom = 0.4;
    private boolean fullScreen = false; //TODO fix fullscreen exiting after changing scene, see https://bugs.openjdk.java.net/browse/JDK-8089209
    private int windowWidth=1366;
    private int windowHeight =768;
    private PowerMode gamingPowerMode=PowerMode.getActivePowerMode();
    private boolean enablePowerGamingMode = false;
    private boolean noMoreTrayMessage = false;
    private boolean minimizeOnStart = false;
    private boolean disableAllNotifications = false;
    private boolean activateXboxControllerSupport = false;
    private String gamesFolder = "";
    private String donationKey = "";
    private boolean disableWallpaper = false;*/

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

            // set the properties value
           /* prop.setProperty("locale", locale);
            prop.setProperty("onLaunchAction", onLaunchAction.getRessourceKey());
            prop.setProperty("tileZoom", Double.toString(tileZoom));
            prop.setProperty("fullScreen", Boolean.toString(fullScreen));
            prop.setProperty("windowWidth", Integer.toString(windowWidth));
            prop.setProperty("windowHeight", Integer.toString(windowHeight));
            prop.setProperty("gamingPowerMode", gamingPowerMode.getUuid().toString());
            prop.setProperty("enablePowerGamingMode", Boolean.toString(enablePowerGamingMode));
            prop.setProperty("noMoreTrayMessage", Boolean.toString(noMoreTrayMessage));
            prop.setProperty("disableAllNotifications", Boolean.toString(disableAllNotifications));
            prop.setProperty("minimizeOnStart", Boolean.toString(minimizeOnStart));
            prop.setProperty("activateXboxControllerSupport", Boolean.toString(activateXboxControllerSupport));
            prop.setProperty("gamesFolder", gamesFolder);
            prop.setProperty("donationKey", donationKey);
            prop.setProperty("disableWallpaper", Boolean.toString(disableWallpaper));*/
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
        return (File) setting.getSettingValue();
    }
    public String getString(PredefinedSetting key){
        SettingValue setting = settingsMap.get(key.getKey());
        return (String) setting.getSettingValue();
    }

    public void setSettingValue(PredefinedSetting key, Object value){
        SettingValue settingValue = new SettingValue(value,value.getClass(),key.getDefaultValue().getCategory());
        settingsMap.put(key.getKey(),settingValue);
        saveSettings();
    }
    /*public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
        saveSettings();
    }

    public OnLaunchAction getOnLaunchAction() {
        return onLaunchAction;
    }

    public void setOnLaunchAction(OnLaunchAction onLaunchAction) {
        this.onLaunchAction = onLaunchAction;
        saveSettings();
    }

    public boolean isFullScreen() {
        return fullScreen;
    }

    public void setFullScreen(boolean fullScreen) {
        this.fullScreen = fullScreen;
        saveSettings();
    }

    public PowerMode getGamingPowerMode() {
        return gamingPowerMode;
    }

    public void setGamingPowerMode(PowerMode gamingPowerMode) {
        this.gamingPowerMode = gamingPowerMode;
        saveSettings();
    }

    public boolean isEnablePowerGamingMode() {
        return enablePowerGamingMode;
    }

    public void setEnablePowerGamingMode(boolean enablePowerGamingMode) {
        this.enablePowerGamingMode = enablePowerGamingMode;
        saveSettings();
    }
    public boolean isNoMoreTrayMessage() {
        return noMoreTrayMessage;
    }

    public void setNoMoreTrayMessage(boolean noMoreTrayMessage) {
        this.noMoreTrayMessage = noMoreTrayMessage;
        saveSettings();
    }

    public boolean isMinimizeOnStart() {
        return minimizeOnStart;
    }

    public void setMinimizeOnStart(boolean minimizeOnStart) {
        this.minimizeOnStart = minimizeOnStart;
        saveSettings();
    }

    public boolean isDisableAllNotifications() {
        return disableAllNotifications;
    }

    public void setDisableAllNotifications(boolean disableAllNotifications) {
        this.disableAllNotifications = disableAllNotifications;
        saveSettings();
    }

    public double getTileZoom() {
        return tileZoom;
    }

    public void setTileZoom(double tileZoom) {
        this.tileZoom = tileZoom;
        saveSettings();
    }

    public boolean isActivateXboxControllerSupport() {
        return activateXboxControllerSupport;
    }

    public void setActivateXboxControllerSupport(boolean activateXboxControllerSupport) {
        this.activateXboxControllerSupport = activateXboxControllerSupport;
        if(activateXboxControllerSupport){
            Main.xboxController.startThreads();
        }else{
            Main.xboxController.stopThreads();
        }
        saveSettings();
    }

    public String getGamesFolder() {
        return gamesFolder;
    }

    public void setGamesFolder(String gamesFolder) {
        this.gamesFolder = gamesFolder;
        saveSettings();
    }
    public void setDonationKey(String donationKey){
        this.donationKey = donationKey;
        saveSettings();
    }
    public String getDonationKey(){
        return donationKey;
    }

    public boolean isDisableWallpaper() {
        return disableWallpaper;
    }

    public void setDisableWallpaper(boolean disableWallpaper) {
        this.disableWallpaper = disableWallpaper;
        saveSettings();
    }*/
}

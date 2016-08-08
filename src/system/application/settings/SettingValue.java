package system.application.settings;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import ui.Main;

import java.util.HashMap;
import java.util.Properties;

/**
 * Created by LM on 08/08/2016.
 */
public class SettingValue<T> {
    protected final static String CATEGORY_GENERAL = "general";
    protected final static String CATEGORY_NONE = "none";
    protected final static String CATEGORY_UI = "ui";
    protected final static String CATEGORY_ON_GAME_START = "onGameStart";


    private final static Gson GSON = new Gson();
    private T settingValue;
    private String category;
    private Class valueClass;

    public SettingValue(T settingValue, Class<T> valueClass, String category){
        this.settingValue = settingValue;
        this.category = category;
        this.valueClass = valueClass;
    }
    public T getSettingValue(){
        return settingValue;
    }

    public void setSettingValue(T settingValue) {
        this.settingValue = settingValue;
    }


    @Override
    public String toString(){
        return GSON.toJson(settingValue);
    }

    public static void loadSetting(HashMap<String, SettingValue> settingsMap, Properties prop, PredefinedSetting predefinedSetting){
        if(prop.getProperty(predefinedSetting.getKey())!=null){
            try {
                SettingValue settingValue = new SettingValue(GSON.fromJson(prop.getProperty(predefinedSetting.getKey()), predefinedSetting.getDefaultValue().getValueClass()),predefinedSetting.getDefaultValue().getValueClass(),predefinedSetting.getDefaultValue().category);
                settingsMap.put(predefinedSetting.getKey(),settingValue);
                return;
            }catch (JsonSyntaxException jse){
                Main.LOGGER.error("Wrong JSON syntax for setting \""+predefinedSetting.getKey()+"\", using value : "+predefinedSetting.getDefaultValue());
            }
        }
        settingsMap.put(predefinedSetting.getKey(),predefinedSetting.getDefaultValue());
    }

    public static void saveSetting(HashMap<String, SettingValue> settingsMap, Properties prop, PredefinedSetting setting){
        prop.setProperty(setting.getKey(),settingsMap.get(setting.getKey()).toString());
    }

    public String getCategory() {
        return category;
    }

    public Class getValueClass() {
        return valueClass;
    }
}

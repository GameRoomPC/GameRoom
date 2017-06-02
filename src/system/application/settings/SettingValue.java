package system.application.settings;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import data.game.entry.GameEntry;
import data.io.DataBase;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import ui.Main;

import java.sql.*;
import java.util.HashMap;
import java.util.Properties;

/**
 * Created by LM on 08/08/2016.
 */
public class SettingValue<T> {
    public final static String CATEGORY_GENERAL = "general";
    public final static String CATEGORY_NONE = "none";
    public final static String CATEGORY_UI = "ui";
    public final static String CATEGORY_ON_GAME_START = "onGameStart";
    public final static String CATEGORY_SCAN= "scan";


    private final static Gson GSON = new Gson();
    private T settingValue;
    private String category;
    private Class valueClass;

    private TypeToken<T> typeToken;

    private SettingValue(T settingValue, TypeToken<T> typeToken, String category){
        this.settingValue = settingValue;
        this.category = category;
        this.typeToken = typeToken;
    }
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
        if(getValueClass()!= null && settingValue instanceof SimpleBooleanProperty){
            return GSON.toJson(((SimpleBooleanProperty)settingValue).getValue());
        }
        return GSON.toJson(settingValue);
    }

    public static void loadSetting(HashMap<String, SettingValue> settingsMap, PredefinedSetting predefinedSetting){
        try {
            Connection connection = DataBase.getUserConnection();
            PreparedStatement statement = connection.prepareStatement("select * from Settings where id=?");
            statement.setString(1,predefinedSetting.getKey());
            ResultSet set = statement.executeQuery();
            if(set.next()){
                String value = set.getString("value");
                if(value!=null){
                    try {
                        SettingValue settingValue = null;
                        if(predefinedSetting.getDefaultValue().getValueClass()!=null){
                            if(predefinedSetting.getDefaultValue().getSettingValue() instanceof SimpleBooleanProperty){
                                Boolean storedValue = GSON.fromJson(value, Boolean.class);
                                SimpleBooleanProperty b = new SimpleBooleanProperty(storedValue);
                                settingValue = new SettingValue(b,predefinedSetting.getDefaultValue().getValueClass(),predefinedSetting.getDefaultValue().category);
                            }else{
                                settingValue = new SettingValue(GSON.fromJson(value, predefinedSetting.getDefaultValue().getValueClass()),predefinedSetting.getDefaultValue().getValueClass(),predefinedSetting.getDefaultValue().category);
                            }
                        }else{
                            settingValue = new SettingValue(GSON.fromJson(value, predefinedSetting.getDefaultValue().typeToken.getType()),predefinedSetting.getDefaultValue().typeToken,predefinedSetting.getDefaultValue().category);
                        }
                        settingsMap.put(predefinedSetting.getKey(),settingValue!=null?settingValue:predefinedSetting.getDefaultValue());
                        return;
                    }catch (JsonSyntaxException jse){
                        Main.LOGGER.error("Wrong JSON syntax for setting \""+predefinedSetting.getKey()+"\", using value : "+predefinedSetting.getDefaultValue());
                    }
                }
                settingsMap.put(predefinedSetting.getKey(),predefinedSetting.getDefaultValue());
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }


    }

    public String getCategory() {
        return category;
    }

    public Class getValueClass() {
        return valueClass;
    }
}

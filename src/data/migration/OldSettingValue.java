package data.migration;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import data.io.DataBase;
import javafx.beans.property.SimpleBooleanProperty;
import ui.Main;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;

/**
 * Created by LM on 08/08/2016.
 */
class OldSettingValue<T> {
    private final static Gson GSON = new Gson();
    private T settingValue;
    private String category;
    private Class valueClass;

    private TypeToken<T> typeToken;

    private OldSettingValue(T settingValue, TypeToken<T> typeToken, String category){
        this.settingValue = settingValue;
        this.category = category;
        this.typeToken = typeToken;
    }
    public OldSettingValue(T settingValue, Class<T> valueClass, String category){
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

    public static void loadSetting(HashMap<String, OldSettingValue> settingsMap, Properties prop, OldPredefinedSetting predefinedSetting){
        if(prop.getProperty(predefinedSetting.getKey())!=null){
            try {
                OldSettingValue settingValue = null;
                if(predefinedSetting.getDefaultValue().getValueClass()!=null){
                    if(predefinedSetting.getDefaultValue().getSettingValue() instanceof SimpleBooleanProperty){
                        Boolean storedValue = GSON.fromJson(prop.getProperty(predefinedSetting.getKey()), Boolean.class);
                        SimpleBooleanProperty b = new SimpleBooleanProperty(storedValue);
                        settingValue = new OldSettingValue(b,predefinedSetting.getDefaultValue().getValueClass(),predefinedSetting.getDefaultValue().category);
                    }else{
                        settingValue = new OldSettingValue(GSON.fromJson(prop.getProperty(predefinedSetting.getKey()), predefinedSetting.getDefaultValue().getValueClass()),predefinedSetting.getDefaultValue().getValueClass(),predefinedSetting.getDefaultValue().category);
                    }
                }else{
                    settingValue = new OldSettingValue(GSON.fromJson(prop.getProperty(predefinedSetting.getKey()), predefinedSetting.getDefaultValue().typeToken.getType()),predefinedSetting.getDefaultValue().typeToken,predefinedSetting.getDefaultValue().category);
                }
                settingsMap.put(predefinedSetting.getKey(),settingValue!=null?settingValue:predefinedSetting.getDefaultValue());
                return;
            }catch (JsonSyntaxException jse){
                Main.LOGGER.error("Wrong JSON syntax for setting \""+predefinedSetting.getKey()+"\", using value : "+predefinedSetting.getDefaultValue());
            }
        }
        settingsMap.put(predefinedSetting.getKey(),predefinedSetting.getDefaultValue());
    }

    public static void toDB(HashMap<String, OldSettingValue> settingsMap, OldPredefinedSetting setting) throws SQLException {
        Connection connection = DataBase.getUserConnection();

        PreparedStatement statement = connection.prepareStatement("INSERT OR REPLACE INTO Settings (id,value) VALUES (?,?)");
        statement.setString(1, setting.getKey());
        statement.setString(2, settingsMap.get(setting.getKey()).toString());
        statement.execute();
        statement.close();
    }

    public String getCategory() {
        return category;
    }

    public Class getValueClass() {
        return valueClass;
    }
}

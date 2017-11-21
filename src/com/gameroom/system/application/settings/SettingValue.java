package com.gameroom.system.application.settings;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.beans.property.SimpleBooleanProperty;

/** This class represents the value of a given {@link PredefinedSetting}. It is generic and thus can be used to store
 * different type of settings (double, boolean, String). Be careful, as the value is jsonified in order to be stored
 * into the DB (for later reads), then it is not possible to use generic types (e.g. ArrayList<GameEntry>) as those will
 * not be correctly read afterwards. It is necessary to store to the lower abstraction possible!
 *
 * @author LM. Garret (admin@gameroom.me)
 * @date 08/08/2016
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

    public TypeToken<T> getTypeToken(){
        return typeToken;
    }

    @Override
    public String toString(){
        if(getValueClass()!= null && settingValue instanceof SimpleBooleanProperty){
            return GSON.toJson(((SimpleBooleanProperty)settingValue).getValue());
        }
        return GSON.toJson(settingValue);
    }

    /** Creates a {@link SettingValue} corresponding to the given predefinedSetting, which value is determined by the jsonValue.
     * Might return null in case of an invalid value
     * @param predefinedSetting the predefined setting this settings value corresponds to.
     * @param jsonValue defines what the value of this setting is
     * @return a {@link SettingValue} if the jsonValue is correct, null otherwise
     */
    public static SettingValue getSettingValue(PredefinedSetting predefinedSetting, String jsonValue){
        SettingValue settingValue = null;
        if (predefinedSetting.getDefaultValue().getValueClass() != null) {
            if (predefinedSetting.getDefaultValue().getSettingValue() instanceof SimpleBooleanProperty) {
                Boolean storedValue = GSON.fromJson(jsonValue, Boolean.class);
                SimpleBooleanProperty b = new SimpleBooleanProperty(storedValue);
                settingValue = new SettingValue(b, predefinedSetting.getDefaultValue().getValueClass(), predefinedSetting.getDefaultValue().getCategory());
            } else {
                settingValue = new SettingValue(GSON.fromJson(jsonValue, predefinedSetting.getDefaultValue().getValueClass()), predefinedSetting.getDefaultValue().getValueClass(), predefinedSetting.getDefaultValue().getCategory());
            }
        } else {
            settingValue = new SettingValue(GSON.fromJson(jsonValue, predefinedSetting.getDefaultValue().getTypeToken().getType()), predefinedSetting.getDefaultValue().getTypeToken().getRawType(), predefinedSetting.getDefaultValue().getCategory());
        }
        return settingValue;
    }

    public String getCategory() {
        return category;
    }

    public Class getValueClass() {
        return valueClass;
    }
}

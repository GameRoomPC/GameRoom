package ui.theme;

import ui.Main;

/**
 * Created by LM on 25/12/2016.
 */
public enum UIScale {
    EXTRA_SMALL("uiscale_extra_small",0.5)
    ,SMALL("uiscale_small",0.75)
    ,NORMAL("uiscale_normal",1.0)
    ,BIG("uiscale_big",1.25)
    ,EXTRA_BIG("uiscale_extra_big",1.5);

    private final static double DEFAULT_FONT_SIZE = 19.0;

    private String displayNameKey;
    private double fontSize = DEFAULT_FONT_SIZE;
    private double scale = 1.0;

    UIScale(String displayNameKey, double scale){
        this.displayNameKey =displayNameKey;
        this.fontSize=DEFAULT_FONT_SIZE*scale;
        this.scale = scale;
    }

    public String getDisplayName(){
        return Main.getString(displayNameKey);
    }

    public double getFontSize(){
        return fontSize;
    }

    public double getScale(){
        return scale;
    }

    public static UIScale fromString(String s){
        if(s == null || s.isEmpty()){
            return NORMAL;
        }
        switch (s){
            case "uiscale_extra_small":
                return EXTRA_SMALL;
            case "uiscale_small":
                return SMALL;
            case "uiscale_normal":
                return NORMAL;
            case "uiscale_big":
                return BIG;
            case "uiscale_extra_big":
                return EXTRA_BIG;
            default:
                return NORMAL;

        }
    }
}

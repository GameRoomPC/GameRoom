package com.gameroom.ui.control.drawer;

import com.gameroom.ui.Main;

/**
 * Created by LM on 09/02/2017.
 */
public enum GroupType {
    DEFAULT("groupBy_default"),
    THEME("groupBy_theme"),
    GENRE("groupBy_genre"),
    SERIE("groupBy_serie"),
    LAUNCHER("groupBy_launcher");

    private String id;

    GroupType(String id){
        this.id = id;
    }

    public String getName(){
        return Main.getString(id);
    }

    public String getId() {
        return id;
    }

    public static GroupType fromId(String id){
        for(GroupType s : GroupType.values()){
            if (s.id.equals(id)){
                return s;
            }
        }
        return DEFAULT;
    }
}

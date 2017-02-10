package ui.control.drawer;

import ui.Main;

/**
 * Created by LM on 09/02/2017.
 */
public enum GroupType {
    ALL("groupBy_all"),
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
}

package system.application;

import ui.Main;

/**
 * Created by LM on 16/07/2016.
 */
public enum OnLaunchAction {
    DO_NOTHING("onLaunch_do_nothing"),
    CLOSE("onLaunch_close_"),
    HIDE("onLaunch_hide")
    ;

    private final String ressourceKey;

    OnLaunchAction(final String ressourceKey) {
        this.ressourceKey = ressourceKey;
    }

    @Override
    public String toString() {
        return Main.getString(ressourceKey);
    }
    public static OnLaunchAction fromString(String key){
        for(OnLaunchAction action : OnLaunchAction.values()){
            if(action.ressourceKey.equals(key)){
                return  action;
            }
        }
        return DO_NOTHING;
    }
    public String getRessourceKey(){
        return ressourceKey;
    }
}

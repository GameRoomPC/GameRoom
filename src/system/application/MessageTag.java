package system.application;

/**
 * Created by LM on 28/07/2016.
 */
public enum MessageTag {
    CLOSE_APP("CLOSE_APP", false)
    ,NEW_UPDATE("NEW_UPDATE", true)
    ,ERROR("ERROR", false)
    ,NO_UPDATE("NO_UPDATE", false);

    private String tag;
    private boolean hasPayload;
    MessageTag(String tag, boolean hasPayload){
        this.tag = tag;
        this.hasPayload = hasPayload;
    }

    @Override
    public String toString(){
        return tag;
    }
    public boolean hasPayload(){
        return hasPayload;
    }
}

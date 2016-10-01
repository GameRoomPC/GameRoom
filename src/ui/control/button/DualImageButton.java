package ui.control.button;

import javafx.event.ActionEvent;
import javafx.scene.image.Image;

/**
 * Created by LM on 02/08/2016.
 */
public class DualImageButton extends ImageButton {
    public static String PAUSE_STATE = "PAUSE";
    public static String PLAY_STATE = "PLAY";

    private Image image1,image2;
    private String state1,state2;
    private String currentState;

    private OnActionHandler[] actions = new OnActionHandler[2];
    public DualImageButton(Image image1, Image image2, String state1, String state2) {
        super(image1);
        this.state1 = state1;
        this.state2 = state2;
        this.image1=image1;
        this.image2=image2;
        this.currentState = state1;

        actions[0] = new OnActionHandler() {
            @Override
            public void handle(ActionEvent me) {
               toggleState();
            }
        };

        setOnAction(event -> {
            for(OnActionHandler action : actions){
                if(action!=null)
                    action.handle(event);
            }
        });

    }
    public void forceState(String state){
        if(!currentState.equals(state)){
            if(state1.equals(state)){
                toggleState();
            }
            if(state2.equals(state)){
                toggleState();
            }
        }
    }
    private void toggleState(){
        Image currentImage = inFirstState() ? image2 : image1;
        setImage(currentImage);
        currentState = inFirstState() ? state2 : state1;
    }

    /**
     * Should call this method, as setOnAction would override icon and state toggling
     */
    public void setOnDualAction(OnActionHandler oah){
        actions[1] = oah;
    }
    public boolean inFirstState(){
        return currentState.equals(state1);
    }
}

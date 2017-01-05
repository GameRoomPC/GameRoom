package ui.control.button;

import javafx.event.ActionEvent;

/**
 * Created by LM on 02/08/2016.
 */
public class DualImageButton extends ImageButton {
    private String state1Id, state2Id;
    private String currentState;

    private OnActionHandler[] actions = new OnActionHandler[2];

    public DualImageButton(String state1Id, String state2Id, double width, double height) {
        super(state1Id, width, height);
        this.state1Id = state1Id;
        this.state2Id = state2Id;
        this.currentState = state1Id;

        actions[0] = new OnActionHandler() {
            @Override
            public void handle(ActionEvent me) {
                toggleState();
            }
        };

        setOnAction(event -> {
            for (OnActionHandler action : actions) {
                if (action != null)
                    action.handle(event);
            }
        });

    }
    public void forceState(String state, boolean applyAction) {
        if (!currentState.equals(state)) {
            toggleState();
        }
        if(applyAction){
            actions[1].handle(null);
        }
    }

    public void forceState(String state) {
        forceState(state,false);
    }

    private void toggleState() {
        String currentId = inFirstState() ? state2Id : state1Id;
        setImageViewId(currentId);
        currentState = inFirstState() ? state2Id : state1Id;
    }

    /**
     * Should call this method, as setOnAction would override icon and state toggling
     */
    public void setOnDualAction(OnActionHandler oah) {
        actions[1] = oah;
    }

    public boolean inFirstState() {
        return currentState.equals(state1Id);
    }
}

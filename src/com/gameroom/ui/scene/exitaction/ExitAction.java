package com.gameroom.ui.scene.exitaction;

/**
 * Created by LM on 17/07/2016.
 */
public abstract class ExitAction {
    private Runnable exitAction;
    ExitAction(Runnable exitAction){
        this.exitAction = exitAction;
    }
    public void run(){
        exitAction.run();
    }
}

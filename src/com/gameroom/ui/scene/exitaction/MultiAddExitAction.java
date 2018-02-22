package com.gameroom.ui.scene.exitaction;

import com.gameroom.ui.scene.GameEditScene;

/**
 * Created by LM on 17/07/2016.
 */
public class MultiAddExitAction extends ExitAction {
    private GameEditScene gameEditScene;

    public MultiAddExitAction(Runnable exitAction,GameEditScene editScene) {
        super(exitAction);
        this.gameEditScene = editScene;
    }

    public GameEditScene getGameEditScene() {
        return gameEditScene;
    }

    public void setGameEditScene(GameEditScene gameEditScene) {
        this.gameEditScene = gameEditScene;
    }
}

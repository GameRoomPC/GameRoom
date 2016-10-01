package ui.scene.exitaction;

import javafx.stage.Stage;
import ui.scene.BaseScene;

/**
 * Created by LM on 17/07/2016.
 */
public class ClassicExitAction extends ExitAction{
    public ClassicExitAction(BaseScene currentScene, Stage parentStage, BaseScene previousScene) {
        super(new Runnable() {
            @Override
            public void run() {
                currentScene.fadeTransitionTo(previousScene, parentStage);
            }
        });
    }
}

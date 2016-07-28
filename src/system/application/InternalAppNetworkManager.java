package system.application;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import ui.Main;

import java.io.File;

import static ui.Main.MAIN_SCENE;

/**
 * Created by LM on 28/07/2016.
 */
public class InternalAppNetworkManager {
    private final static String CLOSE_APP_MESSAGE = "CLOSE_APP";
    private JChannel channel = null;

    public InternalAppNetworkManager(){
        try {
            channel = new JChannel();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    public void connect(){
        try {
            channel.connect("GameRoomNetwork");
            channel.setReceiver(new ReceiverAdapter() {
                @Override
                public void viewAccepted(View newView) {
                    Main.logger.info("Update in internal network cluster, "+newView);
                }

                @Override
                public void receive(Message msg) {
                    Main.logger.info("Received internal network message : "+(String)msg.getObject());
                    if(msg.getObject() instanceof String){
                        switch ((String)msg.getObject()){
                            case CLOSE_APP_MESSAGE:
                                Main.forceStop(MAIN_SCENE.getParentStage());
                                break;
                            default:
                                break;
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void disconnect(){
        channel.disconnect();
    }
}

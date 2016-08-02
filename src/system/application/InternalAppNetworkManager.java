package system.application;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import ui.Main;

import java.util.ArrayList;

import static ui.Main.LOGGER;

/**
 * Created by LM on 28/07/2016.
 */
public class InternalAppNetworkManager {
    private final static String CLOSE_APP_MESSAGE = "CLOSE_APP";
    private final static String NO_UPDATES = "NO_UPDATES";

    private ArrayList<MessageListener> messageListeners = new ArrayList<>();
    private JChannel channel = null;

    public InternalAppNetworkManager() {
        try {
            channel = new JChannel();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void connect() {
        try {
            channel.connect("GameRoomNetwork");
            channel.setReceiver(new ReceiverAdapter() {
                @Override
                public void viewAccepted(View newView) {
                    Main.LOGGER.info("Update in internal network cluster, " + newView);
                }

                @Override
                public void receive(Message msg) {
                    if (msg.getObject() instanceof String) {
                        String message = (String) msg.getObject();
                        boolean isValid = false;
                        for(MessageTag messageTag : MessageTag.values()){
                            if(message.startsWith(messageTag.toString())){
                                isValid = true;
                                String payload = null;
                                if(messageTag.hasPayload() && message.length() > messageTag.toString().length()){
                                    payload = message.substring(messageTag.toString().length());
                                }
                                LOGGER.debug("Received message with tag \""+messageTag+"\" and payload \""+payload+"\"");
                                for(MessageListener listener : messageListeners){
                                    listener.onMessageReceived(messageTag, payload);
                                }
                            }
                        }
                        if(isValid){
                            return;
                        }
                    }
                    Main.LOGGER.warn("Received unvalid message : " + msg.getObject());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        channel.disconnect();
    }

    public void addMessageListener(MessageListener messageListener) {
        messageListeners.add(messageListener);
    }

    public void sendMessage(MessageTag tag, String payload){
        if(!channel.isConnected()){
            connect();
        }
        String messageToString = tag.toString();
        if(payload!=null){
            messageToString+=payload;
        }
        Message m = new Message(null,null,messageToString);
        try {
            channel.send(m);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void sendMessage(MessageTag tag) {
        sendMessage(tag, null);
    }
}

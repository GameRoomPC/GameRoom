package com.gameroom.data.http;

import com.gameroom.ui.Main;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by LM on 03/08/2016.
 */
public class URLTools {
    private final static int HTTPS_PORT = 443;
    private final static int HTTP_PORT = 80;

    public final static String HTTPS_PREFIX=  "https://";
    public final static String HTTP_PREFIX=  "http://";


    public static boolean pingHttps(String host,int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, HTTPS_PORT), timeout);
            Main.LOGGER.debug("Host "+host+" reachable (https)");
            return true;
        } catch (IOException e) {
            Main.LOGGER.debug("Host "+host+" unreachable (https)");
            return false; // Either timeout or unreachable or failed DNS lookup.
        }
    }
}

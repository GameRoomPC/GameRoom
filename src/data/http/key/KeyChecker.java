package data.http.key;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import ui.Main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.*;

/**
 * Created by LM on 05/08/2016.
 */
public class KeyChecker {
    private final static String API_URL = "https://gameroom.me";
    private final static String VALIDATION_KEY = "57a0dc1fa9b0d0.08408190";
    private final static boolean DEBUGGING = false;

    //fields in json response
    public final static String FIELD_RESULT = "result";
    public final static String FIELD_MESSAGE = "message";
    public final static String FIELD_STATUS = "status";
    public final static String FIELD_REGISTERED_DOMAINS = "registered_domains";
    public final static String FIELD_REGISTERED_DOMAIN = "registered_domain";

    //possible results
    public final static String RESULT_SUCCESS = "success";
    public final static String RESULT_ERROR = "error";

    //possible status
    public final static String STATUS_ACTIVE = "active";
    public final static String STATUS_PENDING = "pending";
    public final static String STATUS_BLOCKED = "blocked";
    public final static String STATUS_EXPIRED = "expired";

    public static JSONObject deactivateKey(String key) throws IOException, UnirestException {
        HttpResponse<String> response = Unirest.post(API_URL)
                .field("secret_key", VALIDATION_KEY)
                .field("slm_action", "slm_deactivate")
                .field("registered_domain", getMACAddress())
                .field("license_key", key)
                .asString();


        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getRawBody(), "UTF-8"));
        String json = reader.readLine();
        if (DEBUGGING) {
            Main.LOGGER.debug("deactivateKey response : "+json);
        }
        return new JSONObject(json);
    }

    public static JSONObject activateKey(String key) throws IOException, UnirestException {
        HttpResponse<String> response = Unirest.post(API_URL)
                .field("secret_key",VALIDATION_KEY)
                .field("slm_action", "slm_activate")
                .field("registered_domain", getMACAddress())
                .field("license_key", key)
                .asString();


        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getRawBody(), "UTF-8"));
        String json = reader.readLine();
        if (DEBUGGING) {
            Main.LOGGER.debug("activateKey response : "+json);
        }
        return new JSONObject(json);
    }
    public static boolean isKeyValid(String key){
        if(!testInet("igdb.com") && !testInet(API_URL.replace("https://",""))){
            return false;
        }
        try {
            JSONObject response = askKeyValid(key);
            if(response!=null){
                if(response.getString(FIELD_RESULT).equals(RESULT_SUCCESS)){
                    String status = response.getString(FIELD_STATUS);
                    if(status.equals(STATUS_ACTIVE)){
                        JSONArray registeredDomains = response.getJSONArray(FIELD_REGISTERED_DOMAINS);

                        boolean found = false;
                        for(int i = 0; i<registeredDomains.length() && !found; i++){
                            found = registeredDomains.getJSONObject(i).getString(FIELD_REGISTERED_DOMAIN).equals(getMACAddress());
                        }
                        return found;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    private static JSONObject askKeyValid(String key) throws IOException, UnirestException {
        HttpResponse<String> response = Unirest.post(API_URL)
                .field("secret_key",VALIDATION_KEY)
                .field("slm_action", "slm_check")
                .field("license_key", key)
                .asString();


        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getRawBody(), "UTF-8"));
        String json = reader.readLine();
        if (DEBUGGING) {
            Main.LOGGER.debug("isKeyValid response : "+json);
        }
        return new JSONObject(json);
    }

    public static String getMACAddress() throws UnknownHostException,
            SocketException
    {
        InetAddress ipAddress = InetAddress.getLocalHost();
        NetworkInterface networkInterface = NetworkInterface
                .getByInetAddress(ipAddress);
        byte[] macAddressBytes = networkInterface.getHardwareAddress();
        StringBuilder macAddressBuilder = new StringBuilder();

        for (int macAddressByteIndex = 0; macAddressByteIndex < macAddressBytes.length; macAddressByteIndex++)
        {
            String macAddressHexByte = String.format("%02X",
                    macAddressBytes[macAddressByteIndex]);
            macAddressBuilder.append(macAddressHexByte);

            if (macAddressByteIndex != macAddressBytes.length - 1)
            {
                macAddressBuilder.append(":");
            }
        }

        return macAddressBuilder.toString();
    }
    private static boolean testInet(String site) {
        Socket sock = new Socket();
        InetSocketAddress addr = new InetSocketAddress(site,80);
        try {
            sock.connect(addr,3000);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            try {sock.close();}
            catch (IOException e) {}
        }
    }
    public static void main(String[] args){
        String key = "57a49ece72e10";
        try {

            deactivateKey(key);
            activateKey(key);
            isKeyValid(key);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
    }
}

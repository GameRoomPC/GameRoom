package data.http.key;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;
import system.application.GameRoomUpdater;
import system.application.settings.PredefinedSetting;
import ui.Main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Enumeration;

import static system.application.settings.PredefinedSetting.SUPPORTER_KEY;
import static ui.Main.GENERAL_SETTINGS;
import static ui.Main.LOGGER;
import static ui.Main.SUPPORTER_MODE;

/**
 * Created by LM on 05/08/2016.
 */
public class KeyChecker {
    public final static String API_URL = "https://gameroom.me";
    private final static String VALIDATION_KEY = "57a0dc1fa9b0d0.08408190";
    private final static boolean DEBUGGING = false;

    //fields in json response
    public final static String FIELD_RESULT = "result";
    public final static String FIELD_MESSAGE = "message";
    private final static String FIELD_STATUS = "status";
    private final static String FIELD_REGISTERED_DOMAINS = "registered_domains";
    private final static String FIELD_REGISTERED_DOMAIN = "registered_domain";

    //possible results
    public final static String RESULT_SUCCESS = "success";
    public final static String RESULT_ERROR = "error";

    //possible status
    private final static String STATUS_ACTIVE = "active";
    public final static String STATUS_PENDING = "pending";
    public final static String STATUS_BLOCKED = "blocked";
    public final static String STATUS_EXPIRED = "expired";

    public static JSONObject deactivateKey(String key) throws IOException, UnirestException {
        HttpResponse<String> response = Unirest.post(API_URL)
                .field("secret_key", VALIDATION_KEY)
                .field("slm_action", "slm_deactivate")
                .field("registered_domain", getAllMACAddresses())
                .field("license_key", key)
                .asString();


        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getRawBody(), "UTF-8"));
        String json = reader.readLine();
        if (DEBUGGING) {
            LOGGER.debug("deactivateKey response : " + json);
        }
        return new JSONObject(json);
    }

    public static JSONObject activateKey(String key) throws IOException, UnirestException {
        HttpResponse<String> response = Unirest.post(API_URL)
                .field("secret_key", VALIDATION_KEY)
                .field("slm_action", "slm_activate")
                .field("registered_domain", getAllMACAddresses())
                .field("license_key", key)
                .asString();


        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getRawBody(), "UTF-8"));
        String json = reader.readLine();
        if (DEBUGGING) {
            LOGGER.debug("activateKey response : " + json);
        }
        return new JSONObject(json);
    }

    public static boolean isKeyValid(String key) {
        if (!testInet("igdb.com") && !testInet(API_URL.replace("https://", ""))) {
            LOGGER.error("KeyChecker : IGDB not joinable");
            return false;
        }
        try {
            JSONObject response = askKeyValid(key);
            if (response != null) {
                if (response.getString(FIELD_RESULT).equals(RESULT_SUCCESS)) {
                    String status = response.getString(FIELD_STATUS);
                    if (status.equals(STATUS_ACTIVE)) {
                        JSONArray registeredDomains = response.getJSONArray(FIELD_REGISTERED_DOMAINS);

                        boolean found = false;
                        for (int i = 0; i < registeredDomains.length() && !found; i++) {
                            found = registeredDomains.getJSONObject(i).getString(FIELD_REGISTERED_DOMAIN).contains(getMACAddress());
                        }
                        if(found){
                            LOGGER.info("KeyChecker : Supporter mode activated!");
                        }else{
                            LOGGER.info("KeyChecker : invalid MAC Address, "+getMACAddress()/*+". Valid addresses are : "*/);
                            /*for (int i = 0; i < registeredDomains.length() && !found; i++) {
                                Main.LOGGER.info("\t"+registeredDomains.getJSONObject(i).getString(FIELD_REGISTERED_DOMAIN));
                            }*/
                        }
                        return found;
                    }else {
                        LOGGER.error("KeyChecker : "+response.toString());
                    }
                } else {
                    LOGGER.error("KeyChecker : "+response.toString());
                }
            }else{
                LOGGER.info("KeyChecker : received null");
            }
        } catch (Exception e) {
            if(e.toString().contains("org.apache.http.conn.ConnectTimeoutException")){
                LOGGER.error("[KeyChecker] gameroom.me not reachable");
            }
            e.printStackTrace();
        }
        return false;
    }

    private static JSONObject askKeyValid(String key) throws IOException, UnirestException {
        HttpResponse<String> response = Unirest.post(API_URL)
                .field("secret_key", VALIDATION_KEY)
                .field("slm_action", "slm_check")
                .field("license_key", key)
                .asString();


        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getRawBody(), "UTF-8"));
        String json = reader.readLine();
        if (DEBUGGING) {
            LOGGER.debug("isKeyValid response : " + json);
        }
        return new JSONObject(json);
    }

    private static String getAllMACAddresses() throws SocketException {
        String MACAddresses = " ";
        Enumeration<NetworkInterface> networkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();
        while(networkInterfaceEnumeration.hasMoreElements()){
            NetworkInterface networkInterface = networkInterfaceEnumeration.nextElement();
            byte[] macAddressBytes = networkInterface.getHardwareAddress();
            if(macAddressBytes!=null) {
                StringBuilder macAddressBuilder = new StringBuilder();

                for (int macAddressByteIndex = 0; macAddressByteIndex < macAddressBytes.length; macAddressByteIndex++) {
                    String macAddressHexByte = String.format("%02X",
                            macAddressBytes[macAddressByteIndex]);
                    macAddressBuilder.append(macAddressHexByte);

                    if (macAddressByteIndex != macAddressBytes.length - 1) {
                        macAddressBuilder.append(":");
                    }
                }
                if(!macAddressBuilder.toString().equals("00:00:00:00:00:00:00:E0") && !MACAddresses.contains(macAddressBuilder.toString())){
                    MACAddresses += macAddressBuilder.toString() + ",";
                }
            }
        }
        return MACAddresses.substring(0,MACAddresses.length()-1);
    }
    private static String getMACAddress() throws UnknownHostException,
            SocketException {

        InetAddress ipAddress = InetAddress.getLocalHost();
        NetworkInterface networkInterface = NetworkInterface
                .getByInetAddress(ipAddress);
        byte[] macAddressBytes = networkInterface.getHardwareAddress();
        StringBuilder macAddressBuilder = new StringBuilder();

        for (int macAddressByteIndex = 0; macAddressByteIndex < macAddressBytes.length; macAddressByteIndex++) {
            String macAddressHexByte = String.format("%02X",
                    macAddressBytes[macAddressByteIndex]);
            macAddressBuilder.append(macAddressHexByte);

            if (macAddressByteIndex != macAddressBytes.length - 1) {
                macAddressBuilder.append(":");
            }
        }

        return macAddressBuilder.toString();
    }

    public static boolean testInet(String site) {
        Socket sock = new Socket();
        InetSocketAddress addr = new InetSocketAddress(site, 80);
        try {
            sock.connect(addr, 3000);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            try {
                sock.close();
            } catch (IOException e) {
            }
        }
    }

    public static void main(String[] args) {
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

    public static boolean assumeSupporterMode(){
        String supporterKey = GENERAL_SETTINGS.getString(PredefinedSetting.SUPPORTER_KEY);
        boolean valid = false;
        if(supporterKey == null || supporterKey.isEmpty()){
            valid = false;
        }else if(KeyChecker.testInet(GameRoomUpdater.HTTPS_HOST)){
            valid = KeyChecker.isKeyValid(supporterKey);
        }else{
            valid = supporterKey.startsWith("326b70lt")
                    || supporterKey.equals("5866fdd8b5dc1")
                    || supporterKey.equals("586be5b151ba0")
                    || supporterKey.equals("586d4c24d2ea2");
        }
        Main.SUPPORTER_MODE = valid;
        return valid;
    }
}

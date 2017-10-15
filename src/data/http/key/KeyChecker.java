package data.http.key;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;
import system.application.GameRoomUpdater;
import system.application.settings.PredefinedSetting;
import system.os.WinReg;
import ui.Main;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;

import static system.application.settings.GeneralSettings.settings;
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

    private final static String MESSAGE_DOMAIN_ALREADY_INACTIVE = "The license key on this domain is already inactive";

    public static boolean attemptingUUIDUpdate = false;


    public static JSONObject deactivateKey(String key) throws IOException, UnirestException {
        String guid = WinReg.readHWGUID();
        if (guid == null || guid.isEmpty()) {
            LOGGER.debug("KeyChecker : empty guid, will use mac deactivation");
            return deactivateKey(key, getAllMACAddresses());
        }
        return deactivateKey(key, guid);
    }

    private static JSONObject deactivateKey(String key, String uuid) throws IOException, UnirestException {
        HttpResponse<JsonNode> response = Unirest.post(API_URL)
                .field("secret_key", VALIDATION_KEY)
                .field("slm_action", "slm_deactivate")
                .field("registered_domain", uuid)
                .field("license_key", key)
                .asJson();


        if (response.getBody() != null && response.getBody().getObject() != null) {
            JSONObject obj = response.getBody().getObject();
            if (DEBUGGING) {
                LOGGER.debug("deactivateKey response : " + obj.toString(4));
            }
            return obj;
        }
        return null;
    }

    public static JSONObject activateKey(String key) throws IOException, UnirestException {
        String guid = WinReg.readHWGUID();
        if (guid == null || guid.isEmpty()) {
            LOGGER.debug("KeyChecker : empty guid, will use mac activation");
            return activateKey(key, getAllMACAddresses(), true);
        }
        return activateKey(key, guid, true);
    }

    private static JSONObject activateKey(String key, String uuid, boolean checkValidFirst) throws IOException, UnirestException {
        if (checkValidFirst && isKeyValid(key, uuid)) {
            LOGGER.debug("KeyChecker : key already activated, validating");
            //this allows the user to reactivate a key on the same device !
            JSONObject obj = new JSONObject();
            obj.put(FIELD_RESULT, RESULT_SUCCESS);
            obj.put(FIELD_MESSAGE, "License_key_activated");
            return obj;
        }
        HttpResponse<JsonNode> response = Unirest.post(API_URL)
                .field("secret_key", VALIDATION_KEY)
                .field("slm_action", "slm_activate")
                .field("registered_domain", uuid)
                .field("license_key", key)
                .asJson();

        if (response.getBody() != null && response.getBody().getObject() != null) {
            JSONObject obj = response.getBody().getObject();
            if (DEBUGGING) {
                LOGGER.debug("activateKey response : " + obj.toString(4));
            }
            return obj;
        }
        return null;
    }

    public static boolean isKeyValid(String key) {
        String guid = WinReg.readHWGUID();
        if (guid == null || guid.isEmpty()) {
            LOGGER.debug("KeyChecker : empty guid, will use mac validation");
            try {
                return isKeyValid(key, getAllMACAddresses());
            } catch (SocketException e) {
                e.printStackTrace();
                return false;
            }
        }
        return isKeyValid(key, guid);
    }

    public static boolean isKeyValid(String key, String guid) {
        if (!testInet(API_URL.replace("https://", ""))) {
            LOGGER.error("KeyChecker : GameRoom not reachable");
            return false;
        }
        if (guid == null || guid.isEmpty()) {
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
                        boolean isUsingMACUUID = false;
                        for (int i = 0; i < registeredDomains.length() && !found; i++) {
                            String uuid = registeredDomains.getJSONObject(i).getString(FIELD_REGISTERED_DOMAIN);
                            isUsingMACUUID = isUsingMACUUID || uuid.contains(":");
                            if (isUsingMACUUID) {
                                found = uuid.contains(getMACAddress());
                            } else {
                                found = uuid.contains(guid);
                            }
                        }
                        if (isUsingMACUUID && !attemptingUUIDUpdate) {
                            attemptingUUIDUpdate = true;
                            tryReplaceMACPerGUID();
                        }

                        if (found) {
                            LOGGER.info("KeyChecker : Supporter mode activated!");
                        } else {
                            LOGGER.info("KeyChecker : invalid uuid");
                        }
                        return found;
                    } else {
                        LOGGER.error("KeyChecker : " + response.toString());
                    }
                } else {
                    LOGGER.error("KeyChecker : " + response.toString());
                }
            } else {
                LOGGER.info("KeyChecker : received null");
            }
        } catch (Exception e) {
            if (e.toString().contains("org.apache.http.conn.ConnectTimeoutException") || e.getMessage().contains("java.net.SocketTimeoutException: Read timed out")) {
                LOGGER.error("[KeyChecker] gameroom.me not reachable");
                LOGGER.error(e.getMessage());
            } else {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static JSONObject askKeyValid(String key) throws IOException, UnirestException {
        HttpResponse<JsonNode> response = Unirest.post(API_URL)
                .field("secret_key", VALIDATION_KEY)
                .field("slm_action", "slm_check")
                .field("license_key", key)
                .asJson();


        if (response.getBody() != null && response.getBody().getObject() != null) {
            JSONObject obj = response.getBody().getObject();
            if (DEBUGGING) {
                LOGGER.debug("askKeyValid response : " + obj.toString(4));
            }
            return obj;
        }
        return null;
    }

    private static String getAllMACAddresses() throws SocketException {
        String MACAddresses = " ";
        Enumeration<NetworkInterface> networkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaceEnumeration.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaceEnumeration.nextElement();
            byte[] macAddressBytes = networkInterface.getHardwareAddress();
            if (macAddressBytes != null) {
                StringBuilder macAddressBuilder = new StringBuilder();

                for (int macAddressByteIndex = 0; macAddressByteIndex < macAddressBytes.length; macAddressByteIndex++) {
                    String macAddressHexByte = String.format("%02X",
                            macAddressBytes[macAddressByteIndex]);
                    macAddressBuilder.append(macAddressHexByte);

                    if (macAddressByteIndex != macAddressBytes.length - 1) {
                        macAddressBuilder.append(":");
                    }
                }
                if (!macAddressBuilder.toString().equals("00:00:00:00:00:00:00:E0") && !MACAddresses.contains(macAddressBuilder.toString())) {
                    MACAddresses += macAddressBuilder.toString() + ",";
                }
            }
        }
        return MACAddresses.substring(0, MACAddresses.length() - 1);
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

    public static boolean assumeSupporterMode() {
        if (SUPPORTER_MODE) {
            //we have already checked so no need to check again
            return true;
        }
        String supporterKey = settings().getString(PredefinedSetting.SUPPORTER_KEY);
        boolean valid = false;
        if (supporterKey == null || supporterKey.isEmpty()) {
            valid = false;
        } else if (KeyChecker.testInet(GameRoomUpdater.HTTPS_HOST)) {
            valid = KeyChecker.isKeyValid(supporterKey);
        } else {
            /*/if you are looking at this comment : yes you are a smarty one, congrats. Decompiling a .jar is so hard...*/
            valid = supporterKey.startsWith("326")
                    || supporterKey.equals("586be5b151ba0")
                    || supporterKey.equals("586d4c24d2ea2");
        }
        Main.SUPPORTER_MODE = valid;
        return valid;
    }

    public static void tryReplaceMACPerGUID() {
        LOGGER.debug("KeyChecker : will attempt to update uuid");
        if (DEBUGGING) {
            try {
                LOGGER.debug("KeyChecker MAC Addresses : " + getAllMACAddresses());
            } catch (SocketException ignored) {
            }
        }
        String guid = WinReg.readHWGUID();
        if (guid != null && !guid.isEmpty() && testInet(GameRoomUpdater.HTTPS_HOST)) {
            //System.out.println("MachineGUID : " + guid);
            String key = settings().getString(PredefinedSetting.SUPPORTER_KEY);
            try {
                JSONObject deactResponse = deactivateKey(key,getAllMACAddresses());
                if (deactResponse == null) {
                    LOGGER.error("KeyChecker : error deactivating uuid, null response");
                    return;
                } else {
                    switch (deactResponse.getString(KeyChecker.FIELD_RESULT)) {
                        case KeyChecker.RESULT_SUCCESS:
                            LOGGER.info("KeyChecker : successful deactivation of old uuid");
                            break;
                        case KeyChecker.RESULT_ERROR:
                            String message = deactResponse.getString(KeyChecker.FIELD_MESSAGE);
                            if(! message.equals(MESSAGE_DOMAIN_ALREADY_INACTIVE)){
                                LOGGER.error("KeyChecker : error deactivating old uuid, " + deactResponse.getString(KeyChecker.FIELD_MESSAGE));
                                return;
                            }
                        default:
                            break;
                    }
                }

                JSONObject actResponse = activateKey(key, guid,false);
                if (actResponse == null) {
                    LOGGER.error("KeyChecker : error updating uuid, null response");
                } else {
                    switch (actResponse.getString(KeyChecker.FIELD_RESULT)) {
                        case KeyChecker.RESULT_SUCCESS:
                            LOGGER.info("KeyChecker : successful update of uuid");
                            break;
                        case KeyChecker.RESULT_ERROR:
                            LOGGER.error("KeyChecker : error updating uuid, " + actResponse.getString(KeyChecker.FIELD_MESSAGE));
                            break;
                        default:
                            break;
                    }
                }
            } catch (IOException | UnirestException e) {
                LOGGER.error("KeyChecker : error updating uuid");
                e.printStackTrace();
            }
        }
    }
}

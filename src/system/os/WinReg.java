package system.os;

import ui.Main;

import java.io.File;
import java.io.IOException;

/**
 * @author LM. Garret (admin@gameroom.me)
 * @date 07/06/2017.
 */
public class WinReg {
    public final static String HKLM = "HKLM";

    public static String readString(String hkey, String key, String valueName) {
        Terminal terminal = new Terminal(false);
        try {
            String[] result = terminal.execute("reg query", hkey + "\\" + key, "/v", valueName);
            String lineStart = valueName + "    " + "REG_SZ    ";
            for (String line : result) {
                if (line.trim().startsWith(lineStart)) {
                    return line.trim().substring(lineStart.length());
                }
            }
        } catch (IOException e) {
            Main.LOGGER.error("WinReg: could not find reg key");
            e.printStackTrace();
        }
        return null;
    }

    public static String readDataPath() {
        String dataPath32 = readString(HKLM, "Software\\GameRoom", "DataPath");
        if (dataPath32 != null) {
            return dataPath32;
        }
        String dataPath64 = readString(HKLM, "Software\\WOW6432NODE\\GameRoom", "DataPath");
        if (dataPath64 != null) {
            return dataPath64;
        }
        String appdataFolder = System.getenv("APPDATA");
        return appdataFolder + File.separator + "GameRoom";
    }

    public static String readHWGUID() {
        String machineGuid32 = readString(HKLM, "SOFTWARE\\Microsoft\\Cryptography", "MachineGuid");
        if (machineGuid32 != null && !machineGuid32.isEmpty()) {
            return machineGuid32;
        }
        String machineGuid64 = readString(HKLM, "SOFTWARE\\WOW6432NODE\\Microsoft\\Cryptography", "MachineGuid");
        if (machineGuid64 != null && !machineGuid64.isEmpty()) {
            return machineGuid64;
        }
        return null;
    }
}

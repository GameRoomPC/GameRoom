package system.os;

import ui.Main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by LM on 14/07/2016.
 */
public class PowerMode {
    private UUID uuid;
    private String alias;

    public PowerMode(UUID uuid, String alias) {
        this.uuid = uuid;
        this.alias = alias;
    }
    public PowerMode(UUID uuid){
        this.uuid = uuid;
        this.alias = getAlias(uuid);
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getAlias() {
        return alias;
    }
    public void activate(){
        Terminal terminal = new Terminal();
        try {
            Main.LOGGER.info("Activating power mode : "+alias);
            terminal.execute("powercfg", "-setactive", uuid.toString());
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
    @Override
    public String toString(){
        return "Mode " + alias + " : uuid=" + uuid;
    }

    private static PowerMode readFromLine(String cmdLine) {
        if (cmdLine.contains("GUID")) {
            int guidStart = cmdLine.indexOf(":") + 2;
            int guidEnd = cmdLine.indexOf("  (");

            int aliasStart = cmdLine.indexOf("  (") + 3;
            int aliasEnd = cmdLine.indexOf(")");

            String uuid = cmdLine.substring(guidStart, guidEnd);
            String alias = cmdLine.substring(aliasStart, aliasEnd);

            return new PowerMode(UUID.fromString(uuid), alias);
        }
        return null;
    }

    private static ArrayList<PowerMode> readFromLines(String[] cmdLines) {
        ArrayList<PowerMode> result = new ArrayList<>();
        for (String line : cmdLines) {
            PowerMode mode = readFromLine(line);
            if (mode != null) {
                result.add(mode);
            }
        }
        return result;
    }

    public static ArrayList<PowerMode> getPowerModesAvailable() {
        Terminal terminal = new Terminal();
        try {
            String[] result = terminal.execute("powercfg", "-list");
            ArrayList<PowerMode> powerModes = readFromLines(result);

            Main.LOGGER.info("Available power modes : ");
            for(PowerMode pm : powerModes){
                Main.LOGGER.debug("\t-"+pm);
            }
            return powerModes;

        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return null;
    }
    public static PowerMode getActivePowerMode(){
        Terminal terminal = new Terminal();
        try {
            String[] result = terminal.execute("powercfg", "-getactivescheme");
            ArrayList<PowerMode> powerModes = readFromLines(result);

            for(PowerMode pm : powerModes){
                Main.LOGGER.info("Current power mode : "+pm.getAlias());
                return pm;
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return null;
    }
    private static String getAlias(UUID uuid){
        for(PowerMode pm : getPowerModesAvailable()){
            if(pm.getUuid().equals(uuid)){
                return pm.getAlias();
            }
        }
        return null;
    }

}
package com.gameroom.system.device;

import com.sun.istack.internal.Nullable;
import com.gameroom.data.game.entry.GameEntryUtils;
import edu.umd.cs.findbugs.annotations.NonNull;
import com.gameroom.system.os.Terminal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.StringJoiner;

import static com.gameroom.ui.Main.LOGGER;

/** A class containing methods used to collect some statistics on the device.
 *
 * @author LM. Garret (admin@gameroom.me)
 * @date 13/11/2017.
 */
public class StatsUtils {

    /**
     * List containing prefix of GPUs' names that we don't want to see (if there is an other GPU). See {@link #getGPUNames()}
     */
    private final static String[] EXCLUDED_GPU_PREFIX = new String[]{
            "LogMeIn Mirror Driver",
            "Intel(R) HD Graphics",
            "Rodzina mikrouk",
            "spacedesk Graphics Adapter",
            "mv video hook driver",
            "Microsoft Basic Display Adapter",
            "Intel(R) Iris(TM) Pro Graphics",
            "CyberLink Mirror Driver",
            "Standard VGA Graphics Adapter"
    };

    /**
     * Queries Windows to fetch the different GPU names. Filters out unwanted GPUs, following this politic : if there is
     * only one GPu, we return it anyway but if there is more, then we check {@link #EXCLUDED_GPU_PREFIX} to filter out
     * GPUs that we do not want to appear.
     *
     * @return a comma separated list of GPUs
     */
    @Nullable
    public static String getGPUNames() {
        Terminal t = new Terminal();
        StringJoiner joiner = new StringJoiner(",");

        ArrayList<String> gpuNames = new ArrayList<>();

        try {
            String[] output = t.execute("wmic", "path", "win32_VideoController", "get", "name");
            for (int i = 0; i < output.length; i++) {
                if (i > 1) {
                    if (output[i] != null && !output[i].isEmpty() && !output[i].startsWith("Name")) {
                        gpuNames.add(output[i].trim());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (String gpu : gpuNames) {
            if (gpuNames.size() == 1 || !isExcluded(gpu, EXCLUDED_GPU_PREFIX)) {
                joiner.add(gpu);
            }
        }

        return joiner.toString();
    }

    /**
     * Queries Windows to fetch the different CPU names
     *
     * @return a comma separated list of CPUs
     */
    public static String getCPUNames() {
        Terminal t = new Terminal();
        StringJoiner joiner = new StringJoiner(",");

        try {
            String[] output = t.execute("wmic", "cpu", "get", "name");
            for (int i = 0; i < output.length; i++) {
                if (i > 1) {
                    if (output[i] != null && !output[i].isEmpty() && !output[i].startsWith("Name")) {
                        joiner.add(output[i].trim());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return joiner.toString();
    }

    /**
     * @return a comma separated list of infos about the OS
     */
    public static String getOSInfo() {
        return System.getProperty("os.name") + ","
                + System.getProperty("os.version") + ","
                + System.getProperty("os.arch");
    }

    /**
     * @return the total playtime on every games
     */
    public static long getTotalPlaytime() {
        final long[] totalPlaytime = {0};
        GameEntryUtils.ENTRIES_LIST.forEach(gameEntry -> totalPlaytime[0] += gameEntry.getPlayTimeSeconds());
        return totalPlaytime[0];
    }

    /**
     * @return the RAM amount of the memorychip of the device, -1 if there was an error
     */
    public static long getRAMAmount() {
        Terminal t = new Terminal();
        try {
            String[] output = t.execute("wmic", "memorychip", "get", "capacity");
            long total = 0;
            for (int i = 0; i < output.length; i++) {
                if (i > 1) {
                    try {
                        total += Long.parseLong(output[i].trim()) / (1024 * 1024);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            return total;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return -1;
    }

    public static void printSystemInfo() {
        if (LOGGER != null) {
            LOGGER.info("System info :");
            LOGGER.info("\tOS : " + getOSInfo());
            LOGGER.info("\tGPU : " + getGPUNames());
            LOGGER.info("\tCPU : " + getCPUNames());
            LOGGER.info("\tRAM amount : " + getRAMAmount() + "Mb");
        }
    }

    private static boolean isExcluded(String value, @NonNull String[] excludedPrefix) {
        if (excludedPrefix.length == 0) {
            return false;
        }
        if (value == null || value.isEmpty()) {
            return true;
        }

        for (String anExcludedPrefix : excludedPrefix) {
            if (anExcludedPrefix != null && !anExcludedPrefix.isEmpty()) {
                if (value.startsWith(anExcludedPrefix)) {
                    return true;
                }
            }
        }
        return false;
    }
}

package com.gameroom.system.os;

import com.gameroom.data.game.entry.GameEntry;
import com.gameroom.ui.Main;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;

import static com.gameroom.ui.Main.LOGGER;

/**
 * Created by LM on 14/07/2016.
 */
public class Terminal {

    private final static String PATH_VARIABLE = "%p";
    private final static String ARGS_VARIABLE = "%a";
    private final static String NAME_VARIABLE = "%n";
    private final static String PLATFORM_VARIABLE = "%l";
    private final static String LAST_PLAYTIME_VARIABLE = "%d";
    private final static String TOTAL_PLAYTIME_VARIABLE = "%t";

    private ProcessBuilder processBuilder;
    private boolean redirectErrorStream = true;
    private Process process;
    //\s*([^"]\S*[^"]?|"[^"]*")\s*
    //([^"]\S*|".+?")\s*
    public final static Pattern CMD_SPLIT_PATTERN = Pattern.compile("\\s*([^\"]\\S*[^\"]?|\"[^\"]*\")\\s*");

    public Terminal() {
        this(true);
    }

    public Terminal(boolean redirectErrorStream) {
        this.redirectErrorStream = redirectErrorStream;
        processBuilder = new ProcessBuilder();
    }

    public void execute(String[] commands, File log) throws IOException {
        execute(commands, log, null);
    }

    public void execute(String[] commands, File log, File parentFile) throws IOException {
        processBuilder.inheritIO();
        if (parentFile != null && parentFile.exists()) {
            processBuilder.directory(parentFile);
        }
        if (log != null && log.exists()) {
            processBuilder.redirectOutput(log);
            processBuilder.redirectError(log);
        }
        processBuilder.command().addAll(Arrays.asList("cmd.exe", "/c", "chcp", "65001", "&"));
        Arrays.stream(commands).forEach(s -> {
            //ArrayList<String> cmds = splitCMDLine(s);
            processBuilder.command().addAll(Arrays.asList("cmd.exe", "/c", "\"" + s + "\"", "&"));
        });
        Process process = processBuilder.start();
        Main.getExecutorService().submit(() -> {
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            process.destroy();
            LOGGER.debug("Terminal : killed created process");
        });
    }

    /**
     * Executes a given PowerShell command. Can be used to parse result of max 500 char per line (PowerShell line buffer
     * limitation)
     *
     * @param command the PowerShell command to execute
     * @return the output of the command
     * @throws IOException in case an error occurred.
     */
    public String[] executePowerShell(String command) throws IOException {
        return execute("powershell.exe", "-Command", "mode con:cols=150 lines=50;" + command);
    }

    public String[] execute(String command, String... args) throws IOException {
        StringBuilder cmdLine = new StringBuilder(command);
        for (String arg : args) {
            cmdLine.append(" ").append(arg);
        }

        ArrayList<String> commands = new ArrayList<String>();

        commands.addAll(Arrays.asList("cmd.exe", "/c", "chcp", "65001", "&", "cmd.exe", "/c", command));
        Collections.addAll(commands, args);
        processBuilder.command(commands);

        process = processBuilder.start();

        BufferedReader stdInput =
                new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(process.getErrorStream()));

        String s = "";
        // read any errors from the attempted command
        if (redirectErrorStream) {
            while ((s = stdError.readLine()) != null) {
                System.err.println("[cmd=\"" + cmdLine.toString() + "\"] " + s);
            }
        }
        String[] result = stdInput.lines().toArray(size -> new String[size]);

        stdError.close();
        stdInput.close();
        process.destroy();
        return result;
    }

    public Process getProcess() {
        return process;
    }

    /**
     * Splits a given cmd line using the ' ' separator, excepting when it is surrounded by '"' char.
     *
     * @param line line of command line to split
     * @return a String array containing separated elements of the command line
     */
    public static ArrayList<String> splitCMDLine(String line) {
        ArrayList<String> strings = new ArrayList<>();
        if (line == null || line.isEmpty()) {
            return strings;
        }
        char previous = ' ';
        char cur = ' ';
        int openedQuotes = 0;

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            previous = cur;
            cur = line.charAt(i);
            if (cur == '"' && previous != '\\') {
                if (openedQuotes == 0) {
                    openedQuotes++;
                } else {
                    openedQuotes--;
                    strings.add(builder.toString().trim());
                    builder.setLength(0); // clears the buffer
                }
            } else if (Character.isWhitespace(cur) && !Character.isWhitespace(previous) && openedQuotes == 0) {
                if (!builder.toString().trim().isEmpty()) {
                    strings.add(builder.toString().trim());
                }
                builder.setLength(0); // clears the buffer
            } else {
                builder.append(cur);
            }
        }
        strings.add(builder.toString());
        builder.setLength(0);

        return strings;
    }

    /**
     * Unquotes a given string. For example, receiving ""test"" will output "test" (the remaining are the one to define a
     * String but the value of the string will be test).
     * <p>
     * This is needed for the {@link ProcessBuilder} to execute {@link ProcessBuilder#command(String...)}
     *
     * @param s String to unqote
     * @return the unqoted string
     */
    private static String unquote(String s) {
        if (s == null || s.isEmpty() || s.length() < 2) {
            return s;
        }
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    /**
     * Replaces GameRoom variables with their values in the given command line
     * @param cmdLine the commandline to edit
     * @param entry the entry used to edit the commandline
     * @return an updated commandline with variables updated with their values
     */
    public static String replaceGameRoomVariables(String cmdLine, GameEntry entry) {
        if (entry == null ||cmdLine == null || cmdLine.isEmpty()) {
            return cmdLine;
        }
        String temp = cmdLine;
        temp = temp.replace(PATH_VARIABLE, entry.getPath() == null ? "" : "\"" + entry.getPath() + "\"")
            .replace(ARGS_VARIABLE, entry.getArgs() == null ? "" : entry.getArgs())
            .replace(NAME_VARIABLE, entry.getName() == null ? "" : "\"" + entry.getName() + "\"")
            .replace(PLATFORM_VARIABLE, entry.getPlatform() == null ? "" : "\"" + entry.getPlatform().getName() + "\"")
            .replace(LAST_PLAYTIME_VARIABLE, Long.toString(entry.getLastPlayTime()))
            .replace(TOTAL_PLAYTIME_VARIABLE, Long.toString(entry.getPlayTimeSeconds()));
        return temp;
    }

}

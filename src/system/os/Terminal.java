package system.os;

import ui.Main;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Created by LM on 14/07/2016.
 */
public class Terminal {
    private ProcessBuilder processBuilder;
    private boolean redirectErrorStream = true;

    public Terminal(){
        this(true);
    }
    public Terminal(boolean redirectErrorStream) {
        this.redirectErrorStream = redirectErrorStream;
        processBuilder = new ProcessBuilder();
    }

    public void execute(String[] commands, File log) {
        execute(commands, log, null);
    }

    public void execute(String[] commands, File log, File parentFile) {
        processBuilder.inheritIO();
        if (parentFile != null) {
            processBuilder.directory(parentFile);
        }
        processBuilder.redirectOutput(log);
        processBuilder.redirectError(log);
        processBuilder.command().addAll(Arrays.asList("cmd.exe", "/c", "chcp", "65001", "&"));
        for (int i = 0; i < commands.length; i++) {
            processBuilder.command().addAll(Arrays.asList("cmd.exe", "/c", commands[i], "&"));
        }
        try {
            Process preProcess = processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String[] execute(String command, String... args) throws IOException {
        ArrayList<String> commands = new ArrayList<String>();
        commands.addAll(Arrays.asList("cmd.exe", "/c", "chcp", "65001", "&", "cmd.exe", "/c", command));
        Collections.addAll(commands, args);
        processBuilder.command(commands);

        Process process = processBuilder.start();

        BufferedReader stdInput =
                new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(process.getErrorStream()));

        String s = "";
        // read any errors from the attempted command
        if(redirectErrorStream){
            while ((s = stdError.readLine()) != null) {
                Main.LOGGER.error("[cmd=" + command + "] " + s);
            }
        }
        String[] result = stdInput.lines().toArray(size -> new String[size]);

        stdError.close();
        stdInput.close();
        process.destroy();
        return result;
    }
}

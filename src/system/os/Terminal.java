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
    private Process process;

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
        if (parentFile != null) {
            processBuilder.directory(parentFile);
        }
        processBuilder.redirectOutput(log);
        processBuilder.redirectError(log);
        processBuilder.command().addAll(Arrays.asList("cmd.exe", "/c", "chcp", "65001", "&"));
        for (int i = 0; i < commands.length; i++) {
            processBuilder.command().addAll(Arrays.asList("cmd.exe", "/c", commands[i], "&"));
        }
        Process preProcess = processBuilder.start();
    }

    public String[] execute(String command, String... args) throws IOException {
        StringBuilder cmdLine = new StringBuilder(command);
        for(String arg : args){
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

    public Process getProcess(){
        return process;
    }
}

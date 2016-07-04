package data;

import UI.gamebuttons.GameButton;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;
import java.util.UUID;

/**
 * Created by LM on 03/07/2016.
 */
public class AllGameEntries {

    public void appendEntry(GameEntry entry) throws IOException {
        File file = entriesFile();
        Scanner scanner = new Scanner(file);
        //now read the file line by line...
        boolean containsUUID = false;
        while (scanner.hasNextLine() && !containsUUID) {
            String line = scanner.nextLine();
            containsUUID = line.contains(entry.getUuid().toString());
        }
        scanner.close();
        if(!containsUUID){
            FileWriter writer = new FileWriter(file,true);
            writer.write(entry.getUuid().toString() + System.getProperty("line.separator"));
            writer.close();
        }
    }
    public ArrayList<UUID> readUUIDS(){
        ArrayList<UUID> uuids = new ArrayList<>();
        File file = null;
        try {
            file = entriesFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Scanner scanner = null;
        try {
            scanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        //now read the file line by line...
        int lineNum = 0;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            uuids.add(UUID.fromString(line));
            lineNum++;
        }
        return uuids;
    }

    public boolean removeEntry(GameEntry entry) throws IOException {
        File inputFile = entriesFile();
        File tempFile = new File("temp_entries");

        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

        String lineToRemove = entry.getUuid().toString();
        String currentLine;

        while ((currentLine = reader.readLine()) != null) {
            // trim newline when comparing with lineToRemove
            String trimmedLine = currentLine.trim();
            if (trimmedLine.equals(lineToRemove)) continue;
            writer.write(currentLine + System.getProperty("line.separator"));
        }
        writer.close();
        reader.close();
        return tempFile.renameTo(inputFile);
    }

    private File entriesFile() throws IOException {
        File file = new File("entries");
        if (!file.exists()) {
            file.createNewFile();
        }
        return file;
    }

    public static void main(String[] args) {

    }
}

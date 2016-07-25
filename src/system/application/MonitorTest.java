package system.application;

import data.game.GameEntry;
import ui.Main;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by LM on 24/07/2016.
 */
public class MonitorTest {

    public static void main(String[] args) throws ParseException {
        try {
            Monitor m = new Monitor("notepad++.exe", "Notepad");
            Main.logger.info("Up since : "+ GameEntry.getPlayTimeFormatted(m.computeTrueRunningTime(),GameEntry.TIME_FORMAT_FULL_DOUBLEDOTS));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

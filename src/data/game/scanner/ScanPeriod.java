package data.game.scanner;

import ui.Main;

/**
 * Created by LM on 07/01/2017.
 */
public enum ScanPeriod {
    TEN_MINUTES(0, 10), HALF_HOUR(0, 30), HOUR(1, 0), FIVE_HOURS(5, 0), TEN_HOURS(10, 0), START_ONLY(-1, -1);

    private final static int ONLY_START_CONSTANT = -1;
    private int hours;
    private int minutes;

    ScanPeriod(int hours, int minutes) {
        this.hours = hours;
        this.minutes = minutes;
    }

    public static ScanPeriod fromString(String s){
        for(ScanPeriod period : ScanPeriod.values()){
            if(s.equals(period.toString())){
                return period;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        String s = "";
        if (minutes > 0) {
            if (minutes == 1) {
                s = minutes + " " + Main.getString("minute") + " " + s;
            } else {
                s = minutes + " " + Main.getString("minutes") + " " + s;
            }
        }
        if (hours > 0) {
            if (hours == 1) {
                s = hours + " " + Main.getString("hour") + " " + s;
            } else {
                s = hours + " " + Main.getString("hours") + " " + s;
            }
        }
        if (minutes == ONLY_START_CONSTANT || hours == ONLY_START_CONSTANT) {
            s = Main.getString("only_at_start");
        }
        return s.trim();
    }

    public int toMillis() {
        if (minutes == ONLY_START_CONSTANT || hours == ONLY_START_CONSTANT) {
            return -1;
        }

        return minutes * 60 * 1000 + hours * 60 * 60 * 1000;
    }
}

package ui;

import javafx.geometry.Insets;

/**
 * @author LM. Garret (admin@gameroom.me)
 * @date 16/10/2017.
 */
public enum UIValues {
    CONTROL_HUGE(Constants.OFFSET_HUGE),
    CONTROL_BIG(Constants.OFFSET_BIG),
    CONTROL_MEDIUM(Constants.OFFSET_MEDIUM),
    CONTROL_SMALL(Constants.OFFSET_SMALL);

    private int[] values = new int[4];

    UIValues(int offset) {
        for (int i = 0; i < values.length; i++) {
            values[i] = offset;
        }
    }

    UIValues(int top, int right, int bottom, int left) {
        values[0] = top;
        values[1] = right;
        values[2] = bottom;
        values[3] = left;
    }

    public Insets insets() {
        return new Insets(
                values[0] * Constants.getScreenFactor(),
                values[1] * Constants.getScreenFactor(),
                values[2] * Constants.getScreenFactor(),
                values[3] * Constants.getScreenFactor()
        );
    }

    public static class Constants {
        private final static int OFFSET_NONE = 0;
        private final static int OFFSET_SMALL = 10;
        private final static int OFFSET_MEDIUM = 20;
        private final static int OFFSET_BIG = 30;
        private final static int OFFSET_HUGE = 50;


        public static double offsetSmall() {
            return OFFSET_SMALL * getScreenFactor();
        }

        public static double offsetMedium() {
            return OFFSET_MEDIUM * getScreenFactor();
        }

        public static double offsetNone() {
            return OFFSET_NONE * getScreenFactor();
        }

        public static double getOffsetBig() {
            return OFFSET_BIG * getScreenFactor();
        }

        public static double offsetHuge() {
            return OFFSET_HUGE * getScreenFactor();
        }


        private static double getScreenFactor() {
            return Main.SCREEN_WIDTH / 1920;
        }

    }
}



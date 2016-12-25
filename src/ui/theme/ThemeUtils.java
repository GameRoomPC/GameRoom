package ui.theme;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import ui.Main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by LM on 26/10/2016.
 */
public class ThemeUtils {
    private final static String DEFAULT_THEME_CSS = "res/theme.css";

    private static String getThemeCSS() {
        File themeCSS = Main.FILES_MAP.get("theme_css");
        if (themeCSS == null || !themeCSS.exists()) {
            return DEFAULT_THEME_CSS;
        }
        return "file:///"+themeCSS.getPath().replace("\\","/");
    }

    public static void applyCurrentTheme(Parent parent) {
        parent.getStylesheets().add(getThemeCSS());
    }

    public static void applyCurrentTheme(Scene scene) {
        applyCurrentTheme(scene.getRoot());
    }

    public static void applyCurrentTheme(Alert alert) {
        applyCurrentTheme(alert.getDialogPane());
    }

    public List<Theme> getInstalledThemes(){
        //TODO scan for valid themes and return them
        return new ArrayList<>();
    }
}

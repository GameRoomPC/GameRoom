package ui.theme;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import ui.Main;

import java.io.File;

/**
 * Created by LM on 26/10/2016.
 */
public class ThemeUtils {
    private static String getThemeCSS() {
        File themeCSS = Main.FILES_MAP.get("theme_css");
        if (themeCSS == null || !themeCSS.exists()) {
            return Main.FILES_MAP.get("default_css").getPath();
        }
        return themeCSS.getPath();
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
}

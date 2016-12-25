package ui.theme;

import data.FileUtils;
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
    private final static List<Theme> INSTALLED_THEMES = new ArrayList<>();

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

    public static List<Theme> getInstalledThemes(){
        INSTALLED_THEMES.clear();
        INSTALLED_THEMES.add(Theme.DEFAULT_THEME);
        File themeFolder = Main.FILES_MAP.get("themes");
        for(File file : themeFolder.listFiles()){
            Theme tempTheme = new Theme(file.getName());
            if(tempTheme.isValid()){
                INSTALLED_THEMES.add(tempTheme);
            }
        }
        return INSTALLED_THEMES;
    }

    public static Theme getThemeFromName(String name){
        if(name == null || name.isEmpty() || name.equals("GameRoom")){
            return Theme.DEFAULT_THEME;
        }
        for(Theme theme : INSTALLED_THEMES){
            if(theme.getName().equals(name)){
                return theme;
            }
        }
        return Theme.DEFAULT_THEME;
    }
}

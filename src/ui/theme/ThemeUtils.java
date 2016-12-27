package ui.theme;

import data.FileUtils;
import data.http.URLTools;
import data.http.key.KeyChecker;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import system.application.settings.PredefinedSetting;
import ui.Main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static ui.Main.GENERAL_SETTINGS;
import static ui.Main.LOGGER;
import static ui.Main.SUPPORTER_MODE;

/**
 * Created by LM on 26/10/2016.
 */
public class ThemeUtils {
    private final static String DEFAULT_THEME_CSS = "res/theme.css";
    private final static List<Theme> INSTALLED_THEMES = new ArrayList<>();
    private final static String SERVER_URL = "https://gameroom.me";

    private static String getThemeCSS() {
        File themeCSS = Main.FILES_MAP.get("theme_css");
        Theme currentTheme = Main.GENERAL_SETTINGS.getTheme();

        if(!SUPPORTER_MODE){
            //FileUtils.deleteFolder(Main.FILES_MAP.get("current_theme"));
            //Main.GENERAL_SETTINGS.setSettingValue(PredefinedSetting.THEME,Theme.DEFAULT_THEME);
            return DEFAULT_THEME_CSS;
        }
        if(themeCSS == null || !themeCSS.exists() || currentTheme.equals(Theme.DEFAULT_THEME)){
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

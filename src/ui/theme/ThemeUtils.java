package ui.theme;

import data.http.key.KeyChecker;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import net.lingala.zip4j.exception.ZipException;
import ui.Main;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static system.application.settings.GeneralSettings.settings;
import static ui.Main.LOGGER;

/**
 * Created by LM on 26/10/2016.
 */
public class ThemeUtils {
    private final static String DEFAULT_THEME_CSS = "res/theme.css";
    private final static List<Theme> INSTALLED_THEMES = new ArrayList<>();
    private final static String SERVER_URL = "https://gameroom.me";
    private static boolean hasCheckedForThemeUpdates = false;

    private static String getThemeCSS() {
        File themeCSS = Main.FILES_MAP.get("theme_css");
        Theme currentTheme = settings().getTheme();

        if(!KeyChecker.assumeSupporterMode()){
            return DEFAULT_THEME_CSS;
        }
        if(themeCSS == null || !themeCSS.exists() || currentTheme.equals(Theme.DEFAULT_THEME)){
            return DEFAULT_THEME_CSS;
        }
        return "file:///"+themeCSS.getPath().replace("\\","/");
    }

    public static void applyCurrentTheme(Parent parent) {
        if(!hasCheckedForThemeUpdates){
            Theme updatedTheme = checkForUpdatesOfCurrentTheme();
            if(updatedTheme!=null){
                try {
                    updatedTheme.applyTheme();
                } catch (IOException | ZipException e) {
                    e.printStackTrace();
                }
            }
            hasCheckedForThemeUpdates = true;
        }
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
        File[] children = themeFolder.listFiles();
        if(children!=null){
            for(File file : children){
                if(!file.isDirectory() && file.getName().toLowerCase().contains(".zip")){
                    Theme tempTheme = new Theme(file.getName());
                    if(tempTheme.isValid()){
                        INSTALLED_THEMES.add(tempTheme);
                    }
                }
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

    private static Theme readCurrentTheme() throws IOException {
        File currentThemeFolder = Main.FILES_MAP.get("current_theme");
        if(currentThemeFolder==null || !currentThemeFolder.exists()){
            throw new FileNotFoundException();
        }

        File propertiesFile = new File(currentThemeFolder.getPath()+File.separator+"info.properties");
        if(propertiesFile==null || !propertiesFile.exists()){
            throw new FileNotFoundException();
        }

        InputStream stream = new FileInputStream(propertiesFile);
        return Theme.readConfig(stream);
    }

    private static Theme checkForUpdatesOfCurrentTheme(){
        try {
            Theme currentTheme = readCurrentTheme();
            for(Theme theme : getInstalledThemes()){
                if(theme.getName().equals(currentTheme.getName()) && theme.getAuthor().equals(currentTheme.getAuthor())){
                    int compareason = theme.compareVersion(currentTheme);
                    if(compareason == 1){
                        LOGGER.info("Theme \""+theme.getName()+"\" has a new version to apply : "+theme.getVersion());
                        return theme;
                    }
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }
}

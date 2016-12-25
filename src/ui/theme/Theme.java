package ui.theme;

import javafx.scene.image.Image;
import ui.Main;

import java.io.File;
import java.util.Date;

/**
 * Created by LM on 24/12/2016.
 */
public class Theme {

    /**** META *****/
    private String name;
    private String author;
    private Date creationDate;
    private String version;
    private String description;
    private Image previewImage;

    /**** FILE SYSTEM ***/
    private String fileName;
    private File file;

    /**Constructor of a theme.
     *
     * @param fileName the name of the file containing the theme. Must end with a ".zip"
     */
    public Theme(String fileName){
        if(fileName == null){
            throw new IllegalArgumentException("Theme's filename was null");
        }
        if(!fileName.endsWith(".zip")){
            throw new IllegalArgumentException("Theme's file is not a .zip");
        }
        this.fileName = fileName;
        this.file = new File(Main.FILES_MAP.get("theme_css")+File.separator+fileName);
    }

    public boolean exists(){
        return file.exists();
    }

    public void loadFromZip(){
        //TODO load the info.properties and set fields corresponding
    }

    public void applyTheme() throws IllegalStateException{
        if(!exists()){
            throw new IllegalStateException("Can not apply theme, theme's file does not exist");
        }
        //TODO extract theme's zip into the "current" folder of the "themes" folder
    }
}

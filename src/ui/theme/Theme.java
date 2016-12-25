package ui.theme;

import data.FileUtils;
import javafx.scene.image.Image;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import ui.Main;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.ZipEntry;

/**
 * Created by LM on 24/12/2016.
 */
public class Theme {
    public final static Theme DEFAULT_THEME = new Theme();
    private final static String FILE_NAME_INFO = "info.properties";
    private final static String FILE_NAME_THEME = "theme.css";
    private final static String FILE_NAME_PREVIEW = "preview.jpg";

    private final static String PROPERTY_NAME = "name";
    private final static String PROPERTY_AUTHOR = "author";
    private final static String PROPERTY_DATE = "date";
    private final static String PROPERTY_VERSION = "version";
    private final static String PROPERTY_DESCRIPTION = "description";

    private final static SimpleDateFormat PROPERTY_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

    /**** META *****/
    private String name = "";
    private String author = "";
    private Date creationDate;
    private String version = "";
    private String description;

    /**** FILE SYSTEM ***/
    private String fileName;
    private File file;

    private boolean isDefaultTheme = false;

    /**
     * Constructor of a theme.
     *
     * @param fileName the name of the file containing the theme. Must end with a ".zip"
     */
    public Theme(String fileName) throws IllegalArgumentException {
        if (fileName == null) {
            throw new IllegalArgumentException("Theme's filename was null");
        }
        this.fileName = fileName;
        this.file = new File(Main.FILES_MAP.get("themes") + File.separator + fileName);

        loadFromZip();
    }

    private Theme(){
        this.isDefaultTheme = true;
        this.name = "GameRoom";
    }

    public boolean isValid() {
        if(isDefaultTheme){
            return true;
        }
        if (file.exists()) {
            try {
                ZipFile zipFile = new ZipFile(file);
                FileHeader infoHeader = zipFile.getFileHeader(FILE_NAME_INFO);
                FileHeader themeHeader = zipFile.getFileHeader(FILE_NAME_THEME);

                return (infoHeader != null && themeHeader != null);
            } catch (ZipException e) {
                Main.LOGGER.error("Could not open theme " + name);
                Main.LOGGER.error(e.toString());
                return false;
            }
        }
        return false;
    }

    public void loadFromZip() {
        if(isDefaultTheme){
            return;
        }
        try {
            if (isValid()) {
                java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(file);

                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                ZipEntry infoEntry = zipFile.getEntry(FILE_NAME_INFO);
                InputStream stream = zipFile.getInputStream(infoEntry);

                Properties properties = new Properties();
                properties.load(stream);
                this.author = properties.getProperty(PROPERTY_AUTHOR).replace("\"","");
                this.name = properties.getProperty(PROPERTY_NAME).replace("\"","");
                this.description = properties.getProperty(PROPERTY_DESCRIPTION).replace("\"","");
                this.version = properties.getProperty(PROPERTY_VERSION).replace("\"","");
                try {
                    this.creationDate = PROPERTY_DATE_FORMAT.parse(properties.getProperty(PROPERTY_DATE).replace("\"",""));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                stream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private Image getPreviewImage() throws IOException {
        if(isDefaultTheme){
            return null;
        }
        if(isValid()){
            java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(file);
            ZipEntry previewEntry = zipFile.getEntry(FILE_NAME_PREVIEW);
            if (previewEntry != null) {
                InputStream previewStream = zipFile.getInputStream(previewEntry);
                Image image= new Image(previewStream);
                previewStream.close();
                return image;
            }
        }
        return null;
    }

    public void applyTheme() throws IllegalStateException, IOException, ZipException {
        if (!isValid()) {
            throw new IllegalStateException("Can not apply theme, theme's file does not exist");
        }
        FileUtils.deleteFolder(Main.FILES_MAP.get("current_theme"));
        if(!isDefaultTheme) {
            extractAll(file, Main.FILES_MAP.get("current_theme"));
        }
    }

    private void extractAll(File fromZip, File toDirectory) throws IOException, ZipException {
        net.lingala.zip4j.core.ZipFile zipFile = new net.lingala.zip4j.core.ZipFile(fromZip);
        zipFile.extractAll(toDirectory.getAbsolutePath());
    }

    public String getName() {
        return name;
    }
}

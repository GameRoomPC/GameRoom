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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

/**
 * Created by LM on 24/12/2016.
 */
public class Theme {
    public final static Theme DEFAULT_THEME = new Theme();

    private final static Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.?");
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
    private transient Date creationDate;
    private transient String version = "";
    private transient String description;

    /**** FILE SYSTEM ***/
    private transient String fileName;
    private transient File file;

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

    private Theme() {
        this.isDefaultTheme = true;
        this.name = "GameRoom";
    }

    boolean isValid() {
        if (isDefaultTheme) {
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

    private void loadFromZip() {
        if (isDefaultTheme) {
            return;
        }
        try {
            if (isValid()) {
                java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(file);

                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                ZipEntry infoEntry = zipFile.getEntry(FILE_NAME_INFO);
                InputStream stream = zipFile.getInputStream(infoEntry);

                Theme configTheme = readConfig(stream);
                this.author = configTheme.author;
                this.name = configTheme.name;
                this.description = configTheme.description;
                this.version = configTheme.version;
                stream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    protected static Theme readConfig(InputStream stream) throws IOException {
        Theme theme = new Theme();
        theme.isDefaultTheme = false;
        Properties properties = new Properties();
        properties.load(stream);
        theme.author = properties.getProperty(PROPERTY_AUTHOR).replace("\"", "");
        theme.name = properties.getProperty(PROPERTY_NAME).replace("\"", "");
        theme.description = properties.getProperty(PROPERTY_DESCRIPTION).replace("\"", "");
        theme.version = properties.getProperty(PROPERTY_VERSION).replace("\"", "");

        try {
            theme.creationDate = PROPERTY_DATE_FORMAT.parse(properties.getProperty(PROPERTY_DATE).replace("\"", ""));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return theme;
    }

    private Image getPreviewImage() throws IOException {
        if (isDefaultTheme) {
            return null;
        }
        if (isValid()) {
            java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(file);
            ZipEntry previewEntry = zipFile.getEntry(FILE_NAME_PREVIEW);
            if (previewEntry != null) {
                InputStream previewStream = zipFile.getInputStream(previewEntry);
                Image image = new Image(previewStream);
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
        if (!isDefaultTheme) {
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

    /**
     * Compare versions of themes
     *
     * @param other
     * @return 0 if error or equals, 1 if this>other, -1 if other>this
     */
    public int compareVersion(Theme other) {
        int[] thisVersion = getVersionFromString(version);
        int[] otherVersion = getVersionFromString(other.version);

        if (otherVersion == null || thisVersion == null) {
            return 0;
        }

        int minLength = thisVersion.length < otherVersion.length ? thisVersion.length : otherVersion.length;

        int result = 0;
        for (int i = 0; i < minLength && result == 0; i++) {
            if (thisVersion[i] > otherVersion[i]) {
                result = 1;
            } else if (thisVersion[i] < otherVersion[i]) {
                result = -1;
            } else {
                result = 0;
            }
        }
        if (result == 0 && thisVersion.length > minLength) {
            result = 1;
        } else if (result == 0 && otherVersion.length > minLength) {
            result = -1;
        }
        return result;
    }

    private static int[] getVersionFromString(String version) {
        Matcher matcher = VERSION_PATTERN.matcher(version);
        int[] intVersion = null;
        ArrayList<Integer> integers = new ArrayList<>();

        while (matcher.find()) {
            try {
                int number = Integer.parseInt(matcher.group(1));
                integers.add(number);
            } catch (NumberFormatException ne) {
                return null;
            }

        }
        if(!integers.isEmpty()){
            intVersion = new int[integers.size()];

            int i = 0;
            for(Integer integer : integers){
                intVersion[i++] = integer;
            }
        }
        return intVersion;
    }

    public String getAuthor() {
        return author;
    }

    public String getVersion() {
        return version;
    }
}


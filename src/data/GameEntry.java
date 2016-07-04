package data;

import UI.Main;
import javafx.scene.Node;
import javafx.scene.image.Image;

import java.io.*;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.util.Properties;
import java.util.UUID;

import static UI.scene.MainScene.MAX_SCALE_FACTOR;
import static UI.scene.MainScene.MIN_SCALE_FACTOR;

/**
 * Created by LM on 02/07/2016.
 */
public class GameEntry {
    private final static String[] DEFAULT_IMAGES_PATHS = {"res/defaultImages/cover.jpg"};
    private final static int IMAGES_NUMBER = 3;

    private boolean savedLocally = false;

    private String name = "";
    private String year = "";
    private String description = "";
    private String editor = "";
    private String path = "";
    private UUID uuid;
    private String[] imagesPaths = new String[IMAGES_NUMBER];

    public GameEntry(String name) {
        uuid = UUID.randomUUID();
        this.name = name;
        try {
            Main.ALL_GAMES_ENTRIES.appendEntry(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public GameEntry(UUID uuid) {
        this.uuid = uuid;
        try {
            loadEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File propertyFile() throws IOException {
        File dir = new File(uuid.toString());
        if (!dir.exists()) {
            dir.mkdir();
        }
        File configFile = new File(uuid.toString() + File.separator + "entry.properties");
        if (!configFile.exists()) {
            configFile.createNewFile();
        }
        return configFile;
    }

    private void saveEntry() throws IOException {
        Properties prop = new Properties();
        OutputStream output =  new FileOutputStream(propertyFile());

        // set the properties value
        prop.setProperty("name", name);
        prop.setProperty("year", year);
        prop.setProperty("description", description);
        prop.setProperty("editor", editor);
        prop.setProperty("path", path);

        for (int i = 0; i < IMAGES_NUMBER; i++) {
            if (imagesPaths[i] != null) {
                prop.setProperty("image" + i,imagesPaths[i]);
            } else {
                break;
            }
        }
        // save properties to project root folder
        prop.store(output, null);

        output.close();

    }

    private void loadEntry() throws IOException {
        Properties prop = new Properties();
        InputStream input = null;

        input = new FileInputStream(propertyFile());

        // load a properties file
        prop.load(input);

        if (prop.getProperty("name") != null) {
            name = prop.getProperty("name");
        }
        if (prop.getProperty("year") != null) {
            year = prop.getProperty("year");
        }
        if (prop.getProperty("description") != null) {
            description = prop.getProperty("description");
        }
        if (prop.getProperty("editor") != null) {
            editor = prop.getProperty("editor");
        }
        if (prop.getProperty("path") != null) {
            path = prop.getProperty("path");
        }

        for (int i = 0; i < IMAGES_NUMBER; i++) {
            if (prop.getProperty("image" + i) != null) {
                imagesPaths[i] = prop.getProperty("image" + i);
            } else {
                break;
            }
        }

        input.close();

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        try {
            saveEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
        try {
            saveEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getEditor() {
        return editor;
    }

    public void setEditor(String editor) {
        this.editor = editor;
        try {
            saveEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getImagePath(int index) {
        if(index<imagesPaths.length){
            String result = imagesPaths[index] ;
            if (result == null){
                return DEFAULT_IMAGES_PATHS[index];
            }
            return result;
        }
        //TODO implement basic image if requested one is missing
        return DEFAULT_IMAGES_PATHS[0];
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
        try {
            saveEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        try {
            saveEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setImagePath(int index, String imagePath) {
        if (imagesPaths.length > index) {
            imagesPaths[index] = imagePath;
        }
        try {
            saveEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

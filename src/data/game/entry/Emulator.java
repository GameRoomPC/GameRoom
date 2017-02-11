package data.game.entry;

import ui.Main;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

/**
 * Created by LM on 11/02/2017.
 */
public enum Emulator {
    DOLPHIN("emulator_dolphin", Platform.WII, Platform.GAMECUBE);

    private String key;
    private ArrayList<Platform> platforms;
    private File path;

    public final static HashMap<Platform, Emulator> EMULATOR_MAPPING = new HashMap<>();

    Emulator(String key, Platform platform, Platform... platforms) {
        this.key = key;
        this.platforms = new ArrayList<>();
        this.platforms.add(platform);
        for (Platform p : platforms) {
            this.platforms.add(p);
        }
        try {
            loadPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPath() throws IOException {
        Properties prop = new Properties();
        InputStream input = null;

        input = new FileInputStream(propertyFile());

        // load a properties file
        prop.load(input);

        if (prop.getProperty(key+"_path") != null) {
            path = new File(prop.getProperty(key+"_path"));
        }
        input.close();
    }

    private void save() throws IOException {
        Properties prop = new Properties();
        OutputStream output = new FileOutputStream(propertyFile());
        prop.setProperty(key+"_path",path.getAbsolutePath());
        prop.store(output, null);

        output.close();
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    public String getDisplayName() {
        return Main.PLATFORM_BUNDLE.getString(key);
    }

    public File getPath() {
        return path;
    }

    public void setPath(File path) {
        this.path = path;
        try {
            save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File propertyFile() throws IOException {
        File configFile = new File(Main.FILES_MAP.get("working_dir") + File.separator + "emulators.properties");
        if (!configFile.exists()) {
            configFile.createNewFile();
        }
        return configFile;
    }

    public List<String> getCommandsToExecute(GameEntry entry){
        ArrayList<String> cmds = new ArrayList<>();
        switch (this){
            case DOLPHIN:
                cmds.add("\""+path.getAbsolutePath()+"\"");
                cmds.add("/b"); //batch mode = quit dolphin with emulation
                cmds.add("/e");
                cmds.add("\""+entry.getPath()+"\"");
                break;
        }
        return cmds;
    }

    public String getProcessName(){
        return path.getName();
    }
}

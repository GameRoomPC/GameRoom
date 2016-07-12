package data;

import UI.Main;

import java.io.*;
import java.util.Locale;
import java.util.Properties;

import static UI.scene.MainScene.MAX_SCALE_FACTOR;
import static UI.scene.MainScene.MIN_SCALE_FACTOR;

/**
 * Created by LM on 03/07/2016.
 */
public class GeneralSettings {
    private String locale = Locale.getDefault().toLanguageTag();
    private boolean closeOnLaunch = false;
    private double tileZoom = 0.4;
    private boolean fullScreen = false; //TODO fix fullscreen exiting after changing scene, see https://bugs.openjdk.java.net/browse/JDK-8089209
    private int windowWidth=640;
    private int windowHeight =480;

    public GeneralSettings(){
        loadSettings();
    }
    private void loadSettings(){
        Properties prop = new Properties();
        InputStream input = null;

        try {
            File configFile = new File("config.properties");
            if(!configFile.exists()){
                configFile.createNewFile();
                saveSettings();
            }

            input = new FileInputStream("config.properties");

            // load a properties file
            prop.load(input);

            if(prop.getProperty("locale")!=null){
                locale = prop.getProperty("locale");
            }
            if(prop.getProperty("closeOnLaunch")!= null){
                closeOnLaunch = Boolean.parseBoolean(prop.getProperty("closeOnLaunch"));

            }
            if(prop.getProperty("tileZoom")!=null){
                tileZoom = Double.parseDouble(prop.getProperty("tileZoom"));
                if(tileZoom>MAX_SCALE_FACTOR){
                    tileZoom = MAX_SCALE_FACTOR;
                }else if(tileZoom < MIN_SCALE_FACTOR){
                    tileZoom = MIN_SCALE_FACTOR;
                }
            }
            if(prop.getProperty("fullScreen")!= null){
                fullScreen = Boolean.parseBoolean(prop.getProperty("fullScreen"));
            }
            if(prop.getProperty("windowWidth")!=null){
                windowWidth = Integer.parseInt(prop.getProperty("windowWidth"));
            }else{
                windowWidth = (int) Main.SCREEN_WIDTH;
            }
            if(prop.getProperty("windowHeight")!=null){
                windowHeight = Integer.parseInt(prop.getProperty("windowHeight"));
            }else{
                windowHeight = (int) Main.SCREEN_HEIGHT;
            }


        } catch (IOException ex) {
            ex.printStackTrace();

        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Main.logger.info("Loaded settings : "
                    +"windowWidth="+windowWidth
                    +", windowHeight="+ windowHeight
                    +", locale="+locale
                    +", closeOnLaunch="+closeOnLaunch
                    +", tileZoom=" +tileZoom
                    +", fullScreen="+fullScreen);
        }
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
        saveSettings();
    }

    public boolean isCloseOnLaunch() {
        return closeOnLaunch;
    }

    public void setCloseOnLaunch(boolean closeOnLaunch) {
        this.closeOnLaunch = closeOnLaunch;
        saveSettings();
    }

    public boolean isFullScreen() {
        return fullScreen;
    }

    public void setFullScreen(boolean fullScreen) {
        this.fullScreen = fullScreen;
        saveSettings();
    }

    public double getTileZoom() {
        return tileZoom;
    }

    public void setTileZoom(double tileZoom) {
        this.tileZoom = tileZoom;
        saveSettings();
    }

    public void saveSettings(){
        Properties prop = new Properties();
        OutputStream output = null;

        try {

            output = new FileOutputStream("config.properties");

            // set the properties value
            prop.setProperty("locale", locale);
            prop.setProperty("closeOnLaunch", Boolean.toString(closeOnLaunch));
            prop.setProperty("tileZoom", Double.toString(tileZoom));
            prop.setProperty("fullScreen", Boolean.toString(fullScreen));
            prop.setProperty("windowWidth", Integer.toString(windowWidth));
            prop.setProperty("windowHeight", Integer.toString(windowHeight));


            // save properties to project root folder
            prop.store(output, null);

        } catch (IOException io) {
            io.printStackTrace();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public void setWindowWidth(int windowWidth) {
        this.windowWidth = windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public void setWindowHeight(int windowHeight) {
        this.windowHeight = windowHeight;
    }
}

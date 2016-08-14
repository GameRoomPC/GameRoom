package data.game.entry;

import system.application.GameStarter;
import javafx.scene.image.Image;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Created by LM on 02/07/2016.
 */
public class GameEntry {
    public static DateFormat DATE_DISPLAY_FORMAT = new SimpleDateFormat("dd.MM.yyyy");
    public final static DateFormat DATE_STORE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    public final static File ENTRIES_FOLDER = new File("Games");
    public final static File[] DEFAULT_IMAGES_PATHS = {new File("res/defaultImages/cover.jpg"),null};
    private final static int IMAGES_NUMBER = 3;

    public final static int TIME_FORMAT_FULL_HMS = 0; // 0h12m0s, 0h5m13s
    public final static int TIME_FORMAT_FULL_DOUBLEDOTS = 1; //00:12:00, 00:05:13
    public final static int TIME_FORMAT_HALF_FULL_HMS = 2; // 12m0s, 5m13s
    public final static int TIME_FORMAT_SHORT_HMS = 3; // 12m, 5m

    private boolean savedLocaly = false;

    private String name = "";
    private Date releaseDate;
    private String description = "";
    private String developer = "";
    private String publisher = "";
    private GameGenre[] genres;
    private GameTheme[] themes;
    private String serie = "";
    private int aggregated_rating;
    private String path = "";
    private UUID uuid;
    private boolean alreadyStartedInGameRoom = false;

    private File[] imagesPaths = new File[IMAGES_NUMBER];
    private boolean[] imageNeedsRefresh = new boolean[IMAGES_NUMBER];
    private HashMap<Integer,Image> createdImages = new HashMap<>();

    private long playTime = 0; //Time in seconds


    /*FOR IGDB PURPOSE ONLY, should not be stored*/
    private int igdb_id =-1;
    private String[] igdb_imageHash = new String[IMAGES_NUMBER];

    private int steam_id=-1;

    public GameEntry(String name) {
        uuid = UUID.randomUUID();
        this.name = name;
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
        File dir = new File(ENTRIES_FOLDER+File.separator+uuid.toString());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File configFile = new File(ENTRIES_FOLDER+File.separator+uuid.toString() + File.separator + "entry.properties");
        if (!configFile.exists()) {
            configFile.createNewFile();
        }
        return configFile;
    }

    private void saveEntry() {
        if (savedLocaly) {
            Properties prop = new Properties();
            OutputStream output = null;
            try {
                output = new FileOutputStream(propertyFile());

                // set the properties value
                prop.setProperty("name", name);
                prop.setProperty("releaseDate", releaseDate!=null ? DATE_STORE_FORMAT.format(releaseDate):"");
                prop.setProperty("description", description);
                prop.setProperty("developer", developer);
                prop.setProperty("publisher", publisher);
                prop.setProperty("serie", serie);
                prop.setProperty("path", path);

                for (int i = 0; i < IMAGES_NUMBER; i++) {
                    if (imagesPaths[i] != null) {
                        prop.setProperty("image" + i, imagesPaths[i].getPath());
                    } else {
                        break;
                    }
                }
                prop.setProperty("playTime", Long.toString(playTime));
                prop.setProperty("steam_id", Integer.toString(steam_id));
                prop.setProperty("igdb_id", Integer.toString(igdb_id));
                prop.setProperty("genres", GameGenre.toJson(genres));
                prop.setProperty("themes", GameTheme.toJson(themes));
                prop.setProperty("aggregated_rating", Integer.toString(aggregated_rating));

                // save properties to project root folder
                prop.store(output, null);
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadEntry() throws IOException {
        Properties prop = new Properties();
        InputStream input = null;

        input = new FileInputStream(propertyFile());

        // load a properties file
        prop.load(input);

        if (prop.getProperty("name") != null) {
            name = prop.getProperty("name");
        }
        if (prop.getProperty("releaseDate") != null) {
            try {
                if(!prop.getProperty("releaseDate").equals("")){
                    releaseDate = DATE_STORE_FORMAT.parse(prop.getProperty("releaseDate"));
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        if (prop.getProperty("description") != null) {
            description = prop.getProperty("description");
        }
        if (prop.getProperty("developer") != null) {
            developer = prop.getProperty("developer");
        }
        if (prop.getProperty("publisher") != null) {
            publisher = prop.getProperty("publisher");
        }
        if (prop.getProperty("serie") != null) {
            serie = prop.getProperty("serie");
        }
        if (prop.getProperty("path") != null) {
            path = prop.getProperty("path");
        }
        if (prop.getProperty("playTime") != null) {
            playTime = Long.parseLong(prop.getProperty("playTime"));
        }
        if (prop.getProperty("steam_id") != null) {
            steam_id = Integer.parseInt(prop.getProperty("steam_id"));
        }
        if (prop.getProperty("igdb_id") != null) {
            igdb_id = Integer.parseInt(prop.getProperty("igdb_id"));
        }
        if (prop.getProperty("genres") != null) {
            genres = GameGenre.fromJson(prop.getProperty("genres"));
        }
        if (prop.getProperty("themes") != null) {
            themes = GameTheme.fromJson(prop.getProperty("themes"));
        }
        if (prop.getProperty("aggregated_rating") != null) {
            aggregated_rating = Integer.parseInt(prop.getProperty("aggregated_rating"));
        }

        for (int i = 0; i < IMAGES_NUMBER; i++) {
            if (prop.getProperty("image" + i) != null) {
                imagesPaths[i] = new File(prop.getProperty("image" + i));
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
        saveEntry();
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
        saveEntry();
    }

    public String getDeveloper() {
        return developer;
    }

    public void setDeveloper(String developer) {
        this.developer = developer;
        saveEntry();
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
        saveEntry();
    }

    public UUID getUuid() {
        return uuid;
    }

    public Image getImage(int index, double width, double height, boolean preserveRatio, boolean smooth){
        if(createdImages.get(index)!=null && !imageNeedsRefresh[index]){
            if(createdImages.get(index).getWidth() == width && createdImages.get(index).getHeight()==height){
                return createdImages.get(index);
            }
        }
        File currFile = getImagePath(index);
        if(currFile == null){
            return null;
        }else if(DEFAULT_IMAGES_PATHS.length > index && currFile.equals(DEFAULT_IMAGES_PATHS[index])){
            Image result = new Image(currFile.getPath().replace("\\","/"), width,height,preserveRatio,smooth);
            createdImages.put(index,result);
            return result;
        }else{
            Image result = new Image("file:" + File.separator + File.separator + File.separator +  currFile.getAbsolutePath(), width,height,preserveRatio,smooth);
            createdImages.put(index,result);
            imageNeedsRefresh[index] = false;
            return result;
        }
    }

    /**
     * Should not be used to create a new imageView, use getImage instead
     * @param index
     * @return
     */
    public File getImagePath(int index) {
        if (index < imagesPaths.length) {
            File result = imagesPaths[index];
            if (result == null) {
                return DEFAULT_IMAGES_PATHS[index];
            }
            return result;
        }
        return DEFAULT_IMAGES_PATHS[0];
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
        saveEntry();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        saveEntry();
    }

    public int getAggregated_rating() {
        return aggregated_rating;
    }

    public void setAggregated_rating(int aggregated_rating) {
        this.aggregated_rating = aggregated_rating;
        saveEntry();
    }

    public void setImagePath(int index, File imagePath) {
        if (imagesPaths.length > index) {
            imagesPaths[index] = imagePath;
            imageNeedsRefresh[index] = true;
        }
        saveEntry();
    }

    public int getSteam_id() {
        return steam_id;
    }

    public void setSteam_id(int steam_id,boolean updatePath) {
        this.steam_id = steam_id;
        if(updatePath) {
            this.path = "steam://rungameid/" + steam_id;
        }
        saveEntry();
    }
    public void setSteam_id(int steam_id) {
        setSteam_id(steam_id,true);
    }

    public boolean isSteamGame() {
        return steam_id == -1;
    }

    public long getPlayTimeSeconds() {
        return playTime;
    }
    public void setPlayTimeSeconds(long seconds){
        this.playTime = seconds;
        saveEntry();
    }
    public void addPlayTimeSeconds(long seconds){
        this.playTime+=seconds;
        saveEntry();
    }
    public static String getPlayTimeFormatted(long playTime, int format){
        String result  = "";
        long seconds=playTime, minutes=0,hours=0;

        if(seconds > 60){
            minutes = seconds /60;
            seconds = seconds%60;

            if(minutes > 60){
                hours = minutes/60;
                minutes = minutes%60;
            }
        }
        switch (format){
            case TIME_FORMAT_FULL_DOUBLEDOTS :
                if(hours<10){
                    result+="0";
                }
                result+=hours+":";
                if(minutes<10){
                    result+="0";
                }
                result+=minutes+":";
                if(seconds<10){
                    result+="0";
                }
                result+=seconds;
                //Main.LOGGER.debug("TIME computed : "+result);
                break;
            case TIME_FORMAT_FULL_HMS:
                result+= hours+ "h";
                result+=minutes + "m";
                result+=seconds+"s";
                break;
            case TIME_FORMAT_HALF_FULL_HMS:
                if(hours> 0){
                    result+= hours+ "h";
                    result+=minutes + "m";
                    result+=seconds+"s";
                }else{
                    if(minutes>0){
                        result+=minutes + "m";
                        result+=seconds+"s";
                    }else{
                        result+=seconds+"s";
                    }
                }
                break;
            case TIME_FORMAT_SHORT_HMS:
                if(hours>0){
                    result = hours + "h";
                }else if(minutes > 0){
                    result = minutes + "m";
                }else{
                    result = seconds + "s";
                }
                break;
            default:
                result = getPlayTimeFormatted(playTime,TIME_FORMAT_FULL_HMS);
                break;
        }
        return result;

    }
    public void setPlayTimeFormatted(String time, int format){
        switch (format){
            case TIME_FORMAT_FULL_DOUBLEDOTS:
                Pattern timePattern = Pattern.compile("\\d*:\\d\\d:\\d\\d");
                if (!timePattern.matcher(time).matches()) {
                    throw new IllegalArgumentException("Invalid time: " + time);
                }
                String[] tokens = time.split(":");
                assert tokens.length == 3;
                try {
                    int hours = Integer.parseInt(tokens[0]);
                    int mins = Integer.parseInt(tokens[1]);
                    int secs = Integer.parseInt(tokens[2]);
                    if (hours < 0) {
                        throw new IllegalArgumentException("Invalid time: " + time);
                    }
                    if (mins < 0 || mins > 59) {
                        throw new IllegalArgumentException("Invalid time: " + time);
                    }
                    setPlayTimeSeconds(hours*3600+mins*60+secs);
                } catch (NumberFormatException nfe) {
                    // regex matching should assure we never reach this catch block
                    assert false;
                    throw new IllegalArgumentException("Invalid time: " + time);
                }
                break;
            default:
                break;
        }

    }
    public String getPlayTimeFormatted(int format){
        return getPlayTimeFormatted(playTime,format);
    }

    public void setSavedLocaly(boolean savedLocaly) {
        this.savedLocaly = savedLocaly;
        saveEntry();
    }

    public void deleteFiles() {
        File file = new File(ENTRIES_FOLDER+File.separator+getUuid().toString());
        String[] entries = file.list();
        if(entries!=null) {
            for (String s : entries) {
                File currentFile = new File(file.getAbsolutePath(), s);
                currentFile.delete();
            }
            file.delete();
        }
    }
    public String getProcessName(){
        String name = "";
        for(int i = path.length()-1; i>=0; i--){
            char c = path.charAt(i);
            if(c=='\\' ||c == '/'){
                break;
            }else{
                name = c+name;
            }
        }
        return name;
    }

    public int getIgdb_id() {
        return igdb_id;
    }

    public void setIgdb_id(int igdb_id) {
        this.igdb_id = igdb_id;
        saveEntry();
    }

    public GameGenre[] getGenres() {
        return genres;
    }

    public void setGenres(GameGenre[] genres) {
        this.genres = genres;
        saveEntry();
    }

    public GameTheme[] getThemes() {
        return themes;
    }

    public void setThemes(GameTheme[] themes) {
        this.themes = themes;
        saveEntry();
    }

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
        saveEntry();
    }

    public String getIgdb_imageHash(int index){
        return igdb_imageHash[index];
    }
    public String[] getIgdb_imageHashs(){
        return igdb_imageHash;
    }

    public void setIgdb_imageHashs(String[] igdb_imageHashs) {
        for (int i = 0; i < igdb_imageHashs.length ; i++) {
            setIgdb_imageHash(i,igdb_imageHashs[i]);
        }
    }
    public void setIgdb_imageHash(int index, String hash){
        if(index >= igdb_imageHash.length){
            String[] copy = igdb_imageHash;
            igdb_imageHash = new String[index+1];
            int i=0;
            for(String s : copy){
                igdb_imageHash[i] = s;
                i++;
            }
        }
        igdb_imageHash[index] = hash;
    }

    public boolean isAlreadyStartedInGameRoom() {
        return alreadyStartedInGameRoom;
    }

    public void setAlreadyStartedInGameRoom(boolean alreadyStartedInGameRoom) {
        this.alreadyStartedInGameRoom = alreadyStartedInGameRoom;
    }

    public void startGame(){
        new GameStarter(this).start();
    }
}

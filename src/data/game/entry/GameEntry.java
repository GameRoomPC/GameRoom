package data.game.entry;

import data.io.FileUtils;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import system.application.GameStarter;
import ui.Main;
import ui.dialog.GameRoomAlert;

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
    public final static int CMD_BEFORE_START = 0;
    public final static int CMD_AFTER_END = 1;

    public final static DateFormat DATE_DISPLAY_FORMAT = new SimpleDateFormat("dd.MM.yyyy");
    public final static DateFormat DATE_STORE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss SSS");
    private final static DateFormat DATE_OLD_STORE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    public final static File[] DEFAULT_IMAGES_PATHS = {new File("res/defaultImages/cover.jpg"), null};
    private final static int IMAGES_NUMBER = 3;

    private final static int TIME_FORMAT_FULL_HMS = 0; // 0h12m0s, 0h5m13s
    public final static int TIME_FORMAT_FULL_DOUBLEDOTS = 1; //00:12:00, 00:05:13
    public final static int TIME_FORMAT_HALF_FULL_HMS = 2; // 12m0s, 5m13s
    public final static int TIME_FORMAT_ROUNDED_HMS = 3; // 12m, 5m
    public static final int TIME_FORMAT_HMS_CASUAL = 4 ; //1h05, 20mn, l0s


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
    private String[] cmd = new String[4];
    private String args = "";
    private String youtubeSoundtrackHash = "";
    private Date addedDate;
    private Date lastPlayedDate;
    private boolean notInstalled = false;

    private File[] imagesPaths = new File[IMAGES_NUMBER];
    private boolean[] imageNeedsRefresh = new boolean[IMAGES_NUMBER];
    private HashMap<Integer, Image> createdImages = new HashMap<>();

    private long playTime = 0; //Time in seconds


    private int igdb_id = -1;
        /*FOR IGDB PURPOSE ONLY, should not be stored*/
    private String[] igdb_imageHash = new String[IMAGES_NUMBER];


    private boolean waitingToBeScrapped = false;
    private int steam_id = -1;
    private int gog_id = -1;
    private int uplay_id = -1;
    private int origin_id = -1;
    private int battlenet_id = -1;
    private Platform platform = Platform.WINDOWS;

    private boolean toAdd = false;
    private boolean beingScrapped;

    private transient Runnable onGameLaunched;
    private transient Runnable onGameStopped;

    public GameEntry(String name) {
        uuid = UUID.randomUUID();
        this.name = name;
    }
    public GameEntry(UUID uuid, boolean toAdd) {
        this.uuid = uuid;
        this.toAdd = toAdd;
        try {
            loadEntry();
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
        File dir = new File((isToAdd() ? Main.FILES_MAP.get("to_add") : Main.FILES_MAP.get("games")) + File.separator + uuid.toString());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File configFile = new File((isToAdd() ? Main.FILES_MAP.get("to_add") : Main.FILES_MAP.get("games")) + File.separator + uuid.toString() + File.separator + "entry.properties");
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
                prop.setProperty("releaseDate", releaseDate != null ? DATE_STORE_FORMAT.format(releaseDate) : "");
                prop.setProperty("description", description);
                prop.setProperty("developer", developer);
                prop.setProperty("publisher", publisher);
                prop.setProperty("serie", serie);
                prop.setProperty("path", path);

                for (int i = 0; i < IMAGES_NUMBER; i++) {
                    if (imagesPaths[i] != null) {
                        File relativeFile = FileUtils.relativizePath(imagesPaths[i],Main.FILES_MAP.get("working_dir"));
                        prop.setProperty("image" + i, relativeFile.getPath());
                    }
                }
                prop.setProperty("playTime", Long.toString(playTime));
                prop.setProperty("steam_id", Integer.toString(steam_id));
                prop.setProperty("gog_id", Integer.toString(gog_id));
                prop.setProperty("origin_id", Integer.toString(origin_id));
                prop.setProperty("uplay_id", Integer.toString(uplay_id));
                prop.setProperty("battlenet_id", Integer.toString(battlenet_id));
                prop.setProperty("igdb_id", Integer.toString(igdb_id));
                prop.setProperty("genres", GameGenre.toJson(genres));
                prop.setProperty("themes", GameTheme.toJson(themes));
                prop.setProperty("aggregated_rating", Integer.toString(aggregated_rating));
                for (int i = 0; i < cmd.length; i++) {
                    if (cmd[i] != null) {
                        prop.setProperty("cmd" + i, cmd[i]);
                    }
                }
                prop.setProperty("addedDate", addedDate != null ? DATE_STORE_FORMAT.format(addedDate) : "");
                prop.setProperty("lastPlayedDate", lastPlayedDate != null ? DATE_STORE_FORMAT.format(lastPlayedDate) : "");
                prop.setProperty("notInstalled", Boolean.toString(notInstalled));
                prop.setProperty("waitingToBeScrapped", Boolean.toString(waitingToBeScrapped));
                prop.setProperty("toAdd", Boolean.toString(toAdd));
                prop.setProperty("args", args);
                prop.setProperty("youtubeSoundtrackHash", youtubeSoundtrackHash);


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
                if (!prop.getProperty("releaseDate").equals("")) {
                    try {
                        releaseDate = DATE_STORE_FORMAT.parse(prop.getProperty("releaseDate"));
                    }catch (ParseException dtpe){
                        setReleaseDate(DATE_OLD_STORE_FORMAT.parse(prop.getProperty("releaseDate")));
                    }
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
        if (prop.getProperty("gog_id") != null) {
            gog_id = Integer.parseInt(prop.getProperty("gog_id"));
        }
        if (prop.getProperty("uplay_id") != null) {
            uplay_id = Integer.parseInt(prop.getProperty("uplay_id"));
        }
        if (prop.getProperty("origin_id") != null) {
            origin_id = Integer.parseInt(prop.getProperty("origin_id"));
        }
        if (prop.getProperty("battlenet_id") != null) {
            battlenet_id = Integer.parseInt(prop.getProperty("battlenet_id"));
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
                File relativeFile = FileUtils.relativizePath(new File(prop.getProperty("image" + i)),Main.FILES_MAP.get("working_dir"));
                imagesPaths[i] = relativeFile;
            }
        }
        for (int i = 0; i < cmd.length; i++) {
            cmd[i] = prop.getProperty("cmd" + i);
        }
        if (prop.getProperty("addedDate") != null) {
            try {
                if (!prop.getProperty("addedDate").equals("")) {
                    try {
                        addedDate = DATE_STORE_FORMAT.parse(prop.getProperty("addedDate"));
                    }catch (ParseException dtpe){
                        setAddedDate(DATE_OLD_STORE_FORMAT.parse(prop.getProperty("addedDate")));
                    }
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if(addedDate == null){
                setSavedLocaly(true);
                setAddedDate(new Date());
                setSavedLocaly(false);
            }
        }
        if (prop.getProperty("lastPlayedDate") != null) {
            try {
                if (!prop.getProperty("lastPlayedDate").equals("")) {
                    try{
                    lastPlayedDate = DATE_STORE_FORMAT.parse(prop.getProperty("lastPlayedDate"));
                }catch (ParseException dtpe){
                    setLastPlayedDate(DATE_OLD_STORE_FORMAT.parse(prop.getProperty("lastPlayedDate")));
                }
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        if (prop.getProperty("notInstalled") != null) {
            notInstalled = Boolean.parseBoolean(prop.getProperty("notInstalled"));
        }
        if (prop.getProperty("waitingToBeScrapped") != null) {
            waitingToBeScrapped = Boolean.parseBoolean(prop.getProperty("waitingToBeScrapped"));
        }
        if (prop.getProperty("toAdd") != null) {
            toAdd = Boolean.parseBoolean(prop.getProperty("toAdd"));
        }
        if (prop.getProperty("youtubeSoundtrackHash") != null) {
            youtubeSoundtrackHash = prop.getProperty("youtubeSoundtrackHash");
        }
        if (prop.getProperty("args") != null) {
            args = prop.getProperty("args");
        }
        saveEntry();

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
        this.developer = developer!=null ? developer : "";
        saveEntry();
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher!= null ? publisher : "";
        saveEntry();
    }

    public UUID getUuid() {
        return uuid;
    }

    public Image getImage(int index, double width, double height, boolean preserveRatio, boolean smooth) {
        return getImage(index,width,height,preserveRatio,smooth,false);
    }
    private Image getImage(int index, double width, double height, boolean preserveRatio, boolean smooth, boolean backGroundloading) {
        if (createdImages.get(index) != null && !imageNeedsRefresh[index]) {
            if (createdImages.get(index).getWidth() == width && createdImages.get(index).getHeight() == height) {
                return createdImages.get(index);
            }
        }
        File currFile = getImagePath(index);
        if (currFile == null) {
            return null;
        } else if(currFile.exists()) {
            Image result = new Image("file:" + File.separator + File.separator + File.separator + currFile.getAbsolutePath(), width, height, preserveRatio, smooth, backGroundloading);
            createdImages.put(index, result);
            imageNeedsRefresh[index] = false;
            return result;
        } else{
            Image result = new Image("file:" + File.separator + File.separator + File.separator + Main.FILES_MAP.get("working_dir")+File.separator+currFile.getPath(), width, height, preserveRatio, smooth, backGroundloading);
            createdImages.put(index, result);
            imageNeedsRefresh[index] = false;
            return result;
        }
    }

    /**
     * Should not be used to create a new imageView, use getImage instead
     *
     * @param index
     * @return
     */
    public File getImagePath(int index) {
        if (index < imagesPaths.length) {
            File imagePath = imagesPaths[index];
            File dir = Main.FILES_MAP.get("working_dir");
            if(imagePath!=null && dir != null){
                return FileUtils.relativizePath(imagePath,dir);
            }else{
                return null;
            }
        }
        return null;
    }

    public String getPath() {
        return path.trim();
    }

    public void setPath(String path) {
        this.path = path.trim();
        saveEntry();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description!=null ? description : "";
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
            File relativeFile = FileUtils.relativizePath(imagePath,Main.FILES_MAP.get("working_dir"));
            imagesPaths[index] = relativeFile;
            imageNeedsRefresh[index] = true;
        }
        saveEntry();
    }

    public int getSteam_id() {
        return steam_id;
    }

    private void setSteam_id(int steam_id, boolean updatePath) {
        this.steam_id = steam_id;
        if (updatePath) {
            this.path = "steam://rungameid/" + steam_id;
        }
        saveEntry();
    }

    public void setSteam_id(int steam_id) {
        setSteam_id(steam_id, steam_id!=-1);
    }

    public boolean isSteamGame() {
        return steam_id != -1;
    }

    public boolean isGoGGame() {
        return gog_id != -1;
    }

    public boolean isOriginGame() {
        return origin_id != -1;
    }

    public boolean isBattlenetGame() {
        return battlenet_id != -1;
    }

    public boolean isUplayGame() {
        return uplay_id != -1;
    }


    public int getBattlenet_id() {
        return battlenet_id;
    }

    public void setBattlenet_id(int battlenet_id) {
        this.battlenet_id = battlenet_id;
        saveEntry();
    }

    public int getOrigin_id() {
        return origin_id;
    }

    public void setOrigin_id(int origin_id) {
        this.origin_id = origin_id;
        saveEntry();
    }

    public int getUplay_id() {
        return uplay_id;
    }

    public void setUplay_id(int uplay_id) {
        this.uplay_id = uplay_id;
        saveEntry();
    }

    public int getGog_id() {
        return gog_id;
    }

    public void setGog_id(int gog_id) {
        this.gog_id = gog_id;
        saveEntry();
    }

    public long getPlayTimeSeconds() {
        return playTime;
    }

    public void setPlayTimeSeconds(long seconds) {
        this.playTime = seconds;
        saveEntry();
    }

    public static String getPlayTimeFormatted(long playTime, int format) {
        String result = "";
        long seconds = playTime, minutes = 0, hours = 0;

        if (seconds > 60) {
            minutes = seconds / 60;
            seconds = seconds % 60;

            if (minutes > 60) {
                hours = minutes / 60;
                minutes = minutes % 60;
            }
        }
        switch (format) {
            case TIME_FORMAT_FULL_DOUBLEDOTS:
                if (hours < 10) {
                    result += "0";
                }
                result += hours + ":";
                if (minutes < 10) {
                    result += "0";
                }
                result += minutes + ":";
                if (seconds < 10) {
                    result += "0";
                }
                result += seconds;
                //Main.LOGGER.debug("TIME computed : "+result);
                break;
            case TIME_FORMAT_FULL_HMS:
                result += hours + "h";
                result += minutes + "mn";
                result += seconds + "s";
                break;
            case TIME_FORMAT_HALF_FULL_HMS:
                if (hours > 0) {
                    result += hours + "h";
                    result += minutes + "mn";
                    result += seconds + "s";
                } else {
                    if (minutes > 0) {
                        result += minutes + "mn";
                        result += seconds + "s";
                    } else {
                        result += seconds + "s";
                    }
                }
                break;
            case TIME_FORMAT_ROUNDED_HMS:
                if (hours > 0) {
                    result = hours + "h";
                } else if (minutes > 0) {
                    result = minutes + "mn";
                } else {
                    result = seconds + "s";
                }
                break;
            case TIME_FORMAT_HMS_CASUAL:
                if (hours > 0) {
                    result += hours + "h";
                    if(minutes > 0){
                        if(minutes <10){
                            result += '0';
                        }
                        result+= minutes;
                    }
                } else {
                    if (minutes > 0) {
                        result += minutes + "mn";
                    } else {
                        result += seconds + "s";
                    }
                }
                break;
            default:
                result = getPlayTimeFormatted(playTime, TIME_FORMAT_FULL_HMS);
                break;
        }
        return result;

    }

    public void setPlayTimeFormatted(String time, int format) {
        switch (format) {
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
                    setPlayTimeSeconds(hours * 3600 + mins * 60 + secs);
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

    public String getPlayTimeFormatted(int format) {
        return getPlayTimeFormatted(playTime, format);
    }

    public void setSavedLocaly(boolean savedLocaly) {
        this.savedLocaly = savedLocaly;
        saveEntry();
    }

    public void deleteFiles() {
        File file = new File((isToAdd() ? Main.FILES_MAP.get("to_add") : Main.FILES_MAP.get("games")) + File.separator + getUuid().toString());
        FileUtils.deleteFolder(file);
    }

    public String getProcessName() {
        String name = "";
        for (int i = path.length() - 1; i >= 0; i--) {
            char c = path.charAt(i);
            if (c == '\\' || c == '/') {
                break;
            } else {
                name = c + name;
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
        this.serie = serie!= null ? serie : "";
        saveEntry();
    }

    public void setCmd(int index, String cmd){
        this.cmd[index]=cmd;
    }
    public String getCmd(int index){
        return cmd[index];
    }

    public Date getAddedDate() {
        return addedDate;
    }

    public void setAddedDate(Date addedDate) {
        this.addedDate = addedDate;
        saveEntry();
    }

    public boolean isNotInstalled() {
        return notInstalled;
    }

    public void setNotInstalled(boolean notInstalled) {
        this.notInstalled = notInstalled;
        saveEntry();
    }

    public Date getLastPlayedDate() {
        return lastPlayedDate;
    }

    public void setLastPlayedDate(Date lastPlayedDate) {
        this.lastPlayedDate = lastPlayedDate;
        saveEntry();
    }

    public String getIgdb_imageHash(int index) {
        return igdb_imageHash[index];
    }

    public String[] getIgdb_imageHashs() {
        return igdb_imageHash;
    }

    public void setIgdb_imageHashs(String[] igdb_imageHashs) {
        for (int i = 0; i < igdb_imageHashs.length; i++) {
            setIgdb_imageHash(i, igdb_imageHashs[i]);
        }
    }

    public boolean isWaitingToBeScrapped() {
        return waitingToBeScrapped;
    }

    public void setWaitingToBeScrapped(boolean waitingToBeScrapped) {
        this.waitingToBeScrapped = waitingToBeScrapped;
        saveEntry();
    }

    public void setIgdb_imageHash(int index, String hash) {
        if (index >= igdb_imageHash.length) {
            String[] copy = igdb_imageHash;
            igdb_imageHash = new String[index + 1];
            int i = 0;
            for (String s : copy) {
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

    public void startGame() {
        try {
            new GameStarter(this).start();
        }catch (IOException ioe){
            GameRoomAlert.error(ioe.getMessage());
        }
    }

    @Override
    public String toString() {
        return "GameEntry:name=" + name +
                ",release_date=" + (releaseDate != null ? DATE_DISPLAY_FORMAT.format(releaseDate) : null) +
                ",steam_id=" + steam_id+
                "playTime="+getPlayTimeFormatted(TIME_FORMAT_FULL_DOUBLEDOTS);
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public boolean isToAdd() {
        return toAdd;
    }

    public void setToAdd(boolean toAdd) {
        this.toAdd = toAdd;
        saveEntry();
    }

    public String getYoutubeSoundtrackHash() {
        return youtubeSoundtrackHash;
    }

    public void setYoutubeSoundtrackHash(String youtubeSoundtrackHash) {
        this.youtubeSoundtrackHash = youtubeSoundtrackHash;
        saveEntry();
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    public boolean isBeingScrapped() {
        return beingScrapped;
    }

    public void setBeingScrapped(boolean beingScrapped) {
        this.beingScrapped = beingScrapped;
    }

    public Runnable getOnGameLaunched() {
        return onGameLaunched;
    }

    public void setOnGameLaunched(Runnable onGameLaunched) {
        this.onGameLaunched = onGameLaunched;
    }

    public Runnable getOnGameStopped() {
        return onGameStopped;
    }

    public void setOnGameStopped(Runnable onGameStopped) {
        this.onGameStopped = onGameStopped;
    }

    public Platform getPlatform() {
        return platform;
    }
}

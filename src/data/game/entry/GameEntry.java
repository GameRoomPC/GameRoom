package data.game.entry;

import data.io.FileUtils;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.image.Image;
import system.application.GameStarter;
import ui.Main;
import ui.dialog.GameRoomAlert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import static data.io.FileUtils.getExtension;

/**
 * Created by LM on 02/07/2016.
 */
public class GameEntry {
    public final static int CMD_BEFORE_START = 0;
    public final static int CMD_AFTER_END = 1;

    public final static DateTimeFormatter DATE_DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public final static File[] DEFAULT_IMAGES_PATHS = {new File("res/defaultImages/cover.jpg"), null};
    private final static int IMAGES_NUMBER = 3;

    private final static int TIME_FORMAT_FULL_HMS = 0; // 0h12m0s, 0h5m13s
    public final static int TIME_FORMAT_FULL_DOUBLEDOTS = 1; //00:12:00, 00:05:13
    public final static int TIME_FORMAT_HALF_FULL_HMS = 2; // 12m0s, 5m13s
    public final static int TIME_FORMAT_ROUNDED_HMS = 3; // 12m, 5m
    public static final int TIME_FORMAT_HMS_CASUAL = 4; //1h05, 20mn, l0s


    private boolean savedLocaly = false;

    private String name = "";
    private LocalDateTime releaseDate;
    private String description = "";
    private String developer = "";
    private String publisher = "";
    private ArrayList<GameGenre> genres = new ArrayList<>();
    private ArrayList<GameTheme> themes = new ArrayList<>();
    private String serie = "";
    private int aggregated_rating;
    private String path = "";
    private int id = -1;
    private String[] cmd = new String[4];
    private String args = "";
    private String youtubeSoundtrackHash = "";
    private LocalDateTime addedDate;
    private LocalDateTime lastPlayedDate;
    private boolean installed = false;

    private ArrayList<Developer> developers = new ArrayList<>();
    private ArrayList<Publisher> publishers = new ArrayList<>();


    private File[] imagesFiles = new File[IMAGES_NUMBER];
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

    private boolean toAdd = false;
    private boolean beingScrapped;
    private boolean ignored = false;

    private transient Runnable onGameLaunched;
    private transient Runnable onGameStopped;

    private transient boolean deleted = false;

    private transient SimpleBooleanProperty monitored = new SimpleBooleanProperty(false);

    public GameEntry(String name) {
        this.name = name;
    }

    public GameEntry(int id) {
        this.id = id;
    }

    private void saveEntry() {
        if (savedLocaly && !deleted) {
            //TODO store into db
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public LocalDateTime getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDateTime releaseDate) {
        this.releaseDate = releaseDate;
        saveEntry();
    }

    public String getDeveloper() {
        return developer;
    }

    public void setDeveloper(String developer) {
        this.developer = developer != null ? developer : "";
        saveEntry();
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher != null ? publisher : "";
        saveEntry();
    }

    public int getId() {
        return id;
    }

    public Image getImage(int index, double width, double height, boolean preserveRatio, boolean smooth) {
        return getImage(index, width, height, preserveRatio, smooth, false);
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
        } else if (currFile.exists()) {
            Image result = new Image("file:" + File.separator + File.separator + File.separator + currFile.getAbsolutePath(), width, height, preserveRatio, smooth, backGroundloading);
            createdImages.put(index, result);
            imageNeedsRefresh[index] = false;
            return result;
        } else {
            Image result = new Image("file:" + File.separator + File.separator + File.separator + Main.FILES_MAP.get("working_dir") + File.separator + currFile.getPath(), width, height, preserveRatio, smooth, backGroundloading);
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
        if (index < imagesFiles.length) {
            File imagePath = imagesFiles[index];
            if (imagePath != null) {
                return new File(imagePath.getAbsolutePath());
            } else {
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
        this.description = description != null ? description : "";
        saveEntry();
    }

    public int getAggregated_rating() {
        return aggregated_rating;
    }

    public void setAggregated_rating(int aggregated_rating) {
        this.aggregated_rating = aggregated_rating;
        saveEntry();
    }

    public void updateImage(int index, File newImageFile) throws IOException {
        if (imagesFiles.length > index) {
            if (newImageFile != null && newImageFile.exists() && newImageFile.isFile()) {
                String ext = getExtension(newImageFile);
                String path = "";
                if (index == 0) {
                    path = getCoverPath();
                } else {
                    path = getScreenShotPath();
                }
                path += "." + ext;

                File localFile = new File(path);

                Files.copy(newImageFile.getAbsoluteFile().toPath()
                        , localFile.getAbsoluteFile().toPath()
                        , StandardCopyOption.REPLACE_EXISTING);

                imagesFiles[index] = localFile;
                imageNeedsRefresh[index] = true;

                saveEntry();
            }
        }
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
        setSteam_id(steam_id, steam_id != -1);
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
                    if (minutes > 0) {
                        if (minutes < 10) {
                            result += '0';
                        }
                        result += minutes;
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

    public void deletePermanently() {
        //TODO delete in db
        deleted = true;
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

    public ArrayList<GameGenre> getGenres() {
        return genres;
    }

    public void setGenres(ArrayList<GameGenre> genres) {
        this.genres = genres;
        saveEntry();
    }

    public ArrayList<GameTheme> getThemes() {
        return themes;
    }

    public void setThemes(ArrayList<GameTheme> themes) {
        this.themes = themes;
        saveEntry();
    }

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie != null ? serie : "";
        saveEntry();
    }

    public void setCmd(int index, String cmd) {
        this.cmd[index] = cmd;
    }

    public String getCmd(int index) {
        return cmd[index];
    }

    public LocalDateTime getAddedDate() {
        return addedDate;
    }

    public void setAddedDate(LocalDateTime addedDate) {
        this.addedDate = addedDate;
        saveEntry();
    }

    public boolean isInstalled() {
        return installed;
    }

    public void setInstalled(boolean notInstalled) {
        this.installed = notInstalled;
        saveEntry();
    }

    public LocalDateTime getLastPlayedDate() {
        return lastPlayedDate;
    }

    public void setLastPlayedDate(LocalDateTime lastPlayedDate) {
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

    public void startGame() {
        try {
            new GameStarter(this).start();
        } catch (IOException ioe) {
            GameRoomAlert.error(ioe.getMessage());
        }
    }

    @Override
    public String toString() {
        return "GameEntry:name=" + name +
                ",release_date=" + (releaseDate != null ? DATE_DISPLAY_FORMAT.format(releaseDate) : null) +
                ",steam_id=" + steam_id +
                "playTime=" + getPlayTimeFormatted(TIME_FORMAT_FULL_DOUBLEDOTS);
    }

    public void setId(int id) {
        this.id = id;
        saveEntry();
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

    public void setMonitored(boolean monitored) {
        this.monitored.setValue(monitored);
    }

    public boolean isMonitored() {
        return monitored.getValue();
    }

    public SimpleBooleanProperty monitoredProperty() {
        return monitored;
    }


    public boolean isIgnored() {
        return ignored;
    }

    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }

    public static GameEntry loadFromDB(ResultSet set) throws SQLException {
        if (set == null) {
            return null;
        }
        int id = set.getInt("id");
        GameEntry entry = new GameEntry(id);
        entry.setName(set.getString("name"));
        entry.setDescription(set.getString("description"));
        entry.setPath(set.getString("path"));
        entry.setCmd(GameEntry.CMD_BEFORE_START, set.getString("cmd_before"));
        entry.setCmd(GameEntry.CMD_AFTER_END, set.getString("cmd_after"));
        entry.setYoutubeSoundtrackHash(set.getString("yt_hash"));

        Timestamp addedTimestamp = set.getTimestamp("added_date");
        if (addedTimestamp != null) {
            entry.setAddedDate(addedTimestamp.toLocalDateTime());
        } else {
            entry.setAddedDate(LocalDateTime.now());
        }

        Timestamp releasedTimestamp = set.getTimestamp("release_date");
        if (releasedTimestamp != null) {
            entry.setReleaseDate(releasedTimestamp.toLocalDateTime());
        }

        Timestamp lastPlayedTimestamp = set.getTimestamp("last_played_date");
        if (lastPlayedTimestamp != null) {
            entry.setAddedDate(lastPlayedTimestamp.toLocalDateTime());
        }

        entry.setPlayTimeSeconds(set.getInt("initial_playtime"));
        entry.setInstalled(set.getBoolean("installed"));
        entry.setIgdb_imageHash(0, set.getString("cover_hash"));
        entry.setIgdb_imageHash(1, set.getString("wp_hash"));
        entry.setIgdb_id(set.getInt("igdb_id"));
        entry.setWaitingToBeScrapped(set.getBoolean("waiting_scrap"));
        entry.setToAdd(set.getBoolean("toAdd"));
        entry.setIgnored(set.getBoolean("ignored"));
        entry.setBeingScrapped(set.getBoolean("beingScraped"));
        return entry;
    }

    private String getCoverPath() {
        return GameEntryUtils.coverPath(this);
    }

    private String getScreenShotPath() {
        return GameEntryUtils.screenshotPath(this);
    }
}

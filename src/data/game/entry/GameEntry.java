package data.game.entry;

import data.io.DataBase;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.image.Image;
import system.application.GameStarter;
import ui.Main;
import ui.dialog.GameRoomAlert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

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


    private boolean savedLocally = false;

    private String name = "";
    private LocalDateTime releaseDate;
    private String description = "";
    private String developer = "";
    private String publisher = "";
    private ArrayList<GameGenre> genres = new ArrayList<>();
    private ArrayList<GameTheme> themes = new ArrayList<>();
    private ArrayList<Integer> genresIds = new ArrayList<>();
    private ArrayList<Integer> themesIds = new ArrayList<>();
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

    private final static String[] SQL_PARAMS = new String[]{"name",
            "release_date",
            "description",
            "aggregated_rating",
            "path",
            "cmd_before",
            "cmd_after",
            "launch_args",
            "yt_hash",
            "added_date",
            "last_played_date",
            "initial_playtime",
            "installed",
            "cover_hash",
            "wp_hash",
            "igdb_id",
            "waiting_scrap",
            "toAdd",
            "beingScraped"};

    public GameEntry(String name) {
        this.name = name;
    }

    public GameEntry(int id) {
        this.id = id;
        for (int i = 0; i < IMAGES_NUMBER; i++) {
            String path = "";
            if (i == 0) {
                path = getCoverPath();
            } else {
                path = getScreenShotPath();
            }
            //TODO read here which saved pictures corresponds to this pattern
            path += ".jpg";

            File localFile = new File(path);
            imagesFiles[i] = localFile;
            imageNeedsRefresh[i] = true;
        }
    }

    /*private void saveEntry() {
        if (savedLocally && !deleted) {
            try {
                //TODO store into db
                saveDirectFields();
                saveGenres();
                saveThemes();
                saveDevs();
                savePublishers();
                saveSerie();
                savePlatformInfo();


            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveDirectFields() throws SQLException {
        Connection connection = DataBase.getUserConnection();

        PreparedStatement statement = connection.prepareStatement(getSQLInitLine());
        statement.setString(1, name);
        statement.setString(3, description);
        statement.setInt(4, aggregated_rating);
        statement.setString(5, path);
        statement.setString(6, cmd[0]);
        statement.setString(7, cmd[1]);
        statement.setString(8, args);
        statement.setString(9, youtubeSoundtrackHash);
        statement.setTimestamp(10, Timestamp.valueOf(addedDate));
        statement.setTimestamp(11, Timestamp.valueOf(lastPlayedDate));
        statement.setLong(12, playTime);
        statement.setBoolean(13, installed);
        statement.setString(14, igdb_imageHash[0]);
        statement.setString(15, igdb_imageHash[1]);
        statement.setInt(16, igdb_id);
        statement.setBoolean(17, waitingToBeScrapped);
        statement.setBoolean(18, toAdd);
        statement.setBoolean(19, beingScrapped);
        statement.execute();
        statement.close();
        connection.commit();
    }

    private void saveGenres() throws SQLException {
        if (genres != null) {
            for (GameGenre genre : genres) {
                int genreId = GameGenre.getIGDBId(genre.getKey());
                if (genreId != -1) {
                    PreparedStatement genreStatement = DataBase.getUserConnection().prepareStatement("INSERT OR IGNORE INTO has_genre(game_id,genre_id) VALUES (?,?)");
                    genreStatement.setInt(1, id);
                    genreStatement.setInt(2, genreId);
                    genreStatement.execute();
                    genreStatement.close();
                    DataBase.commit();
                }
            }
        }
    }

    private void saveThemes() throws SQLException {
        if (themes != null) {
            for (GameTheme theme : themes) {
                int themeID = GameTheme.getIGDBId(theme.getKey());
                if (themeID != -1) {
                    PreparedStatement genreStatement = DataBase.getUserConnection().prepareStatement("INSERT OR IGNORE INTO has_theme(game_id,theme_id) VALUES (?,?)");
                    genreStatement.setInt(1, id);
                    genreStatement.setInt(2, themeID);
                    genreStatement.execute();
                    genreStatement.close();
                    DataBase.commit();
                }
            }
        }
    }

    private void saveDevs() throws SQLException {
        if (developer != null && !developer.isEmpty()) {
            String[] devs = developer.split(",\\s");
            for (String s : devs) {
                Developer dev = new Developer(s);
                int devId = dev.insertInDB();

                if (devId != -1) {
                    PreparedStatement devStatement2 = DataBase.getUserConnection().prepareStatement("INSERT OR IGNORE INTO develops(game_id,dev_id) VALUES (?,?)");
                    devStatement2.setInt(1, id);
                    devStatement2.setInt(2, devId);
                    devStatement2.execute();
                    devStatement2.close();
                    DataBase.commit();
                }
            }
        }
    }

    private void savePublishers() throws SQLException {
        if (publisher != null && !publisher.isEmpty()) {
            String[] pubs = publisher.split(",\\s");
            for (String s : pubs) {
                Publisher pub = new Publisher(s);
                int pubId = pub.insertInDB();

                if (pubId != -1) {
                    PreparedStatement pubStatement2 = DataBase.getUserConnection().prepareStatement("INSERT OR IGNORE INTO publishes(game_id,pub_id) VALUES (?,?)");
                    pubStatement2.setInt(1, id);
                    pubStatement2.setInt(2, pubId);
                    pubStatement2.execute();
                    pubStatement2.close();
                    DataBase.commit();
                }
            }
        }
    }

    private void saveSerie() throws SQLException {
        if (serie != null && !serie.isEmpty()) {
            Serie gameSerie = new Serie(serie);
            int serieId = gameSerie.insertInDB();

            if (serieId != -1) {
                PreparedStatement serieStatement2 = DataBase.getUserConnection().prepareStatement("INSERT OR IGNORE INTO regroups(game_id,serie_id) VALUES (?,?)");
                serieStatement2.setInt(1, id);
                serieStatement2.setInt(2, serieId);
                serieStatement2.execute();
                serieStatement2.close();
                DataBase.commit();
            }
        }
    }

    private void savePlatformInfo() throws SQLException {
        int specificId = 0;
        int platformId = -1;

        if (steam_id != -1) {
            specificId = steam_id;
            if (installed) {
                platformId = 1;
            } else {
                platformId = 2;
            }
        } else if (origin_id != -1) {
            specificId = origin_id;
            platformId = 3;
        } else if (uplay_id != -1) {
            specificId = uplay_id;
            platformId = 4;
        } else if (battlenet_id != -1) {
            specificId = battlenet_id;
            platformId = 5;
        } else if (gog_id != -1) {
            specificId = gog_id;
            platformId = 6;
        }
        if (platformId != -1) {
            PreparedStatement genreStatement = DataBase.getUserConnection().prepareStatement("INSERT OR IGNORE INTO runs_on(specific_id,platform_id,game_id) VALUES (?,?,?)");
            genreStatement.setInt(1, specificId);
            genreStatement.setInt(2, platformId);
            genreStatement.setInt(3, id);
            genreStatement.execute();
            genreStatement.close();
            DataBase.commit();
        }
    }*/

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
        try {
            PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set release_date = ? where id = ?");
            statement.setTimestamp(1, Timestamp.valueOf(releaseDate));
            statement.setInt(2, id);
            statement.execute();

            if (savedLocally) {
                DataBase.getUserConnection().commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getDeveloper() {
        return developer;
    }

    public void setDeveloper(String developer) {
        this.developer = developer != null ? developer : "";
        //TODO update developer to work like gamegenres
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher != null ? publisher : "";
        //TODO update publisher to work like gamegenres
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
        try {
            PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set path = ? where id = ?");
            statement.setString(1, path);
            statement.setInt(2, id);
            statement.execute();

            if (savedLocally) {
                DataBase.getUserConnection().commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description : "";
        try {
            PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set description = ? where id = ?");
            statement.setString(1, description);
            statement.setInt(2, id);
            statement.execute();

            if (savedLocally) {
                DataBase.getUserConnection().commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getAggregated_rating() {
        return aggregated_rating;
    }

    public void setAggregated_rating(int aggregated_rating) {
        this.aggregated_rating = aggregated_rating;
        try {
            PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set aggregated_rating = ? where id = ?");
            statement.setInt(1, aggregated_rating);
            statement.setInt(2, id);
            statement.execute();

            if (savedLocally) {
                DataBase.getUserConnection().commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
        //TODO update to use the platform as implemented in db, and have only one setter and getter to rule them all !
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
        //TODO update to use the platform as implemented in db, and have only one setter and getter to rule them all !
    }

    public int getOrigin_id() {
        return origin_id;
    }

    public void setOrigin_id(int origin_id) {
        this.origin_id = origin_id;
        //TODO update to use the platform as implemented in db, and have only one setter and getter to rule them all !
    }

    public int getUplay_id() {
        return uplay_id;
    }

    public void setUplay_id(int uplay_id) {
        this.uplay_id = uplay_id;
        //TODO update to use the platform as implemented in db, and have only one setter and getter to rule them all !
    }

    public int getGog_id() {
        return gog_id;
    }

    public void setGog_id(int gog_id) {
        this.gog_id = gog_id;
        //TODO update to use the platform as implemented in db, and have only one setter and getter to rule them all !
    }

    public long getPlayTimeSeconds() {
        return playTime;
    }

    public void setPlayTimeSeconds(long seconds) {
        this.playTime = seconds;
        try {
            PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set initial_playtime = ? where id = ?");
            statement.setLong(1, playTime);
            statement.setInt(2, id);
            statement.execute();

            if (savedLocally) {
                DataBase.getUserConnection().commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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

    public String getPlayTimeFormatted(int format) {
        return getPlayTimeFormatted(playTime, format);
    }

    /**
     * Similar to auto-commit mode for this entry if set true
     * Commits directly if set to true
     *
     * @param savedLocally wether to auto-commit or not
     */
    public void setSavedLocally(boolean savedLocally) {
        this.savedLocally = savedLocally;
        try {
            if (savedLocally) {
                DataBase.getUserConnection().commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
        try {
            PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set igdb_id = ? where id = ?");
            statement.setInt(1, igdb_id);
            statement.setInt(2, id);
            statement.execute();

            if (savedLocally) {
                DataBase.getUserConnection().commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<GameGenre> getGenres() {
        return genres;
    }

    public void setGenres(ArrayList<GameGenre> genres) {
        this.genres = genres;
        //TODO do a multi "insert or ignore into" here into the relational table
    }

    public ArrayList<GameTheme> getThemes() {
        return themes;
    }

    public void setThemes(ArrayList<GameTheme> themes) {
        this.themes = themes;
        //TODO do a multi "insert or ignore into " here into the relational table
    }

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie != null ? serie : "";
        //TODO do a multi "insert or ignore into " here into the relational table
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
        try {
            PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set added_date = ? where id = ?");
            statement.setTimestamp(1, Timestamp.valueOf(addedDate));
            statement.setInt(2, id);
            statement.execute();

            if (savedLocally) {
                DataBase.getUserConnection().commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isInstalled() {
        return installed;
    }

    public void setInstalled(boolean notInstalled) {
        this.installed = notInstalled;
        try {
            PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set installed = ? where id = ?");
            statement.setBoolean(1, installed);
            statement.setInt(2, id);
            statement.execute();

            if (savedLocally) {
                DataBase.getUserConnection().commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public LocalDateTime getLastPlayedDate() {
        return lastPlayedDate;
    }

    public void setLastPlayedDate(LocalDateTime lastPlayedDate) {
        this.lastPlayedDate = lastPlayedDate;
        try {
            PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set last_played_date = ? where id = ?");
            statement.setTimestamp(1, Timestamp.valueOf(addedDate));
            statement.setInt(2, id);
            statement.execute();

            if (savedLocally) {
                DataBase.getUserConnection().commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
        try {
            PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set waiting_scrap = ? where id = ?");
            statement.setBoolean(1, waitingToBeScrapped);
            statement.setInt(2, id);
            statement.execute();

            if (savedLocally) {
                DataBase.getUserConnection().commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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

    public boolean isToAdd() {
        return toAdd;
    }

    public void setToAdd(boolean toAdd) {
        this.toAdd = toAdd;
        try {
            PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set toAdd = ? where id = ?");
            statement.setBoolean(1, toAdd);
            statement.setInt(2, id);
            statement.execute();

            if (savedLocally) {
                DataBase.getUserConnection().commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getYoutubeSoundtrackHash() {
        return youtubeSoundtrackHash;
    }

    public void setYoutubeSoundtrackHash(String youtubeSoundtrackHash) {
        this.youtubeSoundtrackHash = youtubeSoundtrackHash;
        try {
            PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set yt_hash = ? where id = ?");
            statement.setString(1, youtubeSoundtrackHash);
            statement.setInt(2, id);
            statement.execute();

            if (savedLocally) {
                DataBase.getUserConnection().commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
        try {
            PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set launch_args = ? where id = ?");
            statement.setString(1, args);
            statement.setInt(2, id);
            statement.execute();

            if (savedLocally) {
                DataBase.getUserConnection().commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isBeingScraped() {
        return beingScrapped;
    }

    public void setBeingScraped(boolean beingScraped) {
        this.beingScrapped = beingScraped;
        try {
            PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set beingScraped = ? where id = ?");
            statement.setBoolean(1, beingScraped);
            statement.setInt(2, id);
            statement.execute();

            if (savedLocally) {
                DataBase.getUserConnection().commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
        try {
            PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set ignored = ? where id = ?");
            statement.setBoolean(1, ignored);
            statement.setInt(2, id);
            statement.execute();

            if (savedLocally) {
                DataBase.getUserConnection().commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static GameEntry loadFromDB(ResultSet set) throws SQLException {
        if (set == null) {
            return null;
        }
        int id = set.getInt("id");
        GameEntry entry = new GameEntry(id);
        entry.setSavedLocally(false);
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
        entry.setBeingScraped(set.getBoolean("beingScraped"));
        return entry;
    }

    private String getCoverPath() {
        return GameEntryUtils.coverPath(this);
    }

    private String getScreenShotPath() {
        return GameEntryUtils.screenshotPath(this);
    }

    public static String getSQLInitLine() {
        String temp = "INSERT OR IGNORE INTO GameEntry (";

        for (int i = 0; i < SQL_PARAMS.length; i++) {
            temp+=SQL_PARAMS[i];
            if(i!=SQL_PARAMS.length-1){
                temp+=",";
            }
        }

        temp += ") VALUES (";
        for (int i = 0; i < SQL_PARAMS.length; i++) {
            temp += "?";
            if(i!=SQL_PARAMS.length-1){
                temp+=",";
            }
        }
        return temp + ");";
    }

    public static String getSQLUpdateLine() {
        String temp = "UPDATE GameEntry set ";

        for (int i = 0; i < SQL_PARAMS.length; i++) {
            temp+=SQL_PARAMS[i]+"=?";
            if(i!=SQL_PARAMS.length-1){
                temp+=",";
            }
        }

        temp += " WHERE id = ?";
        return temp;
    }
}

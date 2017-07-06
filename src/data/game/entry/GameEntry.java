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
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

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

    private Serie serie = Serie.NONE;
    private ArrayList<Company> developers = new ArrayList<>();
    private ArrayList<Company> publishers = new ArrayList<>();
    private ArrayList<GameGenre> genres = new ArrayList<>();
    private ArrayList<GameTheme> themes = new ArrayList<>();

    private int aggregated_rating;
    private String path = "";
    private int id = -1;
    private String[] cmd = new String[4];
    private String args = "";
    private String youtubeSoundtrackHash = "";
    private LocalDateTime addedDate;
    private LocalDateTime lastPlayedDate;
    private boolean installed = true;

    private File[] imagesFiles = new File[IMAGES_NUMBER];

    private long playTime = 0; //Time in seconds

    private int igdb_id = -1;
    /*FOR IGDB PURPOSE ONLY, should not be stored*/
    private String[] igdb_imageHash = new String[IMAGES_NUMBER];

    private Platform platform = Platform.NONE;
    private int platformGameId = 0;

    private transient boolean inDb = false;
    private boolean toAdd = false;

    private boolean waitingToBeScrapped = false;
    private boolean beingScrapped = false;
    private boolean ignored = false;
    private transient boolean deleted = false;
    private boolean runAsAdmin = false;

    private transient Runnable onGameLaunched;
    private transient Runnable onGameStopped;


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
            "ignored",
            "runAsAdmin"
    };

    public GameEntry(String name) {
        this.name = name;
    }


    public void saveEntry() {
        if (savedLocally && !deleted) {
            long start = System.currentTimeMillis();
            try {
                saveDirectFields();

                Statement batchStatement = DataBase.getUserConnection().createStatement();

                saveGenres(batchStatement);
                saveThemes(batchStatement);
                saveDevs(batchStatement);
                savePublishers(batchStatement);
                saveSerie(batchStatement);
                savePlatform(batchStatement);

                batchStatement.executeBatch();
                batchStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            Main.LOGGER.debug(name + " saved in " + (System.currentTimeMillis() - start) + "ms");
        }
    }

    private void saveDirectFields() throws SQLException {
        Connection connection = DataBase.getUserConnection();
        String sql = inDb ? getSQLUpdateLine() : getSQLInitLine();

        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, name);
        if (releaseDate != null) {
            statement.setTimestamp(2, Timestamp.valueOf(releaseDate));
        }
        statement.setString(3, description);
        statement.setInt(4, aggregated_rating);
        statement.setString(5, path);
        statement.setString(6, cmd[0]);
        statement.setString(7, cmd[1]);
        statement.setString(8, args);
        statement.setString(9, youtubeSoundtrackHash);
        if (addedDate != null) {
            statement.setTimestamp(10, Timestamp.valueOf(addedDate));
        } else {
            statement.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
        }
        if (lastPlayedDate != null) {
            statement.setTimestamp(11, Timestamp.valueOf(lastPlayedDate));
        }
        statement.setLong(12, playTime);
        statement.setBoolean(13, installed);
        statement.setString(14, igdb_imageHash[0]);
        statement.setString(15, igdb_imageHash[1]);
        statement.setInt(16, igdb_id);
        statement.setBoolean(17, waitingToBeScrapped);
        statement.setBoolean(18, toAdd);
        statement.setBoolean(19, ignored);
        statement.setBoolean(20, runAsAdmin);

        if (inDb) {
            statement.setInt(21, id);
        }

        statement.execute();
        statement.close();

        if (!inDb) {
            id = DataBase.getLastId();
            inDb = true;
        }
        //connection.commit();
    }

    private void saveGenres(Statement batchStatement) throws SQLException {
        batchStatement.addBatch("delete from has_genre where game_id= " + id);

        if (genres != null && !genres.isEmpty()) {
            String insertSQL = "INSERT OR REPLACE INTO has_genre(game_id,genre_id) VALUES ";
            for (GameGenre genre : genres) {
                if (genre != null) {
                    insertSQL += "(" + id + "," + GameGenre.getIGDBId(genre.getKey()) + "),";
                }
            }
            batchStatement.addBatch(insertSQL.substring(0, insertSQL.length() - 1));
        }
    }

    private void saveThemes(Statement batchStatement) throws SQLException {
        batchStatement.addBatch("delete from has_theme where game_id= " + id);

        if (themes != null && !themes.isEmpty()) {
            String insertSQL = "INSERT OR REPLACE INTO has_theme(game_id,theme_id) VALUES ";
            for (GameTheme theme : themes) {
                if (theme != null) {
                    insertSQL += "(" + id + "," + theme.getIGDBId(theme.getKey()) + "),";
                }
            }
            batchStatement.addBatch(insertSQL.substring(0, insertSQL.length() - 1));
        }
    }

    private void saveDevs(Statement batchStatement) throws SQLException {
        batchStatement.addBatch("delete from develops where game_id= " + id);

        if (developers != null && !developers.isEmpty()) {
            String insertSQL = "INSERT OR REPLACE INTO develops(game_id,dev_id) VALUES ";
            for (Company c : developers) {
                if (c != null) {
                    insertSQL += "(" + id + "," + c.getId() + "),";
                }
            }
            batchStatement.addBatch(insertSQL.substring(0, insertSQL.length() - 1));
        }
    }

    private void savePublishers(Statement batchStatement) throws SQLException {
        batchStatement.addBatch("delete from publishes where game_id= " + id);

        if (publishers != null && !publishers.isEmpty()) {
            String insertSQL = "INSERT OR REPLACE INTO publishes(game_id,pub_id) VALUES ";
            for (Company c : publishers) {
                if (c != null) {
                    insertSQL += "(" + id + "," + c.getId() + "),";
                }
            }
            batchStatement.addBatch(insertSQL.substring(0, insertSQL.length() - 1));
        }
    }

    private void saveSerie(Statement batchStatement) throws SQLException {
        if (serie != null) {
            batchStatement.addBatch("delete from regroups where game_id= " + id);
            batchStatement.addBatch("INSERT OR REPLACE INTO regroups(game_id,serie_id) VALUES (" + id + "," + serie.getId() + ")");
        }
    }

    private void savePlatform(Statement batchStatement) throws SQLException {
        if (platform != null) {
            batchStatement.addBatch("delete from runs_on where game_id=" + id);
            batchStatement.addBatch("INSERT OR REPLACE INTO runs_on(platformGameId,platform_id,game_id) VALUES " +
                    "(" + platformGameId + ","
                    + platform.getId() + ","
                    + id + ")");
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
        try {
            if (savedLocally && !deleted) {
                PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set release_date = ? where id = ?");
                statement.setTimestamp(1, Timestamp.valueOf(releaseDate));
                statement.setInt(2, id);
                statement.execute();

                statement.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getId() {
        return id;
    }

    public Image getImage(int index, double width, double height, boolean preserveRatio, boolean smooth) {
        return getImage(index, width, height, preserveRatio, smooth, false);
    }

    private Image getImage(int index, double width, double height, boolean preserveRatio, boolean smooth, boolean backGroundloading) {
        File currFile = getImagePath(index);
        if (currFile == null) {
            return null;
        } else if (currFile.exists()) {
            return new Image("file:" + File.separator + File.separator + File.separator + currFile.getAbsolutePath(), width, height, preserveRatio, smooth, backGroundloading);
        } else {
            return new Image("file:" + File.separator + File.separator + File.separator + Main.FILES_MAP.get("working_dir") + File.separator + currFile.getPath(), width, height, preserveRatio, smooth, backGroundloading);
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
            if (savedLocally && !deleted) {
                PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set path = ? where id = ?");
                statement.setString(1, path);
                statement.setInt(2, id);
                statement.execute();

                statement.close();
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
            if (savedLocally && !deleted) {
                PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set description = ? where id = ?");
                statement.setString(1, description);
                statement.setInt(2, id);
                statement.execute();

                statement.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getAggregated_rating() {
        //TODO fix in MainScene sorting by aggregated_rating returns only zeros
        return aggregated_rating;
    }

    public void setAggregated_rating(int aggregated_rating) {
        this.aggregated_rating = aggregated_rating;
        try {
            if (savedLocally && !deleted) {
                PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set aggregated_rating = ? where id = ?");
                statement.setInt(1, aggregated_rating);
                statement.setInt(2, id);
                statement.execute();
                statement.close();
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
            }
        }
    }

    public int getPlatformGameID() {
        return platformGameId;
    }

    public boolean isSteamGame() {
        return platform.getId() == Platform.STEAM_ID;
    }

    public long getPlayTimeSeconds() {
        return playTime;
    }

    public void setPlayTimeSeconds(long seconds) {
        this.playTime = seconds;
        try {
            if (savedLocally && !deleted) {
                PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set initial_playtime = ? where id = ?");
                statement.setLong(1, playTime);
                statement.setInt(2, id);
                statement.execute();

                statement.close();
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
     *
     * @param savedLocally wether to auto-commit or not
     */
    public void setSavedLocally(boolean savedLocally) {
        this.savedLocally = savedLocally;
    }

    public void delete() {
        deleted = true;
        try {
            PreparedStatement statement = DataBase.getUserConnection().prepareStatement(
                    "delete from GameEntry where id = ?;");
            statement.setInt(1, id);
            statement.execute();

            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
            if (savedLocally && !deleted) {
                PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set igdb_id = ? where id = ?");
                statement.setInt(1, igdb_id);
                statement.setInt(2, id);
                statement.execute();

                statement.close();
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
        if (savedLocally && !deleted) {
            try {
                Statement batchStatement = DataBase.getUserConnection().createStatement();
                saveGenres(batchStatement);
                batchStatement.executeBatch();
                batchStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public ArrayList<GameTheme> getThemes() {
        return themes;
    }

    public void setThemes(ArrayList<GameTheme> themes) {
        this.themes = themes;
        if (savedLocally && !deleted) {
            try {
                Statement batchStatement = DataBase.getUserConnection().createStatement();
                saveThemes(batchStatement);
                batchStatement.executeBatch();
                batchStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public Serie getSerie() {
        return serie;
    }

    public void setSerie(Serie serie) {
        if (serie == null) {
            this.serie = Serie.NONE;
        }
        this.serie = serie;
        if (savedLocally && !deleted) {
            try {
                Statement batchStatement = DataBase.getUserConnection().createStatement();
                saveSerie(batchStatement);
                batchStatement.executeBatch();
                batchStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(int platformId) {
        setPlatform(Platform.getFromId(platformId));
    }

    public void setPlatform(Platform platform) {
        if (platform == null) {
            this.platform = Platform.NONE;
        }
        this.platform = platform;

        if (platform.getId() == Platform.STEAM_ID || platform.getId() == Platform.STEAM_ONLINE_ID) {
            setPath("steam://rungameid/" + platformGameId);
        }
        if (savedLocally && !deleted) {
            try {
                Statement batchStatement = DataBase.getUserConnection().createStatement();
                savePlatform(batchStatement);
                batchStatement.executeBatch();
                batchStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
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
            if (savedLocally && !deleted) {
                PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set added_date = ? where id = ?");
                statement.setTimestamp(1, Timestamp.valueOf(addedDate));
                statement.setInt(2, id);
                statement.execute();

                statement.close();
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
            if (savedLocally && !deleted) {
                PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set installed = ? where id = ?");
                statement.setBoolean(1, installed);
                statement.setInt(2, id);
                statement.execute();

                statement.close();
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
            if (savedLocally && !deleted) {
                PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set last_played_date = ? where id = ?");
                statement.setTimestamp(1, Timestamp.valueOf(addedDate));
                statement.setInt(2, id);
                statement.execute();

                statement.close();
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
            if (savedLocally && !deleted) {
                PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set waiting_scrap = ? where id = ?");
                statement.setBoolean(1, waitingToBeScrapped);
                statement.setInt(2, id);
                statement.execute();

                statement.close();
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
        new GameStarter(this).start();
    }

    @Override
    public String toString() {
        return "GameEntry:name=" + name +
                ",release_date=" + (releaseDate != null ? DATE_DISPLAY_FORMAT.format(releaseDate) : null) +
                ",platform=" + platform.getName() +
                ",platform_game_id=" + platformGameId +
                "playTime=" + getPlayTimeFormatted(TIME_FORMAT_FULL_DOUBLEDOTS);
    }

    public boolean isToAdd() {
        return toAdd;
    }

    public void setToAdd(boolean toAdd) {
        this.toAdd = toAdd;
        try {
            if (savedLocally && !deleted) {
                PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set toAdd = ? where id = ?");
                statement.setBoolean(1, toAdd);
                statement.setInt(2, id);
                statement.execute();

                statement.close();
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
            if (savedLocally && !deleted) {
                PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set yt_hash = ? where id = ?");
                statement.setString(1, youtubeSoundtrackHash);
                statement.setInt(2, id);
                statement.execute();

                statement.close();
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
            if (savedLocally && !deleted) {
                PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set launch_args = ? where id = ?");
                statement.setString(1, args);
                statement.setInt(2, id);
                statement.execute();

                statement.close();
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
            if (savedLocally && !deleted) {
                PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set ignored = ? where id = ?");
                statement.setBoolean(1, ignored);
                statement.setInt(2, id);
                statement.execute();
                statement.close();
                GameEntryUtils.loadIgnoredGames();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void reloadFromDB() {
        try {
            Statement s = DataBase.getUserConnection().createStatement();
            ResultSet set = s.executeQuery("select * from GameEntry where id = " + id);
            if (set.next()) {
                reloadWithSet(set);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void reloadWithSet(ResultSet set) throws SQLException {
        if (set == null) {
            throw new SQLException("Given set is null");
        }
        setId(set.getInt("id"));
        inDb = true;
        setSavedLocally(false);
        setName(set.getString("name"));
        setDescription(set.getString("description"));
        setPath(set.getString("path"));
        setCmd(GameEntry.CMD_BEFORE_START, set.getString("cmd_before"));
        setCmd(GameEntry.CMD_AFTER_END, set.getString("cmd_after"));
        setYoutubeSoundtrackHash(set.getString("yt_hash"));

        Timestamp addedTimestamp = set.getTimestamp("added_date");
        if (addedTimestamp != null) {
            setAddedDate(addedTimestamp.toLocalDateTime());
        } else {
            setAddedDate(LocalDateTime.now());
        }

        Timestamp releasedTimestamp = set.getTimestamp("release_date");
        if (releasedTimestamp != null) {
            setReleaseDate(releasedTimestamp.toLocalDateTime());
        }

        Timestamp lastPlayedTimestamp = set.getTimestamp("last_played_date");
        if (lastPlayedTimestamp != null) {
            setLastPlayedDate(lastPlayedTimestamp.toLocalDateTime());
        }

        setPlayTimeSeconds(set.getInt("initial_playtime"));
        setInstalled(set.getBoolean("installed"));
        setIgdb_imageHash(0, set.getString("cover_hash"));
        setIgdb_imageHash(1, set.getString("wp_hash"));
        setIgdb_id(set.getInt("igdb_id"));
        setWaitingToBeScrapped(set.getBoolean("waiting_scrap"));
        setToAdd(set.getBoolean("toAdd"));
        setIgnored(set.getBoolean("ignored"));

        //LOAD GENRES FROM DB
        try {
            PreparedStatement genreStatement = DataBase.getUserConnection().prepareStatement("SELECT genre_id FROM has_genre WHERE game_id=?");
            genreStatement.setInt(1, id);
            ResultSet genreSet = genreStatement.executeQuery();
            while (genreSet.next()) {
                int genreId = genreSet.getInt("genre_id");
                GameGenre genre = GameGenre.getGenreFromID(genreId);
                addGenre(genre);
            }
            genreStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //LOAD THEMES FROM DB
        try {
            PreparedStatement themeStatement = DataBase.getUserConnection().prepareStatement("SELECT theme_id FROM has_theme WHERE game_id=?");
            themeStatement.setInt(1, id);
            ResultSet themeSet = themeStatement.executeQuery();
            while (themeSet.next()) {
                int themeId = themeSet.getInt("theme_id");
                GameTheme theme = GameTheme.getThemeFromId(themeId);
                if (theme != null) {
                    addTheme(theme);
                }
            }
            themeStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //LOAD DEV FROM DB
        try {
            PreparedStatement devStatement = DataBase.getUserConnection().prepareStatement("SELECT dev_id FROM develops WHERE game_id=?");
            devStatement.setInt(1, id);
            ResultSet devSet = devStatement.executeQuery();
            while (devSet.next()) {
                int devId = devSet.getInt("dev_id");
                Company dev = Company.getFromId(devId);
                if (dev != null) {
                    addDeveloper(dev);
                }
            }
            devStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //LOAD PUBLISHERS FROM DB
        try {
            PreparedStatement pubStatement = DataBase.getUserConnection().prepareStatement("SELECT pub_id FROM publishes WHERE game_id=?");
            pubStatement.setInt(1, id);
            ResultSet pubSet = pubStatement.executeQuery();
            while (pubSet.next()) {
                int publisherId = pubSet.getInt("pub_id");
                Company publisher = Company.getFromId(publisherId);
                if (publisher != null) {
                    addPublisher(publisher);
                }
            }
            pubStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //LOAD SERIE FROM DB
        try {
            PreparedStatement serieStatement = DataBase.getUserConnection().prepareStatement("SELECT serie_id FROM regroups WHERE game_id=?");
            serieStatement.setInt(1, id);
            ResultSet serieSet = serieStatement.executeQuery();
            while (serieSet.next()) {
                int serieId = serieSet.getInt("serie_id");
                Serie serie = Serie.getFromId(serieId);
                if (serie != null) {
                    setSerie(serie);
                }
            }
            serieStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //LOAD PLATFORM FROM DB
        try {
            PreparedStatement platformStatement = DataBase.getUserConnection().prepareStatement("SELECT * FROM runs_on WHERE game_id=?");
            platformStatement.setInt(1, id);
            ResultSet platformSet = platformStatement.executeQuery();
            if (platformSet.next()) {
                int platformId = platformSet.getInt("platform_id");
                platformGameId = platformSet.getInt("platformGameId");
                Platform platform = Platform.getFromId(platformId);
                if (platform != null) {
                    setPlatform(platform);
                }
            }
            platformStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static GameEntry loadFromDB(ResultSet set) throws SQLException {
        GameEntry entry = new GameEntry("need_to_reload");
        entry.reloadWithSet(set);
        return entry;
    }

    private String getCoverPath() {
        return GameEntryUtils.coverPath(this);
    }

    private String getScreenShotPath() {
        return GameEntryUtils.screenshotPath(this);
    }

    public static String getSQLInitLine() {
        StringBuilder temp = new StringBuilder("INSERT OR REPLACE INTO GameEntry (");

        for (int i = 0; i < SQL_PARAMS.length; i++) {
            temp.append(SQL_PARAMS[i]);
            if (i != SQL_PARAMS.length - 1) {
                temp.append(",");
            }
        }

        temp.append(") VALUES (");
        for (int i = 0; i < SQL_PARAMS.length; i++) {
            temp.append("?");
            if (i != SQL_PARAMS.length - 1) {
                temp.append(",");
            }
        }
        return temp + ");";
    }

    public static String getSQLUpdateLine() {
        StringBuilder temp = new StringBuilder("UPDATE GameEntry set ");

        for (int i = 0; i < SQL_PARAMS.length; i++) {
            temp.append(SQL_PARAMS[i]).append("=?");
            if (i != SQL_PARAMS.length - 1) {
                temp.append(", ");
            }
        }

        temp.append(" WHERE id = ?");
        return temp.toString();
    }

    public void addGenre(GameGenre genre) {
        if (genre == null) {
            return;
        }
        genres.add(genre);
        if (savedLocally && !deleted) {
            try {
                Statement batchStatement = DataBase.getUserConnection().createStatement();
                saveGenres(batchStatement);
                batchStatement.executeBatch();
                batchStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void removeGenre(GameGenre genre) {
        if (genre == null) {
            return;
        }
        genres.remove(genre);
        if (savedLocally && !deleted) {
            try {
                Statement batchStatement = DataBase.getUserConnection().createStatement();
                saveGenres(batchStatement);
                batchStatement.executeBatch();
                batchStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void addTheme(GameTheme theme) {
        if (theme == null) {
            return;
        }
        themes.add(theme);
        if (savedLocally && !deleted) {
            try {
                Statement batchStatement = DataBase.getUserConnection().createStatement();
                saveThemes(batchStatement);
                batchStatement.executeBatch();
                batchStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void removeTheme(GameTheme theme) {
        if (theme == null) {
            return;
        }
        themes.remove(theme);
        if (savedLocally && !deleted) {
            try {
                Statement batchStatement = DataBase.getUserConnection().createStatement();
                saveThemes(batchStatement);
                batchStatement.executeBatch();
                batchStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void addDeveloper(Company dev) {
        if (dev == null) {
            return;
        }
        developers.add(dev);
        if (savedLocally && !deleted) {
            try {
                Statement batchStatement = DataBase.getUserConnection().createStatement();
                saveDevs(batchStatement);
                batchStatement.executeBatch();
                batchStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void removeDeveloper(Company dev) {
        if (dev == null) {
            return;
        }
        developers.remove(dev);
        if (savedLocally && !deleted) {
            try {
                Statement batchStatement = DataBase.getUserConnection().createStatement();
                saveDevs(batchStatement);
                batchStatement.executeBatch();
                batchStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * You should never edit this list directly
     *
     * @return a list containing all devs
     */
    public ArrayList<Company> getDevelopers() {
        return developers;
    }

    public void setDevelopers(ArrayList<Company> developers) {
        this.developers = developers;
        if (savedLocally && !deleted) {
            try {
                Statement batchStatement = DataBase.getUserConnection().createStatement();
                saveDevs(batchStatement);
                batchStatement.executeBatch();
                batchStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public ArrayList<Company> getPublishers() {
        return publishers;
    }

    public void setPublishers(ArrayList<Company> publishers) {
        this.publishers = publishers;
        if (savedLocally && !deleted) {
            try {
                Statement batchStatement = DataBase.getUserConnection().createStatement();
                savePublishers(batchStatement);
                batchStatement.executeBatch();
                batchStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void addPublisher(Company dev) {
        if (dev == null) {
            return;
        }
        publishers.add(dev);
        if (savedLocally && !deleted) {
            try {
                Statement batchStatement = DataBase.getUserConnection().createStatement();
                savePublishers(batchStatement);
                batchStatement.executeBatch();
                batchStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void removePublisher(Company dev) {
        if (dev == null) {
            return;
        }
        publishers.remove(dev);
        if (savedLocally && !deleted) {
            try {
                Statement batchStatement = DataBase.getUserConnection().createStatement();
                savePublishers(batchStatement);
                batchStatement.executeBatch();
                batchStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void setId(int id) {
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
        }
    }

    public void setPlatformGameId(int platformGameId) {
        this.platformGameId = platformGameId;
        if (platform.getId() == Platform.STEAM_ID || platform.getId() == Platform.STEAM_ONLINE_ID) {
            setPath("steam://rungameid/" + platformGameId);
        }
        if (savedLocally && !deleted) {
            try {
                Statement batchStatement = DataBase.getUserConnection().createStatement();
                savePlatform(batchStatement);
                batchStatement.executeBatch();
                batchStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean mustRunAsAdmin() {
        return runAsAdmin;
    }

    public void setRunAsAdmin(Boolean runAsAdmin) {
        this.runAsAdmin = runAsAdmin;
        this.path = path.trim();
        try {
            if (savedLocally && !deleted) {
                PreparedStatement statement = DataBase.getUserConnection().prepareStatement("update GameEntry set runAsAdmin = ? where id = ?");
                statement.setBoolean(1, runAsAdmin);
                statement.setInt(2, id);
                statement.execute();

                statement.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isInDb() {
        return inDb;
    }
}

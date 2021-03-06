package com.gameroom.data.migration;

import com.gameroom.data.game.entry.*;
import com.gameroom.data.game.scraper.SteamPreEntry;
import com.gameroom.data.io.DataBase;
import com.gameroom.data.io.FileUtils;
import com.gameroom.ui.Main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.gameroom.ui.Main.FILES_MAP;
import static com.gameroom.ui.Main.LOGGER;

/**
 * Created by LM on 02/07/2016.
 * This class is deprecated and intended to be used only to migrate file-saved GameEntries to the database
 */
public class OldGameEntry {

    public final static DateFormat DATE_STORE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss SSS");
    private final static DateFormat DATE_OLD_STORE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    private final static int IMAGES_NUMBER = 3;

    private int sqlId;

    private String name = "";
    private Date releaseDate;
    private String description = "";
    private String developer = "";
    private String publisher = "";
    private OldGenre[] genres;
    private OldTheme[] themes;
    private String serie = "";
    private int aggregated_rating;
    private String path = "";
    private UUID uuid;
    private String[] cmd = new String[4];
    private String args = "";
    private String youtubeSoundtrackHash = "";
    private Date addedDate;
    private Date lastPlayedDate;
    private boolean notInstalled = false;
    private boolean ignored = false;

    private File[] imagesPaths = new File[IMAGES_NUMBER];

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


    public OldGameEntry(UUID uuid, boolean toAdd) {
        this.uuid = uuid;
        this.toAdd = toAdd;
        try {
            loadEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public OldGameEntry(UUID uuid) {
        this.uuid = uuid;
        try {
            loadEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void transferOldGameEntries() {
        File toAddFolder = FILES_MAP.get("to_add");
        File gamesFolder = FILES_MAP.get("games");
        if(!toAddFolder.exists() && !gamesFolder.exists()){
            return;
        }
        File configFile = FILES_MAP.get("config.properties");
        File[] ignoredFiles = OldSettings.getIgnoredFiles();
        SteamPreEntry[] ignoredSteam = OldSettings.getIgnoredSteamApps();

        ArrayList<UUID> toAddUUIDs = readUUIDS(toAddFolder);

        ArrayList<OldGameEntry> oldEntries = new ArrayList<>();
        for (UUID uuid : toAddUUIDs) {
            OldGameEntry entry = new OldGameEntry(uuid, true);
            oldEntries.add(entry);
        }

        ArrayList<UUID> uuids = readUUIDS(gamesFolder);
        for (UUID uuid : uuids) {
            OldGameEntry entry = new OldGameEntry(uuid);
            entry.checkIgnored(ignoredFiles, ignoredSteam);
            oldEntries.add(entry);
        }

        int finalOpCount = 3;
        LoadingWindow.getInstance().setMaximumProgress(oldEntries.size()+finalOpCount);
        LoadingWindow.getInstance().show();

        try {
            StringJoiner genreSQLJoiner = new StringJoiner(",","INSERT OR IGNORE INTO has_genre(game_id,genre_id) VALUES ",";");
            StringJoiner themeSQLJoiner = new StringJoiner(",","INSERT OR IGNORE INTO has_theme(game_id,theme_id) VALUES ",";");
            StringJoiner platformSQLJoiner = new StringJoiner(",","INSERT OR IGNORE INTO runs_on(platformGameId,platform_id,game_id) VALUES ",";");
            StringJoiner devSQLJoiner = new StringJoiner(",","INSERT OR IGNORE INTO develops(game_id,dev_id) VALUES ",";");
            StringJoiner pubSQLJoiner = new StringJoiner(",","INSERT OR IGNORE INTO publishes(game_id,pub_id) VALUES ",";");
            StringJoiner serieSQLJoiner = new StringJoiner(",","INSERT OR IGNORE INTO regroups(game_id,serie_id) VALUES ",";");

            int i = 0;
            for (OldGameEntry entry : oldEntries) {
                entry.insertInDB(); //needed first to have an assigned id
                LoadingWindow.getInstance().setProgress(i++,Main.getString("updating")+" "+entry.name+"...");

                entry.exportGenres(genreSQLJoiner);
                entry.exportThemes(themeSQLJoiner);
                entry.exportPlatform(platformSQLJoiner);
                entry.exportDevs(devSQLJoiner);
                entry.exportPublishers(pubSQLJoiner);
                entry.exportSerie(serieSQLJoiner);
                entry.movePictures();
            }
            Statement statement = DataBase.getUserConnection().createStatement();

            LoadingWindow.getInstance().setProgress(i++,Main.getString("applying_changes")+"...");
            statement.addBatch(genreSQLJoiner.toString());
            statement.addBatch(themeSQLJoiner.toString());
            statement.addBatch(platformSQLJoiner.toString());
            statement.addBatch(devSQLJoiner.toString());
            statement.addBatch(pubSQLJoiner.toString());
            statement.addBatch(serieSQLJoiner.toString());
            LOGGER.debug(genreSQLJoiner.toString());
            LOGGER.debug(themeSQLJoiner.toString());
            LOGGER.debug(platformSQLJoiner.toString());
            LOGGER.debug(devSQLJoiner.toString());
            LOGGER.debug(pubSQLJoiner.toString());
            LOGGER.debug(serieSQLJoiner.toString());

            statement.executeBatch();

            LoadingWindow.getInstance().setProgress(i++,Main.getString("moving_files")+"...");
            toAddFolder.renameTo(new File(toAddFolder.getAbsolutePath() + ".bak"));
            gamesFolder.renameTo(new File(gamesFolder.getAbsolutePath() + ".bak"));
            configFile.renameTo(new File(configFile.getAbsolutePath() + ".bak"));

            LoadingWindow.getInstance().setProgress(i++,Main.getString("done")+"!");
            LoadingWindow.getInstance().dispose();


        } catch (SQLException e) {
            LoadingWindow.getInstance().setProgress(-1,Main.getString("error_initializing_db"));
            e.printStackTrace();
        }

    }


    private void checkIgnored(File[] ignoredFiles, SteamPreEntry[] ignoredSteamEntries) {
        for (File ignoredFile : ignoredFiles) {
            ignored = path.toLowerCase().trim().equals(ignoredFile.getAbsolutePath().toLowerCase().trim());
            if (ignored) {
                return;
            }
        }
        if (steam_id != -1) {
            for (SteamPreEntry steamPreEntry : ignoredSteamEntries) {
                ignored = steamPreEntry.getId() == steam_id;
                if (ignored) {
                    return;
                }
            }
        }
    }

    private void insertInDB() throws SQLException {
        Connection connection = DataBase.getUserConnection();

        PreparedStatement statement = connection.prepareStatement(GameEntry.getSQLInitLine());
        statement.setString(1, name);
        statement.setDate(2, toSqlDate(releaseDate));
        statement.setString(3, description);
        statement.setInt(4, aggregated_rating);
        statement.setString(5, path);
        statement.setString(6, cmd[0]);
        statement.setString(7, cmd[1]);
        statement.setString(8, args);
        statement.setString(9, youtubeSoundtrackHash);
        statement.setDate(10, toSqlDate(addedDate));
        statement.setDate(11, toSqlDate(lastPlayedDate));
        statement.setLong(12, playTime);
        statement.setBoolean(13, !notInstalled);
        statement.setString(14, igdb_imageHash[0]);
        statement.setString(15, igdb_imageHash[1]);
        statement.setInt(16, igdb_id);
        statement.setBoolean(17, false);
        statement.setBoolean(18, toAdd);
        statement.setBoolean(19, ignored);
        statement.setBoolean(20, false);
        statement.setString(21, "");
        statement.setString(22, "");

        statement.execute();
        statement.close();
        //connection.commit();

        sqlId = DataBase.getLastId();
        LOGGER.debug("Exported game \"" + name + "\" with id " + sqlId);
    }

    private void exportGenres(StringJoiner genreSQLJoiner) throws SQLException {
        if (genres != null) {
            for (OldGenre genre : genres) {
                int genreId = GameGenre.getIGDBId(genre.getKey());
                if (genreId != -1) {
                    genreSQLJoiner.add("("+sqlId+","+genreId+")");
                }
            }
        }
    }

    private void exportThemes(StringJoiner themeSQLJoiner) throws SQLException {
        if (themes != null) {
            for (OldTheme theme : themes) {
                int themeID = GameTheme.getIGDBId(theme.getKey());
                if (themeID != -1) {
                    themeSQLJoiner.add("(" + sqlId + "," + themeID + ")");
                }
            }
        }
    }

    private void exportPlatform(StringJoiner platformSQLJoiner) throws SQLException {
        int specificId = 0;
        int platformId = -1;

        if (steam_id != -1) {
            specificId = steam_id;
            if (!notInstalled) {
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
            platformSQLJoiner.add("("+specificId+","+platformId+","+sqlId+")");
        }

    }

    private void exportDevs(StringJoiner devSQLJoiner) throws SQLException {
        if (developer != null && !developer.isEmpty()) {
            String[] devs = developer.split(",\\s");
            for (String s : devs) {
                Company dev = new Company(s);
                int devId = dev.insertInDB(false);

                if (devId != -1) {
                    devSQLJoiner.add("(" + sqlId + "," + devId + ")");
                }
            }
        }
    }

    private void exportPublishers(StringJoiner pubSQLJoiner) throws SQLException {
        if (publisher != null && !publisher.isEmpty()) {
            String[] pubs = publisher.split(",\\s");
            for (String s : pubs) {
                Company pub = new Company(s);
                int pubId = pub.insertInDB(false);

                if (pubId != -1) {
                    pubSQLJoiner.add("(" + sqlId + "," + pubId + ")");
                }
            }
        }
    }

    private void exportSerie(StringJoiner serieSQLJoiner) throws SQLException {
        if (serie != null && !serie.isEmpty()) {
            Serie gameSerie = new Serie(serie);
            int serieId = gameSerie.insertInDB(false);

            if (gameSerie.getId() != Serie.DEFAULT_ID) {
                serieSQLJoiner.add("(" + sqlId + "," + serieId + ")");
            }
        }
    }

    private void movePictures() {
        File coverFolder = FILES_MAP.get("cover");
        File screenshotFolder = FILES_MAP.get("screenshot");
        File workingdir = Main.FILES_MAP.get("working_dir");

        if (imagesPaths != null) {

            for (int i = 0; i < imagesPaths.length; i++) {
                File f = imagesPaths[i];
                //LOGGER.debug("Image"+i+" :"+f);
                if (f != null) {
                    File absoluteFile = new File(workingdir + File.separator + f.getPath());
                    if (absoluteFile.exists()) {
                        try {
                            if (i == 0) {
                                FileUtils.copyToFolder(absoluteFile, coverFolder, sqlId + "." + FileUtils.getExtension(absoluteFile));
                            } else if (i == 1) {
                                FileUtils.copyToFolder(absoluteFile, screenshotFolder, sqlId + "." + FileUtils.getExtension(absoluteFile));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

    }

    private File propertyFile() throws IOException {
        File configFile = new File((toAdd ? Main.FILES_MAP.get("to_add") : Main.FILES_MAP.get("games")) + File.separator + uuid.toString() + File.separator + "entry.properties");

        return configFile;
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
                    } catch (ParseException dtpe) {
                        releaseDate = (DATE_OLD_STORE_FORMAT.parse(prop.getProperty("releaseDate")));
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
            genres = OldGenre.fromJson(prop.getProperty("genres"));
        }

        if (prop.getProperty("themes") != null) {
            themes = OldTheme.fromJson(prop.getProperty("themes"));
        }

        if (prop.getProperty("aggregated_rating") != null) {
            aggregated_rating = Integer.parseInt(prop.getProperty("aggregated_rating"));
        }

        for (int i = 0; i < IMAGES_NUMBER; i++) {
            if (prop.getProperty("image" + i) != null) {
                File relativeFile = FileUtils.relativizePath(new File(prop.getProperty("image" + i)), Main.FILES_MAP.get("working_dir"));
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
                    } catch (ParseException dtpe) {
                        addedDate = DATE_STORE_FORMAT.parse(prop.getProperty("addedDate"));
                    }
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if (addedDate == null) {
                addedDate = new Date();
            }
        }
        if (prop.getProperty("lastPlayedDate") != null) {
            try {
                if (!prop.getProperty("lastPlayedDate").equals("")) {
                    try {
                        lastPlayedDate = DATE_STORE_FORMAT.parse(prop.getProperty("lastPlayedDate"));
                    } catch (ParseException dtpe) {
                        lastPlayedDate = DATE_OLD_STORE_FORMAT.parse(prop.getProperty("lastPlayedDate"));
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

        input.close();

    }

    public void deleteFiles() {
        File file = new File((toAdd ? Main.FILES_MAP.get("to_add") : Main.FILES_MAP.get("games")) + File.separator + uuid.toString());
        FileUtils.deleteFolder(file);
    }

    public static java.sql.Date toSqlDate(java.util.Date javaDate) {
        java.sql.Date sqlDate = null;
        if (javaDate != null) {
            sqlDate = new java.sql.Date(javaDate.getTime());
        } else {
            return null;
        }
        return sqlDate;
    }

    public static ArrayList<UUID> readUUIDS(File entriesFolder) {
        ArrayList<UUID> uuids = new ArrayList<>();

        if (entriesFolder.exists() && entriesFolder.isDirectory()) {
            for (File gameFolder : entriesFolder.listFiles()) {
                String name = gameFolder.getName();
                try {
                    if (gameFolder.isDirectory()) {
                        uuids.add(UUID.fromString(name));
                    }
                } catch (IllegalArgumentException iae) {
                    Main.LOGGER.warn("Folder " + name + " is not a valid UUID, ignoring");
                }
            }
            Main.LOGGER.info("Loaded " + uuids.size() + " uuids from folder " + entriesFolder.getName());
        }
        return uuids;
    }
}

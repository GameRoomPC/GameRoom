package data.migration;

import com.google.gson.Gson;
import data.game.entry.*;
import data.io.DataBase;
import data.io.FileUtils;
import ui.Main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

import static ui.Main.FILES_MAP;

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
        ArrayList<UUID> toAddUUIDs = GameEntryUtils.readUUIDS(FILES_MAP.get("to_add"));

        ArrayList<OldGameEntry> oldEntries = new ArrayList<>();
        for (UUID uuid : toAddUUIDs) {
            OldGameEntry entry = new OldGameEntry(uuid, true);
            oldEntries.add(entry);
        }

        ArrayList<UUID> uuids = GameEntryUtils.readUUIDS(FILES_MAP.get("games"));
        for (UUID uuid : uuids) {
            OldGameEntry entry = new OldGameEntry(uuid);
            oldEntries.add(entry);
        }

        for (OldGameEntry entry : oldEntries) {
            entry.transferToDB();
        }

    }

    public void transferToDB() {
        try {
            exportDirectFields();
            exportGenres();
            exportThemes();
            exportPlatform();
            exportDevs();
            exportPublishers();
            exportSerie();
            movePictures();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void exportDirectFields() throws SQLException {
        Connection connection = DataBase.getConnection();

        PreparedStatement statement = connection.prepareStatement(getSQLInitLine());
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
        statement.setBoolean(17, waitingToBeScrapped);
        statement.setBoolean(18, toAdd);
        statement.setBoolean(19, beingScrapped);
        statement.execute();
        statement.close();
        connection.commit();

        sqlId = DataBase.getLastId();
    }

    private void exportGenres() throws SQLException {
        if (genres != null) {
            for (OldGenre genre : genres) {
                int genreId = GameGenre.getIGDBId(genre.getKey());
                if (genreId != -1) {
                    PreparedStatement genreStatement = DataBase.getConnection().prepareStatement("INSERT OR IGNORE INTO has_genre(game_id,genre_id) VALUES (?,?)");
                    genreStatement.setInt(1, sqlId);
                    genreStatement.setInt(2, genreId);
                    genreStatement.execute();
                    genreStatement.close();
                    DataBase.commit();
                }
            }
        }
    }

    private void exportThemes() throws SQLException {
        if (themes != null) {
            for (OldTheme theme : themes) {
                int themeID = GameTheme.getIGDBId(theme.getKey());
                if (themeID != -1) {
                    PreparedStatement genreStatement = DataBase.getConnection().prepareStatement("INSERT OR IGNORE INTO has_theme(game_id,theme_id) VALUES (?,?)");
                    genreStatement.setInt(1, sqlId);
                    genreStatement.setInt(2, themeID);
                    genreStatement.execute();
                    genreStatement.close();
                    DataBase.commit();
                }
            }
        }
    }

    private void exportPlatform() throws SQLException {
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
            PreparedStatement genreStatement = DataBase.getConnection().prepareStatement("INSERT OR IGNORE INTO runs_on(specific_id,platform_id,game_id) VALUES (?,?,?)");
            genreStatement.setInt(1, specificId);
            genreStatement.setInt(2, platformId);
            genreStatement.setInt(3, sqlId);
            genreStatement.execute();
            genreStatement.close();
            DataBase.commit();
        }

    }

    private void exportDevs() throws SQLException {
        if (developer != null && !developer.isEmpty()) {
            String[] devs = developer.split(",\\s");
            for (String s : devs) {
                Developer dev = new Developer(s);
                int devId = dev.insertInDB();

                if (devId != -1) {
                    PreparedStatement devStatement2 = DataBase.getConnection().prepareStatement("INSERT OR IGNORE INTO develops(game_id,dev_id) VALUES (?,?)");
                    devStatement2.setInt(1, sqlId);
                    devStatement2.setInt(2, devId);
                    devStatement2.execute();
                    devStatement2.close();
                    DataBase.commit();
                }
            }
        }
    }

    private void exportPublishers() throws SQLException {
        if (publisher != null && !publisher.isEmpty()) {
            String[] pubs = publisher.split(",\\s");
            for (String s : pubs) {
                Publisher pub = new Publisher(s);
                int pubId = pub.insertInDB();

                if (pubId != -1) {
                    PreparedStatement pubStatement2 = DataBase.getConnection().prepareStatement("INSERT OR IGNORE INTO publishes(game_id,pub_id) VALUES (?,?)");
                    pubStatement2.setInt(1, sqlId);
                    pubStatement2.setInt(2, pubId);
                    pubStatement2.execute();
                    pubStatement2.close();
                    DataBase.commit();
                }
            }
        }
    }

    private void exportSerie() throws SQLException {
        if (serie != null && !serie.isEmpty()) {
            Serie gameSerie = new Serie(serie);
            int serieId = gameSerie.insertInDB();

            if (serieId != -1) {
                PreparedStatement serieStatement2 = DataBase.getConnection().prepareStatement("INSERT OR IGNORE INTO regroups(game_id,serie_id) VALUES (?,?)");
                serieStatement2.setInt(1, sqlId);
                serieStatement2.setInt(2, serieId);
                serieStatement2.execute();
                serieStatement2.close();
                DataBase.commit();
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
        File dir = new File((toAdd ? Main.FILES_MAP.get("to_add") : Main.FILES_MAP.get("games")) + File.separator + uuid.toString());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File configFile = new File((toAdd ? Main.FILES_MAP.get("to_add") : Main.FILES_MAP.get("games")) + File.separator + uuid.toString() + File.separator + "entry.properties");
        if (!configFile.exists()) {
            configFile.createNewFile();
        }
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
            Gson gson = new Gson();
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

    private static String getSQLInitLine() {
        String temp = "INSERT OR IGNORE INTO GameEntry (name," +
                "release_date," +
                "description," +
                "aggregated_rating," +
                "path," +
                "cmd_before," +
                "cmd_after," +
                "launch_args," +
                "yt_hash," +
                "added_date," +
                "last_played_date," +
                "initial_playtime," +
                "installed," +
                "cover_hash," +
                "wp_hash," +
                "igdb_id," +
                "waiting_scrap," +
                "toAdd," +
                "beingScraped" +
                ") VALUES (?";
        for (int i = 0; i < 18; i++) {
            temp += ",?";
        }
        return temp + ");";
    }

    public static java.sql.Date toSqlDate(java.util.Date javaDate) {
        java.sql.Date sqlDate = null;
        if (javaDate != null) {
            sqlDate = new java.sql.Date(javaDate.getTime());
        }else{
            return null;
        }
        return sqlDate;
    }
}

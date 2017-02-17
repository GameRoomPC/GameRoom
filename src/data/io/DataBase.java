package data.io;

import data.game.entry.GameGenre;
import data.game.entry.GameTheme;
import ui.Main;

import java.io.File;
import java.sql.*;

/**
 * Created by LM on 17/02/2017.
 */
public class DataBase {
    private final static String DB_NAME = "library.db";

    private final static String TABLE_GAMES = "GAMES";
    private final static String TABLE_GAME_GENRES = "GAME_GENRES";
    private final static String TABLE_GAME_THEMES = "GAME_THEMES";
    private final static String TABLE_SETTINGS = "SETTINGS";


    private static String getDBUrl(){
        File workingDir = Main.FILES_MAP.get("working_dir");
        return "jdbc:sqlite:" + workingDir.toPath().toAbsolutePath() + File.separator + DB_NAME;
    }

    public void connect(){
        String url =  getDBUrl();

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                System.out.println("The driver name is " + meta.getDriverName());
                System.out.println("A new database has been created.");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void initDB(){
        initGameTable();
        initGameGenreTable();
        initGameThemeTable();
    }

    private void initGameTable(){
        // SQLite connection string
        String url = getDBUrl();

        // SQL statement for creating a new table
        String sql = "CREATE TABLE IF NOT EXISTS "+TABLE_GAMES+" (\n"
                + "	id integer PRIMARY KEY,\n"
                + "	name text NOT NULL,\n"
                + "	releaseDate text,\n"
                + "	description text,\n"
                + "	developers text,\n"
                + "	publishers text,\n"
                + "	serie text,\n"
                + "	cmd_before text,\n"
                + "	cmd_after text,\n"
                + "	image0 text,\n"
                + "	image1 text,\n"
                + "	playTime long,\n"
                + "	path text NOT NULL,\n"
                + "	steam_id integer,\n"
                + "	gog_id integer,\n"
                + "	origin_id integer,\n"
                + "	battlenet_id integer,\n"
                + "	uplay_id integer,\n"
                + "	igdb_id integer,\n"
                + "	game_genre_id integer,\n"
                + "	game_theme_id integer,\n"
                + "	aggregated_rating integer,\n"
                + "	addedDate text,\n"
                + "	lastPlayedDate text,\n"
                + "	notInstalled bool,\n"
                + "	waitingToBeScrapped bool,\n"
                + "	youtubeSoundtrackHash text,\n"
                + "	capacity real\n"
                + ");";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            // create a new table
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void initGameGenreTable(){
        // SQLite connection string
        String url = getDBUrl();

        // SQL statement for creating a new table
        String sql = "CREATE TABLE IF NOT EXISTS "+TABLE_GAME_GENRES+" (\n"
                + "	id integer PRIMARY KEY,\n"
                + "	name text NOT NULL,\n"
                + ");";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            // create a new table
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        GameGenre.getIgdbGenreMap().forEach((integer, gameGenre) -> {
            String addElems = "INSERT INTO "+TABLE_GAME_GENRES
                    + "(id,name) VALUES ("+integer+",\""+gameGenre.getKey()+"\")";

            try (Connection conn = DriverManager.getConnection(url);
                 Statement stmt = conn.createStatement()) {
                // create a new table
                stmt.execute(addElems);
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        });
    }

    private void initGameThemeTable(){
        // SQLite connection string
        String url = getDBUrl();

        // SQL statement for creating a new table
        String sql = "CREATE TABLE IF NOT EXISTS "+TABLE_GAME_THEMES+" (\n"
                + "	id integer PRIMARY KEY,\n"
                + "	name text NOT NULL,\n"
                + ");";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            // create a new table
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        GameTheme.getIgdbThemeMap().forEach((integer, gameTheme) -> {
            String addElems = "INSERT INTO "+TABLE_GAME_THEMES
                    + "(id,name) VALUES ("+integer+",\""+gameTheme.getKey()+"\")";

            try (Connection conn = DriverManager.getConnection(url);
                 Statement stmt = conn.createStatement()) {
                // create a new table
                stmt.execute(addElems);
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        });
    }

    private void initSettingsTable(){
        // SQLite connection string
        String url = getDBUrl();

        // SQL statement for creating a new table
        String sql = "CREATE TABLE IF NOT EXISTS "+TABLE_SETTINGS+" (\n"
                + "	id integer PRIMARY KEY,\n"
                + "	name text NOT NULL,\n"
                + ");";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            // create a new table
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}

package data.io;

import data.game.entry.GameGenre;
import data.game.entry.GameTheme;
import ui.Main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.*;
import java.util.List;

/**
 * Created by LM on 17/02/2017.
 */
public class DataBase {
    private final static String DB_NAME = "library.db";

    private final static String TABLE_GAMES = "GameEntry";
    private final static String TABLE_DEVELOPER = "Developer";
    private final static String TABLE_PUBLISHER = "Publisher";
    private final static String TABLE_GAME_GENRES = "GAME_GENRES";
    private final static String TABLE_GAME_THEMES = "GAME_THEMES";
    private final static String TABLE_SETTINGS = "SETTINGS";


    private static String getDBUrl() {
        //TODO uncomment when ready to integrate
        //File workingDir = Main.FILES_MAP.get("working_dir");
        File workingDir = new File("D:\\Desktop");
        String dbPath = workingDir.toPath().toAbsolutePath() + File.separator + DB_NAME;
        System.out.println(dbPath);
        return "jdbc:sqlite:" + dbPath;
    }

    public void connect() {
        String url = getDBUrl();

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

    public void initDB() throws IOException {
        String url = getDBUrl();

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("sql/init.sql").getFile());

        List<String> lines = Files.readAllLines(file.toPath());
        String sql = "";

        try (Connection conn = DriverManager.getConnection(url)) {

            for (String s : lines) {
                sql += s + "\n";
                if (s.contains(";")) {
                    Statement stmt = conn.createStatement();
                    stmt.execute(sql);
                    stmt.close();
                    sql = "";
                }
            }

            conn.close();


        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
        DataBase db = new DataBase();
        db.connect();
        try {
            db.initDB();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

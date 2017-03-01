package data.io;

import data.game.entry.GameGenre;
import data.game.entry.GameTheme;
import ui.Main;
import ui.dialog.GameRoomAlert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.*;
import java.util.List;

import static ui.Main.LOGGER;

/**
 * Created by LM on 17/02/2017.
 */
public class DataBase {
    private final static String DB_NAME = "library.db";
    private final static DataBase INSTANCE = new DataBase();
    private static Connection INSTANCE_CONNECTION ;

    private DataBase() {
        super();
    }

    public static void initDB() {
        try {
            INSTANCE.connect();
            INSTANCE.readAndExecSQLInit();
        } catch (IOException e) {
            //TODO localize
            GameRoomAlert.error("Error while initiliazing database : " + e.getMessage());
        }
    }

    private static String getDBUrl() {
        File workingDir = Main.FILES_MAP.get("working_dir");
        //File workingDir = new File("D:\\Desktop");
        String dbPath = workingDir.toPath().toAbsolutePath() + File.separator + DB_NAME;
        return "jdbc:sqlite:" + dbPath;
    }

    private void connect() {
        String url = getDBUrl();

        try {
            INSTANCE_CONNECTION = DriverManager.getConnection(url);
            if (INSTANCE_CONNECTION != null) {
                DatabaseMetaData meta = INSTANCE_CONNECTION.getMetaData();
                System.out.println("The driver name is " + meta.getDriverName());
                System.out.println("A new database has been created, path is \""+url+"\"");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void readAndExecSQLInit() throws IOException {
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
            LOGGER.error(e);
        }
    }

    public static void execute(String sql) {
        String url = getDBUrl();
        try (Connection conn = DriverManager.getConnection(url)) {

            Statement stmt = conn.createStatement();
            stmt.execute(sql);
            stmt.close();
            conn.close();

        } catch (SQLException e) {
            LOGGER.error("Error for query : \"" + sql + "\"");
            LOGGER.error(e);
        }
    }

    public Connection getConnection() throws SQLException {
        return INSTANCE_CONNECTION;
    }

    public static DataBase getInstance() {
        return INSTANCE;
    }

    public static void main(String[] args) {
        initDB();
    }
}

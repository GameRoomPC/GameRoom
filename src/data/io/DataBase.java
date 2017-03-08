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
    public final static String DB_NAME = "library.db";
    private final static DataBase INSTANCE = new DataBase();
    private static Connection INSTANCE_CONNECTION;

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
        File dbFile = Main.FILES_MAP.get("db");
        return "jdbc:sqlite:" + dbFile.getAbsolutePath();
    }

    private static void connect() {
        String url = getDBUrl();

        try {
            //TODO learn about transactions and isolation levels
            //it should be possible t oestablish an other connection only for Settings, that would auto-commit as we don't want
            //for example when resizing a window in EditScene have to either commit changes to a game or take the risk to discard
            //the window's size if user cancel changes
            INSTANCE_CONNECTION = DriverManager.getConnection(url);
            if (INSTANCE_CONNECTION != null) {
                INSTANCE_CONNECTION.setAutoCommit(false);
                DatabaseMetaData meta = INSTANCE_CONNECTION.getMetaData();
                System.out.println("The driver name is " + meta.getDriverName());
                System.out.println("A new database has been created, path is \"" + url + "\"");
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

    public static Connection getConnection() throws SQLException {
        if(INSTANCE_CONNECTION == null){
            connect();
        }
        return INSTANCE_CONNECTION;
    }

    public static DataBase getInstance() {
        return INSTANCE;
    }

    public static void main(String[] args) {
        initDB();
    }

    public static int getLastId() {
        int id = -1;
        try {
            PreparedStatement getIdQuery = getConnection().prepareStatement("SELECT last_insert_rowid()");
            ResultSet result = getIdQuery.executeQuery();
            id = result.getInt(1);
            result.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return id;
    }

    public static void commit() throws SQLException {
        INSTANCE_CONNECTION.commit();
    }

    public static void rollback() throws SQLException {
        INSTANCE_CONNECTION.rollback();
    }
}

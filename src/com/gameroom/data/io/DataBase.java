package com.gameroom.data.io;

import com.gameroom.ui.Main;
import com.gameroom.ui.dialog.GameRoomAlert;
import org.sqlite.SQLiteErrorCode;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static com.gameroom.ui.Main.LOGGER;

/**
 * Created by LM on 17/02/2017.
 */
public class DataBase {
    public final static String DB_NAME = "library.db";
    private final static DataBase INSTANCE = new DataBase();
    private static Connection USER_CONNECTION;

    private DataBase() {
        super();
    }

    public static ErrorReport initDB() {
        DataBase.ErrorReport report;

        try {
            connect();

            /********* INIT DATABASE **********/
            LOGGER.info("Initializing database...");
            int code = INSTANCE.executeSQLFile("init.sql");

            report = new ErrorReport(code, "Error initializing database");
            if (report.failed) {
                return report;
            }

            /********* UPDATE DATABASE **********/
            //update 1.1.0.1
            LOGGER.info("Applying update 1.1.0.1 to DB...");
            code = INSTANCE.executeSQLFile("update_1.1.0.1.sql");

            report = new ErrorReport(code, "Error applying update 1.1.0.1 to DB");
            if (report.failed) {
                return report;
            }

        } catch (IOException e) {
            LOGGER.error("Error reading SQL File");
            LOGGER.error(e);

            report = new ErrorReport(1,"File error access");
            report.failed = true;
            report.errorDetail = e.getMessage();
            return report;
        }


        return report;
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
            USER_CONNECTION = DriverManager.getConnection(url);
            if (USER_CONNECTION != null) {
                //USER_CONNECTION.setAutoCommit(false);
                DatabaseMetaData meta = USER_CONNECTION.getMetaData();
                System.out.println("The driver name is " + meta.getDriverName());
                System.out.println("DB path is \"" + url + "\"");
                //USER_CONNECTION.prepareStatement("PRAGMA foreign_keys = ON");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private int executeSQLFile(String filename) throws IOException {
        String url = getDBUrl();

        ClassLoader classLoader = getClass().getClassLoader();
        InputStream stream = classLoader.getResourceAsStream("sql/" + filename);
        BufferedReader r = new BufferedReader(new InputStreamReader(stream));

        List<String> lines = new ArrayList<>();
        String line;
        while ((line = r.readLine()) != null) {
            lines.add(line);
        }
        r.close();
        stream.close();

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

            return e.getErrorCode();
        }
        return 0;
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

    public static Connection getUserConnection() throws SQLException {
        if (USER_CONNECTION == null) {
            connect();
        }
        return USER_CONNECTION;
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
            PreparedStatement getIdQuery = getUserConnection().prepareStatement("SELECT last_insert_rowid()");
            ResultSet result = getIdQuery.executeQuery();
            id = result.getInt(1);
            result.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return id;
    }

    public static void commit() throws SQLException {
        USER_CONNECTION.commit();
    }

    public static void rollback() throws SQLException {
        USER_CONNECTION.rollback();
    }

    public static class ErrorReport {
        //flag indicating whether the access to DB failed
        private boolean failed = false;

        //put here what was attempted (init DB, updating DB)
        private String errorTitle;

        //put here what happened (access error, DB locked...)
        private String errorDetail;

        public ErrorReport(int errorCode, String errorTitle) {
            this.errorTitle = errorTitle;

            switch (errorCode) {
                case 0:
                    break;
                default:
                    LOGGER.error("SQL Error Code: " + errorCode);
                    LOGGER.error("SQL Error Message: " + SQLiteErrorCode.getErrorCode(errorCode).message);

                    failed = true;
                    errorDetail = SQLiteErrorCode.getErrorCode(errorCode).message;
            }
        }

        public String toString() {
            return errorTitle + ": " + errorDetail;
        }

        public boolean hasFailed() {
            return failed;
        }
    }
}

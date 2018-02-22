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

    //used to check whether some update script should be applied
    private int dbVersion = 0;

    private DataBase() {
        super();
    }

    /**
     * Initializes the connection to the Database (see {@link #connect()} and then apply updates SQL scripts if necessary
     * @return an {@link ErrorReport}  that indicates whether there was an error during the SQL operations and info about it
     */
    public static ErrorReport initDB() {
        DataBase.ErrorReport report;

        try {
            int code = connect();
            report = new ErrorReport(code, "Error connecting to database");
            if (report.failed) {
                return report;
            }

            /**********************************/
            /*         INIT DATABASE          */
            /**********************************/
            LOGGER.info("Initializing database...");
            code = INSTANCE.executeSQLFile("init.sql");

            report = new ErrorReport(code, "Error initializing database");
            if (report.failed) {
                return report;
            }

            /**********************************/
            /*        UPDATE DATABASE         */
            /**********************************/

            /***** UPDATE 1.1.0.1 *****/
            UpdateProcedure update1101 = new UpdateProcedure(1101);
            report = update1101.applyDBUpdate();
            if (report.failed) {
                return report;
            }

        } catch (IOException e) {
            LOGGER.error("Error reading SQL File");
            LOGGER.error(e);

            report = new ErrorReport(1, "File access error");
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

    private static int connect() {
        String url = getDBUrl();

        //TODO learn about transactions and isolation levels
        //it should be possible t oestablish an other connection only for Settings, that would auto-commit as we don't want
        //for example when resizing a window in EditScene have to either commit changes to a game or take the risk to discard
        //the window's size if user cancel changes
        try {
            USER_CONNECTION = DriverManager.getConnection(url);
            if (USER_CONNECTION != null) {
                //USER_CONNECTION.setAutoCommit(false);
                DatabaseMetaData meta = USER_CONNECTION.getMetaData();
                LOGGER.info("The driver name is " + meta.getDriverName());
                LOGGER.info("DB path is \"" + url + "\"");
                //USER_CONNECTION.prepareStatement("PRAGMA foreign_keys = ON");

                try (Statement statement = USER_CONNECTION.createStatement()) {
                    try (ResultSet rs = statement.executeQuery("PRAGMA user_version;")) {
                        INSTANCE.dbVersion = rs.getInt(1);
                        LOGGER.info("DB Version: " + INSTANCE.dbVersion);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error(e);
            return e.getErrorCode();
        }
        return 0;
    }

    /**
     * Execute SQL commands stored in a specified file, which is used as an update mechanism
     * @param filename name of the internal file to be applied
     * @return 0 if it went well, or the SQL error code
     * @throws IOException in case there was an issue accessing to the file
     */
    private int executeSQLFile(String filename) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream stream = classLoader.getResourceAsStream("sql/" + filename);

        if (stream == null) {
            throw new IOException("File \"" + filename + "\" not found. Please check JAR integrity");
        }

        BufferedReader r = new BufferedReader(new InputStreamReader(stream));

        List<String> lines = new ArrayList<>();
        String line;
        while ((line = r.readLine()) != null) {
            lines.add(line);
        }
        r.close();
        stream.close();

        String sql = "";

        try {

            for (String s : lines) {
                sql += s + "\n";
                if (s.contains(";")) {
                    Statement stmt = USER_CONNECTION.createStatement();
                    stmt.execute(sql);
                    stmt.close();
                    sql = "";
                }
            }
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

    /**
     * Helper class used to represent an update to be applied to the database.
     * SQL commands to execute should be stored in a .sql file in the sql folder, following pattern "update_<version>.sql"
     */
    private static class UpdateProcedure {
        public int version;
        private String fileName;

        /**
         * Creates an {@link UpdateProcedure} instance determined by the version passed as an argument
         * @param version version of the update in the int format (e.g. update 1.1.0.0 is 1100)
         */
        UpdateProcedure(int version) {
            this.version = version;
            fileName = "update_" + version + ".sql";
        }

        /**
         * Applies the update file assoc
         * @return an {@link ErrorReport} indicated whether the process has failed and text messages associated to failures
         */
        private ErrorReport applyDBUpdate() {
            //TODO use transactional connection, to commit or rollback if one step of the update fails
            ErrorReport report = new ErrorReport(0, "Update " + version + " applied");

            //checks if we should apply the DB update
            if (INSTANCE.dbVersion < version) {
                LOGGER.info("Applying update " + version + " to DB...");
                int code;
                try {
                    //execute the SQL file update
                    code = INSTANCE.executeSQLFile(fileName);
                    report = new ErrorReport(code, "Error applying update " + version + " to DB");
                    if (report.failed) {
                        return report;
                    }
                    report = updateDBVersion();
                    if (report.failed) {
                        return report;
                    }

                } catch (IOException e) {
                    LOGGER.error("Error reading SQL File");
                    LOGGER.error(e);

                    report = new ErrorReport(1, "Update " + version + ", file access error");
                    report.failed = true;
                    report.errorDetail = e.getMessage();
                    return report;
                }
            }

            return report;
        }

        /**
         * Updates the PRAGMA user_version attribute to {@link #version}
         * @return an {@link ErrorReport} indicated whether the process has failed and text messages associated to failures
         */
        private ErrorReport updateDBVersion() {
            try {
                try (Statement statement = USER_CONNECTION.createStatement()) {
                    statement.executeUpdate("PRAGMA user_version = " + version + ";");
                }
            } catch (SQLException e) {
                LOGGER.error(e);
                return new ErrorReport(e.getErrorCode(), "Error updating DB user_version to " + version);
            }
            return new ErrorReport(0, "DB user_version updated to " + version);
        }
    }

    /**
     * An object used to report if there was issues while executing SQL  statements.
     */
    public static class ErrorReport {
        //flag indicating whether the access to DB failed
        private boolean failed = false;

        //put here what was attempted (init DB, updating DB)
        private String errorTitle;

        //put here what happened (access error, DB locked...)
        private String errorDetail;

        ErrorReport(int errorCode, String errorTitle) {
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

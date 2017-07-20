package data.game;

import data.game.entry.Platform;
import data.io.DataBase;

import javax.xml.transform.Result;
import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author LM. Garret (admin@gameroom.me)
 * @date 19/07/2017.
 */
public class GameFolderManager {
    private final static String[] EXCLUDED_GAMES_FOLDERS = new String[]{
            "C:\\Program Files (x86)","C:\\Program Files","C:\\ProgramData", "C:\\"
    };
    public static List<File> getFoldersForPlatform(Platform platform){
        ArrayList<File> folders = new ArrayList<>();
        if(platform == null){
            return folders;
        }
        try {
            PreparedStatement statement = DataBase.getUserConnection().prepareStatement("SELECT path FROM GameFolder WHERE platform_id=?");
            statement.setInt(1,platform.getId());
            ResultSet set = statement.executeQuery();
            while(set.next()){
                String path = set.getString("path");
                if(path != null && !path.isEmpty()){
                    folders.add(new File(path));
                }else{
                    folders.add(null);
                }
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return folders;
    }

    public static boolean setFolderForPlatform(Platform platform, File folder){
        if(platform == null){
            throw new IllegalArgumentException("Platform was null");
        }
        if(platform.equals(Platform.NONE)){
            throw new IllegalArgumentException("Cannot set a unique folder for default platform");
        }

        try {
            PreparedStatement statement = DataBase.getUserConnection().prepareStatement("DELETE FROM GameFolder WHERE platform_id=?");
            statement.setInt(1,platform.getId());
            statement.addBatch("INSERT INTO GameFolder(path,platform_id) VALUES (?,?)");
            statement.setString(1,folder != null ? folder.getAbsolutePath() : "");
            statement.setInt(2,platform.getId());
            statement.execute();
            statement.close();

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;

    }

    public static boolean addDefaultFolder(File folder){
        try {
            PreparedStatement statement = DataBase.getUserConnection().prepareStatement("INSERT INTO GameFolder(path,platform_id) VALUES (?,?)");
            statement.setString(1,folder != null ? folder.getAbsolutePath() : "");
            statement.setInt(2,Platform.NONE.getId());
            statement.execute();
            statement.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean deleteDefaultFolder(File folder){
        try {
            PreparedStatement statement = DataBase.getUserConnection().prepareStatement("DELETE FROM GameFolder WHERE platform_id=? AND path=?");
            statement.setInt(1,Platform.NONE.getId());
            statement.setString(1,folder != null ? folder.getAbsolutePath() : "");
            statement.execute();
            statement.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isFolderExcluded(File folder){
        if(folder == null){
            return false;
        }
        for(String s : EXCLUDED_GAMES_FOLDERS){
            if(folder.getAbsolutePath().equals(s)){
                return true;
            }
        }
        return false;
    }
}

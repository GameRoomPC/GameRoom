package data.game.entry;

import data.io.DataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by LM on 02/03/2017.
 */
public class Publisher {
    private int igdb_id = -1;
    private String name;
    private boolean id_needs_update;

    public Publisher(int igdb_id, String name) {
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("Publisher's name was either null or empty : \""+name+"\"");
        }
        this.igdb_id = igdb_id;
        this.name = name;
    }

    //TODO implement a method to check if id_needs_update in db and do it if valid igdb_id. Same for Developer

    public Publisher(String name) {
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("Publisher's name was either null or empty : \""+name+"\"");
        }
        this.name = name;
    }

    public int insertInDB(){
        try {
            String sql = "INSERT OR IGNORE INTO Publisher(name,"+ (igdb_id < 0 ? "id_needs_update) VALUES (?,?)" : "igdb_id) VALUES (?,?)");
            PreparedStatement pubStatement = DataBase.getConnection().prepareStatement(sql);
            pubStatement.setString(1, name);
            if(igdb_id >= 0){
                pubStatement.setInt(2,igdb_id);
            }else{
                pubStatement.setInt(2,1);
            }
            pubStatement.execute();
            pubStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        igdb_id = getIdInDb();
        return igdb_id;
    }

    private int getIdInDb(){
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("Publisher's name was either null or empty : \""+name+"\"");
        }
        int id = -1;
        try {
            Connection connection = DataBase.getConnection();
            PreparedStatement getIdQuery = connection.prepareStatement("SELECT igdb_id FROM Publisher WHERE name = ?");
            getIdQuery.setString(1,name);
            ResultSet result = getIdQuery.executeQuery();

            if(result.next()){
                id = result.getInt(1);
                result.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return id;
    }
}

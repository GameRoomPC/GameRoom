package data.game.entry;

import data.io.DataBase;

import java.sql.*;
import java.util.Collection;
import java.util.HashMap;

/**
 * Created by LM on 02/03/2017.
 */
public class Company {
    private final static int DEFAULT_ID = -1;
    private final static HashMap<Integer, Company> ID_MAP = new HashMap<>();
    private int igdb_id = DEFAULT_ID;
    private int id = DEFAULT_ID;
    private String name;
    private int IGDBId;


    public Company(int igdb_id, String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Company's name was either null or empty : \"" + name + "\"");
        }
        this.igdb_id = igdb_id;
        this.name = name;

        insertInDB();
    }

    public Company(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Company's name was either null or empty : \"" + name + "\"");
        }
        this.name = name;
    }

    public int insertInDB() {
        try {
            if(isInDB()){
                id = getIdInDb();
                String sql = "UPDATE Company set name_key=?"+(igdb_id < 0 ? "" : ", igdb_id=?")+" where id="+id;
                PreparedStatement companyStatement = DataBase.getUserConnection().prepareStatement(sql);
                companyStatement.setString(1, name);
                if (igdb_id >= 0) {
                    companyStatement.setInt(2, igdb_id);
                }
                companyStatement.execute();
                companyStatement.close();
            }else {
                String sql = "INSERT OR IGNORE INTO Company(name_key," + (igdb_id < 0 ? "id_needs_update) VALUES (?,?)" : "igdb_id) VALUES (?,?)");
                PreparedStatement companyStatement = DataBase.getUserConnection().prepareStatement(sql);
                companyStatement.setString(1, name);
                if (igdb_id >= 0) {
                    companyStatement.setInt(2, igdb_id);
                } else {
                    companyStatement.setInt(2, 1);
                }
                companyStatement.execute();
                companyStatement.close();

                id = getIdInDb();
                ID_MAP.put(id, this);
            }

            return id;
            //DataBase.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return DEFAULT_ID;
    }

    private boolean isInDB(){
        return getIdInDb() != DEFAULT_ID;
    }

    private int getIdInDb() {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Company's name was either null or empty : \"" + name + "\"");
        }
        int id = DEFAULT_ID;
        try {
            Connection connection = DataBase.getUserConnection();
            PreparedStatement getIdQuery = connection.prepareStatement("SELECT id FROM Company WHERE name_key = ?");
            getIdQuery.setString(1, name);
            ResultSet result = getIdQuery.executeQuery();

            if (result.next()) {
                id = result.getInt(1);
                result.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return id;
    }

    public static Company getFromIGDBId(int igdb_id) {
        if (ID_MAP.isEmpty()) {
            try {
                initWithDb();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        for (Company company : ID_MAP.values()) {
            if (company.getIGDBId() == igdb_id) {
                return company;
            }
        }
        //try to see if it exists in db
        try {
            Connection connection = DataBase.getUserConnection();
            PreparedStatement statement = connection.prepareStatement("select * from Company where igdb_id = ?");
            statement.setInt(1, igdb_id);
            ResultSet set = statement.executeQuery();
            if (set.next()) {
                int companyId = set.getInt("id");
                String key = set.getString("name_key");
                Company newCompany = new Company(companyId, key);
                newCompany.setIGDBId(igdb_id);
                ID_MAP.put(companyId, newCompany);

                return newCompany;
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Company getFromId(int id) {
        if (ID_MAP.isEmpty()) {
            try {
                initWithDb();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        Company company = ID_MAP.get(id);

        if(company == null){
            //try to see if it exists in db
            try {
                Connection connection = DataBase.getUserConnection();
                PreparedStatement statement = connection.prepareStatement("select * from Company where id = ?");
                statement.setInt(1, id);
                ResultSet set = statement.executeQuery();
                if (set.next()) {
                    int companyId = set.getInt("id");
                    String key = set.getString("name_key");
                    Company newCompany = new Company(companyId, key);
                    newCompany.setIGDBId(set.getInt("igdb_id"));
                    ID_MAP.put(companyId, newCompany);

                    return newCompany;
                }
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return company;
    }

    private static void initWithDb() throws SQLException {
        Connection connection = DataBase.getUserConnection();
        Statement statement = connection.createStatement();
        ResultSet set = statement.executeQuery("select * from Company");
        while (set.next()) {
            int id = set.getInt("id");
            String key = set.getString("name_key");
            ID_MAP.put(id, new Company(id, key));
        }
        statement.close();
    }

    public int getIGDBId() {
        return IGDBId;
    }

    public void setIGDBId(int IGDBId) {
        this.IGDBId = IGDBId;
    }

    public String getName() {
        return name;
    }

    public static Collection<Company> values() {
        return ID_MAP.values();
    }

    public int getId() {
        return id;
    }

    public static String getDisplayString(Collection<Company> companies){
        if(companies == null || companies.isEmpty()){
            return "-";
        }
        String temp = "";
        int i = 0;
        for(Company c : companies){
            if(c!=null) {
                temp += c.getName();
                if (i != companies.size() - 1) {
                    temp += ", ";
                }
            }
            i++;
        }
        return temp;
    }

    @Override
    public String toString(){
        if(name == null){
            return "-";
        }
        return name;
    }
}

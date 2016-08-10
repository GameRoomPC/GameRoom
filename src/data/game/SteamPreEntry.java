package data.game;

/**
 * Created by LM on 10/08/2016.
 */
public class SteamPreEntry {
    private String name;
    private int id;

    public SteamPreEntry(String name, int id){
        this.name = name;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}

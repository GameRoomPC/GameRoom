package data;

import javafx.scene.Node;
import javafx.scene.image.Image;

import java.util.UUID;

/**
 * Created by LM on 02/07/2016.
 */
public class GameEntry {
    private String name = "";
    private String year = "";
    private String editor = "";
    private String path = "";
    private UUID uuid;
    private String[] imagesPaths;

    public GameEntry(String name){
        uuid = UUID.randomUUID();
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getEditor() {
        return editor;
    }

    public void setEditor(String editor) {
        this.editor = editor;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public void setImagesPaths(String[] imagesPaths) {
        this.imagesPaths = imagesPaths;
    }

    public Node createBasicImage(){
        return null;
    }

    public String getImagePath(int index){
        //TODO implement basic image if requested one is missing
        return imagesPaths[0];
    }
}

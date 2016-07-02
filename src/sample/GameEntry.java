package sample;

import javafx.animation.*;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.transform.Scale;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

import java.time.Year;
import java.util.Date;
import java.util.UUID;

import static javafx.scene.input.MouseEvent.MOUSE_EXITED;

/**
 * Created by LM on 02/07/2016.
 */
public class GameEntry {
    private String name = "";
    private String year = "";
    private String editor = "";
    private String path = "";
    private UUID uuid;
    private Image[] images;

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

    public void setImages(Image[] images) {
        this.images = images;
    }

    public Node createBasicImage(){
        return null;
    }

    public Image getImage(int index){
        //TODO implement basic image if requested one is missing
        return images[0];
    }
}

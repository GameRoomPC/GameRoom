package ui.control.drawer;

import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import ui.Main;

/**
 * Created by LM on 09/02/2017.
 */
public class SubMenu extends BorderPane {
    public final static double MIN_WIDTH_RATIO = 0.10;
    private Label titleLabel;
    private String menuId;
    private boolean active = true;
    private VBox itemsBox = new VBox();
    private Timeline openAnim;
    private Timeline closeAnim;


    public SubMenu(String menuId){
        super();
        this.menuId = menuId;
        initTitleLabel(menuId);
        setCenter(itemsBox);
        itemsBox.getStyleClass().add("items");

        itemsBox.setMinWidth(MIN_WIDTH_RATIO * Main.SCREEN_WIDTH);

        getStyleClass().add("drawer-submenu");
        setFocusTraversable(false);
        setManaged(false);
        setVisible(false);
    }

    public void initTitleLabel(String text){
        titleLabel  = new Label(Main.getString(text));
        titleLabel.getStyleClass().add("title");
        titleLabel.setPadding(new Insets(20* Main.SCREEN_HEIGHT/1080
                , 20* Main.SCREEN_HEIGHT/1080
                ,20* Main.SCREEN_HEIGHT/1080
                ,20* Main.SCREEN_HEIGHT/1080));
        setTop(titleLabel);
    }

    public void addItem(Node item){
        itemsBox.getChildren().add(item);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getMenuId() {
        return menuId;
    }

    public Timeline getOpenAnim() {
        return openAnim;
    }

    public void setOpenAnim(Timeline openAnim) {
        this.openAnim = openAnim;
    }

    public Timeline getCloseAnim() {
        return closeAnim;
    }

    public void setCloseAnim(Timeline closeAnim) {
        this.closeAnim = closeAnim;
    }

    public void unselectAllItems(){
        for(Node n : itemsBox.getChildren()){
            if(n instanceof TextItem){
                ((TextItem) n).setSelected(false);
            }
        }
    }
}

package ui.control.drawer.submenu;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import ui.Main;
import ui.control.drawer.DrawerMenu;
import ui.scene.MainScene;

import static ui.control.drawer.DrawerMenu.ANIMATION_TIME;

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

        itemsBox.getStyleClass().add("items-box");
        itemsBox.setPadding(new Insets(0,20*Main.SCREEN_WIDTH/1920,0,20*Main.SCREEN_WIDTH/1920));

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setContent(itemsBox);
        scrollPane.setMinWidth(MIN_WIDTH_RATIO * Main.SCREEN_WIDTH);

        setCenter(scrollPane);

        getStyleClass().add("drawer-submenu");
        setFocusTraversable(false);
        setManaged(false);
        setVisible(false);
    }

    public void initTitleLabel(String text){
        titleLabel  = new Label(Main.getString(text));
        BorderPane.setAlignment(titleLabel, Pos.CENTER);
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
            if(n instanceof SelectableItem){
                ((SelectableItem) n).setSelected(false);
            }
        }
    }

    /**Closes the given submenu
     *
     * @param mainScene the mainScene to draw in
     * @param drawerMenu the parent drawerMenu
     */
    public void close(MainScene mainScene, DrawerMenu drawerMenu){
            if (getOpenAnim() != null) {
                getOpenAnim().stop();
            }
            setMouseTransparent(true);
            Timeline closeAnim = new Timeline(
                    new KeyFrame(Duration.seconds(0),
                            new KeyValue(translateXProperty(), translateXProperty().getValue(), Interpolator.LINEAR),
                            new KeyValue(opacityProperty(), opacityProperty().getValue(), Interpolator.EASE_IN),
                            new KeyValue(mainScene.getBackgroundView().translateXProperty(), mainScene.getBackgroundView().getTranslateX(), Interpolator.LINEAR)),
                    new KeyFrame(Duration.seconds(ANIMATION_TIME),
                            new KeyValue(translateXProperty(), -getWidth(), Interpolator.LINEAR),
                            new KeyValue(opacityProperty(), 0, Interpolator.EASE_IN),
                            new KeyValue(mainScene.getBackgroundView().translateXProperty(), drawerMenu.getWidth() - getWidth(), Interpolator.LINEAR)
                    ));
            closeAnim.setCycleCount(1);
            closeAnim.setAutoReverse(false);
            closeAnim.setOnFinished(event -> {
                setManaged(false);
                setVisible(false);
                setActive(false);
            });
            setCloseAnim(closeAnim);
            closeAnim.play();
    }

    public void open(MainScene mainScene, DrawerMenu drawerMenu){
        boolean changingMenu = drawerMenu.getCurrentSubMenu() != null && drawerMenu.getCurrentSubMenu().isActive();

        setOpacity(0);
        setMouseTransparent(true);

        drawerMenu.setCurrentSubMenu(this);

        if (getCloseAnim() != null) {
            getCloseAnim().stop();
        }

        Timeline openAnim;
        if (changingMenu) {
            openAnim = new Timeline(
                    new KeyFrame(Duration.seconds(0),
                            new KeyValue(opacityProperty(), opacityProperty().doubleValue(), Interpolator.EASE_IN),
                            new KeyValue(mainScene.getBackgroundView().translateXProperty(), mainScene.getBackgroundView().getTranslateX(), Interpolator.LINEAR)),
                    new KeyFrame(Duration.seconds(ANIMATION_TIME),
                            new KeyValue(opacityProperty(), 1.0, Interpolator.EASE_IN),
                            new KeyValue(mainScene.getBackgroundView().translateXProperty(), drawerMenu.getButtonsPaneWidth() + getWidth(), Interpolator.LINEAR)
                    ));
        } else {
            openAnim = new Timeline(
                    new KeyFrame(Duration.seconds(0),
                            new KeyValue(translateXProperty(), -getWidth(), Interpolator.LINEAR),
                            new KeyValue(opacityProperty(), opacityProperty().doubleValue(), Interpolator.EASE_IN),
                            new KeyValue(mainScene.getBackgroundView().translateXProperty(), drawerMenu.getButtonsPaneWidth(), Interpolator.LINEAR)),
                    new KeyFrame(Duration.seconds(ANIMATION_TIME),
                            new KeyValue(translateXProperty(), 0, Interpolator.LINEAR),
                            new KeyValue(opacityProperty(), 1.0, Interpolator.EASE_IN),
                            new KeyValue(mainScene.getBackgroundView().translateXProperty(), drawerMenu.getButtonsPaneWidth() + getWidth(), Interpolator.LINEAR)
                    ));
        }
        openAnim.setCycleCount(1);
        openAnim.setAutoReverse(false);
        openAnim.setOnFinished(event -> {
            setActive(true);
            setMouseTransparent(false);
        });
        setOpenAnim(openAnim);
        openAnim.play();

        setManaged(true);
        setVisible(true);
    }
}

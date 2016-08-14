package ui.pane.gamestilepane;

import data.game.entry.GameEntry;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.*;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.BlendMode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.TilePane;
import javafx.util.Duration;
import ui.Main;
import ui.control.button.gamebutton.GameButton;
import ui.control.button.gamebutton.TileGameButton;
import ui.scene.BaseScene;
import ui.scene.MainScene;

import java.awt.*;
import java.util.*;

import static ui.control.button.gamebutton.GameButton.FADE_IN_OUT_TIME;
import static ui.scene.MainScene.INPUT_MODE_KEYBOARD;

/**
 * Created by LM on 13/08/2016.
 */
public abstract class GamesTilePane extends ScrollPane{
    private final static int SORT_MODE_NAME = 0;
    private final static int SORT_MODE_RATING = 1;
    private final static int SORT_MODE_PLAY_TIME = 2;
    private final static int SORT_MODE_RELEASE_DATE = 3;

    private static int SORT_MODE = SORT_MODE_NAME;

    protected TilePane tilePane;
    protected ArrayList<GameButton> tilesList = new ArrayList<>();

    protected MainScene parentScene;

    public GamesTilePane(MainScene parentScene){
        super();
        this.tilePane = new TilePane();
        this.parentScene = parentScene;
        setFitToWidth(true);
        setFitToHeight(true);
        //centerPane.setPrefViewportHeight(tilePane.getPrefHeight());
        setFocusTraversable(false);
        setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        setContent(getScrollPaneContent());
    }

    protected abstract Node getScrollPaneContent();

    private final void addTile(GameButton button){
        addTileToTilePane(button);
        tilesList.add(button);
    }

    private final void removeTile(GameButton button){
        removeTileFromTilePane(button);
        tilesList.remove(button);
    }

    public final void removeGame(GameEntry entry){
        ArrayList<GameButton> toRemoveButtons = new ArrayList<>();
        for(Integer index : indexesOfTile(entry)){
            toRemoveButtons.add(tilesList.get(index));
        }
        for(GameButton button : toRemoveButtons){
            removeTile(button);
        }
        sort();
    }

    public final void addGame(GameEntry newEntry){
        addTile(createGameButton(newEntry));
        sort();
    }

    public final void updateGame(GameEntry newEntry){
        for(Integer index : indexesOfTile(newEntry)){
            tilesList.get(index).reloadWith(newEntry);
        }
        sort();
    }



    protected ArrayList<Integer> indexesOfTile(GameEntry entry) {
        ArrayList<Integer> integers = new ArrayList<>();
        int i = 0;
        for (Node n : tilesList) {
            if (((TileGameButton) n).getEntry().getUuid().equals(entry.getUuid())) {
                integers.add(i);
                break;
            }
            i++;
        }
        return integers;
    }
    protected abstract void removeTileFromTilePane(GameButton button);

    protected abstract void addTileToTilePane(GameButton button);

    protected abstract GameButton createGameButton(GameEntry newEntry);

    public void sort(){
        switch (SORT_MODE){
            case SORT_MODE_NAME :
                sortByName();;
                break;
            case SORT_MODE_PLAY_TIME :
                sortByTimePlayed();
                break;
            case SORT_MODE_RATING:
                sortByRating();
                break;
            case SORT_MODE_RELEASE_DATE:
                sortByReleaseDate();
                break;
            default:
                sortByName();
                break;
        }
    }

    protected static ObservableList<Node> sortByName(ObservableList<Node> nodes) {
        SORT_MODE = SORT_MODE_NAME;

        nodes.sort(new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                String name1 = ((GameButton) o1).getEntry().getName();
                String name2 = ((GameButton) o2).getEntry().getName();
                return name1.compareToIgnoreCase(name2);
            }
        });
        return nodes;
    }

    protected static ObservableList<Node> sortByRating(ObservableList<Node> nodes) {
        SORT_MODE = SORT_MODE_RATING;
        nodes.sort(new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                int rating1 = ((GameButton) o1).getEntry().getAggregated_rating();
                int rating2 = ((GameButton) o2).getEntry().getAggregated_rating();
                int result = rating1 > rating2 ? -1 : 1;
                if(rating1 == rating2){
                    String name1 = ((GameButton) o1).getEntry().getName();
                    String name2 = ((GameButton) o2).getEntry().getName();
                    result = name1.compareToIgnoreCase(name2);
                }
                return result;
            }
        });
        return nodes;
    }
    protected static ObservableList<Node> sortByTimePlayed(ObservableList<Node> nodes) {
        SORT_MODE = SORT_MODE_PLAY_TIME;

        nodes.sort(new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                long rating1 = ((GameButton) o1).getEntry().getPlayTimeSeconds();
                long rating2 = ((GameButton) o2).getEntry().getPlayTimeSeconds();
                int result = rating1 > rating2 ? -1 : 1;
                if(rating1 == rating2){
                    String name1 = ((GameButton) o1).getEntry().getName();
                    String name2 = ((GameButton) o2).getEntry().getName();
                    result = name1.compareToIgnoreCase(name2);
                }
                return result;
            }
        });
        return nodes;
    }
    protected static ObservableList<Node> sortByReleaseDate(ObservableList<Node> nodes) {
        SORT_MODE = SORT_MODE_RELEASE_DATE;

        nodes.sort(new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                int result = 0;
                Date date1 = ((GameButton) o1).getEntry().getReleaseDate();
                Date date2 = ((GameButton) o2).getEntry().getReleaseDate();

                if(date1 == null && date2 !=null){
                    return -1;
                }else if(date2 == null && date1!=null){
                    return 1;
                }else if(date1 == null && date2 == null){
                    result = 0;
                }else{
                    result = date2.compareTo(date1);
                }
                if(result == 0){
                    String name1 = ((GameButton) o1).getEntry().getName();
                    String name2 = ((GameButton) o2).getEntry().getName();
                    result = name1.compareToIgnoreCase(name2);
                }

                return result;
            }
        });
        return nodes;
    }
    protected void replaceChildrensAfterSort(ObservableList<Node> nodes, Runnable betweenTransition){

        Timeline fadeOutTimeline = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(tilePane.opacityProperty(), tilePane.opacityProperty().getValue(), Interpolator.EASE_IN)),
                new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                        new KeyValue(tilePane.opacityProperty(), 0, Interpolator.EASE_OUT)
                ));
        fadeOutTimeline.setCycleCount(1);
        fadeOutTimeline.setAutoReverse(false);
        fadeOutTimeline.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                tilePane.getChildren().setAll(nodes);
                if(betweenTransition!=null){
                    betweenTransition.run();
                }
                Timeline fadeInTimeline = new Timeline(
                        new KeyFrame(Duration.seconds(0),
                                new KeyValue(tilePane.opacityProperty(), 0, Interpolator.EASE_IN)),
                        new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                                new KeyValue(tilePane.opacityProperty(), 1, Interpolator.EASE_OUT)
                        ));
                fadeInTimeline.setCycleCount(1);
                fadeInTimeline.setAutoReverse(false);
                fadeInTimeline.play();
            }
        });
        fadeOutTimeline.play();
    }
    public final void setPrefTileWidth(double value){
        tilePane.setPrefTileWidth(value);
    }
    public final void setPrefTileHeight(double value){
        tilePane.setPrefTileHeight(value);
    }

    public abstract void sortByReleaseDate();

    public abstract void sortByRating();

    public abstract void sortByTimePlayed();

    public abstract void sortByName();


}

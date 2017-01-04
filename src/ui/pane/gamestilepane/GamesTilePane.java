package ui.pane.gamestilepane;

import data.game.entry.GameEntry;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.TilePane;
import javafx.util.Duration;
import ui.Main;
import ui.control.button.gamebutton.GameButton;
import ui.scene.MainScene;

import java.util.Comparator;
import java.util.Date;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ui.Main.MAIN_SCENE;
import static ui.control.button.gamebutton.GameButton.FADE_IN_OUT_TIME;

/**
 * Created by LM on 13/08/2016.
 */
public abstract class GamesTilePane extends BorderPane {
    private final static int SORT_MODE_NAME = 0;
    private final static int SORT_MODE_RATING = 1;
    private final static int SORT_MODE_PLAY_TIME = 2;
    private final static int SORT_MODE_RELEASE_DATE = 3;

    private static int SORT_MODE = SORT_MODE_NAME;

    private final static int QUICKSEARCH_CLEAR_DELAY = 500;

    TilePane tilePane;
    Label titleLabel;
    ObservableList<GameButton> tilesList = FXCollections.observableArrayList();

    MainScene parentScene;
    private boolean forcedHidden = false;
    private boolean searching = false;
    boolean hidden = false;

    boolean automaticSort = true;

    private boolean quickSearchEnabled = false;
    private char previousTypedChar;
    private char repeatedCharCounter = 0;

    private boolean displayGamesCount = true;



    public boolean isQuickSearchEnabled() {
        return quickSearchEnabled;
    }

    public void setQuickSearchEnabled(boolean quickSearchEnabled) {
        this.quickSearchEnabled = quickSearchEnabled;
    }

    GamesTilePane(MainScene parentScene) {
        super();
        this.tilePane = new TilePane();
        this.titleLabel = new Label();
        this.parentScene = parentScene;
        //centerPane.setPrefViewportHeight(tilePane.getPrefHeight());
        setCenter(getTilePane());
        setTop(titleLabel);
        /*titleLabel.setStyle("-fx-font-family: 'Helvetica Neue';\n" +
                "    -fx-font-size: 28.0px;\n" +
                "    -fx-stroke: black;\n" +
                "    -fx-stroke-width: 1;" +
                "    -fx-font-weight: 200;");*/
        titleLabel.setId("games-tilepane-title-label");
        BorderPane.setAlignment(titleLabel, Pos.CENTER_LEFT);
        titleLabel.setPadding(new Insets(0 * Main.SCREEN_HEIGHT / 1080
                , 10 * Main.SCREEN_WIDTH / 1920
                , 0 * Main.SCREEN_HEIGHT / 1080
                , 10 * Main.SCREEN_WIDTH / 1920));
        managedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    if (forcedHidden) {
                        hide(false);
                    }
                }
            }
        });
        visibleProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    if (forcedHidden) {
                        hide(false);
                    }
                }
            }
        });
        tilePane.getChildren().addListener(new ListChangeListener<Node>() {
            @Override
            public void onChanged(Change<? extends Node> c) {
                boolean hide = checkIfHide();
                if (hide) {
                    hide(false);
                } else {
                    show(false);
                }
            }
        });
        hide(false); //
        initQuickSearch();

    }

    private void initQuickSearch() {
        setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (quickSearchEnabled) {
                    if (!event.isShiftDown()) {
                        if (event.getCode().isLetterKey()
                                || event.getCode().isDigitKey()) {
                            String s = event.getCode().getName();
                            //TODO implement wait for other events to collect letters
                            //LOGGER.debug("Event type : "+event.getEventType()+"Typed : "+s);
                            tilesList.sort(new Comparator<GameButton>() {
                                @Override
                                public int compare(GameButton o1, GameButton o2) {
                                    return o1.getEntry().getName().toLowerCase()
                                            .compareTo(o2.getEntry().getName().toLowerCase());
                                }
                            });
                            if (previousTypedChar != s.charAt(0)) {
                                repeatedCharCounter = 0;
                            } else {
                                repeatedCharCounter++;
                            }
                            FilteredList<GameButton> matchingButtons = tilesList.filtered(new Predicate<GameButton>() {
                                @Override
                                public boolean test(GameButton button) {
                                    String name = button.getEntry().getName().toLowerCase();
                                    return name.startsWith(s.toLowerCase());
                                }
                            });
                            boolean selectedAButton = false;
                            int matchingButtonscounter = 0;
                            for (GameButton b : matchingButtons) {
                                if (matchingButtonscounter == repeatedCharCounter) {
                                    MAIN_SCENE.setInputMode(MainScene.INPUT_MODE_KEYBOARD);
                                    b.requestFocus();
                                    MAIN_SCENE.centerGameButtonInScrollPane(b, GamesTilePane.this);
                                    selectedAButton = true;
                                    break;
                                }
                                matchingButtonscounter++;
                            }
                            if(!selectedAButton && matchingButtons.size() > 0){
                                //means we got to the end of the matching buttons and need to loop

                                MAIN_SCENE.setInputMode(MainScene.INPUT_MODE_KEYBOARD);
                                matchingButtons.get(0).requestFocus();
                                MAIN_SCENE.centerGameButtonInScrollPane(matchingButtons.get(0), GamesTilePane.this);
                                selectedAButton = true;
                                repeatedCharCounter = 0;
                            }
                            previousTypedChar = s.charAt(0);
                        }
                    }
                }else{
                    MAIN_SCENE.triggerKeyPressedOnMainPane(event);
                }
            }
        });

    }

    private boolean checkIfHide() {
        boolean hide = true;
        if (tilePane.getChildren().size() > 0) {
            for (Node n : tilePane.getChildren()) {
                hide = hide && !n.isVisible();
            }
        }
        return hide;
    }

    public abstract TilePane getTilePane();

    private void addTile(GameButton button) {
        addTileToTilePane(button);
        tilesList.add(button);
    }

    private void removeTile(GameButton button) {
        removeTileFromTilePane(button);
        tilesList.remove(button);
    }

    public final void removeGame(GameEntry entry) {
        int index = indexOfTile(entry);
        if (index != -1) {
            removeTile(tilesList.get(index));
        }
        if (automaticSort)
            sort();
        updateTitleGameCount();
    }

    protected abstract boolean isValidToAdd(GameEntry entry);

    public final void addGame(GameEntry newEntry) {
        if (indexOfTile(newEntry) == -1 && isValidToAdd(newEntry)) {
            GameButton b = createGameButton(newEntry);
            //setGameButtonVisible(b,true);
            addTile(b);
            if (automaticSort) {
                sort();
            }
        }
        updateTitleGameCount();
    }

    public ObservableList<GameButton> getGameButtons() {
        return tilesList;
    }

    public final void updateGame(GameEntry newEntry) {
        int index = indexOfTile(newEntry);
        if (index != -1) {
            if (isValidToAdd(newEntry)) {
                Main.runAndWait(new Runnable() {
                    @Override
                    public void run() {
                        tilesList.get(index).reloadWith(newEntry);
                        tilesList.set(index, tilesList.get(index));//to fire updated/replaced event
                    }
                });
            } else {
                removeGame(newEntry);
            }
        } else {
            onNotFoundForUpdate(newEntry);
        }
        if (automaticSort)
            sort();
        updateTitleGameCount();
    }

    void onNotFoundForUpdate(GameEntry newEntry) {
        //by default does nothing
    }


    public final int indexOfTile(GameEntry entry) {
        int i = 0;
        for (Node n : tilesList) {
            if (((GameButton) n).getEntry().getUuid().equals(entry.getUuid())) {
                return i;
            }
            int steamId1 = ((GameButton) n).getEntry().getSteam_id();
            int steamId2 = entry.getSteam_id();
            if (steamId1 == steamId2 && steamId1 != -1) {
                return i;
            }
            i++;
        }
        return -1;
    }

    private void removeTileFromTilePane(GameButton button) {
        tilePane.getChildren().remove(button);
    }

    private void addTileToTilePane(GameButton button) {
        tilePane.getChildren().add(button);
    }

    protected abstract GameButton createGameButton(GameEntry newEntry);

    void sort() {
        switch (SORT_MODE) {
            case SORT_MODE_NAME:
                sortByName();
                break;
            case SORT_MODE_PLAY_TIME:
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

    static ObservableList<Node> sortByName(ObservableList<Node> nodes) {
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

    static ObservableList<Node> sortByRating(ObservableList<Node> nodes) {
        SORT_MODE = SORT_MODE_RATING;
        nodes.sort(new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                int rating1 = ((GameButton) o1).getEntry().getAggregated_rating();
                int rating2 = ((GameButton) o2).getEntry().getAggregated_rating();
                int result = rating1 > rating2 ? -1 : 1;
                if (rating1 == rating2) {
                    String name1 = ((GameButton) o1).getEntry().getName();
                    String name2 = ((GameButton) o2).getEntry().getName();
                    result = name1.compareToIgnoreCase(name2);
                }
                return result;
            }
        });
        return nodes;
    }

    static ObservableList<Node> sortByTimePlayed(ObservableList<Node> nodes) {
        SORT_MODE = SORT_MODE_PLAY_TIME;

        nodes.sort(new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                long rating1 = ((GameButton) o1).getEntry().getPlayTimeSeconds();
                long rating2 = ((GameButton) o2).getEntry().getPlayTimeSeconds();
                int result = rating1 > rating2 ? -1 : 1;
                if (rating1 == rating2) {
                    String name1 = ((GameButton) o1).getEntry().getName();
                    String name2 = ((GameButton) o2).getEntry().getName();
                    result = name1.compareToIgnoreCase(name2);
                }
                return result;
            }
        });
        return nodes;
    }

    static ObservableList<Node> sortByReleaseDate(ObservableList<Node> nodes) {
        SORT_MODE = SORT_MODE_RELEASE_DATE;

        nodes.sort(new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                int result = 0;
                Date date1 = ((GameButton) o1).getEntry().getReleaseDate();
                Date date2 = ((GameButton) o2).getEntry().getReleaseDate();

                if (date1 == null && date2 != null) {
                    return -1;
                } else if (date2 == null && date1 != null) {
                    return 1;
                } else if (date1 == null && date2 == null) {
                    result = 0;
                } else {
                    result = date2.compareTo(date1);
                }
                if (result == 0) {
                    String name1 = ((GameButton) o1).getEntry().getName();
                    String name2 = ((GameButton) o2).getEntry().getName();
                    result = name1.compareToIgnoreCase(name2);
                }

                return result;
            }
        });
        return nodes;
    }

    void replaceChildrensAfterSort(ObservableList<Node> nodes, Runnable betweenTransition) {
        if (!inSameOrder(tilePane.getChildren(), nodes)) {
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
                    if (betweenTransition != null) {
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
        } else {
            betweenTransition.run();
        }
    }

    public final void setPrefTileWidth(double value) {
        tilePane.setPrefTileWidth(value);
    }

    public final void setPrefTileHeight(double value) {
        tilePane.setPrefTileHeight(value);
    }

    public abstract void sortByReleaseDate();

    public abstract void sortByRating();

    public abstract void sortByTimePlayed();

    public abstract void sortByName();

    public void setTitle(String title) {
        if (title == null) {
            titleLabel.setVisible(false);
            titleLabel.setText(null);
            titleLabel.setManaged(false);
        } else {
            titleLabel.setVisible(true);
            titleLabel.setText(title);
            titleLabel.setManaged(true);
        }
    }

    public void hide() {
        hide(true);
    }

    public void show() {
        show(true);
    }

    private void hide(boolean transition) {
        if (checkIfHide() || forcedHidden) {
            hidden = true;
            Runnable hideAction = new Runnable() {
                @Override
                public void run() {
                    setManaged(false);
                    setVisible(false);
                    setMouseTransparent(true);
                }
            };
            if (transition) {
                Timeline fadeOutTimeline = new Timeline(
                        new KeyFrame(Duration.seconds(0),
                                new KeyValue(opacityProperty(), opacityProperty().getValue(), Interpolator.EASE_IN)),
                        new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                                new KeyValue(opacityProperty(), 0, Interpolator.EASE_OUT)
                        ));
                fadeOutTimeline.setCycleCount(1);
                fadeOutTimeline.setAutoReverse(false);
                fadeOutTimeline.setOnFinished(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(javafx.event.ActionEvent event) {
                        hideAction.run();
                    }
                });
                fadeOutTimeline.play();
            } else {
                hideAction.run();
            }
        }
    }

    private void show(boolean transition) {
        boolean hide = checkIfHide();
        if (!forcedHidden && !hide && (hidden || !isVisible() || !isManaged() || getOpacity() != 1.0)) {
            hidden = false;
            Runnable showAction = new Runnable() {
                @Override
                public void run() {
                    setManaged(true);
                    setVisible(true);
                    setMouseTransparent(false);
                }
            };
            if (transition) {

                Timeline fadeInTimeline = new Timeline(
                        new KeyFrame(Duration.seconds(0),
                                new KeyValue(opacityProperty(), 0, Interpolator.EASE_IN)),
                        new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                                new KeyValue(opacityProperty(), 1, Interpolator.EASE_OUT)
                        ));
                fadeInTimeline.setCycleCount(1);
                fadeInTimeline.setAutoReverse(false);
                fadeInTimeline.setOnFinished(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        showAction.run();
                    }
                });
                fadeInTimeline.play();
            } else {
                showAction.run();
            }
        }
    }

    public void setForcedHidden(boolean forcedHidden) {
        this.forcedHidden = forcedHidden;
        if (forcedHidden) {
            hide(false);
        } else {
            show(false);
        }
    }

    public int searchText(String text) {
        searching = true;
        int num = 0;
        for (GameButton button : tilesList) {
            boolean show = button.getEntry().getName().toLowerCase().contains(text.toLowerCase());
            setGameButtonVisible(button, show);
            if (show) {
                if (num == 0) {
                    //TODO implement here so that button is highlighted and if enter pressed this is launched etc for other keys
                }
                num++;
            }
        }
        return num;
    }

    public void cancelSearchText() {
        for (GameButton button : tilesList) {
            setGameButtonVisible(button, true);
        }
        searching = false;
    }

    void setGameButtonVisible(GameButton button, boolean visible) {
        button.setManaged(visible);
        button.setVisible(visible);
        button.setMouseTransparent(!visible);

        updateTitleGameCount();
    }

    private void updateTitleGameCount() {
        if(displayGamesCount) {
            int nbVisible = 0;
            for (GameButton button1 : tilesList) {
                if (button1.isVisible()) {
                    nbVisible++;
                }
            }
            String title = titleLabel.getText();
            Pattern pattern = Pattern.compile(".*\\(\\d*\\)");
            Matcher matcher = pattern.matcher(title);
            if (!matcher.find()) {
                setTitle(getTitle().getText() + " (" + nbVisible + ")");
            } else {
                title = title.replaceAll("\\(\\d*\\)", "(" + nbVisible + ")");
                setTitle(title);
            }
        }
    }

    public boolean isSearching() {
        return searching;
    }

    private static boolean inSameOrder(ObservableList<Node> nodes1, ObservableList<Node> nodes2) {
        if (nodes1.size() != nodes2.size()) {
            return false;
        }
        boolean sameOrder = true;
        for (int i = 0; i < nodes1.size() && sameOrder; i++) {
            sameOrder = ((GameButton) nodes1.get(i)).getEntry().getUuid().equals(((GameButton) nodes2.get(i)).getEntry().getUuid());
        }
        return sameOrder;
    }

    public void setAutomaticSort(boolean automaticSort) {
        this.automaticSort = automaticSort;
    }

    public Label getTitle() {
        return titleLabel;
    }

    public void setCacheGameButtons(boolean cache) {
        for (GameButton b : getGameButtons()) {
            b.setCache(cache);
        }
    }

    public boolean isDisplayGamesCount() {
        return displayGamesCount;
    }

    public void setDisplayGamesCount(boolean displayGamesCount) {
        this.displayGamesCount = displayGamesCount;
    }
}

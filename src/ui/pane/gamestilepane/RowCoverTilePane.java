package ui.pane.gamestilepane;

import data.game.entry.GameEntry;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.input.MouseEvent;
import ui.Main;
import ui.control.button.gamebutton.GameButton;
import ui.dialog.SearchDialog;
import ui.scene.MainScene;

import java.util.Comparator;
import java.util.Date;

/**
 * Created by LM on 16/08/2016.
 */
public class RowCoverTilePane extends CoverTilePane {
    public final static String TYPE_LAST_PLAYED = "last_played";
    public final static String TYPE_RECENTLY_ADDED = "recently_added";

    private Comparator<GameEntry> entriesComparator;
    private int maxColumn = 5;
    private String type;
    private Separator separator = new Separator();

    public RowCoverTilePane(MainScene parentScene, String type) {
        super(parentScene, Main.RESSOURCE_BUNDLE.getString(type));

        ScrollPane horizontalScrollPane = new ScrollPane();
        tilePane.setOrientation(Orientation.HORIZONTAL);
        horizontalScrollPane.setFitToWidth(false);
        horizontalScrollPane.setFitToHeight(true);
        horizontalScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        horizontalScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        horizontalScrollPane.minHeightProperty().bind(tilePane.heightProperty());

        this.type = type;
        tilePane.setPrefRows(1);
        tilePane.setPrefColumns(Integer.MAX_VALUE);
        this.entriesComparator = null;
        switch (type) {
            case TYPE_LAST_PLAYED:
                this.entriesComparator = new Comparator<GameEntry>() {
                    @Override
                    public int compare(GameEntry o1, GameEntry o2) {
                        int result = 0;
                        Date date1 = o1.getLastPlayedDate();
                        Date date2 = o2.getLastPlayedDate();

                        if (date1 == null && date2 != null) {
                            return 1;
                        } else if (date2 == null && date1 != null) {
                            return -1;
                        } else if (date1 == null && date2 == null) {
                            result = 0;
                        } else {
                            result = date2.compareTo(date1);
                        }
                        if (result == 0) {
                            String name1 = o1.getName();
                            String name2 = o2.getName();
                            result = name1.compareToIgnoreCase(name2);
                        }
                        return result;
                    }
                };
                break;
            case TYPE_RECENTLY_ADDED:
                this.entriesComparator = new Comparator<GameEntry>() {
                    @Override
                    public int compare(GameEntry o1, GameEntry o2) {
                        int result = 0;
                        Date date1 = o1.getAddedDate();
                        Date date2 = o2.getAddedDate();

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
                            String name1 = o1.getName();
                            String name2 = o2.getName();
                            result = name1.compareToIgnoreCase(name2);
                        }

                        return result;
                    }
                };
                break;
            default:
                break;
        }

        tilesList.addListener(new ListChangeListener<GameButton>() {
            @Override
            public void onChanged(Change<? extends GameButton> c) {
                boolean sort = false;
                while (c.next() && !sort) {
                    sort = sort || c.wasAdded() || c.wasRemoved() || c.wasReplaced() || c.wasUpdated();
                }
                if (sort) {
                    tilesList.sort(new Comparator<Node>() {
                        @Override
                        public int compare(Node o1, Node o2) {
                            return entriesComparator.compare(((GameButton) o1).getEntry(), ((GameButton) o2).getEntry());
                        }
                    });
                    boolean hideTilePane = true;
                    for (int i = 0; i < tilesList.size(); i++) {
                        boolean hide = false;

                        switch (type) {
                            case TYPE_LAST_PLAYED:
                                hide = ((GameButton) tilesList.get(i)).getEntry().getLastPlayedDate() == null;
                                break;
                            case TYPE_RECENTLY_ADDED:
                                hide = ((GameButton) tilesList.get(i)).getEntry().getAddedDate() == null;
                                break;
                            default:
                                break;
                        }
                        boolean visible = i < maxColumn && !hide;
                        tilesList.get(i).setManaged(visible);
                        tilesList.get(i).setVisible(visible);
                        tilesList.get(i).setMouseTransparent(!visible);

                        double opacity = 0;
                        if(i==0){
                            opacity = 1.0;
                        }else if( i == 1){
                            opacity = 1.0;
                        }else if( i == 2){
                            opacity = 0.9;
                        }else if( i == 3){
                            opacity = 0.7;
                        }else if( i == 4){
                            opacity = 0.5;
                        }
                        tilesList.get(i).setOpacity(opacity);

                        hideTilePane = hideTilePane && !visible;
                    }
                    setForcedHidden(hideTilePane);
                }
            }
        });
        horizontalScrollPane.setContent(getContent());
        setCenter(horizontalScrollPane);
        separator.setPadding(titleLabel.getPadding());
        separator.maxWidthProperty().bind(titleLabel.widthProperty());
        titleLabel.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if(horizontalScrollPane.isManaged()){
                    horizontalScrollPane.setManaged(false);
                    horizontalScrollPane.setVisible(false);
                    horizontalScrollPane.setMouseTransparent(true);
                }else{
                    horizontalScrollPane.setManaged(true);
                    horizontalScrollPane.setVisible(true);
                    horizontalScrollPane.setMouseTransparent(false);
                }
            }
        });

        setBottom(separator);
    }

    private void sort(ObservableList<Node> nodes, Comparator<GameEntry> comparator) {
        nodes.sort(new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                return comparator.compare(((GameButton) o1).getEntry(), ((GameButton) o2).getEntry());
            }
        });
    }

    @Override
    public void sort() {
        ObservableList<Node> nodes = FXCollections.observableArrayList(
                tilePane.getChildren()
        );
        sort(nodes, entriesComparator);
        replaceChildrensAfterSort(nodes, new Runnable() {
            @Override
            public void run() {
                for (Node button : nodes) {
                    ((GameButton) button).hidePlaytime();
                    ((GameButton) button).hideReleaseDate();
                    ((GameButton) button).hideRating();
                }
            }
        });
    }

}

package ui.pane.gamestilepane;

import data.game.entry.GameEntry;
import data.game.entry.GameGenre;
import data.game.entry.GameTheme;
import data.game.scanner.ScannerProfile;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import ui.Main;
import ui.control.button.gamebutton.GameButton;
import ui.scene.MainScene;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Created by LM on 18/08/2016.
 */
public class GroupsFactory {

    public static ArrayList<GroupRowTilePane> createGroupsByGenre(GamesTilePane originalTilePane, MainScene mainScene) {
        ArrayList<GroupRowTilePane> allTilePanes = new ArrayList<>();
        GroupRowTilePane othersTilePane = new GroupRowTilePane(mainScene) {
            @Override
            public boolean fillsRequirement(GameEntry entry) {
                boolean alreadyIn = false;
                for (int i = 0; i < getGameButtons().size() && !alreadyIn; i++) {
                    alreadyIn = indexOfTile(entry) != -1;
                    if (alreadyIn) {
                        return false;
                    }
                }
                return entry.getGenres() == null || entry.getGenres().length == 0;
            }
        };
        othersTilePane.setTitle(Main.getString("others"));
        originalTilePane.getTilePane().prefTileWidthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                othersTilePane.setPrefTileWidth(newValue.doubleValue());
            }
        });
        originalTilePane.getTilePane().prefTileHeightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                othersTilePane.setPrefTileHeight(newValue.doubleValue());
            }
        });
        othersTilePane.setPrefTileHeight(originalTilePane.getTilePane().getPrefTileHeight());
        othersTilePane.setPrefTileWidth(originalTilePane.getTilePane().getPrefTileWidth());
        for (GameGenre genre : GameGenre.values()) {
            GroupRowTilePane tilePane = new GroupRowTilePane(mainScene) {
                @Override
                public boolean fillsRequirement(GameEntry entry) {
                    boolean sameGenre = false;
                    if (entry.getGenres() == null) {
                        return false;
                    }
                    for (GameGenre gameGenre : entry.getGenres()) {
                        sameGenre = genre.equals(gameGenre);
                        if (sameGenre) {
                            break;
                        }
                    }
                    return sameGenre;
                }
            };
            tilePane.setTitle(genre.getDisplayName());
            for (GameButton button : originalTilePane.getGameButtons()) {
                tilePane.addGame(button.getEntry());
                othersTilePane.addGame(button.getEntry());
            }
            if (tilePane.getGameButtons().size() > 0) {
                originalTilePane.getTilePane().prefTileWidthProperty().addListener(new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                        tilePane.setPrefTileWidth(newValue.doubleValue());
                    }
                });
                originalTilePane.getTilePane().prefTileHeightProperty().addListener(new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                        tilePane.setPrefTileHeight(newValue.doubleValue());
                    }
                });
                tilePane.setPrefTileHeight(originalTilePane.getTilePane().getPrefTileHeight());
                tilePane.setPrefTileWidth(originalTilePane.getTilePane().getPrefTileWidth());
                allTilePanes.add(tilePane);
            }
        }
        allTilePanes.sort(new Comparator<RowCoverTilePane>() {
            @Override
            public int compare(RowCoverTilePane o1, RowCoverTilePane o2) {
                return o1.getTitle().getText().compareTo(o2.getTitle().getText());
            }
        });
        if (othersTilePane.getGameButtons().size() > 0) {
            allTilePanes.add(othersTilePane);
        }
        return allTilePanes;
    }

    public static ArrayList<GroupRowTilePane> createGroupsByTheme(GamesTilePane originalTilePane, MainScene mainScene) {
        ArrayList<GroupRowTilePane> allTilePanes = new ArrayList<>();
        GroupRowTilePane othersTilePane = new GroupRowTilePane(mainScene) {
            @Override
            public boolean fillsRequirement(GameEntry entry) {
                boolean alreadyIn = false;
                for (int i = 0; i < getGameButtons().size() && !alreadyIn; i++) {
                    alreadyIn = indexOfTile(entry) != -1;
                    if (alreadyIn) {
                        return false;
                    }
                }
                return entry.getThemes() == null || entry.getThemes().length == 0;
            }
        };
        othersTilePane.setTitle(Main.getString("others"));
        originalTilePane.getTilePane().prefTileWidthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                othersTilePane.setPrefTileWidth(newValue.doubleValue());
            }
        });
        originalTilePane.getTilePane().prefTileHeightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                othersTilePane.setPrefTileHeight(newValue.doubleValue());
            }
        });
        othersTilePane.setPrefTileHeight(originalTilePane.getTilePane().getPrefTileHeight());
        othersTilePane.setPrefTileWidth(originalTilePane.getTilePane().getPrefTileWidth());
        for (GameTheme theme : GameTheme.values()) {
            GroupRowTilePane tilePane = new GroupRowTilePane(mainScene) {
                @Override
                public boolean fillsRequirement(GameEntry entry) {
                    boolean sameTheme = false;
                    if (entry.getThemes() == null) {
                        return false;
                    }
                    for (GameTheme gameTheme : entry.getThemes()) {
                        sameTheme = theme.equals(gameTheme);
                        if (sameTheme) {
                            break;
                        }
                    }
                    return sameTheme;
                }
            };
            tilePane.setTitle(theme.getDisplayName());
            for (GameButton button : originalTilePane.getGameButtons()) {
                tilePane.addGame(button.getEntry());
                othersTilePane.addGame(button.getEntry());
            }
            if (tilePane.getGameButtons().size() > 0) {
                originalTilePane.getTilePane().prefTileWidthProperty().addListener(new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                        tilePane.setPrefTileWidth(newValue.doubleValue());
                    }
                });
                originalTilePane.getTilePane().prefTileHeightProperty().addListener(new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                        tilePane.setPrefTileHeight(newValue.doubleValue());
                    }
                });
                tilePane.setPrefTileHeight(originalTilePane.getTilePane().getPrefTileHeight());
                tilePane.setPrefTileWidth(originalTilePane.getTilePane().getPrefTileWidth());
                allTilePanes.add(tilePane);
            }
        }
        allTilePanes.sort(new Comparator<RowCoverTilePane>() {
            @Override
            public int compare(RowCoverTilePane o1, RowCoverTilePane o2) {
                return o1.getTitle().getText().compareTo(o2.getTitle().getText());
            }
        });
        if (othersTilePane.getGameButtons().size() > 0) {
            allTilePanes.add(othersTilePane);
        }
        return allTilePanes;
    }

    public static ArrayList<GroupRowTilePane> createGroupsBySerie(GamesTilePane originalTilePane, MainScene mainScene) {
        ArrayList<String> allSeries = new ArrayList<>();
        for (GameButton button : originalTilePane.getGameButtons()) {
            if (!allSeries.contains(button.getEntry().getSerie()) && button.getEntry().getSerie() != null && !button.getEntry().getSerie().trim().equals("")) {
                allSeries.add(button.getEntry().getSerie());
            }
        }
        ArrayList<GroupRowTilePane> allTilePanes = new ArrayList<>();
        GroupRowTilePane othersTilePane = new GroupRowTilePane(mainScene) {
            @Override
            public boolean fillsRequirement(GameEntry entry) {
                boolean alreadyIn = false;
                for (int i = 0; i < getGameButtons().size() && !alreadyIn; i++) {
                    alreadyIn = indexOfTile(entry) != -1;
                    if (alreadyIn) {
                        return false;
                    }
                }
                return entry.getSerie() == null || entry.getSerie().trim().equals("");
            }
        };
        othersTilePane.setTitle(Main.getString("others"));
        originalTilePane.getTilePane().prefTileWidthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                othersTilePane.setPrefTileWidth(newValue.doubleValue());
            }
        });
        originalTilePane.getTilePane().prefTileHeightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                othersTilePane.setPrefTileHeight(newValue.doubleValue());
            }
        });
        othersTilePane.setPrefTileHeight(originalTilePane.getTilePane().getPrefTileHeight());
        othersTilePane.setPrefTileWidth(originalTilePane.getTilePane().getPrefTileWidth());

        for (String serie : allSeries) {
            GroupRowTilePane tilePane = new GroupRowTilePane(mainScene) {
                @Override
                public boolean fillsRequirement(GameEntry entry) {
                    if (entry.getSerie() == null) {
                        return false;
                    }
                    return entry.getSerie().equals(serie);
                }
            };
            tilePane.setTitle(serie);
            for (GameButton button : originalTilePane.getGameButtons()) {
                tilePane.addGame(button.getEntry());
                othersTilePane.addGame(button.getEntry());
            }
            if (tilePane.getGameButtons().size() > 0) {
                originalTilePane.getTilePane().prefTileWidthProperty().addListener(new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                        tilePane.setPrefTileWidth(newValue.doubleValue());
                    }
                });
                originalTilePane.getTilePane().prefTileHeightProperty().addListener(new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                        tilePane.setPrefTileHeight(newValue.doubleValue());
                    }
                });
                tilePane.setPrefTileHeight(originalTilePane.getTilePane().getPrefTileHeight());
                tilePane.setPrefTileWidth(originalTilePane.getTilePane().getPrefTileWidth());
                allTilePanes.add(tilePane);
            }
        }
        allTilePanes.sort(new Comparator<RowCoverTilePane>() {
            @Override
            public int compare(RowCoverTilePane o1, RowCoverTilePane o2) {
                return o1.getTitle().getText().compareTo(o2.getTitle().getText());
            }
        });
        if (othersTilePane.getGameButtons().size() > 0) {
            allTilePanes.add(othersTilePane);
        }
        return allTilePanes;
    }

    public static ArrayList<GroupRowTilePane> createGroupsByLaunchers(GamesTilePane originalTilePane, MainScene mainScene) {
        ArrayList<GroupRowTilePane> allTilePanes = new ArrayList<>();
        GroupRowTilePane othersTilePane = new GroupRowTilePane(mainScene) {
            @Override
            public boolean fillsRequirement(GameEntry entry) {
                boolean alreadyIn = false;
                for (int i = 0; i < getGameButtons().size() && !alreadyIn; i++) {
                    alreadyIn = indexOfTile(entry) != -1;
                    if (alreadyIn) {
                        return false;
                    }
                }
                boolean notFromLaunchers = true;
                for(ScannerProfile launcher : ScannerProfile.values()){
                    notFromLaunchers = !isEntryFromLauncher(entry,launcher);
                    if(!notFromLaunchers){
                        break;
                    }
                }
                return notFromLaunchers;
            }
        };
        othersTilePane.setTitle(Main.getString("others"));
        originalTilePane.getTilePane().prefTileWidthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                othersTilePane.setPrefTileWidth(newValue.doubleValue());
            }
        });
        originalTilePane.getTilePane().prefTileHeightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                othersTilePane.setPrefTileHeight(newValue.doubleValue());
            }
        });
        othersTilePane.setPrefTileHeight(originalTilePane.getTilePane().getPrefTileHeight());
        othersTilePane.setPrefTileWidth(originalTilePane.getTilePane().getPrefTileWidth());
        for (ScannerProfile launcher : ScannerProfile.values()) {
            GroupRowTilePane tilePane = new GroupRowTilePane(mainScene) {
                @Override
                public boolean fillsRequirement(GameEntry entry) {
                    return isEntryFromLauncher(entry,launcher);
                }
            };
            tilePane.setTitle(launcher.toString());
            tilePane.getIconButton().setImageViewId(launcher.getIconCSSID());
            tilePane.getIconButton().setManaged(true);

            for (GameButton button : originalTilePane.getGameButtons()) {
                tilePane.addGame(button.getEntry());
                othersTilePane.addGame(button.getEntry());
            }
            if (tilePane.getGameButtons().size() > 0) {
                originalTilePane.getTilePane().prefTileWidthProperty().addListener(new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                        tilePane.setPrefTileWidth(newValue.doubleValue());
                    }
                });
                originalTilePane.getTilePane().prefTileHeightProperty().addListener(new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                        tilePane.setPrefTileHeight(newValue.doubleValue());
                    }
                });
                tilePane.setPrefTileHeight(originalTilePane.getTilePane().getPrefTileHeight());
                tilePane.setPrefTileWidth(originalTilePane.getTilePane().getPrefTileWidth());
                allTilePanes.add(tilePane);
            }
        }
        allTilePanes.sort(new Comparator<RowCoverTilePane>() {
            @Override
            public int compare(RowCoverTilePane o1, RowCoverTilePane o2) {
                return o1.getTitle().getText().compareTo(o2.getTitle().getText());
            }
        });
        if (othersTilePane.getGameButtons().size() > 0) {
            allTilePanes.add(othersTilePane);
        }
        return allTilePanes;
    }

    private static boolean isEntryFromLauncher(GameEntry entry, ScannerProfile profile){
        switch (profile){
            case STEAM:
                return entry.isSteamGame() && !entry.isInstalled();
            case STEAM_ONLINE:
                return entry.isSteamGame() && entry.isInstalled();
            case BATTLE_NET:
                return entry.isBattlenetGame();
            case GOG:
                return entry.isGoGGame();
            case ORIGIN:
                return entry.isOriginGame();
            case UPLAY:
                return entry.isUplayGame();
            default:
                return false;
        }
    }
}

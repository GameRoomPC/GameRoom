package ui.pane.gamestilepane;

import data.http.images.ImageUtils;
import data.game.entry.GameEntry;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tooltip;
import ui.Main;
import ui.control.button.ImageButton;
import ui.control.button.gamebutton.AddIgnoreGameButton;
import ui.control.button.gamebutton.GameButton;
import ui.GeneralToast;
import ui.dialog.ChoiceDialog;
import ui.scene.GameEditScene;
import ui.scene.MainScene;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;

import static ui.Main.MAIN_SCENE;
import static ui.Main.SCREEN_WIDTH;

/**
 * Created by LM on 17/08/2016.
 */
public abstract class ToAddRowTilePane extends RowCoverTilePane {

    public ToAddRowTilePane(MainScene parentScene) {
        super(parentScene, RowCoverTilePane.TYPE_RECENTLY_ADDED);
        setTitle(Main.getString("to_add"));
        maxColumn = Integer.MAX_VALUE;
        automaticSort = false;

        double imgSize =  SCREEN_WIDTH / 65;
        ImageButton addAllButton = new ImageButton("tile-addAll-button", imgSize, imgSize);
        addAllButton.setOnAction(event -> {
            parentScene.getRootStackPane().setMouseTransparent(true);
            ChoiceDialog choiceDialog = new ChoiceDialog(
                    new ChoiceDialog.ChoiceDialogButton(Main.getString("add_all_no_edit"), Main.getString("add_all_no_edit_long")),
                    new ChoiceDialog.ChoiceDialogButton(Main.getString("add_all_edit"), Main.getString("add_all_edit_long"))
            );
            choiceDialog.setTitle(Main.getString("add_all_games"));
            choiceDialog.setHeader(Main.getString("choose_action"));

            Optional<ButtonType> result = choiceDialog.showAndWait();
            result.ifPresent(letter -> {
                if (letter.getText().equals(Main.getString("add_all_no_edit"))) {
                    ArrayList<GameEntry> entries = new ArrayList<GameEntry>();
                    for (GameButton b : tilesList) { //have to do this, as MAIN_SCENE.addGame call deleteGame here -> concurrent modif exception
                        entries.add(b.getEntry());
                    }
                    for (GameEntry entry : entries) {
                        for (int i = 0; i < GameEntry.DEFAULT_IMAGES_PATHS.length; i++) {
                            String type = i == 0 ? ImageUtils.IGDB_TYPE_COVER : ImageUtils.IGDB_TYPE_SCREENSHOT;
                            GameEditScene.moveImage(entry, entry.getImagePath(i), type);
                        }


                        if (entry.isToAdd()) {
                            entry.setToAdd(false);
                        }
                        entry.setSavedLocaly(true);
                        entry.setAddedDate(LocalDateTime.now());
                        MAIN_SCENE.addGame(entry);
                    }
                    if(MAIN_SCENE!=null){
                        String end = entries.size() > 1 ? Main.getString("new_games") : Main.getString("new_game");
                        GeneralToast.displayToast(Main.getString("gameroom_has_found")+" "+entries.size()+" "+end,MAIN_SCENE.getParentStage(),GeneralToast.DURATION_LONG);
                    }

                } else if (letter.getText().equals(Main.getString("add_all_edit"))) {
                    ArrayList<GameEntry> entries = new ArrayList<GameEntry>();
                    for (GameButton b : tilesList) {
                        entries.add(b.getEntry());
                    }
                    batchAddEntries(entries);
                }
            });
            parentScene.getRootStackPane().setMouseTransparent(false);
        });
        addAllButton.setFocusTraversable(false);
        addAllButton.setTooltip(new Tooltip(Main.getString("add_all_games")));
        topBox.getChildren().add(addAllButton);
    }

    protected abstract void batchAddEntries(ArrayList<GameEntry> entries);

    @Override
    protected GameButton createGameButton(GameEntry newEntry) {
        return new AddIgnoreGameButton(newEntry, parentScene, tilePane, this);

    }

    public GameButton getGameButton(GameEntry entry) {
        if (indexOfTile(entry) != -1) {
            return getGameButtons().get(indexOfTile(entry));
        }
        return null;
    }

}

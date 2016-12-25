package ui.pane.gamestilepane;

import data.ImageUtils;
import data.game.entry.GameEntry;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import ui.Main;
import ui.control.button.ImageButton;
import ui.control.button.gamebutton.AddIgnoreGameButton;
import ui.control.button.gamebutton.GameButton;
import ui.dialog.ChoiceDialog;
import ui.scene.GameEditScene;
import ui.scene.MainScene;

import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;

import static ui.Main.*;

/**
 * Created by LM on 17/08/2016.
 */
public abstract class ToAddRowTilePane extends RowCoverTilePane {
    public ToAddRowTilePane(MainScene parentScene) {
        super(parentScene, RowCoverTilePane.TYPE_RECENTLY_ADDED);
        setTitle(Main.RESSOURCE_BUNDLE.getString("to_add"));
        maxColumn = Integer.MAX_VALUE;
        automaticSort = false;

        //ImageButton addAllButton = new ImageButton(new Image("res/ui/doubleValidIcon.png", SCREEN_WIDTH / 65, SCREEN_WIDTH / 65, true, true));
        double imgSize =  SCREEN_WIDTH / 65;
        ImageButton addAllButton = new ImageButton("tile-addAll-button", imgSize, imgSize);
        addAllButton.setOnAction(event -> {
            parentScene.getRootStackPane().setMouseTransparent(true);
            ChoiceDialog choiceDialog = new ChoiceDialog(
                    new ChoiceDialog.ChoiceDialogButton(RESSOURCE_BUNDLE.getString("add_all_no_edit"), RESSOURCE_BUNDLE.getString("add_all_no_edit_long")),
                    new ChoiceDialog.ChoiceDialogButton(RESSOURCE_BUNDLE.getString("add_all_edit"), RESSOURCE_BUNDLE.getString("add_all_edit_long"))
            );
            choiceDialog.setTitle(RESSOURCE_BUNDLE.getString("add_all_games"));
            choiceDialog.setHeader(RESSOURCE_BUNDLE.getString("choose_action"));

            Optional<ButtonType> result = choiceDialog.showAndWait();
            result.ifPresent(letter -> {
                if (letter.getText().equals(RESSOURCE_BUNDLE.getString("add_all_no_edit"))) {
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
                            entry.deleteFiles();
                            entry.setToAdd(false);
                        }
                        entry.setSavedLocaly(true);
                        entry.setAddedDate(new Date());
                        MAIN_SCENE.addGame(entry);
                    }
                } else if (letter.getText().equals(RESSOURCE_BUNDLE.getString("add_all_edit"))) {
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
        addAllButton.setTooltip(new Tooltip(Main.RESSOURCE_BUNDLE.getString("add_all_games")));
        buttonsBox.getChildren().add(addAllButton);
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

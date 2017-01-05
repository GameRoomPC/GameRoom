package ui.dialog.selector;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import system.application.settings.PredefinedSetting;
import ui.Main;
import ui.dialog.GameRoomDialog;
import ui.pane.SelectListPane;

import java.io.File;
import java.io.IOException;

import static ui.Main.GENERAL_SETTINGS;

/**
 * Created by LM on 19/08/2016.
 */
public class GameFoldersIgnoredSelector extends GameRoomDialog<ButtonType> {
        public final static int MODE_REMOVE_FROM_LIST = 0;
        public final static int MODE_ADD_TO_LIST = 1;
        private File[] selectedList;
        private Label statusLabel;

        public GameFoldersIgnoredSelector() throws IOException {
            statusLabel = new Label(Main.getString("loading")+"...");
            rootStackPane.getChildren().add(statusLabel);
            Label titleLabel = new Label(Main.getString("select_steam_games_ignore"));
            titleLabel.setWrapText(true);
            titleLabel.setTooltip(new Tooltip(Main.getString("select_steam_games_ignore")));
            titleLabel.setPadding(new Insets(0 * Main.SCREEN_HEIGHT / 1080
                    , 20 * Main.SCREEN_WIDTH / 1920
                    , 20 * Main.SCREEN_HEIGHT / 1080
                    , 20 * Main.SCREEN_WIDTH / 1920));
            mainPane.setTop(titleLabel);
            mainPane.setPadding(new Insets(30 * Main.SCREEN_HEIGHT / 1080
                    , 30 * Main.SCREEN_WIDTH / 1920
                    , 20 * Main.SCREEN_HEIGHT / 1080
                    , 30 * Main.SCREEN_WIDTH / 1920));
            BorderPane.setAlignment(titleLabel, Pos.CENTER);

            mainPane.setPrefWidth(1.0 / 3.5 * Main.SCREEN_WIDTH);
            mainPane.setPrefHeight(2.0 / 3 * Main.SCREEN_HEIGHT);

            GameFoldersList list = new GameFoldersList();

            loadIgnoredFolders(list);
            statusLabel.setText(null);

            mainPane.setCenter(list);
            list.setPrefWidth(mainPane.getPrefWidth());

            getDialogPane().getButtonTypes().addAll(
                    new ButtonType(Main.getString("ok"), ButtonBar.ButtonData.OK_DONE)
                    , new ButtonType(Main.getString("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE));

            setOnHiding(event -> {
                File[] temp_entries = new File[list.getSelectedValues().size()];
                for(int i = 0; i< temp_entries.length; i++){
                    temp_entries[i] = (File) list.getSelectedValues().get(i);
                }
                selectedList = temp_entries;
            });
        }
    private void loadIgnoredFolders(GameFoldersList list){
        File gameFolder = new File(GENERAL_SETTINGS.getString(PredefinedSetting.GAMES_FOLDER));
        if(gameFolder.exists() && gameFolder.isDirectory()){
            list.addItems(gameFolder.listFiles());
        }
        File[] ignoredFiles = GENERAL_SETTINGS.getFiles(PredefinedSetting.IGNORED_GAME_FOLDERS);
        list.addSeparator(Main.getString("others"));
        for(File ignoredFile : ignoredFiles){
            ignoredFile = new File(ignoredFile.getPath());
            boolean addFolder = true;
            if(gameFolder.exists() && gameFolder.isDirectory()) {
                for(File subFolder : gameFolder.listFiles()){
                    addFolder = !subFolder.getAbsolutePath().toLowerCase().trim().equals(ignoredFile.getAbsolutePath().toLowerCase().trim());
                    if(!addFolder){
                        break;
                    }
                }
            }
            if(addFolder){
                list.addItems(new File[]{ignoredFile});
            }
        }
    }

        public File[] getSelectedEntries() {
            return selectedList;
        }

        private static class GameFoldersList<File> extends SelectListPane {

            public GameFoldersList() {
                super( true);

            }

            @Override
            protected ListItem createListItem(Object value) {
                GameFolderItem item = new GameFolderItem(value,this);

                java.io.File[] filesIgnored = GENERAL_SETTINGS.getFiles(PredefinedSetting.IGNORED_GAME_FOLDERS);
                for (java.io.File ignoredFile : filesIgnored) {
                    if (((java.io.File)value).compareTo(ignoredFile) == 0) {
                        item.setSelected(true);
                        break;
                    }
                }
                return item;
            }
        }

        private static class GameFolderItem extends SelectListPane.ListItem {
            private File file;
            private final static int IMAGE_WIDTH = 45;
            private final static int IMAGE_HEIGHT = 45;
            private StackPane coverPane = new StackPane();

            public GameFolderItem(Object value, SelectListPane parentList) {
                super(value,parentList);
                file = ((File) value);
                addContent();
            }

            @Override
            protected void addContent() {
                ImageView iconView = new ImageView();
                double scale = GENERAL_SETTINGS.getUIScale().getScale();
                iconView.setFitHeight(32*scale);
                iconView.setFitWidth(32*scale);
                if(file.isDirectory()){
                    iconView.setId("folder-button");
                }else{
                    iconView.setImage(AppSelectorDialog.getIcon(file));
                }
                coverPane.getChildren().add(iconView);

                GridPane.setMargin(coverPane, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 10 * Main.SCREEN_WIDTH / 1920));
                add(coverPane, columnCount++, 0);

                VBox vbox = new VBox(5 * Main.SCREEN_HEIGHT / 1080);
                Label titleLabel = new Label(file.getName());
                titleLabel.setTooltip(new Tooltip(file.getAbsolutePath()));

                Label idLabel = new Label(file.getParent());
                idLabel.setStyle("-fx-font-size: 0.7em;");

                vbox.getChildren().addAll(titleLabel,idLabel);
                GridPane.setMargin(vbox, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 10 * Main.SCREEN_WIDTH / 1920));
                add(vbox, columnCount++, 0);

            }

        }
}

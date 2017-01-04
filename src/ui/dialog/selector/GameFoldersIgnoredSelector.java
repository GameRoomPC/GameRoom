package ui.dialog.selector;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
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
            statusLabel = new Label(Main.RESSOURCE_BUNDLE.getString("loading")+"...");
            rootStackPane.getChildren().add(statusLabel);
            Label titleLabel = new Label(Main.RESSOURCE_BUNDLE.getString("select_steam_games_ignore"));
            titleLabel.setWrapText(true);
            titleLabel.setTooltip(new Tooltip(Main.RESSOURCE_BUNDLE.getString("select_steam_games_ignore")));
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

            mainPane.setPrefWidth(Main.SCREEN_WIDTH * 1 / 3 * Main.SCREEN_WIDTH / 1920);
            mainPane.setPrefHeight(Main.SCREEN_HEIGHT * 2 / 3 * Main.SCREEN_HEIGHT / 1080);

            GameFoldersList list = new GameFoldersList(Main.SCREEN_HEIGHT / 3.0, mainPane.prefWidthProperty());

            loadIgnoredFolders(list);
            statusLabel.setText(null);

            mainPane.setCenter(list);

            getDialogPane().getButtonTypes().addAll(
                    new ButtonType(Main.RESSOURCE_BUNDLE.getString("ok"), ButtonBar.ButtonData.OK_DONE)
                    , new ButtonType(Main.RESSOURCE_BUNDLE.getString("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE));

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
        list.addSeparator(Main.RESSOURCE_BUNDLE.getString("others"));
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
            private ReadOnlyDoubleProperty prefRowWidth;

            public GameFoldersList(double prefHeight, ReadOnlyDoubleProperty prefRowWidth) {
                super(prefHeight, true);
                this.prefRowWidth = prefRowWidth;

            }

            @Override
            protected ListItem createListItem(Object value) {
                GameFolderItem item = new GameFolderItem(value,this);
                item.prefWidthProperty().bind(prefRowWidth);

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
            private static Image DEFAULT_FOLDER_IMAGE ;
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
                if(file.isDirectory()){
                    /*if(DEFAULT_FOLDER_IMAGE == null){
                        DEFAULT_FOLDER_IMAGE = new Image("res/ui/folderButton128.png", IMAGE_WIDTH, IMAGE_HEIGHT, true, true);
                    }*/
                    coverPane.getChildren().add(new ImageView(DEFAULT_FOLDER_IMAGE));
                    //TODO implement search of ico file or first .exe file icone if not null
                }else{
                    //TODO implement search of ico file in .exe file icone if not null
                }
                GridPane.setMargin(coverPane, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 10 * Main.SCREEN_WIDTH / 1920));
                add(coverPane, columnCount++, 0);
                Label titleLabel = new Label(file.getName());
                titleLabel.setTooltip(new Tooltip(file.getAbsolutePath()));

                GridPane.setMargin(titleLabel, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 10 * Main.SCREEN_WIDTH / 1920));
                add(titleLabel, columnCount++, 0);
                //add(idLabel, columnCount++, 0);
            }

        }
}

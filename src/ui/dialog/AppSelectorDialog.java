package ui.dialog;

import data.ImageUtils;
import data.game.scanner.FolderGameScanner;
import data.game.scrapper.OnDLDoneHandler;
import data.game.scrapper.SteamPreEntry;
import data.http.SimpleImageInfo;
import javafx.application.Platform;
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
import ui.Main;
import ui.pane.SelectListPane;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static ui.Main.GENERAL_SETTINGS;

/**
 * Created by LM on 03/01/2017.
 */
public class AppSelectorDialog extends GameRoomDialog<ButtonType> {
    private File folder;
    private File selectedFile;

    public AppSelectorDialog(File folder){
        if(folder == null || !folder.isDirectory()){
            throw new IllegalArgumentException("Given folder is either null or not a dir : \""+folder.getAbsolutePath()+"\"");
        }
        this.folder = folder;

        Label titleLabel = new Label(Main.RESSOURCE_BUNDLE.getString("select_an_app"));
        titleLabel.setWrapText(true);
        titleLabel.setTooltip(new Tooltip(Main.RESSOURCE_BUNDLE.getString("select_an_app")));
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

        ApplicationList list = new ApplicationList<>(Main.SCREEN_HEIGHT / 3.0,mainPane.prefWidthProperty());
        list.addItems(getValidAppFiles(folder));
        mainPane.setCenter(list);

        getDialogPane().getButtonTypes().addAll(
                new ButtonType(Main.RESSOURCE_BUNDLE.getString("ok"), ButtonBar.ButtonData.OK_DONE)
                , new ButtonType(Main.RESSOURCE_BUNDLE.getString("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE));

        setOnHiding(event -> {
            selectedFile = (File) list.getSelectedValue();
        });

    }

    public List<File> getValidAppFiles(File folder){
        List<File> potentialApps = new ArrayList<>();
        for(File children : folder.listFiles()){
            if(children.isDirectory()){
                potentialApps.addAll(getValidAppFiles(children));
            }else if(FolderGameScanner.fileHasValidExtension(children)){
                potentialApps.add(children);
            }
        }
        return potentialApps;
    }

    public File getSelectedFile() {
        return selectedFile;
    }

    private static class ApplicationList<File> extends SelectListPane {
        private ReadOnlyDoubleProperty prefRowWidth;

        public ApplicationList(double prefHeight, ReadOnlyDoubleProperty prefRowWidth) {
            super(prefHeight, false);
            this.prefRowWidth = prefRowWidth;

        }

        @Override
        protected ListItem createListItem(Object value) {
            AppSelectorDialog.ApplicationItem item = new AppSelectorDialog.ApplicationItem(value,this);
            item.prefWidthProperty().bind(prefRowWidth);
            return item;
        }
    }

    private static class ApplicationItem extends SelectListPane.ListItem {
        private File file;
        private final static int IMAGE_WIDTH = 100;
        private final static int IMAGE_HEIGHT = IMAGE_WIDTH *45 /120;
        private StackPane coverPane = new StackPane();
        private ImageView coverView = new ImageView();

        public ApplicationItem(Object value, SelectListPane parentList) {
            super(value,parentList);

            file = ((File) value);

            addContent();
        }

        @Override
        protected void addContent() {
            //TODO get the image of the file and set the coverpane
            //coverPane.getChildren().add(new ImageView(new Image("res/defaultImages/barTile120.jpg", IMAGE_WIDTH, IMAGE_WIDTH *45 /120, false, true)));
            //coverPane.getChildren().add(coverView);

            GridPane.setMargin(coverPane, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 10 * Main.SCREEN_WIDTH / 1920));
            add(coverPane, columnCount++, 0);
            Label titleLabel = new Label(file.getName());

            GridPane.setMargin(titleLabel, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 10 * Main.SCREEN_WIDTH / 1920));
            add(titleLabel, columnCount++, 0);
        }

    }
}



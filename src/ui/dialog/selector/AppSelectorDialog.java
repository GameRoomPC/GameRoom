package ui.dialog.selector;

import data.game.scanner.FolderGameScanner;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.embed.swing.SwingFXUtils;
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
import javafx.scene.layout.VBox;
import sun.awt.shell.ShellFolder;
import ui.Main;
import ui.dialog.GameRoomDialog;
import ui.pane.SelectListPane;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by LM on 03/01/2017.
 */
public class AppSelectorDialog extends GameRoomDialog<ButtonType> {
    private File folder;
    private File selectedFile;

    public AppSelectorDialog(File folder) {
        if (folder == null || !folder.isDirectory()) {
            throw new IllegalArgumentException("Given folder is either null or not a dir : \"" + folder.getAbsolutePath() + "\"");
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

        ApplicationList list = new ApplicationList<>();

        mainPane.setPrefWidth(1.0 / 3.5 * Main.SCREEN_WIDTH);
        mainPane.setPrefHeight(1.0 / 3 * Main.SCREEN_HEIGHT);

        list.addItems(getValidAppFiles(folder));
        mainPane.setCenter(list);

        getDialogPane().getButtonTypes().addAll(
                new ButtonType(Main.RESSOURCE_BUNDLE.getString("ok"), ButtonBar.ButtonData.OK_DONE)
                , new ButtonType(Main.RESSOURCE_BUNDLE.getString("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE));

        setOnHiding(event -> {
            selectedFile = (File) list.getSelectedValue();
        });

    }

    public List<File> getValidAppFiles(File folder) {
        List<File> potentialApps = new ArrayList<>();
        for (File children : folder.listFiles()) {
            if (children.isDirectory()) {
                potentialApps.addAll(getValidAppFiles(children));
            } else if (FolderGameScanner.fileHasValidExtension(children)) {
                potentialApps.add(children);
            }
        }
        return potentialApps;
    }

    public File getSelectedFile() {
        return selectedFile;
    }

    public static Image getIcon(File file) {
        ShellFolder sf = null;
        try {
            sf = ShellFolder.getShellFolder(file);
        } catch (FileNotFoundException e) {
            return null;
        }
        if(sf == null){
            return null;
        }
        java.awt.Image img = sf.getIcon(true);

        if(img == null){
            return null;
        }

        BufferedImage buff = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = buff.createGraphics();
        graphics.drawImage(img, 0, 0, null);
        graphics.dispose();

        return SwingFXUtils.toFXImage(buff, null);
    }

    private static class ApplicationList<File> extends SelectListPane {

        public ApplicationList() {
            super(false);

        }

        @Override
        protected ListItem createListItem(Object value) {
            AppSelectorDialog.ApplicationItem item = new AppSelectorDialog.ApplicationItem(value, this);
            return item;
        }
    }

    private static class ApplicationItem extends SelectListPane.ListItem {
        private File file;
        private StackPane coverPane = new StackPane();
        private ImageView coverView = new ImageView();

        public ApplicationItem(Object value, SelectListPane parentList) {
            super(value, parentList);
            file = ((File) value);
            addContent();
        }

        @Override
        protected void addContent() {
            Image icon = getIcon(file);
            coverView.setFitHeight(icon.getHeight());
            coverView.setFitWidth(icon.getWidth());
            coverView.setImage(icon);
            coverPane.getChildren().add(coverView);

            //GridPane.setMargin(coverPane, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 10 * Main.SCREEN_WIDTH / 1920));
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



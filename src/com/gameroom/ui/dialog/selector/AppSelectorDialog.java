package com.gameroom.ui.dialog.selector;

import com.gameroom.data.game.scanner.FolderGameScanner;
import javafx.application.Platform;
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
import com.gameroom.ui.Main;
import com.gameroom.ui.dialog.GameRoomDialog;
import com.gameroom.ui.pane.SelectListPane;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.gameroom.data.game.scanner.FolderGameScanner.EXCLUDED_FILE_NAMES;

/**
 * Created by LM on 03/01/2017.
 */
public class AppSelectorDialog extends GameRoomDialog<ButtonType> {
    private File folder;
    private File selectedFile;
    private ApplicationList list;
    private Thread searchAppsThread;
    private volatile boolean KEEP_SEARCHING = true;
    private Label statusLabel;
    private String[] extensions = new String[0];

    public AppSelectorDialog(File folder,String[] extensions) throws IllegalArgumentException {
        if (folder == null || !folder.isDirectory()) {
            throw new IllegalArgumentException("Given folder is either null or not a dir : \"" + folder.getAbsolutePath() + "\"");
        }
        this.folder = folder;
        this.extensions = extensions;

        Label titleLabel = new Label(Main.getString("select_an_app"));
        titleLabel.setWrapText(true);
        titleLabel.setTooltip(new Tooltip(Main.getString("select_an_app")));
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

        list = new ApplicationList<>();

        mainPane.setPrefWidth(1.0 / 3.5 * Main.SCREEN_WIDTH);
        mainPane.setPrefHeight(1.0 / 3 * Main.SCREEN_HEIGHT);


        statusLabel = new Label(Main.getString("loading") + "...");
        StackPane contentPane = new StackPane();
        contentPane.getChildren().addAll(list, statusLabel);
        mainPane.setCenter(contentPane);

        getDialogPane().getButtonTypes().addAll(
                new ButtonType(Main.getString("ok"), ButtonBar.ButtonData.OK_DONE)
                , new ButtonType(Main.getString("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE));

        setOnHiding(event -> {
            selectedFile = (File) list.getSelectedValue();
            stopSearching();
        });

    }

    public void searchApps() {
        if (searchAppsThread == null) {
            searchAppsThread = new Thread(() -> {
                addAppFiles(folder);
                Platform.runLater(() -> statusLabel.setText(Main.getString("no_result")));
            });
            searchAppsThread.setDaemon(true);
        } else {
            stopSearching();
            try {
                searchAppsThread.join();
            } catch (InterruptedException ignored) {
            }
        }
        KEEP_SEARCHING = true;
        searchAppsThread.start();
    }

    public void stopSearching() {
        KEEP_SEARCHING = false;
    }

    private void addAppFiles(File file) {
        if (!KEEP_SEARCHING) {
            return;
        }
        if (file.isDirectory()) {
            for (String excludedName : EXCLUDED_FILE_NAMES) {
                if (file.getName().toLowerCase().equals(excludedName.toLowerCase())) {
                    return;
                }
            }
            List<File> potentialApps = new ArrayList<>();
            File[] files = file.listFiles();

            if (files == null) {
                return;
            }
            Collections.addAll(potentialApps, files);

            potentialApps.sort(FolderGameScanner.APP_FINDER_COMPARATOR);

            for (File children : potentialApps) {
                addAppFiles(children);
            }
        } else {
            if (FolderGameScanner.isPotentiallyAGame(file,extensions) && KEEP_SEARCHING) {
                Platform.runLater(() -> {
                    list.addItem(file);
                    statusLabel.setVisible(false);
                });
            }
        }
    }

    private static int getDeepness(File file) {
        String path = file.getAbsolutePath();
        path = path.replace('\\', File.pathSeparatorChar);
        path = path.replace('/', File.pathSeparatorChar);
        int charCount = 0;
        for (char c : path.toCharArray()) {
            if (c == File.pathSeparatorChar) {
                charCount++;
            }
        }
        return charCount;
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
        if (sf == null) {
            return null;
        }
        java.awt.Image img = sf.getIcon(true);

        if (img == null) {
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

            vbox.getChildren().addAll(titleLabel, idLabel);
            GridPane.setMargin(vbox, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 10 * Main.SCREEN_WIDTH / 1920));
            add(vbox, columnCount++, 0);
        }
    }
}



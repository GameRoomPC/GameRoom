package ui.pane;

import data.game.entry.Emulator;
import data.game.entry.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;
import org.controlsfx.control.CheckComboBox;
import ui.Main;
import ui.control.textfield.PathTextField;

import java.io.File;
import java.sql.SQLException;
import java.util.Comparator;

import static ui.Main.SCREEN_WIDTH;

/**
 * @author LM. Garret (admin@gameroom.me)
 * @date 08/06/2017.
 */
public class EmulatorSettingsPane extends BorderPane {
    public Emulator emulator;

    public EmulatorSettingsPane(Emulator emulator, Window window) {
        this.emulator = emulator;

        try {
            Platform.initWithDb();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        initTop();
        initCenter(window);
    }

    private void initTop() {
        StackPane topPane = new StackPane();
        topPane.getStyleClass().add("header");

        Label titleLabel = new Label(emulator.toString());
        titleLabel.setId("titleLabel");
        titleLabel.getStyleClass().add("title-label");

        topPane.getChildren().add(titleLabel);
        StackPane.setAlignment(titleLabel, Pos.CENTER);
        StackPane.setMargin(titleLabel, new Insets(30 * Main.SCREEN_HEIGHT / 1080
                , 12 * Main.SCREEN_WIDTH / 1920
                , 15 * Main.SCREEN_HEIGHT / 1080
                , 15 * Main.SCREEN_WIDTH / 1920));
        setTop(topPane);
    }

    private void initCenter(Window window) {
        int rowCount = 0;

        //init gridpane
        GridPane contentPane = new GridPane();
        //contentPane.setGridLinesVisible(true);
        contentPane.setVgap(20 * SCREEN_WIDTH / 1920);
        contentPane.setHgap(10 * SCREEN_WIDTH / 1920);
        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setPercentWidth(20);
        contentPane.getColumnConstraints().add(cc1);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setPercentWidth(70);
        contentPane.getColumnConstraints().add(cc2);
        ColumnConstraints cc3 = new ColumnConstraints();
        cc3.setPercentWidth(10);
        contentPane.getColumnConstraints().add(cc3);

        /********PROGRAM PATH **************/
        PathTextField pathField = new PathTextField(emulator.getPath().getAbsolutePath(), window, PathTextField.FILE_CHOOSER_APPS, Main.getString("select_program"));
        pathField.getTextField().setPrefColumnCount(50);
        pathField.getTextField().textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                File newPath = new File(newValue);
                if (newPath.exists()) {
                    emulator.setPath(newPath);
                }
            }
        });
        Label pathLabel = new Label(Main.getString("path") + " :");
        pathLabel.setTooltip(new Tooltip(Main.getString("path")));
        contentPane.add(pathLabel, 0, rowCount);
        contentPane.add(pathField, 1, rowCount);
        rowCount++;

        /******** EMULATES CHOICES *********/
        final ObservableList<Platform> supportedPlatforms = FXCollections.observableArrayList(emulator.getSupportedPlatforms());
        supportedPlatforms.sort(Comparator.comparing(Platform::getName));

        final CheckComboBox<Platform> platformComboBox = new CheckComboBox<Platform>(supportedPlatforms);
        for (Platform platform : supportedPlatforms) {
            if (emulator.equals(Emulator.getChosenEmulator(platform))) {
                platformComboBox.getCheckModel().check(platformComboBox.getCheckModel().getItemIndex(platform));
            }
        }

        platformComboBox.getCheckModel().getCheckedItems().addListener((ListChangeListener<Platform>) c -> {
            while (c.next()) {
                c.getAddedSubList().forEach(o -> {
                    o.setChosenEmulator(emulator);
                });
                c.getRemoved().forEach(o -> {
                    o.setChosenEmulator(null);
                });
            }
        });
        Label platformLabel = new Label(Main.getString("use_for") + " :");
        platformLabel.setTooltip(new Tooltip(Main.getString("platform_label")));
        contentPane.add(platformLabel, 0, rowCount);
        contentPane.add(platformComboBox, 1, rowCount);
        rowCount++;

        /************ARGS SCHEMA*********/
        //TODO add a resest option
        TextField textField = new TextField(emulator.getArgSchema());
        textField.setPrefColumnCount(50);
        textField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                emulator.setArgSchema(newValue);
            }
        });
        Label argLabel = new Label(Main.getString("emulator_args_schema") + " :");
        argLabel.setTooltip(new Tooltip(Main.getString("emulator_args_schema_tooltip")));
        contentPane.add(argLabel, 0, rowCount);
        contentPane.add(textField, 1, rowCount);
        rowCount++;


        /**** DO NOT ADD ANY CONTENT PAST HERE ****/
        setCenter(contentPane);
    }
}

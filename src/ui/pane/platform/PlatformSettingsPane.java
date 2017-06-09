package ui.pane.platform;

import data.game.entry.Emulator;
import data.game.entry.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import ui.Main;
import ui.control.button.HelpButton;
import ui.control.textfield.PathTextField;

import java.io.File;
import java.sql.SQLException;
import java.util.Comparator;

import static ui.Main.SCREEN_WIDTH;

/**
 * @author LM. Garret (admin@gameroom.me)
 * @date 09/06/2017.
 */
public class PlatformSettingsPane extends BorderPane {
    private Platform platform;

    public PlatformSettingsPane(Platform platform) {
        this.platform = platform;
        try {
            Platform.initWithDb();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        initTop();
        initCenter();
    }

    private void initTop() {
        StackPane topPane = new StackPane();
        topPane.getStyleClass().add("header");

        Label titleLabel = new Label(platform.getName());
        titleLabel.getStyleClass().add("small-title-label");

        topPane.getChildren().add(titleLabel);
        StackPane.setAlignment(titleLabel, Pos.CENTER);
        StackPane.setMargin(titleLabel, new Insets(30 * Main.SCREEN_HEIGHT / 1080
                , 12 * Main.SCREEN_WIDTH / 1920
                , 15 * Main.SCREEN_HEIGHT / 1080
                , 15 * Main.SCREEN_WIDTH / 1920));
        setTop(topPane);
    }

    private void initCenter() {
        int rowCount = 0;
        Emulator chosenEmulator = platform.getChosenEmulator();

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
        //TODO add a better folder management to have a folder per platform
        /*PathTextField pathField = new PathTextField(emulator.getPath().getAbsolutePath(), window, PathTextField.FILE_CHOOSER_APPS, Main.getString("select_program"));
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
        rowCount++;*/

        /************ARGS SCHEMA*********/
        //TODO add a reset option
        TextField textField = new TextField();
        textField.setPrefColumnCount(50);
        if(chosenEmulator != null){
            textField.setText(chosenEmulator.getArgSchema(platform));
        }
        textField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                platform.getChosenEmulator().setArgSchema(newValue,platform);
            }
        });

        HBox argBox = new HBox();
        argBox.setAlignment(Pos.CENTER_LEFT);
        argBox.setSpacing(5 * SCREEN_WIDTH / 1920);
        argBox.getChildren().addAll( new Label(Main.getString("emulator_args_schema") + " :")
                ,new HelpButton(Main.getString("emulator_args_schema_tooltip")));
        argBox.setVisible(chosenEmulator != null);
        argBox.setManaged(chosenEmulator!=null);

        /******** EMULATOR CHOICE *********/
        //TODO add a configure button
        final ObservableList<Emulator> possibleEmulators = FXCollections.observableArrayList(Emulator.getPossibleEmulators(platform));
        possibleEmulators.sort(Comparator.comparing(Emulator::toString));

        final ComboBox<Emulator> emuComboBox = new ComboBox<Emulator>(possibleEmulators);
        for (Emulator emulator : possibleEmulators) {
            if (emulator.equals(Emulator.getChosenEmulator(platform))) {
                emuComboBox.getSelectionModel().select(emulator);
            }
        }

        emuComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            platform.setChosenEmulator(newValue);
            argBox.setManaged(true);
            argBox.setVisible(true);
            textField.setText(newValue.getArgSchema(platform));
        });

        Label platformLabel = new Label(Main.getString("emulate_with") + " :");
        platformLabel.setTooltip(new Tooltip(Main.getString("emulate_with")));
        contentPane.add(platformLabel, 0, rowCount);
        contentPane.add(emuComboBox, 1, rowCount);
        rowCount++;

        contentPane.add(argBox, 0, rowCount);
        contentPane.add(textField, 1, rowCount);
        rowCount++;


        /**** DO NOT ADD ANY CONTENT PAST HERE ****/
        setCenter(contentPane);
    }
}

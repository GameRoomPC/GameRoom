package ui.pane.platform;

import data.game.entry.Emulator;
import data.game.entry.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventDispatchChain;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Window;
import ui.Main;
import ui.control.button.HelpButton;
import ui.control.textfield.PathTextField;
import ui.dialog.GameRoomDialog;

import java.sql.SQLException;
import java.util.Comparator;

import static system.application.settings.GeneralSettings.settings;
import static ui.Main.SCREEN_WIDTH;

/**
 * @author LM. Garret (admin@gameroom.me)
 * @date 09/06/2017.
 */
public class PlatformSettingsPane extends BorderPane {
    private Platform platform;

    public PlatformSettingsPane(Platform platform, Window window) {
        this.platform = platform;
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
        topPane.getStyleClass().add("small-header");

        Label titleLabel = new Label(platform.getName());
        titleLabel.getStyleClass().add("small-title-label");

        ImageView iconView = new ImageView();
        double width = 35 * Main.SCREEN_WIDTH / 1920 * settings().getUIScale().getScale();
        double height = 35 * Main.SCREEN_HEIGHT / 1080 * settings().getUIScale().getScale();
        iconView.setSmooth(true);
        iconView.setPreserveRatio(true);
        iconView.setFitWidth(width);
        iconView.setFitHeight(height);
        platform.setCSSIcon(iconView,false);

        HBox box = new HBox();
        box.setSpacing(15 * Main.SCREEN_WIDTH / 1920);
        box.getChildren().addAll(iconView, titleLabel);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("title-box");
        box.setPickOnBounds(false);

        topPane.getChildren().add(box);
        StackPane.setAlignment(box, Pos.CENTER);
        StackPane.setMargin(box, new Insets(10 * Main.SCREEN_HEIGHT / 1080
                , 10 * Main.SCREEN_WIDTH / 1920
                , 25 * Main.SCREEN_HEIGHT / 1080
                , 10 * Main.SCREEN_WIDTH / 1920));
        setTop(topPane);
    }

    private void initCenter(Window window) {
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
        PathTextField pathField = new PathTextField(platform.getROMFolder(), window, PathTextField.FILE_CHOOSER_FOLDER, Main.getString("select_a_folder"));
        pathField.getTextField().setPrefColumnCount(40);
        pathField.getTextField().textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                platform.setROMFolder(newValue);
            }
        });
        HBox romHBox = new HBox();
        romHBox.setAlignment(Pos.CENTER_LEFT);
        romHBox.setSpacing(5 * SCREEN_WIDTH / 1920);
        romHBox.getChildren().addAll(new Label(Main.getString("rom_folder") + " :")
                , new HelpButton(Main.getString("rom_folders_tooltip")));
        contentPane.add(romHBox, 0, rowCount);
        contentPane.add(pathField, 1, rowCount);
        rowCount++;

        /************SUPPORTED EXTENSIONS*********/
        TextField extField = new TextField();
        extField.setPrefColumnCount(40);
        extField.setText(platform.getSupportedExtensionsString());
        extField.textProperty().addListener((observable, oldValue, newValue) -> platform.setSupportedExtensions(newValue));

        HBox extBox = new HBox();
        extBox.setAlignment(Pos.CENTER_LEFT);
        extBox.setSpacing(5 * SCREEN_WIDTH / 1920);
        extBox.getChildren().addAll(new Label(Main.getString("romExtensions") + " :")
                , new HelpButton(Main.getString("romExtensions_tooltip")));

        Button extResetButton = new Button(Main.getString("default"));
        extResetButton.setOnAction(event -> {
            String defaultExts = platform.getDefaultSupportedExtensionsString();
            extField.setText(defaultExts != null ? defaultExts : "");
        });

        contentPane.add(extBox, 0, rowCount);
        contentPane.add(extField, 1, rowCount);
        contentPane.add(extResetButton, 2, rowCount);
        rowCount++;

        /************ARGS SCHEMA*********/
        TextField argField = new TextField();
        argField.setPrefColumnCount(40);
        if (chosenEmulator != null) {
            argField.setText(chosenEmulator.getArgSchema(platform));
        }
        argField.textProperty().addListener((observable, oldValue, newValue) -> platform.getChosenEmulator().setArgSchema(newValue, platform));
        argField.setManaged(chosenEmulator != null);
        argField.setVisible(chosenEmulator != null);

        HBox argBox = new HBox();
        argBox.setAlignment(Pos.CENTER_LEFT);
        argBox.setSpacing(5 * SCREEN_WIDTH / 1920);
        argBox.getChildren().addAll(new Label(Main.getString("emulator_args_schema") + " :")
                , new HelpButton(Main.getString("emulator_args_schema_tooltip"),true));
        argBox.setVisible(chosenEmulator != null);
        argBox.setManaged(chosenEmulator != null);

        Button argResetButton = new Button(Main.getString("default"));
        argResetButton.setOnAction(event -> {
            if (platform.getChosenEmulator() != null) {
                String defaultArgs = platform.getChosenEmulator().getDefaultArgSchema();
                argField.setText(defaultArgs != null ? defaultArgs : "");
            }
        });
        argResetButton.setVisible(chosenEmulator != null);
        argResetButton.setManaged(chosenEmulator != null);

        /******** EMULATOR CHOICE *********/
        Button configureButton = new Button("configure");
        configureButton.setVisible(chosenEmulator != null);
        configureButton.setManaged(chosenEmulator != null);

        final ObservableList<Emulator> possibleEmulators = FXCollections.observableArrayList(Emulator.getPossibleEmulators(platform));
        possibleEmulators.sort(Comparator.comparing(Emulator::toString));

        final ComboBox<Emulator> emuComboBox = new ComboBox<Emulator>(possibleEmulators);
        Emulator chosenEmu = Emulator.getChosenEmulator(platform);
        for (Emulator emulator : possibleEmulators) {
            if (emulator.equals(chosenEmu)) {
                emuComboBox.getSelectionModel().select(emulator);
                break;
            }
        }

        emuComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            platform.setChosenEmulator(newValue);
            argBox.setManaged(true);
            argBox.setVisible(true);
            argField.setVisible(true);
            argField.setManaged(true);
            argField.setText(newValue.getArgSchema(platform));
            argResetButton.setVisible(true);
            argResetButton.setManaged(true);
            configureButton.setVisible(true);
            configureButton.setManaged(true);
        });

        configureButton.setOnAction(event -> {
            GameRoomDialog<ButtonType> dialog = new GameRoomDialog<ButtonType>() {
                @Override
                public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
                    return super.buildEventDispatchChain(tail);
                }
            };
            ButtonType okButton = new ButtonType(Main.getString("close"), ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().add(okButton);
            EmulatorSettingsPane pane = new EmulatorSettingsPane(emuComboBox.getValue(), window);
            pane.setMaxWidth(1.5 * Main.SCREEN_WIDTH / 4);
            dialog.getMainPane().setCenter(pane);
            dialog.showAndWait();
        });

        HBox box = new HBox(10 * Main.SCREEN_WIDTH / 1920);
        box.getChildren().addAll(emuComboBox, configureButton);

        Label platformLabel = new Label(Main.getString("emulate_with") + " :");
        platformLabel.setTooltip(new Tooltip(Main.getString("emulate_with")));
        contentPane.add(platformLabel, 0, rowCount);
        contentPane.add(box, 1, rowCount);
        rowCount++;

        contentPane.add(argBox, 0, rowCount);
        contentPane.add(argField, 1, rowCount);
        contentPane.add(argResetButton, 2, rowCount);
        rowCount++;


        /**** DO NOT ADD ANY CONTENT PAST HERE ****/
        setCenter(contentPane);
    }
}

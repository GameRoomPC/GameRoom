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

import java.io.File;
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

        Label titleLabel = new Label(platform.getName());
        titleLabel.getStyleClass().add("small-title-label");

        ImageView iconView = new ImageView();
        double width = 35 * Main.SCREEN_WIDTH / 1920 * settings().getUIScale().getScale();
        double height = 35 * Main.SCREEN_HEIGHT / 1080 * settings().getUIScale().getScale();
        iconView.setSmooth(false);
        iconView.setPreserveRatio(true);
        iconView.setFitWidth(width);
        iconView.setFitHeight(height);
        iconView.setId(platform.getIconCSSId());

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
        cc1.setPercentWidth(30);
        contentPane.getColumnConstraints().add(cc1);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setPercentWidth(70);
        contentPane.getColumnConstraints().add(cc2);

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
        TextField argField = new TextField();
        argField.setPrefColumnCount(40);
        if(chosenEmulator != null){
            argField.setText(chosenEmulator.getArgSchema(platform));
        }
        argField.textProperty().addListener((observable, oldValue, newValue) -> platform.getChosenEmulator().setArgSchema(newValue,platform));
        argField.setManaged(chosenEmulator != null);
        argField.setVisible(chosenEmulator != null);

        HBox argBox = new HBox();
        argBox.setAlignment(Pos.CENTER_LEFT);
        argBox.setSpacing(5 * SCREEN_WIDTH / 1920);
        argBox.getChildren().addAll( new Label(Main.getString("emulator_args_schema") + " :")
                ,new HelpButton(Main.getString("emulator_args_schema_tooltip")));
        argBox.setVisible(chosenEmulator != null);
        argBox.setManaged(chosenEmulator!=null);

        Button configureButton = new Button("configure");
        configureButton.setVisible(chosenEmulator != null);
        configureButton.setManaged(chosenEmulator!=null);

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
            argField.setVisible(true);
            argField.setManaged(true);
            argField.setText(newValue.getArgSchema(platform));
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
            EmulatorSettingsPane pane = new EmulatorSettingsPane(emuComboBox.getValue(),window);
            pane.setMaxWidth(1.5*Main.SCREEN_WIDTH/4);
            dialog.getMainPane().setCenter(pane);
            dialog.showAndWait();
        });

        HBox box = new HBox(10*Main.SCREEN_WIDTH/1920);
        box.getChildren().addAll(emuComboBox,configureButton);

        Label platformLabel = new Label(Main.getString("emulate_with") + " :");
        platformLabel.setTooltip(new Tooltip(Main.getString("emulate_with")));
        contentPane.add(platformLabel, 0, rowCount);
        contentPane.add(box, 1, rowCount);
        rowCount++;

        contentPane.add(argBox, 0, rowCount);
        contentPane.add(argField, 1, rowCount);
        rowCount++;


        /**** DO NOT ADD ANY CONTENT PAST HERE ****/
        setCenter(contentPane);
    }
}

package ui.scene;

import com.mashape.unirest.http.exceptions.UnirestException;
import data.http.key.KeyChecker;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import org.json.JSONObject;
import system.application.MessageListener;
import system.application.MessageTag;
import system.application.OnLaunchAction;
import system.application.settings.PredefinedSetting;
import system.os.PowerMode;
import ui.Main;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import ui.control.textfield.PathTextField;
import ui.dialog.ActivationKeyDialog;
import ui.dialog.GameRoomAlert;
import ui.dialog.SteamIgnoredSelector;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

import static ui.Main.*;

/**
 * Created by LM on 03/07/2016.
 */
public class SettingsScene extends BaseScene {
    public final static String ADVANCE_MODE_LABEL_STYLE = "    -fx-text-fill: derive(-flatter-red, -20.0%);";
    private BorderPane wrappingPane;
    private TilePane contentPane = new TilePane();

    public SettingsScene(StackPane root, Stage parentStage, BaseScene previousScene) {
        super(root, parentStage);
        this.previousScene = previousScene;

        getStylesheets().add("res/flatterfx.css");

        initTop();
        initCenter();
        initBottom();
    }

    private void initBottom() {
        Label igdbLabel = new Label(RESSOURCE_BUNDLE.getString("credit_igdb"));
        wrappingPane.setBottom(igdbLabel);

        BorderPane.setAlignment(igdbLabel, Pos.CENTER_RIGHT);
        BorderPane.setMargin(igdbLabel, new Insets(15, 15, 15, 15));
    }

    private void initCenter() {
        contentPane.setHgap(50);
        contentPane.setVgap(15);
        contentPane.setPrefColumns(2);
        //contentPane.setAlignment(Pos.TOP_LEFT);
        contentPane.setOrientation(Orientation.VERTICAL);
        contentPane.setTileAlignment(Pos.CENTER_LEFT);

        addPropertyLine(PredefinedSetting.LOCALE);
        addPropertyLine(PredefinedSetting.ON_GAME_LAUNCH_ACTION);
        addPropertyLine(PredefinedSetting.NO_NOTIFICATIONS);
        addPropertyLine(PredefinedSetting.START_MINIMIZED,true);
        addPropertyLine(PredefinedSetting.DISABLE_GAME_MAIN_THEME,true);
        addPropertyLine(PredefinedSetting.DISABLE_MAINSCENE_WALLPAPER,true, new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    MAIN_SCENE.setChangeBackgroundNextTime(false);
                    MAIN_SCENE.setImageBackground(null);
                }
            }
        });
        addPropertyLine(PredefinedSetting.ENABLE_XBOX_CONTROLLER_SUPPORT,true, new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    Main.xboxController.startThreads();
                } else {
                    Main.xboxController.stopThreads();
                }
            }
        });
        addPropertyLine(PredefinedSetting.ENABLE_GAMING_POWER_MODE,false, new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                Node node = searchNode(PredefinedSetting.GAMING_POWER_MODE.getKey());
                if (node != null) {
                    node.setDisable(!newValue);
                }
            }
        });
        addPropertyLine(PredefinedSetting.GAMING_POWER_MODE);
        addPropertyLine(PredefinedSetting.GAMES_FOLDER);

        /***********************STEAM GAMES IGNORED****************************/
        Label steamIgnoredGamesLabel = new Label(Main.SETTINGS_BUNDLE.getString("manage_ignored_steam_games_label") + ": ");
        steamIgnoredGamesLabel.setTooltip(new Tooltip(Main.SETTINGS_BUNDLE.getString("manage_ignored_steam_games_tooltip")));
        Button manageSteamGamesIgnoredButton = new Button(Main.RESSOURCE_BUNDLE.getString("manage"));

        manageSteamGamesIgnoredButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    SteamIgnoredSelector selector = new SteamIgnoredSelector();
                    Optional<ButtonType> ignoredOptionnal = selector.showAndWait();
                    ignoredOptionnal.ifPresent(pairs -> {
                        if (pairs.getButtonData().equals(ButtonBar.ButtonData.OK_DONE)) {
                            GENERAL_SETTINGS.setSettingValue(PredefinedSetting.IGNORED_STEAM_APPS, selector.getSelectedEntries());
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        contentPane.getChildren().add(createLine(steamIgnoredGamesLabel, manageSteamGamesIgnoredButton));

        /***********************SUPPORTER KEY****************************/
        String keyStatus = Main.SUPPORTER_MODE ? GENERAL_SETTINGS.getString(PredefinedSetting.SUPPORTER_KEY) : Main.RESSOURCE_BUNDLE.getString("none");
        String buttonText = Main.SUPPORTER_MODE ? Main.RESSOURCE_BUNDLE.getString("deactivate") : Main.RESSOURCE_BUNDLE.getString("activate");

        Label supporterKeyLabel = new Label(PredefinedSetting.SUPPORTER_KEY.getLabel() + ": " + keyStatus);
        supporterKeyLabel.setTooltip(new Tooltip(PredefinedSetting.SUPPORTER_KEY.getTooltip()));
        Button actDeactButton = new Button(buttonText);

        actDeactButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (SUPPORTER_MODE) {
                    try {
                        JSONObject response = KeyChecker.deactivateKey(GENERAL_SETTINGS.getString(PredefinedSetting.SUPPORTER_KEY));
                        if (response.getString(KeyChecker.FIELD_RESULT).equals(KeyChecker.RESULT_SUCCESS)) {
                            GameRoomAlert successDialog = new GameRoomAlert(Alert.AlertType.INFORMATION, Main.RESSOURCE_BUNDLE.getString("key_deactivated_message"));
                            successDialog.showAndWait();

                            GENERAL_SETTINGS.setSettingValue(PredefinedSetting.SUPPORTER_KEY, "");
                            SUPPORTER_MODE = false;
                            String keyStatus = Main.SUPPORTER_MODE ? GENERAL_SETTINGS.getString(PredefinedSetting.SUPPORTER_KEY) : Main.RESSOURCE_BUNDLE.getString("none");
                            String buttonText = Main.SUPPORTER_MODE ? Main.RESSOURCE_BUNDLE.getString("deactivate") : Main.RESSOURCE_BUNDLE.getString("activate");
                            actDeactButton.setText(buttonText);
                            supporterKeyLabel.setText(PredefinedSetting.SUPPORTER_KEY.getLabel() + ": " + keyStatus);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (UnirestException e) {
                        e.printStackTrace();
                    }
                } else {
                    ActivationKeyDialog dialog = new ActivationKeyDialog();

                    Optional<ButtonType> result = dialog.showAndWait();
                    result.ifPresent(letter -> {
                        if (letter.getText().contains(Main.RESSOURCE_BUNDLE.getString("supporter_key_buy_one"))) {
                            try {
                                Desktop.getDesktop().browse(new URI("https://gameroom.me/downloads/key"));
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            } catch (URISyntaxException e1) {
                                e1.printStackTrace();
                            }
                        } else if (letter.getText().equals(Main.RESSOURCE_BUNDLE.getString("activate"))) {
                            try {
                                JSONObject response = KeyChecker.activateKey(dialog.getSupporterKey());
                                String message = Main.RESSOURCE_BUNDLE.getString(response.getString(KeyChecker.FIELD_MESSAGE).replace(' ', '_'));

                                switch (response.getString(KeyChecker.FIELD_RESULT)) {
                                    case KeyChecker.RESULT_SUCCESS:
                                        GameRoomAlert successDialog = new GameRoomAlert(Alert.AlertType.INFORMATION, message);
                                        successDialog.showAndWait();

                                        GENERAL_SETTINGS.setSettingValue(PredefinedSetting.SUPPORTER_KEY, dialog.getSupporterKey());
                                        SUPPORTER_MODE = KeyChecker.isKeyValid(GENERAL_SETTINGS.getString(PredefinedSetting.SUPPORTER_KEY));
                                        String keyStatus = Main.SUPPORTER_MODE ? GENERAL_SETTINGS.getString(PredefinedSetting.SUPPORTER_KEY) : Main.RESSOURCE_BUNDLE.getString("none");
                                        String buttonText = Main.SUPPORTER_MODE ? Main.RESSOURCE_BUNDLE.getString("deactivate") : Main.RESSOURCE_BUNDLE.getString("activate");
                                        actDeactButton.setText(buttonText);
                                        supporterKeyLabel.setText(PredefinedSetting.SUPPORTER_KEY.getLabel() + ": " + keyStatus);
                                        break;
                                    case KeyChecker.RESULT_ERROR:
                                        GameRoomAlert errorDialog = new GameRoomAlert(Alert.AlertType.ERROR, message);
                                        errorDialog.showAndWait();
                                        break;
                                    default:
                                        break;
                                }
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            } catch (UnirestException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                }
            }
        });

        contentPane.getChildren().add(createLine(supporterKeyLabel, actDeactButton));

        /***********************VERSION CHECK****************************/
        Label versionLabel = new Label(Main.RESSOURCE_BUNDLE.getString("version") + ": " + Main.getVersion());
        Button checkUpdatesButton = new Button(Main.RESSOURCE_BUNDLE.getString("check_now"));

        NETWORK_MANAGER.addMessageListener(new MessageListener() {
            @Override
            public void onMessageReceived(MessageTag tag, String payload) {
                if (tag.equals(MessageTag.ERROR)) {
                    Platform.runLater(() -> checkUpdatesButton.setText(Main.RESSOURCE_BUNDLE.getString("error")));
                } else if (tag.equals(MessageTag.NO_UPDATE)) {
                    Platform.runLater(() -> checkUpdatesButton.setText(Main.RESSOURCE_BUNDLE.getString("up_to_date!")));
                } else if (tag.equals(MessageTag.NEW_UPDATE)) {
                    Platform.runLater(() -> checkUpdatesButton.setText(Main.RESSOURCE_BUNDLE.getString("new_version") + ": " + payload));
                }
            }
        });
        checkUpdatesButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                checkUpdatesButton.setText(Main.RESSOURCE_BUNDLE.getString("loading") + "...");
                Main.startUpdater();
            }
        });
        contentPane.getChildren().add(createLine(versionLabel, checkUpdatesButton));


        /***********************ADVANCED MODE **************************************/
        addPropertyLine(PredefinedSetting.ADVANCED_MODE, false, new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                fadeTransitionTo(new SettingsScene(new StackPane(), getParentStage(), previousScene), getParentStage());
            }
        });

        /***********************ROW CONSTRAINTS****************************/
        /**********************NO CONTROL INIT BELOW THIS*******************/


        /*GridPane pane = new GridPane();
        pane.add(contentPane,0,0);
        GridPane.setFillWidth(contentPane,false);*/
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setContent(contentPane);
        contentPane.maxWidthProperty().bind(scrollPane.widthProperty());
        contentPane.setPrefRows(8);
        wrappingPane.setCenter(scrollPane);
        BorderPane.setMargin(scrollPane, new Insets(50 * SCREEN_HEIGHT / 1080, 50 * SCREEN_WIDTH / 1920, 20 * SCREEN_HEIGHT / 1080, 50 * SCREEN_WIDTH / 1920));

    }

    private void addPropertyLine(PredefinedSetting setting) {
        addPropertyLine(setting, false);
    }

    private void addPropertyLine(PredefinedSetting setting, boolean advanceSetting) {
        addPropertyLine(setting, advanceSetting,null);
    }

    /**
     * Property line creator!
     *
     * @param setting the predifined settings to generate the line and the value linked that will be updated
     * @return null if value class is not recognized, a line to edit the value otherwise
     */
    private void addPropertyLine(PredefinedSetting setting, boolean advancedSetting, ChangeListener changeListener) {
        if (!advancedSetting || (advancedSetting && GENERAL_SETTINGS.getBoolean(PredefinedSetting.ADVANCED_MODE))) {
            Label label = new Label(setting.getLabel() + " :");
            label.setTooltip(new Tooltip(setting.getTooltip()));
            if ((advancedSetting && GENERAL_SETTINGS.getBoolean(PredefinedSetting.ADVANCED_MODE))){
                label.setStyle(ADVANCE_MODE_LABEL_STYLE);
            }

            Node node2 = null;
            if (setting.isClass(Boolean.class)) {
                /**************** BOOLEAN **************/
                CheckBox checkBox = new CheckBox();
                checkBox.setSelected(Main.GENERAL_SETTINGS.getBoolean(setting));
                checkBox.setWrapText(true);
                checkBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                        Main.GENERAL_SETTINGS.setSettingValue(setting, newValue);
                        if (changeListener != null) {
                            changeListener.changed(observable, oldValue, newValue);
                        }
                    }
                });
                node2 = checkBox;
            } else if (setting.isClass(PowerMode.class)) {
                /**************** POWER MODE **************/
                ComboBox<PowerMode> powerModeComboBox = new ComboBox<>();
                powerModeComboBox.getItems().addAll(PowerMode.getPowerModesAvailable());
                powerModeComboBox.setConverter(new StringConverter<PowerMode>() {
                    @Override
                    public String toString(PowerMode object) {
                        return object.getAlias();
                    }

                    @Override
                    public PowerMode fromString(String string) {
                        return null;
                    }
                });
                powerModeComboBox.setValue(GENERAL_SETTINGS.getPowerMode(PredefinedSetting.GAMING_POWER_MODE));
                powerModeComboBox.setDisable(!GENERAL_SETTINGS.getBoolean(PredefinedSetting.ENABLE_GAMING_POWER_MODE));
                powerModeComboBox.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        Main.GENERAL_SETTINGS.setSettingValue(PredefinedSetting.GAMING_POWER_MODE, powerModeComboBox.getValue());
                        if (changeListener != null) {
                            changeListener.changed(null, null, powerModeComboBox.getValue());
                        }
                    }
                });
                node2 = powerModeComboBox;
            } else if (setting.isClass(OnLaunchAction.class)) {
                /**************** ON LAUNCH ACTION **************/
                ComboBox<OnLaunchAction> onLaunchActionComboBox = new ComboBox<>();
                onLaunchActionComboBox.getItems().addAll(OnLaunchAction.values());
                onLaunchActionComboBox.setConverter(new StringConverter<OnLaunchAction>() {
                    @Override
                    public String toString(OnLaunchAction object) {
                        return object.toString();
                    }

                    @Override
                    public OnLaunchAction fromString(String string) {
                        return OnLaunchAction.fromString(string);
                    }
                });
                onLaunchActionComboBox.setValue(GENERAL_SETTINGS.getOnLaunchAction(PredefinedSetting.ON_GAME_LAUNCH_ACTION));
                onLaunchActionComboBox.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        Main.GENERAL_SETTINGS.setSettingValue(PredefinedSetting.ON_GAME_LAUNCH_ACTION, onLaunchActionComboBox.getValue());
                        if (onLaunchActionComboBox.getValue().equals(OnLaunchAction.CLOSE)) {
                            GameRoomAlert alert = new GameRoomAlert(Alert.AlertType.WARNING);
                            alert.setContentText(RESSOURCE_BUNDLE.getString("onLaunch_close_dialog_warning"));
                            alert.showAndWait();
                        }
                        if (changeListener != null) {
                            changeListener.changed(null, null, onLaunchActionComboBox.getValue());
                        }
                    }
                });
                node2 = onLaunchActionComboBox;
            } else if (setting.isClass(Locale.class)) {
                /**************** LOCALE **************/
                ComboBox<Locale> localeComboBox = new ComboBox<>();
                localeComboBox.getItems().addAll(Locale.FRENCH, Locale.ENGLISH);
                localeComboBox.setConverter(new StringConverter<Locale>() {
                    @Override
                    public String toString(Locale object) {
                        return object.getDisplayLanguage(object);
                    }

                    @Override
                    public Locale fromString(String string) {
                        return null;
                    }
                });
                localeComboBox.setValue(Main.GENERAL_SETTINGS.getLocale(PredefinedSetting.LOCALE));
                localeComboBox.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        Main.RESSOURCE_BUNDLE = ResourceBundle.getBundle("strings", localeComboBox.getValue());
                        Main.SETTINGS_BUNDLE = ResourceBundle.getBundle("settings", localeComboBox.getValue());
                        Main.GAME_GENRES_BUNDLE = ResourceBundle.getBundle("gamegenres", localeComboBox.getValue());
                        Main.GAME_THEMES_BUNDLE = ResourceBundle.getBundle("gamethemes", localeComboBox.getValue());
                        Main.GENERAL_SETTINGS.setSettingValue(PredefinedSetting.LOCALE, localeComboBox.getValue());

                        if (changeListener != null) {
                            changeListener.changed(null, null, localeComboBox.getValue());
                        }
                    }
                });
                node2 = localeComboBox;
            } else if (setting.isClass(File.class)) {
                /**************** PATH **************/
                File p = GENERAL_SETTINGS.getFile(setting);
                PathTextField gamesFolderField = new PathTextField(p != null ? p.toString() : "", this, PathTextField.FILE_CHOOSER_FOLDER, RESSOURCE_BUNDLE.getString("select_a_folder"));
                gamesFolderField.getTextField().textProperty().addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                        GENERAL_SETTINGS.setSettingValue(setting, new File(newValue));

                        if (changeListener != null) {
                            changeListener.changed(observable, oldValue, newValue);
                        }
                    }
                });
                node2 = gamesFolderField;
            }
            if (node2 != null) {
                node2.setId(setting.getKey());
            }
            contentPane.getChildren().add(createLine(label, node2));
        }
    }

    private HBox createLine(Node nodeLeft, Node nodeRight) {
        HBox box = new HBox();

        final HBox leftSection = new HBox(nodeLeft);
        final HBox centerSection = new HBox(new Region());
        final HBox rightSection = new HBox(nodeRight);


    /* Center all sections and always grow them. Has the effect known as JUSTIFY. */
        HBox.setHgrow(leftSection, Priority.ALWAYS);
        HBox.setHgrow(centerSection, Priority.ALWAYS);
        HBox.setHgrow(rightSection, Priority.ALWAYS);

        leftSection.setAlignment(Pos.CENTER_LEFT);
        centerSection.setAlignment(Pos.CENTER);
        rightSection.setAlignment(Pos.CENTER_RIGHT);
        box.getChildren().addAll(leftSection, centerSection, rightSection);
        box.setSpacing(20 * Main.SCREEN_WIDTH / 1920);
        return box;
    }

    private void initTop() {
        wrappingPane.setTop(createTop(event -> {
            fadeTransitionTo(previousScene, getParentStage(), true);
        }, RESSOURCE_BUNDLE.getString("Settings")));
    }

    private Node searchNode(String id) {
        return searchNodeInPane(id, contentPane);
    }

    private Node searchNodeInPane(String id, Pane pane) {
        for (Node n : pane.getChildren()) {
            if (n instanceof Pane) {
                Node result = searchNodeInPane(id, (Pane) n);
                if (result != null) {
                    return result;
                }
            } else {
                if (n != null && n.getId() != null && n.getId().equals(id)) {
                    return n;
                }
            }

        }
        return null;
    }

    @Override
    public Pane getWrappingPane() {
        return wrappingPane;
    }

    @Override
    void initAndAddWrappingPaneToRoot() {
        wrappingPane = new BorderPane();
        maskView.setOpacity(0);
        getRootStackPane().getChildren().add(wrappingPane);
    }
}

package ui.scene;

import com.mashape.unirest.http.exceptions.UnirestException;
import data.game.entry.GameEntry;
import data.http.key.KeyChecker;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import system.os.mslinks.ShellLink;
import org.json.JSONObject;
import system.application.MessageListener;
import system.application.MessageTag;
import system.application.OnLaunchAction;
import system.application.settings.PredefinedSetting;
import system.application.settings.SettingValue;
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
import ui.control.ValidEntryCondition;
import ui.control.textfield.PathTextField;
import ui.dialog.ActivationKeyDialog;
import ui.dialog.GameFoldersIgnoredSelector;
import ui.dialog.GameRoomAlert;
import ui.dialog.SteamIgnoredSelector;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.BooleanSupplier;

import static ui.Main.*;

/**
 * Created by LM on 03/07/2016.
 */
public class SettingsScene extends BaseScene {
    private final static String GAMEROOM_LNK_NAME = "GameRoom.lnk";
    public final static String ADVANCE_MODE_LABEL_STYLE = "    -fx-text-fill: derive(-flatter-red, -20.0%);";
    private BorderPane wrappingPane;
    private HashMap<String, FlowPane> flowPaneHashMap = new HashMap<>();
    private HashMap<String, Tab> tabHashMap = new HashMap<>();
    private TabPane tabPane = new TabPane();
    private ArrayList<ValidEntryCondition> validEntriesConditions = new ArrayList<>();


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
        String[] categoriesToDisplay = new String[]{SettingValue.CATEGORY_GENERAL
                , SettingValue.CATEGORY_ON_GAME_START
                , SettingValue.CATEGORY_UI
        };

        for (String category : categoriesToDisplay) {
            FlowPane flowPane = new FlowPane();
            flowPane.setHgap(50 * GENERAL_SETTINGS.getWindowHeight() / 1080);
            flowPane.setVgap(30 * GENERAL_SETTINGS.getWindowWidth() / 1920);
            //tilePane.setPrefColumns(2);
            flowPane.setOrientation(Orientation.VERTICAL);
            //flowPane.setAlignment(Pos.TOP_LEFT);
            //tilePane.setTileAlignment(Pos.CENTER_LEFT);
            //tilePane.setPrefRows(8);
            flowPaneHashMap.put(category, flowPane);

            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setContent(flowPane);
            //flowPane.maxWidthProperty().bind(scrollPane.widthProperty());
            flowPane.maxWidthProperty().bind(scrollPane.widthProperty());
            flowPane.prefHeightProperty().bind(scrollPane.heightProperty());

            flowPane.setPadding(new Insets(20 * SCREEN_HEIGHT / 1080, 20 * SCREEN_WIDTH / 1920, 20 * SCREEN_HEIGHT / 1080, 20 * SCREEN_WIDTH / 1920));


            Tab tab = new Tab(Main.RESSOURCE_BUNDLE.getString(category));
            tab.setClosable(false);
            tab.setTooltip(new Tooltip(Main.RESSOURCE_BUNDLE.getString(category)));
            tab.setContent(scrollPane);
            tabHashMap.put(category, tab);

            tabPane.getTabs().add(tab);

        }


        addPropertyLine(PredefinedSetting.LOCALE);
        addPropertyLine(PredefinedSetting.ON_GAME_LAUNCH_ACTION);
        addPropertyLine(PredefinedSetting.NO_NOTIFICATIONS);
        addPropertyLine(PredefinedSetting.START_WITH_WINDOWS, false, new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                Node startMinimizedNode = getNode(PredefinedSetting.START_MINIMIZED.getKey());
                if (startMinimizedNode != null && startMinimizedNode instanceof CheckBox) {
                    boolean startMinimized = ((CheckBox) startMinimizedNode).isSelected();
                    if (newValue != null && newValue instanceof Boolean) {
                        startMinimizedNode.setDisable(!(Boolean) newValue);

                        if ((Boolean) newValue) {
                            try {
                                createStartupInk(!startMinimized);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            deleteStartupLink();
                        }
                    }
                }
            }
        });
        addPropertyLine(PredefinedSetting.START_MINIMIZED, false, new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if (newValue != null && newValue instanceof Boolean) {
                    try {
                        createStartupInk(!(Boolean) newValue);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        getNode(PredefinedSetting.START_MINIMIZED.getKey()).setDisable(!GENERAL_SETTINGS.getBoolean(PredefinedSetting.START_WITH_WINDOWS));
        addPropertyLine(PredefinedSetting.DISABLE_GAME_MAIN_THEME, true);
        addPropertyLine(PredefinedSetting.DISABLE_MAINSCENE_WALLPAPER, true, new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    MAIN_SCENE.setChangeBackgroundNextTime(false);
                    MAIN_SCENE.setImageBackground(null);
                }
            }
        });
        addPropertyLine(PredefinedSetting.ENABLE_XBOX_CONTROLLER_SUPPORT, true, new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    Main.xboxController.startThreads();
                } else {
                    Main.xboxController.stopThreads();
                }
            }
        });
        addPropertyLine(PredefinedSetting.ENABLE_GAMING_POWER_MODE, false, new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                Node node = getNode(PredefinedSetting.GAMING_POWER_MODE.getKey());
                if (node != null) {
                    node.setDisable(!newValue);
                }
            }
        });
        addPropertyLine(PredefinedSetting.GAMING_POWER_MODE);
        addPropertyLine(PredefinedSetting.GAMES_FOLDER);

        /***********************GAME FOLDER IGNORED****************************/
        Label gameFoldersIgnoredLabel = new Label(Main.SETTINGS_BUNDLE.getString("manage_ignored_game_folders_label") + ": ");
        gameFoldersIgnoredLabel.setTooltip(new Tooltip(Main.SETTINGS_BUNDLE.getString("manage_ignored_game_folders_tooltip")));
        Button manageGameFoldersIgnoredButton = new Button(Main.RESSOURCE_BUNDLE.getString("manage"));

        manageGameFoldersIgnoredButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    GameFoldersIgnoredSelector selector = new GameFoldersIgnoredSelector();
                    Optional<ButtonType> ignoredOptionnal = selector.showAndWait();
                    ignoredOptionnal.ifPresent(pairs -> {
                        if (pairs.getButtonData().equals(ButtonBar.ButtonData.OK_DONE)) {
                            GENERAL_SETTINGS.setSettingValue(PredefinedSetting.IGNORED_GAME_FOLDERS, selector.getSelectedEntries());
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        flowPaneHashMap.get(PredefinedSetting.IGNORED_GAME_FOLDERS.getCategory()).getChildren().add(createLine(gameFoldersIgnoredLabel, manageGameFoldersIgnoredButton));


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

        flowPaneHashMap.get(PredefinedSetting.IGNORED_STEAM_APPS.getCategory()).getChildren().add(createLine(steamIgnoredGamesLabel, manageSteamGamesIgnoredButton));

        /***********************SUPPORTER KEY****************************/
        //TODO pay IGDB play and set supporter key pay public, and replace by true

        if (DEV_MODE) {
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
                            } else {
                                Main.LOGGER.error("Error while trying to deactivate key : " + response.toString(4));
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

            flowPaneHashMap.get(PredefinedSetting.SUPPORTER_KEY.getCategory()).getChildren().add(createLine(supporterKeyLabel, actDeactButton));
        }

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
        flowPaneHashMap.get(SettingValue.CATEGORY_GENERAL).getChildren().add(createLine(versionLabel, checkUpdatesButton));

        /***********************CMD****************************/
        if (GENERAL_SETTINGS.getBoolean(PredefinedSetting.ADVANCED_MODE)) {
            Label cmdBeforeLabel = new Label(RESSOURCE_BUNDLE.getString("cmd_before_label") + " :");
            cmdBeforeLabel.setTooltip(new Tooltip(RESSOURCE_BUNDLE.getString("cmd_before_tooltip")));
            cmdBeforeLabel.setStyle(SettingsScene.ADVANCE_MODE_LABEL_STYLE);

            TextArea cmdBeforeField = new TextArea(GENERAL_SETTINGS.getStrings(PredefinedSetting.CMD)[GameEntry.CMD_BEFORE_START]);
            cmdBeforeField.setWrapText(true);
            cmdBeforeField.setId("cmd_before");
            cmdBeforeField.setPrefRowCount(5);
            cmdBeforeField.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    String[] cmds = GENERAL_SETTINGS.getStrings(PredefinedSetting.CMD);
                    cmds[GameEntry.CMD_BEFORE_START] = newValue;
                    GENERAL_SETTINGS.setSettingValue(PredefinedSetting.CMD, cmds);
                }
            });
            flowPaneHashMap.get(SettingValue.CATEGORY_ON_GAME_START).getChildren().add(createLine(cmdBeforeLabel, cmdBeforeField));

            Label cmdAfterLabel = new Label(RESSOURCE_BUNDLE.getString("cmd_after_label") + " :");
            cmdAfterLabel.setTooltip(new Tooltip(RESSOURCE_BUNDLE.getString("cmd_after_tooltip")));
            cmdAfterLabel.setStyle(SettingsScene.ADVANCE_MODE_LABEL_STYLE);

            TextArea cmdAfterField = new TextArea(GENERAL_SETTINGS.getStrings(PredefinedSetting.CMD)[GameEntry.CMD_AFTER_END]);
            cmdAfterField.setWrapText(true);
            cmdAfterField.setId("cmd_after");
            cmdAfterField.setPrefRowCount(5);
            cmdAfterField.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    String[] cmds = GENERAL_SETTINGS.getStrings(PredefinedSetting.CMD);
                    cmds[GameEntry.CMD_AFTER_END] = newValue;
                    GENERAL_SETTINGS.setSettingValue(PredefinedSetting.CMD, cmds);
                }
            });
            flowPaneHashMap.get(SettingValue.CATEGORY_ON_GAME_START).getChildren().add(createLine(cmdAfterLabel, cmdAfterField));

        }

        /***********************ADVANCED MODE **************************************/
        addPropertyLine(PredefinedSetting.ADVANCED_MODE, false, new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                fadeTransitionTo(new SettingsScene(new StackPane(), getParentStage(), previousScene), getParentStage());
            }
        });
        addPropertyLine(PredefinedSetting.DEBUG_MODE, true);

        /***********************ROW CONSTRAINTS****************************/
        /**********************NO CONTROL INIT BELOW THIS*******************/

        wrappingPane.setCenter(tabPane);
        BorderPane.setMargin(tabPane, new Insets(10 * SCREEN_HEIGHT / 1080, 50 * SCREEN_WIDTH / 1920, 20 * SCREEN_HEIGHT / 1080, 50 * SCREEN_WIDTH / 1920));

    }

    private void addPropertyLine(PredefinedSetting setting) {
        addPropertyLine(setting, false);
    }

    private void addPropertyLine(PredefinedSetting setting, boolean advanceSetting) {
        addPropertyLine(setting, advanceSetting, null);
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
            if ((advancedSetting && GENERAL_SETTINGS.getBoolean(PredefinedSetting.ADVANCED_MODE))) {
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
            } else if (setting.isClass(String.class)) {
                /**************** PATH **************/
                String p = GENERAL_SETTINGS.getString(setting);
                PathTextField gamesFolderField = new PathTextField(p, this, PathTextField.FILE_CHOOSER_FOLDER, RESSOURCE_BUNDLE.getString("select_a_folder"));
                gamesFolderField.setId(setting.getKey());
                gamesFolderField.getTextField().textProperty().addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                        GENERAL_SETTINGS.setSettingValue(setting, newValue);

                        if (changeListener != null) {
                            changeListener.changed(observable, oldValue, newValue);
                        }
                    }
                });
                validEntriesConditions.add(new ValidEntryCondition() {

                    @Override
                    public boolean isValid() {
                        if (setting.equals(PredefinedSetting.GAMES_FOLDER)) {
                            String dir = GENERAL_SETTINGS.getString(PredefinedSetting.GAMES_FOLDER);
                            if (dir.equals("")) {
                                return true;
                            }
                            File gamesFolder = new File(dir);
                            if (!gamesFolder.exists()) {
                                message.replace(0, message.length(), Main.RESSOURCE_BUNDLE.getString("invalid_gamesFolder_exist"));
                                return false;
                            }
                            if (!gamesFolder.isDirectory()) {
                                message.replace(0, message.length(), Main.RESSOURCE_BUNDLE.getString("invalid_gamesFolder_is_no_folder"));
                                return false;
                            }
                            return true;
                        }
                        return true;
                    }

                    @Override
                    public void onInvalid() {
                        setLineInvalid(setting.getKey());
                    }
                });
                node2 = gamesFolderField;
            }
            if (node2 != null) {
                node2.setId(setting.getKey());
            }
            flowPaneHashMap.get(setting.getCategory()).getChildren().add(createLine(label, node2));
        }
    }

    private void setLineInvalid(String property_key) {
        String style = "-fx-text-inner-color: red;\n";
        for (FlowPane contentPane : flowPaneHashMap.values()) {
            for (Node node : contentPane.getChildren()) {
                if (node.getId() != null && node.getId().equals(property_key)) {
                    node.setStyle(style);
                    node.focusedProperty().addListener(new ChangeListener<Boolean>() {
                        @Override
                        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                            if (newValue) {
                                node.setStyle("");
                            }
                        }
                    });
                    break;
                }
            }
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
            boolean allConditionsMet = true;
            for (ValidEntryCondition condition : validEntriesConditions) {
                allConditionsMet = allConditionsMet && condition.isValid();
                if (!condition.isValid()) {
                    condition.onInvalid();
                    GameRoomAlert alert = new GameRoomAlert(Alert.AlertType.ERROR, condition.message.toString());
                    alert.showAndWait();
                }
            }
            if (allConditionsMet) {
                fadeTransitionTo(previousScene, getParentStage(), true);
            }
        }, RESSOURCE_BUNDLE.getString("Settings")));
    }

    private Node getNode(String id) {
        for (FlowPane contentPane : flowPaneHashMap.values()) {
            Node n = searchNodeInPane(id, contentPane);
            if (n != null) {
                return n;
            }
        }
        return null;
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

    private static void deleteStartupLink() {
        File f = new File(getUserStartupFolder() + GAMEROOM_LNK_NAME);
        f.delete();
    }

    private static String getUserStartupFolder() {
        return System.getProperty("user.home") + "\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Startup\\";
    }

    private static void createStartupInk(boolean show) throws IOException {
        try {
            String currentDir = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            if (currentDir.startsWith("/")) {
                currentDir = currentDir.substring(1);
            }
            currentDir.replace("/", "\\");

            String showString = show ? "1" : "0";
            ShellLink sl = ShellLink.createLink(currentDir)
                    .setWorkingDir(".")
                    .setCMDArgs("-show " + showString);

            sl.saveTo(getUserStartupFolder() + GAMEROOM_LNK_NAME);
        } catch (Exception e) {
            e.printStackTrace();
        }

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

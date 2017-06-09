package ui.scene;

import com.mashape.unirest.http.exceptions.UnirestException;
import data.game.GameWatcher;
import data.game.entry.Emulator;
import data.game.entry.GameEntry;
import data.game.entry.Platform;
import data.game.scanner.ScanPeriod;
import data.game.scraper.SteamOnlineScraper;
import data.game.scraper.SteamProfile;
import data.http.key.KeyChecker;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventDispatchChain;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import net.lingala.zip4j.exception.ZipException;
import org.json.JSONObject;
import system.application.GameRoomUpdater;
import system.application.OnLaunchAction;
import system.application.settings.PredefinedSetting;
import system.application.settings.SettingValue;
import system.os.PowerMode;
import system.os.mslinks.ShellLink;
import ui.Main;
import ui.control.ValidEntryCondition;
import ui.control.button.HelpButton;
import ui.control.textfield.CMDTextField;
import ui.control.textfield.PathTextField;
import ui.dialog.*;
import ui.dialog.selector.GameScannerSelector;
import ui.dialog.selector.IgnoredEntrySelector;
import ui.pane.platform.EmulatorSettingsPane;
import ui.pane.platform.PlatformSettingsPane;
import ui.theme.Theme;
import ui.theme.ThemeUtils;
import ui.theme.UIScale;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static system.application.settings.GeneralSettings.settings;
import static system.application.settings.SettingValue.CATEGORY_ON_GAME_START;
import static system.application.settings.SettingValue.CATEGORY_SCAN;
import static ui.Main.*;

/**
 * Created by LM on 03/07/2016.
 */
public class SettingsScene extends BaseScene {
    private final static String GAMEROOM_LNK_NAME = "GameRoom.lnk";
    private BorderPane wrappingPane;
    private HashMap<String, FlowPane> flowPaneHashMap = new HashMap<>();
    private HashMap<String, Tab> tabHashMap = new HashMap<>();
    private TabPane tabPane = new TabPane();
    private ArrayList<ValidEntryCondition> validEntriesConditions = new ArrayList<>();


    public SettingsScene(StackPane root, Stage parentStage, BaseScene previousScene) {
        super(root, parentStage);
        this.previousScene = previousScene;

        initTop();
        initCenter();
        initBottom();
    }

    private void initBottom() {
        //Removed as there is no need to display it if paying IGDB
        /*Label igdbLabel = new Label(Main.getString("credit_igdb"));
        wrappingPane.setBottom(igdbLabel);

        BorderPane.setAlignment(igdbLabel, Pos.CENTER_RIGHT);
        BorderPane.setMargin(igdbLabel, new Insets(15, 15, 15, 15));*/
    }

    private void initCenter() {
        String[] categoriesToDisplay = new String[]{SettingValue.CATEGORY_GENERAL
                , CATEGORY_SCAN
                , SettingValue.CATEGORY_ON_GAME_START
                , SettingValue.CATEGORY_UI
        };

        for (String category : categoriesToDisplay) {
            FlowPane flowPane = new FlowPane();
            flowPane.setHgap(50 * settings().getWindowHeight() / 1080);
            flowPane.setVgap(30 * settings().getWindowWidth() / 1920);
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


            Tab tab = new Tab(Main.getString(category));
            tab.setClosable(false);
            tab.setTooltip(new Tooltip(Main.getString(category)));
            tab.setContent(scrollPane);
            tabHashMap.put(category, tab);

            tabPane.getTabs().add(tab);

        }


        addPropertyLine(PredefinedSetting.LOCALE, false);
        addPropertyLine(PredefinedSetting.ON_GAME_LAUNCH_ACTION);
        addPropertyLine(PredefinedSetting.NO_NOTIFICATIONS);
        addPropertyLine(PredefinedSetting.NO_TOASTS, true);
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
        getNode(PredefinedSetting.START_MINIMIZED.getKey()).setDisable(!settings().getBoolean(PredefinedSetting.START_WITH_WINDOWS));
        addPropertyLine(PredefinedSetting.DISABLE_GAME_MAIN_THEME, true, new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if (newValue instanceof Boolean) {
                    if (!(Boolean) newValue) {
                        GameRoomAlert alert = new GameRoomAlert(Alert.AlertType.WARNING);
                        alert.setContentText(Main.getString("warning_youtube_player"));
                        alert.showAndWait();
                    }
                }
            }
        });
        /*addPropertyLine(PredefinedSetting.DISABLE_MAINSCENE_WALLPAPER, true, new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    MAIN_SCENE.setChangeBackgroundNextTime(false);
                    MAIN_SCENE.setImageBackground(null);
                }
            }
        });*/
        addPropertyLine(PredefinedSetting.DISABLE_SCROLLBAR_IN_FULLSCREEN, true);

            addPropertyLine(PredefinedSetting.ENABLE_GAME_CONTROLLER_SUPPORT,false, new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    Main.gameController.startThreads();
                } else {
                    Main.gameController.stopThreads();
                }
            }
        });
        addPropertyLine(PredefinedSetting.UI_SCALE, false);
        addPropertyLine(PredefinedSetting.THEME, false);

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
        //TODO create a dialog to manage game folders
        //addPropertyLine(PredefinedSetting.GAMES_FOLDER);

        /***********************GAME SCANNERS GAMES IGNORED****************************/
        Label scannersLabel = new Label(Main.getSettingsString("enabledGameScanners_label") + " : ");
        scannersLabel.setTooltip(new Tooltip(Main.getSettingsString("enabledGameScanners_tooltip")));
        Button manageScannersButton = new Button(Main.getString("manage"));

        manageScannersButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                GameScannerSelector selector = new GameScannerSelector();
                Optional<ButtonType> ignoredOptionnal = selector.showAndWait();
                ignoredOptionnal.ifPresent(pairs -> {
                    if (pairs.getButtonData().equals(ButtonBar.ButtonData.OK_DONE)) {
                        settings().setSettingValue(PredefinedSetting.ENABLED_GAME_SCANNERS, selector.getDisabledScanners());
                        GameWatcher.getInstance().start();
                    }
                });
            }
        });

        flowPaneHashMap.get(PredefinedSetting.ENABLED_GAME_SCANNERS.getCategory()).getChildren().add(createLine(scannersLabel, manageScannersButton));

        addPropertyLine(PredefinedSetting.SCAN_PERIOD, false, (observable, oldValue, newValue) -> {
            GameWatcher.setScanPeriod((ScanPeriod) newValue,true);
        });

        addPropertyLine(PredefinedSetting.SYNC_STEAM_PLAYTIMES);

        addPropertyLine(PredefinedSetting.STEAM_PROFILE);


        /***********************GAME FOLDER IGNORED****************************/
        Label gameFoldersIgnoredLabel = new Label(Main.getSettingsString("manage_ignored_game_folders_label") + " : ");
        gameFoldersIgnoredLabel.setTooltip(new Tooltip(Main.getSettingsString("manage_ignored_game_folders_tooltip")));
        Button manageGameFoldersIgnoredButton = new Button(Main.getString("manage"));

        manageGameFoldersIgnoredButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    IgnoredEntrySelector selector = new IgnoredEntrySelector();
                    Optional<ButtonType> ignoredOptionnal = selector.showAndWait();
                    ignoredOptionnal.ifPresent(pairs -> {
                        if (pairs.getButtonData().equals(ButtonBar.ButtonData.OK_DONE)) {
                            selector.getSelectedEntries().forEach(entry -> {
                                entry.setSavedLocally(true);
                                entry.setIgnored(true);
                                entry.setSavedLocally(false);
                            });
                            selector.getUnselectedEntries().forEach(entry -> {
                                entry.setSavedLocally(true);
                                entry.setIgnored(false);
                                entry.setSavedLocally(false);
                                GameWatcher.getInstance().loadToAddEntries();
                            });
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        flowPaneHashMap.get(CATEGORY_SCAN).getChildren().add(createLine(gameFoldersIgnoredLabel, manageGameFoldersIgnoredButton));

        /***********************STEAM GAMES IGNORED****************************/
        /*Label steamIgnoredGamesLabel = new Label(Main.getSettingsString("manage_ignored_steam_games_label") + " : ");
        steamIgnoredGamesLabel.setTooltip(new Tooltip(Main.getSettingsString("manage_ignored_steam_games_tooltip")));
        Button manageSteamGamesIgnoredButton = new Button(Main.getString("manage"));

        manageSteamGamesIgnoredButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    SteamIgnoredSelector selector = new SteamIgnoredSelector();
                    Optional<ButtonType> ignoredOptionnal = selector.showAndWait();
                    ignoredOptionnal.ifPresent(pairs -> {
                        if (pairs.getButtonData().equals(ButtonBar.ButtonData.OK_DONE)) {
                            settings().setSettingValue(PredefinedSetting.IGNORED_STEAM_APPS, selector.getSelectedEntries());
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        flowPaneHashMap.get(PredefinedSetting.IGNORED_STEAM_APPS.getCategory()).getChildren().add(createLine(steamIgnoredGamesLabel, manageSteamGamesIgnoredButton));
*/
        /***********************EMULATORS********************************/
        Label emulatorsLabel = new Label(Main.getSettingsString("emulators_label") + " : ");
        emulatorsLabel.setTooltip(new Tooltip(Main.getSettingsString("emulators_tooltip")));
        Button manageEmulatorsButton = new Button(Main.getString("manage"));

        manageEmulatorsButton.setOnAction(event -> {
            PlatformSettingsDialog dialog = new PlatformSettingsDialog();
            dialog.showAndWait();
        });

        flowPaneHashMap.get(CATEGORY_ON_GAME_START).getChildren().add(createLine(emulatorsLabel, manageEmulatorsButton));


        /***********************SUPPORTER KEY****************************/
        //TODO see if possible to have 2 IGDB keys, for supporters

        String keyStatus = Main.SUPPORTER_MODE ? settings().getString(PredefinedSetting.SUPPORTER_KEY) : Main.getString("none");
        String buttonText = Main.SUPPORTER_MODE ? Main.getString("deactivate") : Main.getString("activate");

        Label supporterKeyLabel = new Label(PredefinedSetting.SUPPORTER_KEY.getLabel() + " : " + keyStatus);
        supporterKeyLabel.setTooltip(new Tooltip(PredefinedSetting.SUPPORTER_KEY.getTooltip()));
        Button actDeactButton = new Button(buttonText);

        actDeactButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (SUPPORTER_MODE) {
                    try {
                        JSONObject response = KeyChecker.deactivateKey(settings().getString(PredefinedSetting.SUPPORTER_KEY));
                        if (response.getString(KeyChecker.FIELD_RESULT).equals(KeyChecker.RESULT_SUCCESS)) {
                            settings().setSettingValue(PredefinedSetting.SUPPORTER_KEY, "");
                            SUPPORTER_MODE = false;
                            String keyStatus = Main.SUPPORTER_MODE ? settings().getString(PredefinedSetting.SUPPORTER_KEY) : Main.getString("none");
                            String buttonText = Main.SUPPORTER_MODE ? Main.getString("deactivate") : Main.getString("activate");
                            actDeactButton.setText(buttonText);
                            supporterKeyLabel.setText(PredefinedSetting.SUPPORTER_KEY.getLabel() + " : " + keyStatus);

                            settings().onSupporterModeDeactivated();
                        } else {
                            Main.LOGGER.error("Error while trying to deactivate key : " + response.toString(4));
                        }
                    } catch (IOException | UnirestException e) {
                        e.printStackTrace();
                    }
                } else {
                    displayRegisterDialog(actDeactButton, supporterKeyLabel);
                }
            }
        });

        flowPaneHashMap.get(PredefinedSetting.SUPPORTER_KEY.getCategory()).getChildren().add(createLine(supporterKeyLabel, actDeactButton));

        /********** SHORTCUTS ******************/
        Label shortcutsLabel = new Label(Main.getString("shortcuts_label"));
        Button shortcutButton = new Button(Main.getString("see"));
        shortcutButton.setOnAction(event -> {
            GameRoomAlert.info(Main.getString("shortcuts_content"));
        });

        flowPaneHashMap.get(SettingValue.CATEGORY_GENERAL).getChildren().add(createLine(shortcutsLabel, shortcutButton));


        /***********************VERSION CHECK****************************/
        Label versionLabel = new Label(Main.getString("version") + " : " + Main.getVersion());
        Button checkUpdatesButton = new Button(Main.getString("check_now"));

        /*NETWORK_MANAGER.addMessageListener(new MessageListener() {
            @Override
            public void onMessageReceived(MessageTag tag, String payload) {
                if (tag.equals(MessageTag.ERROR)) {
                    Platform.runLater(() -> checkUpdatesButton.setText(Main.RESSOURCE_BUNDLE.getString("error")));
                } else if (tag.equals(MessageTag.NO_UPDATE)) {
                    Platform.runLater(() -> checkUpdatesButton.setText(Main.RESSOURCE_BUNDLE.getString("up_to_date!")));
                } else if (tag.equals(MessageTag.NEW_UPDATE)) {
                    Platform.runLater(() -> checkUpdatesButton.setText(Main.RESSOURCE_BUNDLE.getString("new_version") + " : " + payload));
                }
            }
        });*/
        GameRoomUpdater updater = GameRoomUpdater.getInstance();
        updater.setChangeListener((observable, oldValue, newValue) -> {
            checkUpdatesButton.setText(Main.getString("loading") + " " + (int) (newValue.doubleValue() * 100) + "%");
            checkUpdatesButton.setMouseTransparent(true);
        });
        updater.setFailedPropertyListener((observable, oldValue, newValue) -> {
            checkUpdatesButton.setText(Main.getString("error"));
            checkUpdatesButton.setMouseTransparent(false);
        });
        updater.setSucceedPropertyListener((observable, oldValue, newValue) -> {
            checkUpdatesButton.setText(Main.getString("Downloaded"));
            checkUpdatesButton.setMouseTransparent(false);
        });
        updater.setNoUpdateListener((observable, oldValue, newValue) -> {
            checkUpdatesButton.setText(Main.getString("up_to_date!"));
            checkUpdatesButton.setMouseTransparent(false);
        });
        updater.setCancelledListener((observable, oldValue, newValue) -> {
            checkUpdatesButton.setText(Main.getString("check_now"));
            checkUpdatesButton.setMouseTransparent(false);
        });
        updater.setOnUpdatePressedListener(null);

        checkUpdatesButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (!updater.isStarted()) {
                    checkUpdatesButton.setText(Main.getString("loading") + "...");
                    updater.start();
                }
            }
        });
        flowPaneHashMap.get(SettingValue.CATEGORY_GENERAL).getChildren().add(createLine(versionLabel, checkUpdatesButton));

        /***********************CMD****************************/
        if (settings().getBoolean(PredefinedSetting.ADVANCED_MODE)) {
            Label cmdBeforeLabel = new Label(Main.getString("cmd_before_label") + " :");
            cmdBeforeLabel.setTooltip(new Tooltip(Main.getString("cmd_before_tooltip")));
            //cmdBeforeLabel.setStyle(SettingsScene.ADVANCE_MODE_LABEL_STYLE);
            cmdBeforeLabel.setId("advanced-setting-label");

            CMDTextField cmdBeforeField = new CMDTextField(settings().getStrings(PredefinedSetting.CMD)[GameEntry.CMD_BEFORE_START]);
            cmdBeforeField.setWrapText(true);
            cmdBeforeField.setId("cmd_before");
            cmdBeforeField.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    String[] cmds = settings().getStrings(PredefinedSetting.CMD);
                    cmds[GameEntry.CMD_BEFORE_START] = newValue;
                    settings().setSettingValue(PredefinedSetting.CMD, cmds);
                }
            });
            flowPaneHashMap.get(SettingValue.CATEGORY_ON_GAME_START).getChildren().add(createLine(cmdBeforeLabel, cmdBeforeField));

            Label cmdAfterLabel = new Label(Main.getString("cmd_after_label") + " :");
            cmdAfterLabel.setTooltip(new Tooltip(Main.getString("cmd_after_tooltip")));
            //cmdAfterLabel.setStyle(SettingsScene.ADVANCE_MODE_LABEL_STYLE);
            cmdAfterLabel.setId("advanced-setting-label");

            CMDTextField cmdAfterField = new CMDTextField(settings().getStrings(PredefinedSetting.CMD)[GameEntry.CMD_AFTER_END]);
            cmdAfterField.setWrapText(true);
            cmdAfterField.setId("cmd_after");
            cmdAfterField.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    String[] cmds = settings().getStrings(PredefinedSetting.CMD);
                    cmds[GameEntry.CMD_AFTER_END] = newValue;
                    settings().setSettingValue(PredefinedSetting.CMD, cmds);
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

        /***********************OPEN LOG FOLDER **************************************/
        if ((settings().getBoolean(PredefinedSetting.ADVANCED_MODE))) {
            Label logLabel = new Label(Main.getString("open_logs_folder") + " : ");
            //logLabel.setStyle(ADVANCE_MODE_LABEL_STYLE);
            logLabel.setId("advanced-setting-label");
            Button logButton = new Button(Main.getString("open"));

            logButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    try {
                        Desktop.getDesktop().open(Main.FILES_MAP.get("log"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            flowPaneHashMap.get(SettingValue.CATEGORY_GENERAL).getChildren().add(createLine(logLabel, logButton));
        }

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
        if (!advancedSetting || (advancedSetting && settings().getBoolean(PredefinedSetting.ADVANCED_MODE))) {
            Node node2 = null;
            if (setting.isClass(Boolean.class)) {
                /**************** BOOLEAN **************/
                CheckBox checkBox = new CheckBox();
                checkBox.setSelected(settings().getBoolean(setting));
                checkBox.setWrapText(true);
                checkBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                        settings().setSettingValue(setting, newValue);
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
                powerModeComboBox.setValue(settings().getPowerMode(PredefinedSetting.GAMING_POWER_MODE));
                powerModeComboBox.setDisable(!settings().getBoolean(PredefinedSetting.ENABLE_GAMING_POWER_MODE));
                powerModeComboBox.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        settings().setSettingValue(PredefinedSetting.GAMING_POWER_MODE, powerModeComboBox.getValue());
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
                onLaunchActionComboBox.setValue(settings().getOnLaunchAction(PredefinedSetting.ON_GAME_LAUNCH_ACTION));
                onLaunchActionComboBox.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        settings().setSettingValue(PredefinedSetting.ON_GAME_LAUNCH_ACTION, onLaunchActionComboBox.getValue());
                        if (onLaunchActionComboBox.getValue().equals(OnLaunchAction.CLOSE)) {
                            GameRoomAlert alert = new GameRoomAlert(Alert.AlertType.WARNING);
                            alert.setContentText(Main.getString("onLaunch_close_dialog_warning"));
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
                localeComboBox.getItems().addAll(Locale.FRENCH, Locale.ENGLISH, Locale.GERMAN);
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
                localeComboBox.setValue(settings().getLocale(PredefinedSetting.LOCALE));
                localeComboBox.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        if (changeListener != null) {
                            changeListener.changed(null, settings().getLocale(PredefinedSetting.LOCALE), localeComboBox.getValue());
                        }
                        Main.setRessourceBundle(ResourceBundle.getBundle("strings", localeComboBox.getValue()));
                        Main.setSettingsBundle(ResourceBundle.getBundle("settings", localeComboBox.getValue()));
                        Main.GAME_GENRES_BUNDLE = ResourceBundle.getBundle("gamegenres", localeComboBox.getValue());
                        Main.GAME_THEMES_BUNDLE = ResourceBundle.getBundle("gamethemes", localeComboBox.getValue());

                        settings().setSettingValue(PredefinedSetting.LOCALE, localeComboBox.getValue());

                        displayRestartDialog();
                        Main.restart(getParentStage(), "Applying language");
                    }
                });
                node2 = localeComboBox;
            } else if (setting.isClass(SteamProfile.class)) {
                /**************** LOCALE **************/
                Button manageButton = new Button(Main.getString("manage"));
                manageButton.setOnAction(event -> {
                    SteamOnlineScraper.checkIfCanScanSteam(true);
                });
                node2 = manageButton;
            } else if (setting.isClass(String.class)) {
                /**************** PATH **************/
                String p = settings().getString(setting);
                PathTextField gamesFolderField = new PathTextField(p, getWindow(), PathTextField.FILE_CHOOSER_FOLDER, Main.getString("select_a_folder"));
                gamesFolderField.setId(setting.getKey());
                gamesFolderField.getTextField().textProperty().addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                        settings().setSettingValue(setting, newValue);

                        if (changeListener != null) {
                            changeListener.changed(observable, oldValue, newValue);
                        }
                    }
                });
                validEntriesConditions.add(new ValidEntryCondition() {

                    @Override
                    public boolean isValid() {
                        //TODO create a dialog to manage game folders
                        /*if (setting.equals(PredefinedSetting.GAMES_FOLDER)) {
                            String dir = settings().getString(PredefinedSetting.GAMES_FOLDER);
                            if (dir.equals("")) {
                                return true;
                            }
                            File gamesFolder = new File(dir);
                            if (!gamesFolder.exists()) {
                                message.replace(0, message.length(), Main.getString("invalid_gamesFolder_exist"));
                                return false;
                            }
                            if (!gamesFolder.isDirectory()) {
                                message.replace(0, message.length(), Main.getString("invalid_gamesFolder_is_no_folder"));
                                return false;
                            }
                            return true;
                        }*/
                        return true;
                    }

                    @Override
                    public void onInvalid() {
                        setLineInvalid(setting.getKey());
                    }
                });
                node2 = gamesFolderField;
            } else if (setting.isClass(UIScale.class)) {
                /**************** ON LAUNCH ACTION **************/
                ComboBox<UIScale> uiScaleComboBox = new ComboBox<>();
                uiScaleComboBox.getItems().addAll(UIScale.values());
                uiScaleComboBox.setConverter(new StringConverter<UIScale>() {
                    @Override
                    public String toString(UIScale object) {
                        return object.getDisplayName();
                    }

                    @Override
                    public UIScale fromString(String string) {
                        return UIScale.fromString(string);
                    }
                });
                uiScaleComboBox.setValue(settings().getUIScale());
                uiScaleComboBox.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        settings().setSettingValue(PredefinedSetting.UI_SCALE, uiScaleComboBox.getValue());
                        displayRestartDialog();
                        Main.restart(getParentStage(), "Applying new UI scale");
                        if (changeListener != null) {
                            changeListener.changed(null, null, uiScaleComboBox.getValue());
                        }
                    }
                });
                node2 = uiScaleComboBox;
            } else if (setting.isClass(Theme.class)) {
                /**************** THEMES **************/
                ComboBox<Theme> themeComboBox = new ComboBox<>();
                themeComboBox.getItems().addAll(ThemeUtils.getInstalledThemes());
                themeComboBox.setConverter(new StringConverter<Theme>() {
                    @Override
                    public String toString(Theme object) {
                        return object.getName();
                    }

                    @Override
                    public Theme fromString(String string) {
                        return ThemeUtils.getThemeFromName(string);
                    }
                });
                themeComboBox.setValue(settings().getTheme());
                themeComboBox.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        boolean registered = checkAndDisplayRegisterDialog();
                        if (registered) {
                            Theme chosenTheme = themeComboBox.getValue();
                            settings().setSettingValue(PredefinedSetting.THEME, chosenTheme);
                            try {
                                if (changeListener != null) {
                                    changeListener.changed(null, null, themeComboBox.getValue());
                                }
                                chosenTheme.applyTheme();
                                displayRestartDialog();
                                Main.restart(getParentStage(), "ApplyingTheme");
                            } catch (IOException | ZipException e) {
                                GameRoomAlert.error(Main.getString("error_applying_theme"));
                            }
                        }
                    }
                });

                node2 = themeComboBox;
            } else if (setting.isClass(ScanPeriod.class)) {
                /**************** SCAN PERIOD **************/
                ComboBox<ScanPeriod> scanPeriodComboBox = new ComboBox<>();
                scanPeriodComboBox.getItems().addAll(ScanPeriod.values());
                scanPeriodComboBox.setConverter(new StringConverter<ScanPeriod>() {
                    @Override
                    public String toString(ScanPeriod object) {
                        return object.toString();
                    }

                    @Override
                    public ScanPeriod fromString(String string) {
                        return ScanPeriod.fromString(string);
                    }
                });
                scanPeriodComboBox.setValue(settings().getScanPeriod());
                scanPeriodComboBox.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        settings().setSettingValue(PredefinedSetting.SCAN_PERIOD, scanPeriodComboBox.getValue());
                        if (changeListener != null) {
                            changeListener.changed(null, null, scanPeriodComboBox.getValue());
                        }
                    }
                });
                node2 = scanPeriodComboBox;
            }
            if (node2 != null) {
                node2.setId(setting.getKey());
            }
            flowPaneHashMap.get(setting.getCategory()).getChildren().add(createLine(setting,advancedSetting, node2));
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

    private HBox createLine(PredefinedSetting setting, boolean advancedSetting, Node nodeRight){
        Node left = null;
        Label label = new Label(setting.getLabel() + " :");
        if ((advancedSetting && settings().getBoolean(PredefinedSetting.ADVANCED_MODE))) {
            //label.setStyle(ADVANCE_MODE_LABEL_STYLE);
            label.setId("advanced-setting-label");
        }

        String tooltip = setting.getTooltip();
        if(!tooltip.equals(setting.getLabel())){
            HBox hBox = new HBox();
            hBox.setAlignment(Pos.CENTER_LEFT);
            hBox.setSpacing(5 * SCREEN_WIDTH / 1920);
            hBox.getChildren().addAll(label,new HelpButton(tooltip));
            left = hBox;
        }else{
            label.setTooltip(new Tooltip(tooltip));
            left = label;
        }

        return createLine(left,nodeRight);
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
                    GameRoomAlert.error(condition.message.toString());
                }
            }
            if (allConditionsMet) {
                fadeTransitionTo(previousScene, getParentStage(), true);
            }
        }, Main.getString("Settings")));
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
            currentDir = currentDir.replace("/", "\\");
            currentDir = currentDir.replace(".jar",".exe");

            String showString = show ? "1" : "0";
            ShellLink sl = ShellLink.createLink(currentDir)
                    .setWorkingDir(".")
                    .setCMDArgs("-show " + showString);

            sl.saveTo(getUserStartupFolder() + GAMEROOM_LNK_NAME);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void displayRestartDialog() {
        GameRoomAlert alert = new GameRoomAlert(Alert.AlertType.WARNING);
        alert.setContentText(Main.getString("GameRoom_will_restart"));

        Optional<ButtonType> result = alert.showAndWait();
        //TODO restart
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

    public static boolean checkAndDisplayRegisterDialog() {
        if (!SUPPORTER_MODE) {
            displayRegisterDialog(null, null);
            return false;
        }
        return true;
    }

    private static void displayRegisterDialog(Button actDeactButton, Label supporterModeLabel) {
        ActivationKeyDialog dialog = new ActivationKeyDialog();

        Optional<ButtonType> result = dialog.showAndWait();
        result.ifPresent(letter -> {
            if (letter.getText().contains(Main.getString("more_infos"))) {
                WebBrowser.openSupporterKeyBuyBrowser();
            } else if (letter.getText().equals(Main.getString("activate"))) {
                try {
                    JSONObject response = KeyChecker.activateKey(dialog.getSupporterKey());
                    String message = Main.getString(response.getString(KeyChecker.FIELD_MESSAGE).replace(' ', '_'));

                    switch (response.getString(KeyChecker.FIELD_RESULT)) {
                        case KeyChecker.RESULT_SUCCESS:
                            GameRoomAlert.info(message);

                            settings().setSettingValue(PredefinedSetting.SUPPORTER_KEY, dialog.getSupporterKey());
                            SUPPORTER_MODE = KeyChecker.isKeyValid(settings().getString(PredefinedSetting.SUPPORTER_KEY));
                            String keyStatus = Main.SUPPORTER_MODE ? settings().getString(PredefinedSetting.SUPPORTER_KEY) : Main.getString("none");
                            String buttonText = Main.SUPPORTER_MODE ? Main.getString("deactivate") : Main.getString("activate");
                            if (actDeactButton != null) {
                                actDeactButton.setText(buttonText);
                            }
                            if (supporterModeLabel != null) {
                                supporterModeLabel.setText(PredefinedSetting.SUPPORTER_KEY.getLabel() + " : " + keyStatus);
                            }
                            settings().onSupporterModeActivated();

                            break;
                        case KeyChecker.RESULT_ERROR:
                            GameRoomAlert.error(message);
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

package ui.scene;

import com.mashape.unirest.http.exceptions.UnirestException;
import data.http.key.KeyChecker;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import org.json.JSONObject;
import sun.java2d.windows.GDIRenderer;
import system.application.MessageListener;
import system.application.MessageTag;
import system.application.OnLaunchAction;
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
    private BorderPane wrappingPane;
    private TilePane contentPane = new TilePane();

    public SettingsScene(StackPane root, Stage parentStage, BaseScene previousScene){
        super(root, parentStage);
        this.previousScene=previousScene;

        getStylesheets().add("res/flatterfx.css");

        initTop();
        initCenter();
        initBottom();
    }
    private void initBottom(){
        Label igdbLabel = new Label(RESSOURCE_BUNDLE.getString("credit_igdb"));
        wrappingPane.setBottom(igdbLabel);

        BorderPane.setAlignment(igdbLabel, Pos.CENTER_RIGHT);
        BorderPane.setMargin(igdbLabel, new Insets(15, 15, 15, 15));
    }
    private void initCenter(){
        contentPane.setHgap(30);
        contentPane.setVgap(15);
        contentPane.setPrefColumns(2);
        contentPane.setAlignment(Pos.TOP_LEFT);
        contentPane.setOrientation(Orientation.VERTICAL);
        contentPane.setTileAlignment(Pos.CENTER_LEFT);


        /*****************************LOCALE*********************************/
        //TODO Implement real modularized locale selection
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
        localeComboBox.setValue(Locale.forLanguageTag(Main.GENERAL_SETTINGS.getLocale()));
        localeComboBox.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Main.RESSOURCE_BUNDLE = ResourceBundle.getBundle("strings", localeComboBox.getValue());
                Main.GENERAL_SETTINGS.setLocale(localeComboBox.getValue().toLanguageTag());
            }
        });
        addLine(new Label(Main.RESSOURCE_BUNDLE.getString("Language")+" :"), localeComboBox);


        /*****************************CLOSE ON LAUNCH*********************************/
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
        onLaunchActionComboBox.setValue(GENERAL_SETTINGS.getOnLaunchAction());
        onLaunchActionComboBox.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Main.GENERAL_SETTINGS.setOnLaunchAction(onLaunchActionComboBox.getValue());
                if(onLaunchActionComboBox.getValue().equals(OnLaunchAction.CLOSE)){
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setHeaderText(null);
                    alert.initStyle(StageStyle.UNDECORATED);
                    alert.getDialogPane().getStylesheets().add("res/flatterfx.css");
                    alert.initModality(Modality.APPLICATION_MODAL);
                    alert.setContentText(RESSOURCE_BUNDLE.getString("onLaunch_close_dialog_warning"));
                    alert.showAndWait();
                }
            }
        });
        addLine(new Label(Main.RESSOURCE_BUNDLE.getString("onLaunch_action")+" :"),onLaunchActionComboBox);

        /*******************************NOTIFICATIONS*******************************/
        CheckBox noNotifCheckBox = new CheckBox();
        noNotifCheckBox.setSelected(Main.GENERAL_SETTINGS.isDisableAllNotifications());
        noNotifCheckBox.setWrapText(true);
        noNotifCheckBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                Main.GENERAL_SETTINGS.setDisableAllNotifications(newValue);
            }
        });
        addLine(new Label(Main.RESSOURCE_BUNDLE.getString("disable_tray_icon_notifications")+" :"),noNotifCheckBox);

        /*******************************MINIMIZE ON START*******************************/
        CheckBox minimizeOnStartCheckBox = new CheckBox();
        minimizeOnStartCheckBox.setSelected(Main.GENERAL_SETTINGS.isMinimizeOnStart());
        minimizeOnStartCheckBox.setWrapText(true);
        minimizeOnStartCheckBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                Main.GENERAL_SETTINGS.setMinimizeOnStart(newValue);
            }
        });
        addLine(new Label(Main.RESSOURCE_BUNDLE.getString("minimize_on_start")+" :"),minimizeOnStartCheckBox);

        /*******************************XBOX CONTROLLER SUPPORT*******************************/
        CheckBox xboxControllerCheckBox = new CheckBox();
        xboxControllerCheckBox.setSelected(Main.GENERAL_SETTINGS.isMinimizeOnStart());
        xboxControllerCheckBox.setWrapText(true);
        xboxControllerCheckBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                Main.GENERAL_SETTINGS.setActivateXboxControllerSupport(newValue);
            }
        });
        Label xboxControllerLabel= new Label(Main.RESSOURCE_BUNDLE.getString("xbox_controller_support")+" :");
        xboxControllerLabel.setTooltip(new Tooltip(Main.RESSOURCE_BUNDLE.getString("xbox_controller_support_tooltip")));
        addLine(xboxControllerLabel,xboxControllerCheckBox);

        /*****************************POWER MODE*********************************/
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
        powerModeComboBox.setValue(GENERAL_SETTINGS.getGamingPowerMode());
        powerModeComboBox.setDisable(!GENERAL_SETTINGS.isEnablePowerGamingMode());
        powerModeComboBox.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Main.GENERAL_SETTINGS.setGamingPowerMode(powerModeComboBox.getValue());
            }
        });
        CheckBox enablePowerMode = new CheckBox();
        enablePowerMode.setSelected(Main.GENERAL_SETTINGS.isEnablePowerGamingMode());
        enablePowerMode.setWrapText(true);
        enablePowerMode.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                Main.GENERAL_SETTINGS.setEnablePowerGamingMode(newValue);
                powerModeComboBox.setDisable(!newValue);
            }
        });
        addLine(new Label(Main.RESSOURCE_BUNDLE.getString("enable_gaming_power_mode")+" :"),enablePowerMode);
        addLine(new Label(Main.RESSOURCE_BUNDLE.getString("gaming_power_mode")+" :"),powerModeComboBox);


        /*****************************GAMES FOLDER*********************************/
        PathTextField gamesFolderField = new PathTextField(GENERAL_SETTINGS.getGamesFolder(), this,PathTextField.FILE_CHOOSER_FOLDER,RESSOURCE_BUNDLE.getString("select_a_folder"));
        gamesFolderField.setId("games_folder");
        gamesFolderField.getTextField().textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                GENERAL_SETTINGS.setGamesFolder(newValue);
            }
        });
        Label gamesFolderLabel= new Label(Main.RESSOURCE_BUNDLE.getString("games_folder")+" :");
        gamesFolderLabel.setTooltip(new Tooltip(Main.RESSOURCE_BUNDLE.getString("games_folder_tooltip")));
        addLine(gamesFolderLabel,gamesFolderField);

        /***********************DONATION KEY****************************/
        String keyStatus = Main.DONATOR ? GENERAL_SETTINGS.getDonationKey() : Main.RESSOURCE_BUNDLE.getString("none");
        String buttonText = Main.DONATOR ? Main.RESSOURCE_BUNDLE.getString("deactivate") : Main.RESSOURCE_BUNDLE.getString("activate");

        Label donationKeyLabel = new Label(Main.RESSOURCE_BUNDLE.getString("donation_key")+": "+ keyStatus);
        Button actDeactButton = new Button(buttonText);

        actDeactButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if(DONATOR){
                    try {
                        JSONObject response = KeyChecker.deactivateKey(GENERAL_SETTINGS.getDonationKey());
                        if(response.getString(KeyChecker.FIELD_RESULT).equals(KeyChecker.RESULT_SUCCESS)){
                            Alert successDialog = new Alert(Alert.AlertType.INFORMATION, Main.RESSOURCE_BUNDLE.getString("key_deactivated_message"));
                            successDialog.getDialogPane().getStylesheets().add("res/flatterfx.css");
                            successDialog.getDialogPane().getStyleClass().add("custom-choice-dialog");
                            successDialog.initModality(Modality.APPLICATION_MODAL);
                            successDialog.initStyle(StageStyle.UNDECORATED);
                            successDialog.setHeaderText(null);
                            successDialog.showAndWait();

                            GENERAL_SETTINGS.setDonationKey("");
                            DONATOR = false;
                            String keyStatus = Main.DONATOR ? GENERAL_SETTINGS.getDonationKey() : Main.RESSOURCE_BUNDLE.getString("none");
                            String buttonText = Main.DONATOR ? Main.RESSOURCE_BUNDLE.getString("deactivate") : Main.RESSOURCE_BUNDLE.getString("activate");
                            actDeactButton.setText(buttonText);
                            donationKeyLabel.setText(Main.RESSOURCE_BUNDLE.getString("donation_key")+": "+ keyStatus);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (UnirestException e) {
                        e.printStackTrace();
                    }
                }else {
                    ActivationKeyDialog dialog = new ActivationKeyDialog();

                    Optional<ButtonType> result = dialog.showAndWait();
                    result.ifPresent(letter -> {
                        if (letter.getText().contains(Main.RESSOURCE_BUNDLE.getString("donation_key_buy_one"))) {
                            try {
                                Desktop.getDesktop().browse(new URI("https://gameroom.me/downloads/donation-key"));
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            } catch (URISyntaxException e1) {
                                e1.printStackTrace();
                            }
                        } else if (letter.getText().equals(Main.RESSOURCE_BUNDLE.getString("activate"))) {
                            try {
                                JSONObject response = KeyChecker.activateKey(dialog.getDonationKey());
                                String message = Main.RESSOURCE_BUNDLE.getString(response.getString(KeyChecker.FIELD_MESSAGE).replace(' ', '_'));

                                switch (response.getString(KeyChecker.FIELD_RESULT)) {
                                    case KeyChecker.RESULT_SUCCESS:
                                        Alert successDialog = new Alert(Alert.AlertType.INFORMATION, message);
                                        successDialog.getDialogPane().getStylesheets().add("res/flatterfx.css");
                                        successDialog.getDialogPane().getStyleClass().add("custom-choice-dialog");
                                        successDialog.initModality(Modality.APPLICATION_MODAL);
                                        successDialog.initStyle(StageStyle.UNDECORATED);
                                        successDialog.setHeaderText(null);
                                        successDialog.showAndWait();

                                        GENERAL_SETTINGS.setDonationKey(dialog.getDonationKey());
                                        DONATOR = KeyChecker.isKeyValid(GENERAL_SETTINGS.getDonationKey());
                                        String keyStatus = Main.DONATOR ? GENERAL_SETTINGS.getDonationKey() : Main.RESSOURCE_BUNDLE.getString("none");
                                        String buttonText = Main.DONATOR ? Main.RESSOURCE_BUNDLE.getString("deactivate") : Main.RESSOURCE_BUNDLE.getString("activate");
                                        actDeactButton.setText(buttonText);
                                        donationKeyLabel.setText(Main.RESSOURCE_BUNDLE.getString("donation_key")+": "+ keyStatus);
                                        break;
                                    case KeyChecker.RESULT_ERROR:
                                        Alert errorDialog = new Alert(Alert.AlertType.ERROR, message);
                                        errorDialog.getDialogPane().getStylesheets().add("res/flatterfx.css");
                                        errorDialog.getDialogPane().getStyleClass().add("custom-choice-dialog");
                                        errorDialog.initModality(Modality.APPLICATION_MODAL);
                                        errorDialog.initStyle(StageStyle.UNDECORATED);
                                        errorDialog.setHeaderText(null);
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

        addLine(donationKeyLabel,actDeactButton);

        /***********************VERSION CHECK****************************/
        Label versionLabel = new Label(Main.RESSOURCE_BUNDLE.getString("version")+": "+Main.getVersion());
        Button checkUpdatesButton = new Button(Main.RESSOURCE_BUNDLE.getString("check_now"));

        NETWORK_MANAGER.addMessageListener(new MessageListener() {
            @Override
            public void onMessageReceived(MessageTag tag, String payload) {
                if(tag.equals(MessageTag.ERROR)){
                    Platform.runLater(() -> checkUpdatesButton.setText(Main.RESSOURCE_BUNDLE.getString("error")));
                }else if(tag.equals(MessageTag.NO_UPDATE)){
                    Platform.runLater(() -> checkUpdatesButton.setText(Main.RESSOURCE_BUNDLE.getString("up_to_date!")));
                }else if(tag.equals(MessageTag.NEW_UPDATE)){
                    Platform.runLater(() -> checkUpdatesButton.setText(Main.RESSOURCE_BUNDLE.getString("new_version")+": "+payload));
                }
            }
        });
        checkUpdatesButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                checkUpdatesButton.setText(Main.RESSOURCE_BUNDLE.getString("loading")+"...");
                Main.startUpdater();
            }
        });
        addLine(versionLabel,checkUpdatesButton);

        /***********************ROW CONSTRAINTS****************************/
        /**********************NO CONTROL INIT BELOW THIS*******************/


        /*GridPane pane = new GridPane();
        pane.add(contentPane,0,0);
        GridPane.setFillWidth(contentPane,false);*/
        wrappingPane.setCenter(contentPane);
        BorderPane.setMargin(contentPane, new Insets(50*SCREEN_HEIGHT/1080,50*SCREEN_WIDTH/1920,50*SCREEN_HEIGHT/1080,50*SCREEN_WIDTH/1920));

    }
    private void addLine(Node nodeLeft, Node nodeRight){
        HBox box = new HBox();

        final HBox leftSection = new HBox( nodeLeft);
        final HBox centerSection = new HBox(new Region());
        final HBox rightSection = new HBox( nodeRight);


    /* Center all sections and always grow them. Has the effect known as JUSTIFY. */
        HBox.setHgrow( leftSection, Priority.ALWAYS );
        HBox.setHgrow( centerSection, Priority.ALWAYS );
        HBox.setHgrow( rightSection, Priority.ALWAYS );

        leftSection.setAlignment( Pos.CENTER_LEFT );
        centerSection.setAlignment( Pos.CENTER );
        rightSection.setAlignment( Pos.CENTER_RIGHT );
        box.getChildren().addAll(leftSection,centerSection,rightSection);
        box.setSpacing(20*Main.SCREEN_WIDTH/1920);

        contentPane.getChildren().add(box);
    }
    private void initTop(){
        wrappingPane.setTop(createTop(RESSOURCE_BUNDLE.getString("Settings")));
    }

    @Override
    public Pane getWrappingPane() {
        return wrappingPane;
    }

    @Override
    void initAndAddWrappingPaneToRoot() {
        wrappingPane = new BorderPane();
        getRootStackPane().getChildren().add(wrappingPane);
    }
}

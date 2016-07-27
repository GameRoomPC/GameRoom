package ui.scene;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import sun.java2d.windows.GDIRenderer;
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

import java.util.Locale;
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
        contentPane.setOrientation(Orientation.HORIZONTAL);
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
        contentPane.getChildren().add(new Label(Main.RESSOURCE_BUNDLE.getString("Language")+" :"));
        contentPane.getChildren().add(localeComboBox);


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
        contentPane.getChildren().add(new Label(Main.RESSOURCE_BUNDLE.getString("onLaunch_action")+" :"));
        contentPane.getChildren().add(onLaunchActionComboBox);

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
        contentPane.getChildren().add(new Label(Main.RESSOURCE_BUNDLE.getString("disable_tray_icon_notifications")+" :"));
        contentPane.getChildren().add(noNotifCheckBox);

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
        contentPane.getChildren().add(new Label(Main.RESSOURCE_BUNDLE.getString("minimize_on_start")+" :"));
        contentPane.getChildren().add(minimizeOnStartCheckBox);

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
        contentPane.getChildren().add(xboxControllerLabel);
        contentPane.getChildren().add(xboxControllerCheckBox);

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
        contentPane.getChildren().add(new Label(Main.RESSOURCE_BUNDLE.getString("enable_gaming_power_mode")+" :"));
        contentPane.getChildren().add(enablePowerMode);

        contentPane.getChildren().add(new Label(Main.RESSOURCE_BUNDLE.getString("gaming_power_mode")+" :"));
        contentPane.getChildren().add(powerModeComboBox);

        /***********************ROW CONSTRAINTS****************************/
        /**********************NO CONTROL INIT BELOW THIS*******************/
        GridPane pane = new GridPane();
        pane.add(contentPane,0,0);
        GridPane.setFillWidth(contentPane,false);
        wrappingPane.setCenter(pane);
        BorderPane.setMargin(pane, new Insets(50*SCREEN_HEIGHT/1080,50*SCREEN_WIDTH/1920,50*SCREEN_HEIGHT/1080,50*SCREEN_WIDTH/1920));

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

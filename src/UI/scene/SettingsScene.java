package ui.scene;

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

import static ui.Main.GENERAL_SETTINGS;
import static ui.Main.RESSOURCE_BUNDLE;

/**
 * Created by LM on 03/07/2016.
 */
public class SettingsScene extends BaseScene {
    private BorderPane wrappingPane;
    private GridPane contentPane = new GridPane();

    public SettingsScene(StackPane root,int width, int height, Stage parentStage, BaseScene previousScene){
        super(root, parentStage);
        this.previousScene=previousScene;

        getStylesheets().add("res/flatterfx.css");

        initTop();
        initCenter();
        initBottom();
        wrappingPane.setCenter(contentPane);
    }
    private void initBottom(){
        Label igdbLabel = new Label(RESSOURCE_BUNDLE.getString("credit_igdb"));
        wrappingPane.setBottom(igdbLabel);

        BorderPane.setAlignment(igdbLabel, Pos.CENTER_RIGHT);
        BorderPane.setMargin(igdbLabel, new Insets(15, 15, 15, 15));
    }
    private void initCenter(){
        BorderPane.setMargin(contentPane, new Insets(50,50,50,50));
        contentPane.setHgap(30);
        contentPane.setVgap(15);


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
        contentPane.add(new Label(Main.RESSOURCE_BUNDLE.getString("Language")+" :"),0,0);
        contentPane.add(localeComboBox,1,0);


        /*****************************CLOSE ON LAUNCH*********************************/

        CheckBox closeOnLaunchBox = new CheckBox();
        closeOnLaunchBox.setSelected(Main.GENERAL_SETTINGS.isCloseOnLaunch());
        closeOnLaunchBox.setWrapText(true);
        closeOnLaunchBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                Main.GENERAL_SETTINGS.setCloseOnLaunch(newValue);
            }
        });
        contentPane.add(new Label(Main.RESSOURCE_BUNDLE.getString("Close_on_launch")+" :"),0,1);
        contentPane.add(closeOnLaunchBox,1,1);


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
        contentPane.add(new Label(Main.RESSOURCE_BUNDLE.getString("enable_gaming_power_mode")+" :"),0,2);
        contentPane.add(enablePowerMode,1,2);

        contentPane.add(new Label(Main.RESSOURCE_BUNDLE.getString("gaming_power_mode")+" :"),0,3);
        contentPane.add(powerModeComboBox,1,3);
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

package UI.scene;

import UI.button.ImageButton;
import UI.Main;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.Locale;
import java.util.ResourceBundle;

import static UI.Main.RESSOURCE_BUNDLE;
import static UI.Main.SCREEN_WIDTH;

/**
 * Created by LM on 03/07/2016.
 */
public class SettingsScene extends BaseScene {
    private BorderPane wrappingPane;
    private GridPane contentPane = new GridPane();
    private BaseScene previousScene;

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
        CheckBox closeOnLaunchBox = new CheckBox();
        closeOnLaunchBox.setSelected(Main.GENERAL_SETTINGS.isCloseOnLaunch());
        closeOnLaunchBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                Main.GENERAL_SETTINGS.setCloseOnLaunch(newValue);
            }
        });
        contentPane.add(new Label(Main.RESSOURCE_BUNDLE.getString("Close_on_launch")+" :"),0,1);
        contentPane.add(closeOnLaunchBox,1,1);

    }
    private void initTop(){
        Image leftArrowImage = new Image("res/ui/arrowLeft.png", SCREEN_WIDTH /45, SCREEN_WIDTH /45,true,true);
        ImageButton backButton = new ImageButton(leftArrowImage);
        backButton.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                fadeTransitionTo(previousScene,getParentStage());
            }
        });
        HBox hbox = new HBox();
        hbox.setPadding(new Insets(15, 12, 15, 10));
        hbox.setSpacing(15);
        hbox.getChildren().addAll(backButton);
        hbox.setAlignment(Pos.CENTER_LEFT);

        //HBox.setMargin(sizeSlider, new Insets(15, 12, 15, 12));

        wrappingPane.setTop(hbox);
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

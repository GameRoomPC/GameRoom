package UI.scene;

import UI.Main;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.ResourceBundle;

import static UI.Main.WIDTH;

/**
 * Created by LM on 03/07/2016.
 */
public class SettingsScene extends GameRoomScene {
    private BorderPane wrappingPane;
    private GridPane contentPane = new GridPane();
    private GameRoomScene previousScene;

    public SettingsScene(StackPane root,int width, int height, Stage parentStage, GameRoomScene previousScene){
        super(root, width, height, parentStage);
        this.previousScene=previousScene;

        getStylesheets().add("res/flatterfx.css");

        initTop();
        initCenter();
        wrappingPane.setCenter(contentPane);
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
        CheckBox closeOnLaunchBox = new CheckBox(Main.RESSOURCE_BUNDLE.getString("Close_on_launch"));
        closeOnLaunchBox.setSelected(Main.GENERAL_SETTINGS.isCloseOnLaunch());
        closeOnLaunchBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                Main.GENERAL_SETTINGS.setCloseOnLaunch(newValue);
            }
        });
        contentPane.add(closeOnLaunchBox,0,2);

    }
    private void initTop(){
        Image leftArrowImage = new Image("res/ui/arrowLeft.png",WIDTH/45,WIDTH/45,true,true);
        ImageView backButton = new ImageView(leftArrowImage);
        backButton.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                fadeTransitionTo(previousScene,getParentStage());
            }
        });
        Main.addEffectsToButton(backButton);
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

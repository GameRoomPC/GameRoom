package ui.scene;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import system.application.settings.PredefinedSetting;
import ui.Main;
import ui.control.button.ImageButton;

import static ui.Main.GENERAL_SETTINGS;
import static ui.Main.SCREEN_WIDTH;

/**
 * Created by LM on 03/07/2016.
 */
public abstract class BaseScene extends Scene {
    private static Image backgroundMaskImage;
    public final static double FADE_IN_OUT_TIME = 0.1;
    public final static double BACKGROUND_IMAGE_MAX_OPACITY = 0.65;
    final static double BACKGROUND_IMAGE_BLUR = 7;
    public final static double BACKGROUND_IMAGE_LOAD_RATIO = 2/3.0;


    private Runnable onSceneFadedOutAction;

    private StackPane rootStackPane;
    private Stage parentStage;
    BaseScene previousScene;
    ImageView backgroundView;
    ImageView maskView;
    private ImageButton backButton;

    BaseScene(StackPane stackPane, Stage parentStage){
        super(stackPane, Main.GENERAL_SETTINGS.getWindowWidth(), Main.GENERAL_SETTINGS.getWindowHeight());
        this.rootStackPane = stackPane;
        setParentStage(parentStage);
        getStylesheets().add("res/flatterfx.css");
        if(backgroundMaskImage == null || backgroundMaskImage.getWidth()!=Main.GENERAL_SETTINGS.getWindowWidth() || backgroundMaskImage.getHeight() != Main.GENERAL_SETTINGS.getWindowHeight()){
            backgroundMaskImage = new Image("res/ui/backgroundMask.png"
                    , Main.GENERAL_SETTINGS.getWindowWidth()
                    , Main.GENERAL_SETTINGS.getWindowHeight()
                    ,false
                    ,true);
        }
        backgroundView = new ImageView();
        maskView = new ImageView(backgroundMaskImage);


        widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                maskView.setFitWidth(newValue.doubleValue());
                backgroundView.setFitWidth(newValue.doubleValue());
            }
        });
        heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                maskView.setFitHeight(newValue.doubleValue());
                backgroundView.setFitHeight(newValue.doubleValue());
            }
        });

        rootStackPane.getChildren().add(backgroundView);
        rootStackPane.getChildren().add(maskView);
        initAndAddWrappingPaneToRoot();

        widthProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneWidth, Number newSceneWidth) {
                //ui.Main.LOGGER.debug("New window's width : "+ newSceneWidth);
                Main.GENERAL_SETTINGS.setSettingValue(PredefinedSetting.WINDOW_WIDTH, newSceneWidth.intValue());
            }
        });
        heightProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneHeight, Number newSceneHeight) {
                //ui.Main.LOGGER.debug("New window's height : "+ newSceneHeight);
                Main.GENERAL_SETTINGS.setSettingValue(PredefinedSetting.WINDOW_HEIGHT, newSceneHeight.intValue());
            }
        });

    }
    public StackPane getRootStackPane(){
        return rootStackPane;
    }

    public void fadeTransitionTo(BaseScene scene2, Stage stage) {
        fadeTransitionTo(scene2,stage,false);
    }
    void fadeTransitionTo(BaseScene scene2, Stage stage, boolean backgroundViewToo){
        if(scene2 instanceof MainScene){

            ((MainScene)scene2).setChangeBackgroundNextTime(true);
        }
        Timeline fadeOutTimeline = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(getWrappingPane().opacityProperty(), getWrappingPane().opacityProperty().getValue(), Interpolator.LINEAR),
                        new KeyValue(backgroundView.opacityProperty(), backgroundView.opacityProperty().getValue(), Interpolator.LINEAR)),
                new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                        new KeyValue(getWrappingPane().opacityProperty(), 0, Interpolator.LINEAR),
                        new KeyValue(backgroundView.opacityProperty(), backgroundViewToo ? 0 : BACKGROUND_IMAGE_MAX_OPACITY, Interpolator.LINEAR)
                ));
        fadeOutTimeline.setCycleCount(1);
        fadeOutTimeline.setAutoReverse(false);
        fadeOutTimeline.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if(onSceneFadedOutAction !=null){
                    onSceneFadedOutAction.run();
                }
                scene2.getWrappingPane().setOpacity(0);
                stage.setScene(scene2);
                stage.setFullScreen(GENERAL_SETTINGS.getBoolean(PredefinedSetting.FULL_SCREEN));
                Timeline fadeInTimeline = new Timeline(
                        new KeyFrame(Duration.seconds(0),
                                new KeyValue(scene2.getWrappingPane().opacityProperty(), 0, Interpolator.LINEAR),
                                new KeyValue(scene2.getBackgroundView().opacityProperty(), backgroundViewToo ? 0 : scene2.getBackgroundView().opacityProperty().getValue(), Interpolator.LINEAR)),
                        new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                                new KeyValue(scene2.getWrappingPane().opacityProperty(), 1, Interpolator.LINEAR),
                                new KeyValue(scene2.getBackgroundView().opacityProperty(), backgroundViewToo ? BACKGROUND_IMAGE_MAX_OPACITY : BACKGROUND_IMAGE_MAX_OPACITY, Interpolator.LINEAR)
                        ));
                stage.getScene().getRoot().setMouseTransparent(false);
                fadeInTimeline.setCycleCount(1);
                fadeInTimeline.setAutoReverse(false);
                fadeInTimeline.play();
            }
        });
        fadeOutTimeline.play();
    }

    /**
     *
     * @return wrapping pane, which is just under the root pane
     */
    protected abstract Pane getWrappingPane();

    abstract void initAndAddWrappingPaneToRoot();

    public Stage getParentStage() {
        return parentStage;
    }

    public void setParentStage(Stage parentStage) {
        this.parentStage = parentStage;
    }

    void setOnSceneFadedOutAction(Runnable onSceneFadedOutAction) {
        this.onSceneFadedOutAction = onSceneFadedOutAction;
    }

    private ImageButton createBackButton(EventHandler<ActionEvent> eventHandler){
        Image leftArrowImage = new Image("res/ui/arrowLeft.png", SCREEN_WIDTH /45, SCREEN_WIDTH /45,true,true);
        ImageButton backButton = new ImageButton(leftArrowImage);
        backButton.setOnAction(event -> eventHandler.handle(event));
        backButton.setId("backButton");
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            switch (event.getCode()){
                case ESCAPE:
                    backButton.fireEvent(new ActionEvent());
                    break;
                default:
                    break;
            }
        });
        return backButton;
    }
    private static Label createTitleLabel(String title){
        Label titleLabel = new Label(title);
        titleLabel.setScaleX(2.5);
        titleLabel.setScaleY(2.5);
        titleLabel.setId("titleLabel");
        return titleLabel;
    }
    StackPane createTop(EventHandler<ActionEvent> backButtonEventHandler, String title){
        StackPane topPane = new StackPane();
        backButton = createBackButton(backButtonEventHandler);
        Label titleLabel = createTitleLabel(title);

        topPane.getChildren().addAll(backButton, titleLabel);
        StackPane.setAlignment(backButton, Pos.TOP_LEFT);
        StackPane.setAlignment(titleLabel, Pos.TOP_CENTER);
        StackPane.setMargin(titleLabel, new Insets(55 * Main.SCREEN_HEIGHT / 1080
                , 12 * Main.SCREEN_WIDTH / 1920
                , 15 * Main.SCREEN_HEIGHT / 1080
                , 15 * Main.SCREEN_WIDTH / 1920));
        return topPane;
    }
    StackPane createTop(String title){
        return createTop(event -> {
            fadeTransitionTo(previousScene,parentStage);
        },title);
    }
    void disableBackButton(){
        backButton.setDisable(true);
        backButton.setVisible(false);
    }

    private ImageView getBackgroundView() {
        return backgroundView;
    }
}

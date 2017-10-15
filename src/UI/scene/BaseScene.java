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
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import system.application.settings.PredefinedSetting;
import ui.Main;
import ui.control.button.ImageButton;
import ui.theme.ThemeUtils;

import static system.application.settings.GeneralSettings.settings;
import static ui.Main.SCREEN_WIDTH;

/**
 * Created by LM on 03/07/2016.
 */
public abstract class BaseScene extends Scene {
    public final static double FADE_IN_OUT_TIME = 0.1;
    public final static double BACKGROUND_IMAGE_MAX_OPACITY = 0.65;


    private Runnable onSceneFadedOutAction;

    private StackPane rootStackPane;
    private Stage parentStage;
    BaseScene previousScene;
    ImageView backgroundView;
    ImageView maskView;
    private ImageButton backButton;

    BaseScene(StackPane stackPane, Stage parentStage, double sceneWidth, double sceneHeight){
        super(stackPane, sceneWidth, sceneHeight);
        this.rootStackPane = stackPane;
        rootStackPane.getStyleClass().add("base-scene-root");
        setParentStage(parentStage);
        ThemeUtils.applyCurrentTheme(this);
        getRoot().setStyle("-fx-font-size: " + Double.toString(settings().getUIScale().getFontSize()) + "px;");

        backgroundView = new ImageView();
        maskView = new ImageView();
        maskView.setId("background-mask");

        //resizeBackgrounds();
        backgroundView.fitWidthProperty().bind(widthProperty());
        backgroundView.fitHeightProperty().bind(heightProperty());
        maskView.fitWidthProperty().bind(widthProperty());
        maskView.fitHeightProperty().bind(heightProperty());

        /*getParentStage().widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                resizeBackgrounds();
            }
        });
        getParentStage().heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                resizeBackgrounds();
            }
        });*/

        rootStackPane.getChildren().add(backgroundView);
        rootStackPane.getChildren().add(maskView);
        initAndAddWrappingPaneToRoot();

        getWrappingPane().maxHeightProperty().bind(heightProperty());
        getWrappingPane().prefHeightProperty().bind(heightProperty());
        getWrappingPane().maxWidthProperty().bind(widthProperty());
        getWrappingPane().prefWidthProperty().bind(widthProperty());
    }
    BaseScene(StackPane stackPane, Stage parentStage) {
        this(stackPane,parentStage,parentStage.getScene().getWidth(),parentStage.getScene().getHeight());
    }

    public StackPane getRootStackPane() {
        return rootStackPane;
    }

    public void fadeTransitionTo(BaseScene scene2, Stage stage) {
        fadeTransitionTo(scene2, stage, false);
    }

    public void fadeTransitionTo(BaseScene scene2, Stage stage, boolean backgroundViewToo) {
        if (scene2 instanceof MainScene) {

            ((MainScene) scene2).setChangeBackgroundNextTime(true);
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
        fadeOutTimeline.setOnFinished(event -> {
            if (onSceneFadedOutAction != null) {
                onSceneFadedOutAction.run();
            }
            scene2.getWrappingPane().setOpacity(0);
            stage.setScene(scene2);
            stage.setFullScreen(settings().getBoolean(PredefinedSetting.FULL_SCREEN));
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
        });
        fadeOutTimeline.play();
    }

    /**
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

    private ImageButton createBackButton(EventHandler<ActionEvent> eventHandler) {
        //Image leftArrowImage = new Image("res/ui/arrowLeft.png", SCREEN_WIDTH /45, SCREEN_WIDTH /45,true,true);
        ImageButton backButton = new ImageButton("arrow-left-button", SCREEN_WIDTH / 45, SCREEN_WIDTH / 45);
        if (eventHandler != null) {
            backButton.setOnAction(event -> eventHandler.handle(event));
        }
        backButton.setId("backButton");
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            switch (event.getCode()) {
                case ESCAPE:
                    event.consume();
                    backButton.fireEvent(new ActionEvent());
                    break;
                default:
                    break;
            }
        });
        return backButton;
    }

    private static Label createTitleLabel(String title) {
        Label titleLabel = new Label(title);
        titleLabel.setId("titleLabel");
        return titleLabel;
    }

    StackPane createTop(EventHandler<ActionEvent> backButtonEventHandler, String title, String cssIconStyle) {
        StackPane topPane = new StackPane();
        topPane.getStyleClass().add("header");
        backButton = createBackButton(backButtonEventHandler);

        Label titleLabel = createTitleLabel(title);
        titleLabel.getStyleClass().add("title-label");

        Node titleNode = null;
        if (cssIconStyle != null) {
            ImageView iconView = new ImageView();
            double width = 42 * Main.SCREEN_WIDTH / 1920 * settings().getUIScale().getScale();
            double height = 42 * Main.SCREEN_HEIGHT / 1080 * settings().getUIScale().getScale();
            iconView.setSmooth(false);
            iconView.setPreserveRatio(true);
            iconView.setFitWidth(width);
            iconView.setFitHeight(height);
            iconView.setStyle(cssIconStyle);

            HBox box = new HBox();
            box.setSpacing(15 * Main.SCREEN_WIDTH / 1920);
            box.getChildren().addAll(iconView, titleLabel);
            box.setAlignment(Pos.CENTER);
            box.getStyleClass().add("title-box");
            box.setPickOnBounds(false);

            titleNode = box;
        } else {
            titleNode = titleLabel;
        }

        topPane.getChildren().addAll(backButton, titleNode);
        StackPane.setAlignment(titleNode, Pos.CENTER);
        StackPane.setMargin(titleNode, new Insets(30 * Main.SCREEN_HEIGHT / 1080
                , 12 * Main.SCREEN_WIDTH / 1920
                , 15 * Main.SCREEN_HEIGHT / 1080
                , 15 * Main.SCREEN_WIDTH / 1920));

        StackPane.setAlignment(backButton, Pos.CENTER_LEFT);
        return topPane;
    }

    StackPane createTop(EventHandler<ActionEvent> backButtonEventHandler, String title) {
        return createTop(backButtonEventHandler,title,null);
    }

    StackPane createTop(String title,String cssIconStyle) {
        return createTop(event -> {
            fadeTransitionTo(previousScene, parentStage);
        }, title,cssIconStyle);
    }

    void disableBackButton() {
        backButton.setDisable(true);
        backButton.setVisible(false);
    }

    public ImageView getBackgroundView() {
        return backgroundView;
    }
}

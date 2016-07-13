package UI;

/**
 * Created by LM on 14/07/2016.
 */
import javafx.application.Application;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class SSCCE extends Application {
    private Stage primaryStage;

    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("SSCCE");

    }

    public SSCCE() {
        FileChooser fileChooser = new FileChooser();

        FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter(
                "XML Files (*.xml)", "*.xml");
        fileChooser.getExtensionFilters().add(extensionFilter);

        fileChooser.showOpenDialog(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
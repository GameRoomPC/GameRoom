package ui.control.textfield;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextArea;
import ui.Main;

/**
 * Created by LM on 06/09/2016.
 */
public class CMDTextField extends TextArea {
    private final static int DEFAULT_PREFERRED_ROW_MAX = 5;
    private int maxRow;

    private CMDTextField(String initialValue, int maxRow){
        super(initialValue);
        setPrefRowCount(1);
        setPromptText(Main.getString("warning_cmd"));
        this.maxRow = maxRow;
        textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                setPrefRowCount(getRowCount(newValue));
            }
        });
        setPrefRowCount(getRowCount(initialValue));
    }
    private int getRowCount(String text){
        if(text!=null) {
            int lineCount = 1;
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) == '\n') {
                    lineCount++;
                }
            }
            if (lineCount > maxRow) {
                lineCount = maxRow;
            }
            return lineCount;
        }
        return 1;
    }
    public CMDTextField(String initialValue){
        this(initialValue,DEFAULT_PREFERRED_ROW_MAX);
    }
}

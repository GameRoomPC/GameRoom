package ui.control.textfield;

import data.game.entry.GameEntry;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import ui.Main;

import java.util.regex.Pattern;

/**
 * Created by LM on 17/07/2016.
 */
public class PlayTimeField extends HBox{
    private TextField hoursField;
    private TimeTextField minutesField;
    private TimeTextField secondsField;
    private GameEntry entry;

    public PlayTimeField(GameEntry entry){
        super();
        this.entry = entry;
        setSpacing(15* Main.SCREEN_WIDTH/1920);
        setAlignment(Pos.CENTER_LEFT);

        String time = entry.getPlayTimeFormatted(GameEntry.TIME_FORMAT_FULL_DOUBLEDOTS);

        Pattern timePattern = Pattern.compile("\\d*:\\d*:\\d*");
        if (!timePattern.matcher(time).matches()) {
            throw new IllegalArgumentException("Invalid time: " + time);
        }
        String hours="",mins="",secs="";
        int mode = 0; //0 for hours, 1 for minutes, 2 for seconds

        for(int i = 0; i<time.length();i++){
            if(time.charAt(i)!= ':'){
                if(mode == 0){
                    hours+=time.charAt(i);
                }else if(mode == 1){
                    mins+=time.charAt(i);

                }else if(mode == 2){
                    secs+=time.charAt(i);
                }
            }else{
                mode++;
            }
        }


        hoursField = new TextField(hours);
        hoursField.setPrefColumnCount(4);
        hoursField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if(!newValue.equals("")) {
                    try {
                        int time = Integer.parseInt(newValue);
                        if (time < 0) {
                            throw new NumberFormatException();
                        }
                        updateEntryPlayTime();
                    } catch (NumberFormatException nfe) {
                        hoursField.setText(oldValue);
                    }
                }else{
                    hoursField.setText("0");
                }
            }
        });
        minutesField = new TimeTextField(mins);
        minutesField.setPrefColumnCount(2);
        minutesField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                updateEntryPlayTime();
            }
        });
        secondsField = new TimeTextField(secs);
        secondsField.setPrefColumnCount(2);
        secondsField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                updateEntryPlayTime();
            }
        });
        getChildren().addAll(hoursField,new Label(":"),minutesField,new Label(":"),secondsField);
        styleProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                hoursField.setStyle(newValue);
                minutesField.setStyle(newValue);
                secondsField.setStyle(newValue);

            }
        });

    }
    private void updateEntryPlayTime(){
        int hours = Integer.parseInt(hoursField.getText());
        long minutes = minutesField.getTime();
        long seconds = secondsField.getTime();

        entry.setPlayTimeSeconds(hours*3600+minutes*60+seconds);
    }
}

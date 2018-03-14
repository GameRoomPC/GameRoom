package com.gameroom.ui.pane;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import com.gameroom.ui.Main;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by LM on 09/08/2016.
 */
public abstract class SelectListPane<T> extends ScrollPane {
    private final ToggleGroup toggleGroup = new ToggleGroup();
    private VBox resultsPane = new VBox();
    private int selected= -1;
    private ArrayList<ListItem<T>> items = new ArrayList<>();
    private ArrayList<T> selectedValues = new ArrayList<T>();
    private int itemCount = 0;

    private boolean multiSelection = false;


    public SelectListPane(){
        this(false);
    }
    public SelectListPane(boolean multiSelection){
        super();
        setFitToWidth(true);
        this.multiSelection = multiSelection;
        resultsPane.setFillWidth(true);
        resultsPane.setSpacing(10 * Main.SCREEN_HEIGHT / 1080);
        resultsPane.setPadding(new Insets(5 * Main.SCREEN_HEIGHT / 1080));
        //resultsPane.setPrefHeight(prefHeight);
        resultsPane.getStyleClass().add("vbox");

        setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        setContent(resultsPane);
    }

    public void clearItems(){
        resultsPane.getChildren().clear();
        selectedValues.clear();
        items.clear();
        selected=-1;
        itemCount = 0;
    }
    public ArrayList<T> getSelectedValues(){
        return (multiSelection) ? selectedValues : null;
    }

    public ArrayList<T> getUnselectedValues(){
        if(!multiSelection){
            return null;
        }
        ArrayList<T> unselectedValues = new ArrayList<>();
        items.forEach(tListItem -> {
            unselectedValues.add(tListItem.getValue());
        });
        unselectedValues.removeAll(selectedValues);
        return unselectedValues;
    }
    public T  getSelectedValue(){
        return (selected != -1 && !multiSelection) ? items.get(selected).getValue() : null;
    }
    private void setSelectedId(int id){
        selected = id;
    }
    private void addSelectedValue(T value){
        if(!selectedValues.contains(value)){
            selectedValues.add(value);
        }
    }
    private void removeSelectedValue(T value){
        selectedValues.remove(value);
    }

    public void selectAllValues(boolean selected){
        if(!multiSelection){
            throw new IllegalStateException("This SelectListPane is not using multiSelection.");
        }

        for(ListItem item : items){
            item.setSelected(selected);
        }

    }

    public void addItem(T value) {
        ListItem item = createListItem(value);
        item.setItemId(itemCount++);
        if (!multiSelection) {
            item.radioButton.setToggleGroup(toggleGroup);
        }
        resultsPane.getChildren().add(item);
        items.add(item);

        item.widthProperty().addListener((observable, oldValue, newValue) -> {
            double prefWitdh = getPrefWidth();
            if(prefWitdh < newValue.doubleValue()){
                setPrefWidth(newValue.doubleValue());
            }
        });
    }
    public void addSeparator(String text){
        Label textLabel = new Label(text);
        textLabel.setPadding(new Insets(20 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920));
        textLabel.setAlignment(Pos.CENTER);
        resultsPane.getChildren().add(new Label(text));
    }

    //override if necessary
    public void onItemSelected(ListItem item) {

    }

    public void addItems(Object[] values){
        for(Object value : values){
            addItem((T) value);
        }
    }
    public void addItems(Iterable<Object> values){
        for(Object value : values){
            addItem((T) value);
        }
    }
    public void addItems(Iterator<Object> values){
        while (values.hasNext()){
            addItem((T) values.next());
        }
    }

    public ArrayList<ListItem<T>> getListItems() {
        return items;
    }

    protected abstract ListItem<T> createListItem(T value);

    public static abstract class ListItem<T> extends GridPane{
        private boolean selected=false;
        private T value;
        RadioButton radioButton;
        private int id;
        protected int columnCount = 0;
        private boolean multiSelection = false;
        private SelectListPane parentList;

        public ListItem(T value,SelectListPane parentList){
            this.value = value;
            this.parentList = parentList;
            this.multiSelection = parentList.multiSelection;
            getStyleClass().addAll("search-result-row");
            setWidth(Double.MAX_VALUE);
            radioButton = new RadioButton();
            radioButton.selectedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    if(!oldValue && newValue){
                        if(multiSelection){
                            parentList.addSelectedValue(getValue());
                        }
                    }else if(oldValue && !newValue){

                    }
                }
            });
            add(radioButton, columnCount++, 0);
            GridPane.setMargin(radioButton, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 10 * Main.SCREEN_WIDTH / 1920));

            setAlignment(Pos.CENTER_LEFT);
            setFocusTraversable(true);

            setOnMouseClicked(me -> {
                setSelected(!isSelected());
            });
            radioButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if(newValue){
                    setId("selected-item");
                }else{
                    setId("unselected-item");
                    getStylesheets().clear();
                }
                if(!multiSelection && oldValue && !newValue){
                    setId("unselected-item");
                    getStylesheets().clear();
                    parentList.removeSelectedValue(getValue());
                }
                if(newValue && !selected){
                    setSelected(true);
                }else if(!newValue && selected){
                    setSelected(false);
                }
            });
        }
        protected abstract void addContent();

        public T getValue(){
            return value;
        }

        private int getItemId() {
            return id;
        }

        private void setItemId(int id) {
            this.id = id;
        }

        public void setSelected(boolean selected){
            boolean oldSelection = this.selected;
            this.selected= selected;
            if(!oldSelection && selected){
                radioButton.setSelected(true);
                parentList.setSelectedId(getItemId());
                parentList.onItemSelected(this);
            }else if (oldSelection && !selected){
                if(multiSelection){
                    parentList.removeSelectedValue(getValue());
                    radioButton.setSelected(false);
                }
            }
        }

        public boolean isSelected() {
            return selected;
        }

    }
}

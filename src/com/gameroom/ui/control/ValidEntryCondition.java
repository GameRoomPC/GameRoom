package com.gameroom.ui.control;

/**
 * Created by LM on 10/08/2016.
 */
public interface ValidEntryCondition{
    StringBuilder message = new StringBuilder();

    boolean isValid();
    void onInvalid();

}

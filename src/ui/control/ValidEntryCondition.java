package ui.control;

/**
 * Created by LM on 10/08/2016.
 */
public interface ValidEntryCondition{
    StringBuilder message = new StringBuilder();

    public boolean isValid();
    public void onInvalid();

}

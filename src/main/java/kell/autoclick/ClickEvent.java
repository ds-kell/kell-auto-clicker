package kell.autoclick;


import java.awt.*;

public interface ClickEvent {

    void stopClick();

    void resetData();

    void deleteSelected();

    void startClick();

    void infiniteAutoClick();

    void limitedAutoClick();

    void performClick(ClickPoint model) throws AWTException;

    void globalKeyListener();

    void showTransparentFrame();
}

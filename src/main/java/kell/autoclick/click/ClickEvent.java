package kell.autoclick.click;


import kell.autoclick.model.ClickItem;

import java.awt.*;

public interface ClickEvent {

    void stopClick();

    void resetData();

    void deleteSelected();

    void startClick();

    void infiniteAutoClick();

    void limitedAutoClick();

    void performClick(ClickItem clickItem) throws AWTException;

    void globalKeyListener();

    void showAddPointFrame();
}

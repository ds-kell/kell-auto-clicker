package kell.autoclick.click;


import kell.autoclick.model.ClickItem;

import java.awt.*;
import java.util.concurrent.CountDownLatch;

public interface ClickEvent {

    void stopClick();

    void resetData();

    void deleteSelected();

    void startClick();

    void infiniteAutoClick();

    void limitedAutoClick();

    void performClick(ClickItem clickItem) throws AWTException;

    void performPress() throws AWTException, InterruptedException;

    void performInput(String input, CountDownLatch latch);

    void globalKeyListener();

    void showAddPointFrame();

    void showAddPressKeyFrame();
}

package kell.autoclick.frame;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

@Getter
@Setter

public class SubFrame {
    private final JFrame subFrame = new JFrame("Notification");

    private final int SUB_HEIGHT = 100;

    private final int SUB_WIDTH = 200;

    public SubFrame(){
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        int screenWidth = (int) screenSize.getWidth();
        int screenHeight = (int) screenSize.getHeight();

        subFrame.setSize(SUB_WIDTH, SUB_HEIGHT);
        subFrame.setLocation((screenWidth - SUB_WIDTH) / 2, (screenHeight - SUB_WIDTH) / 2);

        subFrame.setUndecorated(true);
    }

    public void initSubFrame() {
    }
}

package kell.autoclick.frame;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

@Getter
@Setter
public class MainFrame {
    private final JFrame mainFrame = new JFrame("Auto Clicker");

    private final int WIDTH = 500;

    private final int HEIGHT = 400;

    private boolean isHideFrame = true;

    public MainFrame(){
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        int screenWidth = (int) screenSize.getWidth();
        int screenHeight = (int) screenSize.getHeight();

        mainFrame.setSize(WIDTH, HEIGHT);
        mainFrame.setLocation((screenWidth - WIDTH) / 2, (screenHeight - HEIGHT) / 2);
    }

    public void initMainFrame() {
    }
}

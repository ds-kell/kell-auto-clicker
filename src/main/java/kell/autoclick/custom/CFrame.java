package kell.autoclick.custom;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

@Getter
@Setter
public abstract class CFrame {

    protected final JFrame frame = new JFrame();

    protected CFrame(int WIDTH, int HEIGHT) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        int screenWidth = (int) screenSize.getWidth();
        int screenHeight = (int) screenSize.getHeight();

        frame.setSize(WIDTH, HEIGHT);
        frame.setLocation((screenWidth - WIDTH) / 2, (screenHeight - HEIGHT) / 2);
        frame.getRootPane().setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.DARK_GRAY));

    }

    protected void addItemToPanel(JPanel panel, JComponent... components) {
        for (JComponent component : components) {
            panel.add(component);
        }
    }
}

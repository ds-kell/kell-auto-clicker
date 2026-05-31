package kell.autoclick.custom;

import javax.swing.*;
import java.awt.*;

public class CFrame {

    protected final JFrame frame = new JFrame();

    private static final Font UI_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    protected CFrame(int width, int height) {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize(width, height);
        frame.setLocation((screen.width - width) / 2, (screen.height - height) / 2);
        frame.getRootPane().setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.DARK_GRAY));
    }

    public JFrame getFrame() { return frame; }

    /** Apply consistent font + right-align to a text field. */
    public static void styleInput(JTextField f) {
        f.setFont(UI_FONT);
        f.setHorizontalAlignment(JTextField.RIGHT);
        Dimension d = f.getPreferredSize();
        f.setPreferredSize(new Dimension(d.width, 28));
    }

    /** Apply consistent font to a label and remove focus border. */
    public static void styleLabel(JLabel l) {
        l.setFont(UI_FONT);
        l.setFocusable(false);
    }

    /** Apply consistent font to a button. */
    public static void styleButton(AbstractButton b) {
        b.setFont(UI_FONT);
    }

    protected void addComponentToPanel(JPanel panel, JComponent... components) {
        for (JComponent c : components) panel.add(c);
    }
}

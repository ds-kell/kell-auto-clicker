package kell.autoclick.frame;

import kell.autoclick.custom.CFrame;

import javax.swing.*;
import java.awt.*;

/**
 * Full-screen transparent overlay for picking a click point.
 * User selects Left or Right via toggle buttons, then clicks anywhere on screen.
 */
public class AddPointFrame extends CFrame {

    private static final Font  FONT_HINT = new Font("Segoe UI", Font.PLAIN,  13);
    private static final Font  FONT_BTN  = new Font("Segoe UI", Font.BOLD,   14);

    // Active = highlighted, inactive = gray
    private static final Color COLOR_LEFT_ON   = new Color(0x1E, 0x88, 0xE5); // blue
    private static final Color COLOR_RIGHT_ON  = new Color(0xE5, 0x39, 0x35); // red
    private static final Color COLOR_OFF       = new Color(0x55, 0x55, 0x55); // dark gray
    private static final Color COLOR_OFF_TEXT  = new Color(0xAA, 0xAA, 0xAA);
    private static final Color COLOR_ON_TEXT   = Color.WHITE;

    private final JButton leftBtn  = new JButton("  Left Click  ");
    private final JButton rightBtn = new JButton("  Right Click  ");
    private final JLabel  hint     = new JLabel("Click anywhere on screen to set the point");

    private boolean isRight = false; // default: left

    /** The top panel — clicks here must NOT register as point picks. */
    public final JPanel topPanel;

    public AddPointFrame() {
        super(100, 100);
        frame.setUndecorated(true);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setBackground(new Color(0, 0, 0, 50));

        topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 10));
        topPanel.setBackground(new Color(25, 25, 25, 230));
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(60, 60, 60)));

        hint.setFont(FONT_HINT);
        hint.setForeground(new Color(180, 180, 180));
        hint.setFocusable(false);

        styleToggleBtn(leftBtn);
        styleToggleBtn(rightBtn);

        leftBtn.addActionListener(e -> select(false));
        rightBtn.addActionListener(e -> select(true));

        topPanel.add(hint);
        topPanel.add(leftBtn);
        topPanel.add(rightBtn);

        frame.setLayout(new BorderLayout());
        frame.add(topPanel, BorderLayout.NORTH);

        // Apply initial selection state
        select(false);
    }

    private void styleToggleBtn(JButton btn) {
        btn.setFont(FONT_BTN);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Dimension d = btn.getPreferredSize();
        btn.setPreferredSize(new Dimension(Math.max(d.width, 140), 36));
    }

    private void select(boolean right) {
        isRight = right;

        // Left button
        leftBtn.setBackground(right ? COLOR_OFF : COLOR_LEFT_ON);
        leftBtn.setForeground(right ? COLOR_OFF_TEXT : COLOR_ON_TEXT);

        // Right button
        rightBtn.setBackground(right ? COLOR_RIGHT_ON : COLOR_OFF);
        rightBtn.setForeground(right ? COLOR_ON_TEXT : COLOR_OFF_TEXT);

        topPanel.revalidate();
        topPanel.repaint();
    }

    /** Reset to default (left) each time the overlay is shown. */
    public void reset() {
        select(false);
    }

    /** Returns "Left" or "Right". */
    public String getSelectedButton() {
        return isRight ? "Right" : "Left";
    }
}

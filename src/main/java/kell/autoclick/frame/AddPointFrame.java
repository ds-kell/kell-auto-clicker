package kell.autoclick.frame;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import kell.autoclick.custom.CFrame;

/**
 * Full-screen transparent overlay for picking a click point.
 * The user simply left-clicks or right-clicks anywhere on the overlay; the
 * mouse button used is captured automatically by the caller (AutoClicker).
 */
public class AddPointFrame extends CFrame {

    private static final Font FONT_HINT = new Font("Segoe UI", Font.BOLD, 13);

    private final JLabel hint = new JLabel(
            "Left-click or right-click anywhere to set the point  •  Esc to cancel");

    /** The hint panel — clicks here must NOT register as point picks. */
    public final JPanel topPanel;

    public AddPointFrame() {
        super(100, 100);
        frame.setUndecorated(true);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setBackground(new Color(0, 0, 0, 50));

        topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 10));
        topPanel.setBackground(new Color(25, 25, 25, 230));
        topPanel.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, new Color(60, 60, 60)));

        hint.setFont(FONT_HINT);
        hint.setForeground(new Color(220, 220, 220));
        hint.setFocusable(false);
        topPanel.add(hint);

        // Center the hint bar in the middle of the screen via a GridBagLayout
        // wrapper. Same look as before, just relocated.
        var wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.add(topPanel, new GridBagConstraints());

        frame.setLayout(new BorderLayout());
        frame.add(wrapper, BorderLayout.CENTER);

        // Esc cancels the pick.
        var root = frame.getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "cancel-pick");
        root.getActionMap().put("cancel-pick", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                frame.setVisible(false);
            }
        });
    }

    /**
     * Screen-relative bounds of the hint bar in frame coordinates. Caller
     * compares the click point against these bounds to ignore clicks that
     * fall on the hint.
     */
    public Rectangle hintBounds() {
        if (!topPanel.isShowing()) return new Rectangle(0, 0, 0, 0);
        var loc = topPanel.getLocationOnScreen();
        var fl  = frame.getLocationOnScreen();
        var sz  = topPanel.getSize();
        return new Rectangle(loc.x - fl.x, loc.y - fl.y, sz.width, sz.height);
    }

    /** Reset hook kept for API compatibility; nothing to reset anymore. */
    public void reset() { /* no-op */ }
}

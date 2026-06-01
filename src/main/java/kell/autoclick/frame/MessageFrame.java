package kell.autoclick.frame;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import kell.autoclick.custom.CButton;

/**
 * Message popup. Backed by a non-modal JDialog owned by the main frame so
 * it always renders on top of the owner — fixes the bug where on macOS the
 * popup would appear behind the main window when both were JFrames.
 */
public class MessageFrame {

    private static final Font UI_FONT = new Font("Segoe UI", Font.BOLD, 13);

    private JDialog dialog;
    public  final JLabel  messageLabel = new JLabel("", SwingConstants.CENTER);
    private final JButton confirmBtn   = new CButton("OK");
    private JFrame owner;

    public void initWithOwner(JFrame owner) {
        this.owner = owner;
        // Non-modal so the EDT keeps spinning. Owned dialog stacks above its
        // owner on every platform without focus-stealing tricks.
        dialog = new JDialog(owner, false);
        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

        messageLabel.setFont(UI_FONT);
        messageLabel.setFocusable(false);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(8, 12, 4, 12));

        JPanel root = new JPanel(new BorderLayout(0, 6));
        root.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x99, 0x99, 0x99)),
            BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        root.add(messageLabel, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnRow.add(confirmBtn);
        root.add(btnRow, BorderLayout.SOUTH);

        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(root, BorderLayout.CENTER);
        dialog.setSize(360, 140);
    }

    /**
     * Show the popup with a message. Wraps long messages onto two lines so
     * the text doesn't overflow the dialog.
     */
    public void showMessage(String text) {
        messageLabel.setText(wrapTwoLines(text));
        // Resize to fit (preferredSize from the wrapped HTML label).
        dialog.pack();
        ensureMinSize();
        // Center on the owner so it always appears in front of the user.
        if (owner != null && owner.isShowing()) {
            var owLoc = owner.getLocationOnScreen();
            var owSize = owner.getSize();
            int x = owLoc.x + (owSize.width - dialog.getWidth()) / 2;
            int y = owLoc.y + (owSize.height - dialog.getHeight()) / 3;
            dialog.setLocation(x, y);
        } else {
            dialog.setLocationRelativeTo(null);
        }
        dialog.setVisible(true);
        dialog.toFront();
    }

    public void hideMessage() {
        if (dialog != null) dialog.setVisible(false);
    }

    private void ensureMinSize() {
        var d = dialog.getSize();
        if (d.width < 280) d.width = 280;
        if (d.height < 120) d.height = 120;
        dialog.setSize(d);
    }

    /**
     * If the message is longer than ~50 chars, wrap it onto two lines using
     * HTML so JLabel renders the line break.
     */
    private static String wrapTwoLines(String text) {
        if (text == null || text.isEmpty()) return text;
        int wrapAt = 50;
        if (text.length() <= wrapAt) return text;

        // Find the nearest space around the midpoint to break cleanly.
        int mid = text.length() / 2;
        int left = text.lastIndexOf(' ', mid);
        int right = text.indexOf(' ', mid);
        int breakIdx;
        if (left < 0 && right < 0) {
            breakIdx = mid; // no space — hard break
        } else if (left < 0) {
            breakIdx = right;
        } else if (right < 0) {
            breakIdx = left;
        } else {
            breakIdx = (mid - left) <= (right - mid) ? left : right;
        }
        var first  = text.substring(0, breakIdx).trim();
        var second = text.substring(breakIdx).trim();
        return "<html><div style='text-align:center;'>"
                + escape(first) + "<br>" + escape(second) + "</div></html>";
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public JDialog getDialog()       { return dialog; }
    public JLabel  getMessageLabel() { return messageLabel; }
    public JButton getConfirmBtn()   { return confirmBtn; }

    /** @deprecated kept for AutoClicker compatibility — returns the owning JFrame. */
    @Deprecated
    public JFrame getFrame() { return owner; }
}

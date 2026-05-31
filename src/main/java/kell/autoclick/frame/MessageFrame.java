package kell.autoclick.frame;

import kell.autoclick.custom.CButton;
import kell.autoclick.custom.CFrame;

import javax.swing.*;
import java.awt.*;

/**
 * Simple message popup — uses JFrame (not JDialog) so it never blocks EDT.
 * Main frame is disabled via setEnabled(false) while this is visible.
 */
public class MessageFrame extends CFrame {

    private static final Font UI_FONT = new Font("Segoe UI", Font.BOLD, 13);

    public  final JLabel  messageLabel = new JLabel("", SwingConstants.CENTER);
    private final JButton confirmBtn   = new CButton("OK");

    public MessageFrame() {
        super(300, 120);
        frame.setUndecorated(true);
    }

    public void init() {
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

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(root, BorderLayout.CENTER);
    }

    public JLabel  getMessageLabel() { return messageLabel; }
    public JButton getConfirmBtn()   { return confirmBtn; }
}

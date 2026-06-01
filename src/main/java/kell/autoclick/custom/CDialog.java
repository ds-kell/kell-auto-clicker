package kell.autoclick.custom;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Base class for all popup dialogs.
 * Uses JDialog (non-modal) so closing it never affects the owner window.
 */
public class CDialog {

    protected final JDialog dialog;

    protected static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD,  14);
    protected static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD,  13);
    protected static final Font FONT_INPUT = new Font("Segoe UI", Font.PLAIN, 13);
    protected static final int  INPUT_H    = 28;
    protected static final int  INPUT_W    = 100;

    protected CDialog(JFrame owner, String title, int width, int height) {
        dialog = new JDialog(owner, title, false); // NON-modal — never blocks EDT
        dialog.setSize(width, height);
        dialog.setResizable(false);
        dialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        center(width, height);
    }

    private void center(int w, int h) {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        dialog.setLocation((screen.width - w) / 2, (screen.height - h) / 2);
    }

    public JDialog getDialog() { return dialog; }

    public boolean isVisible() { return dialog.isVisible(); }
    public void setVisible(boolean v) { dialog.setVisible(v); }

    /** Styled input field: bold font, right-aligned, fixed size. */
    protected JTextField input(int cols) {
        JTextField f = new JTextField(cols);
        f.setFont(FONT_INPUT);
        f.setHorizontalAlignment(JTextField.RIGHT);
        f.setPreferredSize(new Dimension(INPUT_W, INPUT_H));
        return f;
    }

    /** Digit-only input. Caller can wire {@link CDigitField#setOnInvalid}. */
    protected CDigitField digitInput(int cols) {
        var f = new CDigitField(cols);
        f.setPreferredSize(new Dimension(INPUT_W, INPUT_H));
        return f;
    }

    /** Free-text input (digits + letters). */
    protected CTextField textInput(int cols) {
        var f = new CTextField(cols);
        f.setPreferredSize(new Dimension(INPUT_W, INPUT_H));
        return f;
    }

    /** Styled label: bold. */
    protected JLabel label(String text) {
        JLabel l = new JLabel(text, JLabel.RIGHT);
        l.setFont(FONT_LABEL);
        l.setFocusable(false);
        return l;
    }

    /** Title label. */
    protected JLabel title(String text) {
        JLabel l = new JLabel(text, JLabel.CENTER);
        l.setFont(FONT_TITLE);
        l.setFocusable(false);
        return l;
    }

    /**
     * Build a 2-column form panel using GridBagLayout so labels and inputs align.
     * Pass pairs: label1, field1, label2, field2, ...
     */
    protected JPanel formPanel(Object... pairs) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.LINE_END;
        lc.insets = new Insets(4, 4, 4, 6);
        lc.gridx = 0;

        GridBagConstraints fc = new GridBagConstraints();
        fc.anchor = GridBagConstraints.LINE_START;
        fc.fill   = GridBagConstraints.HORIZONTAL;
        fc.insets = new Insets(4, 0, 4, 4);
        fc.gridx  = 1;
        fc.weightx = 1.0;

        int row = 0;
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            lc.gridy = row;
            fc.gridy = row;
            p.add((JComponent) pairs[i],     lc);
            p.add((JComponent) pairs[i + 1], fc);
            row++;
        }
        return p;
    }

    /** Centered button row. */
    protected JPanel btnRow(JButton... btns) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        for (JButton b : btns) {
            b.setFont(FONT_LABEL);
            p.add(b);
        }
        return p;
    }
}

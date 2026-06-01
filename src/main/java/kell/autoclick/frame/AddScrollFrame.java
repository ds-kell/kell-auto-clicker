package kell.autoclick.frame;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import kell.autoclick.custom.CButton;
import kell.autoclick.custom.CDialog;
import kell.autoclick.custom.CDigitField;

public class AddScrollFrame extends CDialog {

    public  final CDigitField wheelAmtTxt = digitInput(6);
    public  final CDigitField nextTimeTxt = digitInput(6);
    private final JButton     addBtn      = new CButton("Add");
    private final JButton     cancelBtn   = new CButton("Cancel");

    public AddScrollFrame(JFrame owner) {
        super(owner, "Add Scroll Action", 380, 230);
        nextTimeTxt.setText("500");
        wheelAmtTxt.setAllowNegative(true);

        JPanel root = new JPanel(new BorderLayout(0, 6));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 10, 12));

        root.add(title("Add Scroll Action"), BorderLayout.NORTH);

        // Center: form + a left-aligned italic gray hint right under it.
        var center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(formPanel(
            label("Wheel notches:"),    wheelAmtTxt,
            label("Pause after (ms):"), nextTimeTxt
        ));
        center.add(Box.createVerticalStrut(4));
        center.add(hint(
                "(Wheel notches: positive = scroll down, negative = scroll up. "
              + "Each notch ≈ 3 lines.)"));
        root.add(center, BorderLayout.CENTER);

        root.add(btnRow(cancelBtn, addBtn), BorderLayout.SOUTH);

        dialog.getContentPane().add(root);
    }

    /** Left-aligned, gray, italic helper text. */
    private static JLabel hint(String text) {
        var lbl = new JLabel(
                "<html><div style='width: 320px;'>" + text + "</div></html>",
                SwingConstants.LEFT);
        lbl.setFont(lbl.getFont().deriveFont(Font.ITALIC, 12f));
        lbl.setForeground(new Color(0x88, 0x88, 0x88));
        lbl.setFocusable(false);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        lbl.setAlignmentX(0f);
        return lbl;
    }

    public CDigitField getWheelAmtTxt() { return wheelAmtTxt; }
    public CDigitField getNextTimeTxt() { return nextTimeTxt; }
    public JButton     getAddBtn()      { return addBtn; }
    public JButton     getCancelBtn()   { return cancelBtn; }
}

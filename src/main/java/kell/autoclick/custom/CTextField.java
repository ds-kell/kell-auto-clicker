package kell.autoclick.custom;

import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JTextField;

/**
 * Text field that accepts both digits and free text. Same styling as
 * {@link CDigitField} for visual consistency.
 */
public class CTextField extends JTextField {

    private static final Font UI_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final int  HEIGHT  = 28;

    public CTextField(int columns) {
        super(columns);
        setFont(UI_FONT);
        var pref = getPreferredSize();
        setPreferredSize(new Dimension(pref.width, HEIGHT));
    }
}

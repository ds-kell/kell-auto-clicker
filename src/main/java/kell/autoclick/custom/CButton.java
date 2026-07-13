package kell.autoclick.custom;

import javax.swing.*;
import java.awt.*;
import java.io.Serial;

import static kell.autoclick.contants.GlobalConst.SYSTEM_FONT;

public class CButton extends JButton {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Font UI_FONT   = new Font(SYSTEM_FONT, Font.PLAIN, 13);
    private static final int  BTN_HEIGHT = 28;

    public CButton(String text) {
        super(text);
        setFont(UI_FONT);
        setFocusPainted(false);
        Dimension pref = getPreferredSize();
        setPreferredSize(new Dimension(pref.width + 10, BTN_HEIGHT));
    }

    public CButton(String text, int width, int height) {
        super(text);
        setFont(UI_FONT);
        setFocusPainted(false);
        setPreferredSize(new Dimension(width, height));
    }
}

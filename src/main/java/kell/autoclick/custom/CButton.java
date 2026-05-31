package kell.autoclick.custom;

import javax.swing.*;
import java.awt.*;

public class CButton extends JButton {

    private static final Font UI_FONT   = new Font("Segoe UI", Font.PLAIN, 13);
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

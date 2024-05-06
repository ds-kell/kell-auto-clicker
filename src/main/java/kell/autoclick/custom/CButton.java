package kell.autoclick.custom;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class CButton extends JButton {
    public CButton(String name) {
        super(name);
        setFocusPainted(false);
        Color oldBg = getBackground();
        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
//                setBackground(Color.GREEN);
            }

            @Override
            public void focusLost(FocusEvent e) {
//                setBackground(oldBg);
            }
        });
    }
}

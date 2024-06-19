package kell.autoclick.custom;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class CButton extends JButton {
    public CButton(String name) {
        super(name);
        setFocusPainted(false);

        Dimension size = new Dimension(getPreferredSize().width, 25);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);

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

    public CButton(String name, int width, int height) {
        super(name);
        setFocusPainted(false);

        Dimension size = new Dimension(width, height);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);

        Color oldBg = getBackground();
        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
            }
        });
    }

    public CButton(String name, int width) {
        super(name);
        setFocusPainted(false);

        Dimension size = new Dimension(width, 25);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);

        Color oldBg = getBackground();
        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
            }
        });
    }
}

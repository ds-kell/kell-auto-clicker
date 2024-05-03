package kell.autoclick.frame;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

@Getter
@Setter
public class AddPointFrame {
    private final JFrame addPointFrame = new JFrame("Add Point Frame");

    public AddPointFrame(){
        addPointFrame.setUndecorated(true);
        addPointFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        addPointFrame.setBackground(new Color(100, 100, 100, 100));
    }
}

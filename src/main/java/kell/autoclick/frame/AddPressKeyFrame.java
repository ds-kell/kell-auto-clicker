package kell.autoclick.frame;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

@Getter
@Setter
public class AddPressKeyFrame {
    private final JFrame addPressKeyFrame = new JFrame("Add Press Key Frame");

    public AddPressKeyFrame(){
        addPressKeyFrame.setUndecorated(true);
        addPressKeyFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        addPressKeyFrame.setBackground(new Color(100, 100, 100, 100));
    }
}

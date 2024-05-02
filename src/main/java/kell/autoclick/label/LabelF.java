package kell.autoclick.label;

import lombok.Getter;

import javax.swing.*;
@Getter

public class LabelF {
    public final JLabel repeatTimesLabel = new JLabel("Repeat times: ", JLabel.RIGHT);
    public final JTextField repeatTimesTxt = new JTextField(8);

    public final JTextField messageTxt = new JTextField(8);

    public LabelF(){
        repeatTimesTxt.setText("");
        messageTxt.setText("Fuck you");
    }
}

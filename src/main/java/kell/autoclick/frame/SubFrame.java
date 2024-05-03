package kell.autoclick.frame;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

@Getter
@Setter
public class SubFrame extends CustomFrame{

    private final int SUB_HEIGHT = 100;

    private final int SUB_WIDTH = 200;

    private final  JPanel msgPanel = new JPanel(new BorderLayout());

    public final JTextField messageTxt = new JTextField(8);

    private final JButton confirmBtn = new JButton("OK");

    public SubFrame(){
        super(200, 100);
        frame.setUndecorated(true);
    }

    public void initSubFrame() {
        messageTxt.setEditable(false);
        msgPanel.add(messageTxt, BorderLayout.CENTER);
        msgPanel.add(confirmBtn, BorderLayout.SOUTH);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(msgPanel, BorderLayout.CENTER);
    }
}

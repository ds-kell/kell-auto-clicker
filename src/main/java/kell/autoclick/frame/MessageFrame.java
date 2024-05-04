package kell.autoclick.frame;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

@Getter
@Setter
public class MessageFrame extends CustomFrame{

    public final JTextField messageTxt = new JTextField(8);

    private final JButton confirmBtn = new JButton("OK");

    public MessageFrame(){
        super(200, 100);
        frame.setUndecorated(true);
    }

    public void init() {
        JPanel msgPanel = new JPanel(new BorderLayout());

        msgPanel.add(messageTxt, BorderLayout.CENTER);
        msgPanel.add(confirmBtn, BorderLayout.SOUTH);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(msgPanel, BorderLayout.CENTER);

        messageTxt.setEditable(false);
    }
}

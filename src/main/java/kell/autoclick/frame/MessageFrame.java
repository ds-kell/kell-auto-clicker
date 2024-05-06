package kell.autoclick.frame;

import kell.autoclick.custom.CButton;
import kell.autoclick.custom.CFrame;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

@Getter
@Setter
public class MessageFrame extends CFrame {

    public final JLabel messageLabel = new JLabel();

    private final JButton confirmBtn = new CButton("OK");

    public MessageFrame(){
        super(200, 100);
        frame.setUndecorated(true);
    }

    public void init() {
        messageLabel.setHorizontalAlignment(JLabel.CENTER);
        Font unBoldFont=new Font(messageLabel.getFont().getName(), Font.PLAIN, messageLabel.getFont().getSize());
        messageLabel.setFont(unBoldFont);
        confirmBtn.setSize(80, 20);

        JPanel msgPanel = new JPanel(new BorderLayout());
        msgPanel.add(messageLabel, BorderLayout.CENTER);
        msgPanel.add(confirmBtn, BorderLayout.SOUTH);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(msgPanel, BorderLayout.CENTER);
    }
}

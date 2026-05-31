package kell.autoclick.frame;

import kell.autoclick.custom.CButton;
import kell.autoclick.custom.CDialog;

import javax.swing.*;
import java.awt.*;

public class AddScrollFrame extends CDialog {

    public  final JTextField wheelAmtTxt = input(6);
    public  final JTextField nextTimeTxt = input(6);
    private final JButton    addBtn      = new CButton("Add");
    private final JButton    cancelBtn   = new CButton("Cancel");

    public AddScrollFrame(JFrame owner) {
        super(owner, "Add Scroll Action", 320, 200);
        nextTimeTxt.setText("500");

        JPanel root = new JPanel(new BorderLayout(0, 6));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 10, 12));

        root.add(title("Add Scroll Action"), BorderLayout.NORTH);
        root.add(formPanel(
            label("Scroll amount:"),  wheelAmtTxt,
            label("Delay after (ms):"), nextTimeTxt
        ), BorderLayout.CENTER);
        root.add(btnRow(cancelBtn, addBtn), BorderLayout.SOUTH);

        dialog.getContentPane().add(root);
    }

    public JTextField getWheelAmtTxt() { return wheelAmtTxt; }
    public JTextField getNextTimeTxt() { return nextTimeTxt; }
    public JButton    getAddBtn()      { return addBtn; }
    public JButton    getCancelBtn()   { return cancelBtn; }
}

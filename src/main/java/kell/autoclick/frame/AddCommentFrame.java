package kell.autoclick.frame;

import kell.autoclick.custom.CButton;
import kell.autoclick.custom.CDialog;

import javax.swing.*;
import java.awt.*;

public class AddCommentFrame extends CDialog {

    public  final JTextField commentTxt = new JTextField(20);
    private final JButton    addBtn     = new CButton("Add");
    private final JButton    cancelBtn  = new CButton("Cancel");

    public AddCommentFrame(JFrame owner) {
        super(owner, "Add Note / Comment", 380, 180);

        commentTxt.setFont(FONT_INPUT);
        commentTxt.setPreferredSize(new Dimension(220, INPUT_H));

        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 12, 14));
        root.add(title("Add Note / Comment"), BorderLayout.NORTH);
        root.add(formPanel(label("Note text:"), commentTxt), BorderLayout.CENTER);
        root.add(btnRow(cancelBtn, addBtn), BorderLayout.SOUTH);

        dialog.getContentPane().add(root);
    }

    public JTextField getCommentTxt() { return commentTxt; }
    public JButton    getAddBtn()     { return addBtn; }
    public JButton    getCancelBtn()  { return cancelBtn; }
}

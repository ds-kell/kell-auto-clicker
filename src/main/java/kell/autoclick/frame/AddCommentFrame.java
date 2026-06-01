package kell.autoclick.frame;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import kell.autoclick.custom.CButton;
import kell.autoclick.custom.CDialog;
import kell.autoclick.custom.CTextField;

public class AddCommentFrame extends CDialog {

    public  final CTextField commentTxt = textInput(20);
    private final JButton    addBtn     = new CButton("Add");
    private final JButton    cancelBtn  = new CButton("Cancel");

    public AddCommentFrame(JFrame owner) {
        super(owner, "Add Note / Comment", 380, 180);

        commentTxt.setPreferredSize(new Dimension(220, INPUT_H));

        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 12, 14));
        root.add(title("Add Note / Comment"), BorderLayout.NORTH);
        root.add(formPanel(label("Note text:"), commentTxt), BorderLayout.CENTER);
        root.add(btnRow(cancelBtn, addBtn), BorderLayout.SOUTH);

        dialog.getContentPane().add(root);
    }

    public CTextField getCommentTxt() { return commentTxt; }
    public JButton    getAddBtn()     { return addBtn; }
    public JButton    getCancelBtn()  { return cancelBtn; }
}

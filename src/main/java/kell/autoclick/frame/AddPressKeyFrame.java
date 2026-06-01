package kell.autoclick.frame;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import kell.autoclick.custom.CButton;
import kell.autoclick.custom.CDialog;
import kell.autoclick.custom.CDigitField;
import kell.autoclick.custom.CTextField;

public class AddPressKeyFrame extends CDialog {

    public  final CTextField   keyTxt            = textInput(6);
    public  final CDigitField  pressTimeTxt      = digitInput(6);
    public  final CDigitField  nextTimeTxt       = digitInput(6);
    private final JRadioButton isSinglePressRBtn = new JRadioButton("Single press", true);
    private final JRadioButton isMultiPressRBtn  = new JRadioButton("Hold & repeat", false);
    private final JButton      addBtn            = new CButton("Add");
    private final JButton      cancelBtn         = new CButton("Cancel");
    private boolean isSinglePress = true;

    public AddPressKeyFrame(JFrame owner) {
        super(owner, "Add Key Press Action", 340, 280);
        pressTimeTxt.setText("100");
        nextTimeTxt.setText("500");

        ButtonGroup bg = new ButtonGroup();
        bg.add(isSinglePressRBtn);
        bg.add(isMultiPressRBtn);
        isSinglePressRBtn.setFont(FONT_LABEL);
        isSinglePressRBtn.setFocusable(false);
        isMultiPressRBtn.setFont(FONT_LABEL);
        isMultiPressRBtn.setFocusable(false);

        JPanel radioRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 2));
        radioRow.add(isSinglePressRBtn);
        radioRow.add(isMultiPressRBtn);

        JPanel root = new JPanel(new BorderLayout(0, 6));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 10, 12));
        root.add(title("Add Key Press Action"), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 4));
        center.add(formPanel(
            label("Key:"),                keyTxt,
            label("Hold duration (ms):"), pressTimeTxt,
            label("Pause after (ms):"),   nextTimeTxt
        ), BorderLayout.CENTER);
        center.add(radioRow, BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);
        root.add(btnRow(cancelBtn, addBtn), BorderLayout.SOUTH);

        dialog.getContentPane().add(root);
    }

    public CTextField   getKeyTxt()            { return keyTxt; }
    public CDigitField  getPressTimeTxt()      { return pressTimeTxt; }
    public CDigitField  getNextTimeTxt()       { return nextTimeTxt; }
    public JRadioButton getIsSinglePressRBtn() { return isSinglePressRBtn; }
    public JRadioButton getIsMultiPressRBtn()  { return isMultiPressRBtn; }
    public JButton      getAddBtn()            { return addBtn; }
    public JButton      getCancelBtn()         { return cancelBtn; }
    public boolean      getIsSinglePress()     { return isSinglePress; }
    public void         setIsSinglePress(boolean b) { this.isSinglePress = b; }
}

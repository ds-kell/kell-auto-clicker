package kell.autoclick.frame;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import kell.autoclick.custom.CButton;
import kell.autoclick.custom.CDialog;
import kell.autoclick.custom.CDigitField;

public class AddDADFrame extends CDialog {

    public  final CDigitField nextTimeTxt = digitInput(6);
    private final JLabel      fromLabel   = new JLabel("Start: (not set)");
    private final JLabel      toLabel     = new JLabel("End:   (not set)");
    private final JButton     fromBtn     = new CButton("Pick Start");
    private final JButton     toBtn       = new CButton("Pick End");
    private final JButton     addBtn      = new CButton("Add");
    private final JButton     cancelBtn   = new CButton("Cancel");

    private int fromX = -1;
    private int fromY = -1;
    private int toX = -1;
    private int toY = -1;

    public AddDADFrame(JFrame owner) {
        super(owner, "Add Drag & Drop Action", 360, 280);
        nextTimeTxt.setText("500");

        fromLabel.setFont(FONT_LABEL);
        fromLabel.setFocusable(false);
        toLabel.setFont(FONT_LABEL);
        toLabel.setFocusable(false);

        JPanel pickPanel = new JPanel(new GridBagLayout());
        pickPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        GridBagConstraints bc = new GridBagConstraints();
        bc.insets = new Insets(4, 4, 4, 8);
        bc.anchor = GridBagConstraints.LINE_START;

        bc.gridx = 0; bc.gridy = 0; pickPanel.add(fromBtn, bc);
        bc.gridx = 1; bc.gridy = 0; bc.weightx = 1; pickPanel.add(fromLabel, bc);
        bc.gridx = 0; bc.gridy = 1; bc.weightx = 0; pickPanel.add(toBtn, bc);
        bc.gridx = 1; bc.gridy = 1; bc.weightx = 1; pickPanel.add(toLabel, bc);

        JPanel root = new JPanel(new BorderLayout(0, 6));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 10, 12));
        root.add(title("Add Drag & Drop Action"), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 4));
        center.add(formPanel(label("Pause after (ms):"), nextTimeTxt), BorderLayout.NORTH);
        center.add(pickPanel, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);
        root.add(btnRow(cancelBtn, addBtn), BorderLayout.SOUTH);

        dialog.getContentPane().add(root);
    }

    public void resetCoords() {
        fromX = fromY = toX = toY = -1;
        fromLabel.setText("Start: (not set)");
        toLabel.setText("End:   (not set)");
    }

    public boolean isFromSet() { return fromX >= 0; }
    public boolean isToSet()   { return toX >= 0; }

    public void setFrom(int x, int y) { fromX = x; fromY = y; fromLabel.setText("Start: (" + x + ", " + y + ")"); }
    public void setTo(int x, int y)   { toX = x;   toY = y;   toLabel.setText("End:   (" + x + ", " + y + ")"); }

    public int getFromX() { return fromX; }
    public int getFromY() { return fromY; }
    public int getToX()   { return toX; }
    public int getToY()   { return toY; }

    public CDigitField getNextTimeTxt() { return nextTimeTxt; }
    public JButton     getFromBtn()     { return fromBtn; }
    public JButton     getToBtn()       { return toBtn; }
    public JButton     getAddBtn()      { return addBtn; }
    public JButton     getCancelBtn()   { return cancelBtn; }
}

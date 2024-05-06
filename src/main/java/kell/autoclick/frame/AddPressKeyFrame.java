package kell.autoclick.frame;

import kell.autoclick.custom.CButton;
import kell.autoclick.custom.CFrame;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

@Getter
@Setter
public class AddPressKeyFrame extends CFrame {

    private final  JPanel labelPanel = new JPanel();

    private final JLabel titleLabel = new JLabel("Add press key", JLabel.CENTER);

    private final JLabel keyLabel = new JLabel("Press Key: ", JLabel.RIGHT);

    public final JTextField keyTxt = new JTextField(8);

    private final JLabel pressTimeLabel = new JLabel("Press time: ", JLabel.RIGHT);

    public final JTextField pressTimeTxt = new JTextField(8);

    private final JLabel nextTimeLabel = new JLabel("Time to next: ", JLabel.RIGHT);

    public final JTextField nextTimeTxt = new JTextField(8);

    private Boolean isSinglePress;

    private final JRadioButton isMultiPressRBtn = new JRadioButton("Multi");

    private final JRadioButton isSinglePressRBtn = new JRadioButton("Single");

    private final JButton addBtn = new CButton("Add");

    private final JButton cancelBtn = new CButton("Cancel");

    public AddPressKeyFrame(){
        super(300, 300);
    }

    public void init(){
        frame.setTitle("Add Key Press");
//        frame.setUndecorated(true);

        isSinglePress = true;
        isMultiPressRBtn.setSelected(false);
        isSinglePressRBtn.setSelected(true);
        nextTimeTxt.setText("500");
        pressTimeTxt.setText("100");

        JPanel addPressPanel = new JPanel();
        addPressPanel.setLayout(new BoxLayout(addPressPanel, BoxLayout.Y_AXIS));
        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.Y_AXIS));
        JPanel fieldPanel = new JPanel();
        fieldPanel.setLayout(new BoxLayout(fieldPanel, BoxLayout.Y_AXIS));
        addPressPanel.setLayout(new BoxLayout(addPressPanel, BoxLayout.Y_AXIS));

        addPressPanel.add(createRow(titleLabel));

        labelPanel.add(createRow(keyLabel));
        labelPanel.add(createRow(pressTimeLabel));
        labelPanel.add(createRow(nextTimeLabel));

        fieldPanel.add(createRow(keyTxt));
        fieldPanel.add(createRow(pressTimeTxt));
        fieldPanel.add(createRow(nextTimeTxt));

        addPressPanel.add(createRow(labelPanel, fieldPanel));
        addPressPanel.add(createRow(isSinglePressRBtn, isMultiPressRBtn));
        addPressPanel.add(createRow(cancelBtn, addBtn));

        frame.getContentPane().add(addPressPanel, BorderLayout.CENTER);
    }

    private JPanel createRow(JComponent... components) {
        JPanel rowPanel = new JPanel(new FlowLayout());
        for (JComponent component : components) {
            rowPanel.add(component);
        }
        return rowPanel;
    }
}

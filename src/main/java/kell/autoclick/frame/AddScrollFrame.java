package kell.autoclick.frame;

import kell.autoclick.custom.CButton;
import kell.autoclick.custom.CFrame;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

@Getter
@Setter
public class AddScrollFrame extends CFrame {

    private final  JPanel labelPanel = new JPanel();

    private final JLabel titleLabel = new JLabel("Add Scroll Event", JLabel.CENTER);

    private final JLabel wheelAmtLabel = new JLabel("Wheel Amt : ", JLabel.RIGHT);

    public final JTextField wheelAmtTxt = new JTextField(8);

    private final JLabel nextTimeLabel = new JLabel("Time to next: ", JLabel.RIGHT);

    public final JTextField nextTimeTxt = new JTextField(8);

    private Boolean isSinglePress;

    private final JButton addBtn = new CButton("Add", 80, 25);

    private final JButton cancelBtn = new CButton("Cancel");

    public AddScrollFrame(){
        super(300, 300);
    }

    public void init(){
        frame.setTitle("Add Scroll Event");

        isSinglePress = true;
        nextTimeTxt.setText("500");

        JPanel addPressPanel = new JPanel();
        addPressPanel.setLayout(new BoxLayout(addPressPanel, BoxLayout.Y_AXIS));
        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.Y_AXIS));
        JPanel fieldPanel = new JPanel();
        fieldPanel.setLayout(new BoxLayout(fieldPanel, BoxLayout.Y_AXIS));
        addPressPanel.setLayout(new BoxLayout(addPressPanel, BoxLayout.Y_AXIS));

        addPressPanel.add(createRow(titleLabel));

        labelPanel.add(createRow(nextTimeLabel));
        labelPanel.add(createRow(wheelAmtLabel));

        fieldPanel.add(createRow(nextTimeTxt));
        fieldPanel.add(createRow(wheelAmtTxt));

        addPressPanel.add(createRow(labelPanel, fieldPanel));
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

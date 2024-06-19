package kell.autoclick.frame;

import kell.autoclick.custom.CButton;
import kell.autoclick.custom.CFrame;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

@Getter
@Setter
public class AddDADFrame extends CFrame {

    private final  JPanel labelPanel = new JPanel();

    private final JLabel titleLabel = new JLabel("Add DAD Event", JLabel.CENTER);

    private final JLabel nextTimeLabel = new JLabel("Time to next: ", JLabel.RIGHT);

    public final JTextField nextTimeTxt = new JTextField(8);

    private final JButton fromBtn = new CButton("From", 100, 50);

    private final JButton toBtn = new CButton("To", 100, 50);

    private final JButton addBtn = new CButton("Add");

    private final JButton cancelBtn = new CButton("Cancel");

    public AddDADFrame(){
        super(300, 300);
    }

    public void init(){
        frame.setTitle("Add DAD Event");

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

        fieldPanel.add(createRow(nextTimeTxt));

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

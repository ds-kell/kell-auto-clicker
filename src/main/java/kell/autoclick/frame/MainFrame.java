package kell.autoclick.frame;

import kell.autoclick.custom.CButton;
import kell.autoclick.custom.CFrame;
import kell.autoclick.table.TableItems;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

@Getter
@Setter
public class MainFrame extends CFrame {

//    Label
    private final JLabel repeatTimesLabel = new JLabel("Repeat times: ", SwingConstants.RIGHT);

    public final JTextField repeatTimesTxt = new JTextField(8);

//    Button
    private final JButton addClickBtn = new CButton("Add Click");

    private final JButton addKeyBtn = new CButton("Add Key");

    private final JButton addScrollBtn = new CButton("Add Scroll");

    private final JButton addDADBtn = new CButton("Add DAD");

    private final JButton startBtn = new CButton("Start-(F7)");

    private final JButton stopBtn = new CButton("Stop-(F8)");

    private final JButton resetBtn = new CButton("Reset");

    private final JButton deleteBtn = new CButton("Delete");

    private final JButton saveBtn = new CButton("Save");

    private final JButton loadBtn = new CButton("Load");

    private final JRadioButton hideMainFrameRBtn = new JRadioButton("Hide frame");

    private final JRadioButton infiniteRBtn = new JRadioButton("Until stop");

    private boolean isHideFrame = true;

//    Table
    private final TableItems tableItems = new TableItems();

    public MainFrame(){
        super(500, 400);
        frame.setTitle("Auto clicker");
    }

    public void init() {
        hideMainFrameRBtn.setSelected(isHideFrame);
        //    Panel
        JScrollPane scrollPane = new JScrollPane(tableItems.getTable());
        JPanel controlPanel = new JPanel(new FlowLayout());
        JPanel addPanel = new JPanel(new FlowLayout());
        JPanel loopPanel = new JPanel(new FlowLayout());

        addComponentToPanel(addPanel, addClickBtn, addKeyBtn, addScrollBtn, deleteBtn, resetBtn);
        addComponentToPanel(controlPanel, startBtn, stopBtn, saveBtn, loadBtn);
        addComponentToPanel(loopPanel, repeatTimesLabel, repeatTimesTxt, infiniteRBtn, hideMainFrameRBtn);

        frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));

        frame.getContentPane().add(addPanel);
        frame.getContentPane().add(controlPanel);
        frame.getContentPane().add(scrollPane);
        frame.getContentPane().add(loopPanel);

        frame.getContentPane().requestFocus();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }
}

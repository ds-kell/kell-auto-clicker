package kell.autoclick.frame;

import kell.autoclick.table.TableItems;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

@Getter
@Setter
public class MainFrame extends CustomFrame {

//    Label
    private final JLabel repeatTimesLabel = new JLabel("Repeat times: ", JLabel.RIGHT);

    public final JTextField repeatTimesTxt = new JTextField(8);

//    Button
    private final JButton addClickBtn = new JButton("Add Click");

    private final JButton addKeyBtn = new JButton("Add Key");

    private final JButton startBtn = new JButton("Start-(F7)");

    private final JButton stopBtn = new JButton("Stop-(F8)");

    private final JButton resetBtn = new JButton("Reset");

    private final JButton deleteBtn = new JButton("Delete");

    private final JButton saveBtn = new JButton("Save");

    private final JButton loadBtn = new JButton("Load");

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
        //    Panel
        JScrollPane scrollPane = new JScrollPane(tableItems.getTable());
        JPanel controlPanel = new JPanel(new FlowLayout());
        JPanel addPanel = new JPanel(new FlowLayout());
        JPanel loopPanel = new JPanel(new FlowLayout());

        addItemToPanel(addPanel, addClickBtn, addKeyBtn, deleteBtn, resetBtn);
        addItemToPanel(controlPanel, startBtn, stopBtn, saveBtn, loadBtn);
        addItemToPanel(loopPanel, repeatTimesLabel, repeatTimesTxt, infiniteRBtn, hideMainFrameRBtn);

        frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));

        frame.getContentPane().add(addPanel);
        frame.getContentPane().add(controlPanel);
        frame.getContentPane().add(scrollPane);
        frame.getContentPane().add(loopPanel);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        hideMainFrameRBtn.setSelected(isHideFrame);
    }
}

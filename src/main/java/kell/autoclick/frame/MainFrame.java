package kell.autoclick.frame;

import kell.autoclick.table.TableClickItems;
import kell.autoclick.table.TableItems;
import kell.autoclick.table.TablePressItems;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

@Getter
@Setter
public class MainFrame extends CustomFrame {

//    Panel
    private JScrollPane scrollPane;

    private final  JPanel topPanel = new JPanel(new FlowLayout());

    private final  JPanel controlPanel = new JPanel(new FlowLayout());

    private final  JPanel addPanel = new JPanel(new FlowLayout());

    private final  JPanel loopPanel = new JPanel(new FlowLayout());

    private final  JPanel bottomPanel = new JPanel(new BorderLayout());

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
    private TableClickItems tableClickItems = new TableClickItems();

    private TablePressItems tablePressItems = new TablePressItems();

    public MainFrame(){
        super(500, 400);
        frame.setTitle("Auto clicker");
    }

    public void initMainFrame() {
        scrollPane = new JScrollPane(tableClickItems.getTable())
        ;
        addItemToPanel(addPanel, addClickBtn, addKeyBtn, deleteBtn, resetBtn);
        addItemToPanel(controlPanel, startBtn, stopBtn, saveBtn, loadBtn);
        addItemToPanel(loopPanel, repeatTimesLabel, repeatTimesTxt, infiniteRBtn, hideMainFrameRBtn);

        topPanel.setLayout(new BorderLayout());
        topPanel.add(addPanel, BorderLayout.NORTH);
        topPanel.add(controlPanel, BorderLayout.SOUTH);

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(topPanel, BorderLayout.NORTH);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.add(loopPanel, BorderLayout.NORTH);

        frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        hideMainFrameRBtn.setSelected(isHideFrame);
    }
}

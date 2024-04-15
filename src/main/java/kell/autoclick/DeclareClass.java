package kell.autoclick;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Vector;

public class DeclareClass {
    // Frame
    public static final JFrame frame = new JFrame("Java code app");
    public static final int WIDTH = 400;
    public static final int HEIGHT = 300;
    public static final JRadioButton hideFrameRBtn = new JRadioButton("Hide frame");
    public static boolean isHideFrame = false;
    // Button
    public static final JButton addBtn = new JButton("Add");
    public static final JButton startBtn = new JButton("Start");
    public static final JButton stopBtn = new JButton("Stop");
    public static final JButton resetBtn = new JButton("Reset");
    public static final JButton deleteBtn = new JButton("Delete");
    // Loop vars
    public static final JRadioButton infiniteRBtn = new JRadioButton("Until stop");
    public static final JLabel repeatLabel = new JLabel("Repeat times: ", JLabel.RIGHT);
    public static final JTextField repeatTimesTxt = new JTextField(8);
    public static final JTextField messageTxt = new JTextField(8);
    public static final DefaultTableModel model = new DefaultTableModel();
    public static final JTable table = new JTable(model);
    public static final Vector<Model> listItems = new Vector<>();
    public static boolean isStopClick = false;
    public static boolean infinite = false;
    public static final Object lock = new Object();
    public static int repeatTimes = 0;
    //Panel
    public static final JPanel panelBtn = new JPanel(new FlowLayout());
    public static final JPanel panelLoop = new JPanel(new FlowLayout());
    public static final JScrollPane scrollPane = new JScrollPane(table);
    public static final JPanel bottomPanel = new JPanel(new BorderLayout());
}

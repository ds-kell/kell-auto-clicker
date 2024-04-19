package kell.autoclick;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeclareClass {
    // Frame
    public static final JFrame frame = new JFrame("Auto Clicker");
    public static final JFrame subFrame = new JFrame("Notification");
    public static final int WIDTH = 400;
    public static final int subWIDTH = 200;
    public static final int HEIGHT = 300;
    public static final int subHEIGHT = 100;
    public static final JRadioButton hideFrameRBtn = new JRadioButton("Hide frame");
    public static boolean isHideFrame = false;
    // Button
    public static final JButton addBtn = new JButton("Add");
    public static final JButton startBtn = new JButton("Start-(F7)");
    public static final JButton stopBtn = new JButton("Stop-(F8)");
    public static final JButton resetBtn = new JButton("Reset");
    public static final JButton deleteBtn = new JButton("Delete");
    public static final JButton confirmBtn = new JButton("OK");
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
    public static final JPanel msgPanel = new JPanel(new BorderLayout());

    public static void initFrame() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = (int) screenSize.getWidth();
        int screenHeight = (int) screenSize.getHeight();
        frame.setSize(WIDTH, HEIGHT);
        frame.setLocation((screenWidth - WIDTH) / 2, (screenHeight - HEIGHT) / 2);

        subFrame.setSize(subWIDTH, subHEIGHT);
        subFrame.setLocation((screenWidth - subWIDTH) / 2, (screenHeight - subWIDTH) / 2);
        subFrame.setUndecorated(true);
        messageTxt.setText("");
        messageTxt.setEditable(false);

        confirmBtn.setSize(subWIDTH/2, subHEIGHT/2);
        model.addColumn("xAxis");
        model.addColumn("yAxis");
        model.addColumn("Time");
    }

    public static void addPanel() {
        addComponentsToPanel(panelBtn, addBtn, startBtn, stopBtn, resetBtn, deleteBtn);
        addComponentsToPanel(panelLoop, repeatLabel, repeatTimesTxt, infiniteRBtn, hideFrameRBtn);

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(panelBtn, BorderLayout.NORTH);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

        bottomPanel.add(panelLoop, BorderLayout.NORTH);

        frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        msgPanel.add(messageTxt, BorderLayout.CENTER);
        msgPanel.add(confirmBtn, BorderLayout.SOUTH);
        subFrame.getContentPane().setLayout(new BorderLayout());
        subFrame.getContentPane().add(msgPanel, BorderLayout.CENTER);
        subFrame.setVisible(false);
    }

    private static void addComponentsToPanel(JPanel panel, JComponent... components) {
        for (JComponent component : components) {
            panel.add(component);
        }
    }

    public static void deleteSelectedRows() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }

        int[] selectedRows = table.getSelectedRows();
        for (int i = selectedRows.length - 1; i >= 0; i--) {
            model.removeRow(selectedRows[i]);
            listItems.remove(selectedRows[i]);
        }
    }

    public static void resetData() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        listItems.clear();
        infinite = false;
        repeatTimes = 0;
        infiniteRBtn.setSelected(false);
        repeatTimesTxt.setText("");
        model.setRowCount(0);
    }

    public static void stopClick() {
        isStopClick = true;
        synchronized (lock) {
            lock.notifyAll();
        }
    }
    public static void showTransparentFrame() {
        JFrame transparentFrame = new JFrame();
        transparentFrame.setUndecorated(true);
        transparentFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        transparentFrame.setBackground(new Color(100, 100, 100, 100));

        transparentFrame.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                addData(e.getX(), e.getY());
                transparentFrame.setVisible(false);
            }
        });
        transparentFrame.setVisible(true);
    }

    public static void addData(int x, int y) {
        listItems.add(new Model(x, y));
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.addRow(new Object[]{x, y, listItems.get(listItems.size() - 1).getTimeToNext()});
    }

    public static void startClick() {
        if(!listItems.isEmpty()){
            if (infinite) {
                repeatTimesTxt.setEnabled(false);
                isStopClick = false;
                frame.setVisible(false);
                infiniteRBtn.setEnabled(false);
                startInfiniteAutoClick();
            } else {
                String input = repeatTimesTxt.getText();
                try {
                    repeatTimes = Integer.parseInt(input);
                } catch (NumberFormatException ex) {
                    messageTxt.setText("Fuck you. Enter repeat times");
                    subFrame.setVisible(true);
                    frame.setEnabled(false);
                    System.out.println("Invalid input: Please enter a valid integer.");
                }
                if(repeatTimes > 0) {
                    repeatTimesTxt.setEnabled(false);
                    isStopClick = false;
                    frame.setVisible(false);
                    infiniteRBtn.setEnabled(false);
                    startLimitedAutoClick();
                }
            }
        } else{
            messageTxt.setText("Fuck, add position");
            subFrame.setVisible(true);
            frame.setEnabled(false);
        }
    }

    public static void startInfiniteAutoClick() {
        new Thread(() -> {
            try {
                synchronized (lock) {
                    while (!isStopClick) {
                        for (Model m : listItems) {
                            if (isStopClick) {
                                break;
                            }
                            performClick(m);
                            lock.wait(m.getTimeToNext());
                        }
                    }
                    infiniteRBtn.setEnabled(true);
                    repeatTimesTxt.setEnabled(true);
                }
            } catch (AWTException | InterruptedException ex) {
                Logger.getLogger(AutoClickerApp.class.getName()).log(Level.SEVERE, "Error in auto click", ex);
            }
        }).start();
    }

    public static void startLimitedAutoClick() {
        new Thread(() -> {
            try {
                synchronized (lock) {
                    while (!isStopClick && repeatTimes > 0) {
                        for (Model m : listItems) {
                            if (isStopClick) {
                                break;
                            }
                            performClick(m);
                            lock.wait(m.getTimeToNext());
                        }
                        repeatTimes--;
                    }
                    infiniteRBtn.setEnabled(true);
                    repeatTimesTxt.setEnabled(true);
                    frame.setVisible(true);
                }
            } catch (AWTException | InterruptedException ex) {
                Logger.getLogger(AutoClickerApp.class.getName()).log(Level.SEVERE, "Error in auto click", ex);
            }
        }).start();
    }

    public static void performClick(Model m) throws AWTException {
        Robot robot = new Robot();
        robot.mouseMove(m.getXAxis(), m.getYAxis());
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    public static void globalKeyListener() {
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);

        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException ex) {
            System.err.println("Could not register native hook. Exiting.");
            System.exit(1);
        }

        GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
            @Override
            public void nativeKeyPressed(NativeKeyEvent e) {
                if (e.getKeyCode() == NativeKeyEvent.VC_F7) {
                    startClick();
                } else if (e.getKeyCode() == NativeKeyEvent.VC_F8) {
                    isStopClick = true;
                    SwingUtilities.invokeLater(() -> frame.setVisible(true));
                }
            }

            @Override
            public void nativeKeyReleased(NativeKeyEvent e) {
            }

            @Override
            public void nativeKeyTyped(NativeKeyEvent e) {
            }
        });
    }
}

package kell.autoclick;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AutoClickerApp {

    private static final int WIDTH = 400;
    private static final int HEIGHT = 300;
    private static final ArrayList<Model> listItems = new ArrayList<>();
    private static boolean isStopClick = false;
    private static final Object lock = new Object();
    private static boolean infinite = false;
    private static int repeatTimes = 0;

    public static void main(String[] args) {
        JFrame frame = new JFrame("Java code app");
        frame.setSize(WIDTH, HEIGHT);
        initFrame(frame);

        // Button
        JButton addBtn = new JButton("Add");
        JButton startBtn = new JButton("Start");
        JButton stopBtn = new JButton("Stop");
        JButton resetBtn = new JButton("Reset");
        JButton deleteBtn = new JButton("Delete");

        // Loop variables
        JRadioButton infiniteRBtn = new JRadioButton("Until stop");
        JLabel repeatLabel = new JLabel("Repeat times: ", JLabel.RIGHT);
        JTextField repeatTimesTxt = new JTextField(8);
        JTextField messageTxt = new JTextField(8);
        messageTxt.setText("Fuck you. Enter a valid integer.");
        messageTxt.setEditable(false);
        // Table
        DefaultTableModel model = new DefaultTableModel();
        JTable table = new JTable(model);
        initTable(table, model);
        model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                int row = e.getFirstRow();
                int column = e.getColumn();
                if (row >= 0 && column >= 0) {
                    Object data = model.getValueAt(row, column);
                    switch (column) {
                        case 0:
                            listItems.get(row).setXAxis(Integer.parseInt((String) data));
                            break;
                        case 1:
                            listItems.get(row).setYAxis(Integer.parseInt((String) data));
                            break;
                        case 2:
                            listItems.get(row).setTimeToNext(Integer.parseInt((String) data));
                            break;
                    }
                }
            }
        });

        infiniteRBtn.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                infinite = e.getStateChange() == ItemEvent.SELECTED;
                repeatTimesTxt.setEnabled(!(e.getStateChange() == ItemEvent.SELECTED));
            }
        });


        repeatTimesTxt.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String input = repeatTimesTxt.getText();
                try {
                    repeatTimes = Integer.parseInt(input);
                } catch (NumberFormatException ex) {
                    System.out.println("Invalid input: Please enter a valid integer.");
                }
            }
        });

        addBtn.addActionListener(e -> showTransparentFrame(table));

        startBtn.addActionListener(e -> startClick(frame, infiniteRBtn, repeatTimesTxt));

        deleteBtn.addActionListener(e -> deleteSelectedRows(table, model));

        resetBtn.addActionListener(e -> resetTable(table, model));

        stopBtn.addActionListener(e -> stopClick());

        globalKeyListener(frame, infiniteRBtn, repeatTimesTxt);

        JPanel panelBtn = new JPanel(new FlowLayout());
        JPanel panelLoop = new JPanel(new FlowLayout());
        addComponentsToPanel(panelBtn, addBtn, startBtn, stopBtn, resetBtn, deleteBtn);
        addComponentsToPanel(panelLoop, repeatLabel, repeatTimesTxt, infiniteRBtn);

        JScrollPane scrollPane = new JScrollPane(table);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(panelBtn, BorderLayout.NORTH);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(panelLoop, BorderLayout.NORTH);
        bottomPanel.add(messageTxt, BorderLayout.SOUTH);

        frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private static void initFrame(JFrame frame) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = (int) screenSize.getWidth();
        int screenHeight = (int) screenSize.getHeight();
        frame.setLocation((screenWidth - WIDTH) / 2, (screenHeight - HEIGHT) / 2);
    }

    private static void initTable(JTable table, DefaultTableModel model) {
        model.addColumn("xAxis");
        model.addColumn("yAxis");
        model.addColumn("Time");
    }

    private static void showTransparentFrame(JTable table) {
        JFrame transparentFrame = new JFrame();
        transparentFrame.setUndecorated(true);
        transparentFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        transparentFrame.setBackground(new Color(100, 100, 100, 100));

        transparentFrame.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                addData(table, e.getX(), e.getY());
                transparentFrame.setVisible(false);
            }
        });

        transparentFrame.setVisible(true);
    }

    private static void addData(JTable table, int x, int y) {
        listItems.add(new Model(x, y));
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.addRow(new Object[]{x, y, listItems.get(listItems.size() - 1).getTimeToNext()});
    }

    private static void startClick(JFrame frame, JRadioButton infiniteRBtn, JTextField repeatTimesTxt) {
        repeatTimesTxt.setEnabled(false);
        String input = repeatTimesTxt.getText();
        try {
            repeatTimes = Integer.parseInt(input);
        } catch (NumberFormatException ex) {
            System.out.println("Invalid input: Please enter a valid integer.");
        }
        if (!listItems.isEmpty() && (repeatTimes > 0 || infinite)) {
            isStopClick = false;
            frame.setVisible(false);
            infiniteRBtn.setEnabled(false);
            if (infinite) {
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
            } else {
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
                        }
                    } catch (AWTException | InterruptedException ex) {
                        Logger.getLogger(AutoClickerApp.class.getName()).log(Level.SEVERE, "Error in auto click", ex);
                    }
                }).start();
            }
        }
    }


    private static void performClick(Model m) throws AWTException {
        Robot robot = new Robot();
        robot.mouseMove(m.getXAxis(), m.getYAxis());
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    private static void randomClick() throws AWTException {
        Robot robot = new Robot();
        Random random = new Random();
        int x = random.nextInt(600) + 200;
        int y = random.nextInt(600) + 200;
        robot.mouseMove(x, y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    private static void globalKeyListener(JFrame frame, JRadioButton infiniteRBtn, JTextField repeatTimesTxt) {
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
                    startClick(frame, infiniteRBtn, repeatTimesTxt);
                } else if (e.getKeyCode() == NativeKeyEvent.VC_F8) {
                    isStopClick = true;
//                    infinite = false;
//                    repeatTimes = 0;
//                    infiniteRBtn.setSelected(false);
//                    repeatTimesTxt.setText("");
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

    private static void deleteSelectedRows(JTable table, DefaultTableModel model) {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }

        int[] selectedRows = table.getSelectedRows();
        for (int i = selectedRows.length - 1; i >= 0; i--) {
            model.removeRow(selectedRows[i]);
            listItems.remove(selectedRows[i]);
        }
    }

    private static void resetTable(JTable table, DefaultTableModel model) {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }

        listItems.clear();
        model.setRowCount(0);
    }

    private static void stopClick() {
        isStopClick = true;
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    private static void addComponentsToPanel(JPanel panel, JComponent... components) {
        for (JComponent component : components) {
            panel.add(component);
        }
    }

    static class Model {
        private int xAxis;

        private int yAxis;

        private int timeToNext;

        public Model(int xAxis, int yAxis) {
            this.xAxis = xAxis;
            this.yAxis = yAxis;
            this.timeToNext = 1000;
        }

        public Model(int xAxis, int yAxis, int timeToNext) {
            this.xAxis = xAxis;
            this.yAxis = yAxis;
            this.timeToNext = timeToNext;
        }

        public void setXAxis(int value) {
            this.xAxis = value;
        }

        public int getXAxis() {
            return xAxis;
        }

        public void setYAxis(int value) {
            this.yAxis = value;
        }

        public int getYAxis() {
            return yAxis;
        }

        public void setTimeToNext(int timeToNext) {
            this.timeToNext = timeToNext;
        }

        public int getTimeToNext() {
            return timeToNext;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(xAxis);
            sb.append(", ");
            sb.append(yAxis);
            sb.append(", ");
            sb.append(timeToNext);
            return sb.toString();
        }
    }
}

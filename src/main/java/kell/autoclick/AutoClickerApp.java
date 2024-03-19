package kell.autoclick;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.awt.event.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

public class AutoClickerApp {

    private static final int WIDTH = 400;

    private static final int HEIGHT = 300;

    private static boolean isAutoClick = false;

    private static boolean isStopClick = false;

    private static ArrayList<Model> listItems = new ArrayList<>();

    private static int repeatTimes = 0;

    public static void main(String[] args) {

        JFrame frame = new JFrame("AutoClicker App");
        frame.setSize(WIDTH, HEIGHT);

        final JFrame transparentFrame = new JFrame();
        transparentFrame.setUndecorated(true);
        transparentFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        transparentFrame.setBackground(new Color(100, 100, 100, 100));

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = (int) screenSize.getWidth();
        int screenHeight = (int) screenSize.getHeight();
        frame.setLocation((screenWidth - WIDTH) / 2, (screenHeight - HEIGHT) / 2);


        JButton addBtn = new JButton("Add");
        JButton startBtn = new JButton("Start");
        JButton stopBtn = new JButton("Stop");
        JButton resetBtn = new JButton("Reset");
        JButton deleteBtn = new JButton("Delete");

        DefaultTableModel model = new DefaultTableModel();
        JTable tableItems = new JTable(model);
        model.addColumn("xAxis");
        model.addColumn("yAxis");
        model.addColumn("Time");

        for (int i = 0; i < tableItems.getColumnCount(); i++) {
            tableItems.getColumnModel().getColumn(i).setCellRenderer(new CustomTableCellRenderer());
        }
        tableItems.setRowHeight(30);
        tableItems.setIntercellSpacing(new Dimension(10, 10));
        tableItems.setFont(new Font("Arial", Font.PLAIN, 12));
        tableItems.getTableHeader().setFont(new Font("Arial", Font.PLAIN, 12));
        tableItems.getTableHeader().setPreferredSize(new Dimension(tableItems.getTableHeader().getWidth(), 30));
        tableItems.setFillsViewportHeight(true);

        MouseListener mouseListenerOnTransparentFrame = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                listItems.add(new Model(e.getX(), e.getY()));
                transparentFrame.removeMouseListener(this);
                transparentFrame.setVisible(false);
                model.addRow(new Object[]{listItems.get(listItems.size() - 1).getXAxis(),
                        listItems.get(listItems.size() - 1).getYAxis(),
                        listItems.get(listItems.size() - 1).getTimeToNext()});
            }
        };

        addBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                transparentFrame.addMouseListener(mouseListenerOnTransparentFrame);
                transparentFrame.setVisible(true);
            }
        });

        startBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!listItems.isEmpty()) {
                    isStopClick = false;
                    new Thread(new Runnable() {
                         boolean loop = true;
                        @Override
                        public void run() {
                            try {
                                while (loop){
                                    for (Model m : listItems) {
                                        if(isStopClick) {
                                            loop = false;
                                            break;
                                        }
                                        Robot robot = new Robot();
                                        robot.mouseMove(m.getXAxis(), m.getYAxis());
                                        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                                        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                                        Thread.sleep(m.getTimeToNext());
                                    }
                                }
                            } catch (AWTException | InterruptedException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }).start();
                }
            }
        });

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
                if (e.getKeyCode() == NativeKeyEvent.VC_F11) {
                    isStopClick = true;
                }
            }
            @Override
            public void nativeKeyReleased(NativeKeyEvent e) {}

            @Override
            public void nativeKeyTyped(NativeKeyEvent e) {}
        });

        deleteBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selectedRows = tableItems.getSelectedRows();
                DefaultTableModel model = (DefaultTableModel) tableItems.getModel();
                for (int i = selectedRows.length - 1; i >= 0; i--) {
                    model.removeRow(selectedRows[i]);
                    listItems.remove(selectedRows[i]);
                }
            }
        });
        resetBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                listItems.clear();
                model.setRowCount(0);
            }
        });

        stopBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                isStopClick = true;
            }
        });

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

        JPanel panel = new JPanel(new FlowLayout());
        panel.add(addBtn);
        panel.add(startBtn);
        panel.add(stopBtn);
        panel.add(resetBtn);
        panel.add(deleteBtn);

        JScrollPane scrollPane = new JScrollPane(tableItems);

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(panel, BorderLayout.NORTH);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}

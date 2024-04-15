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
import java.util.logging.Level;
import java.util.logging.Logger;

public class AutoClickerApp extends DeclareClass{

    public static void main(String[] args) {

        // Init
        frame.setSize(WIDTH, HEIGHT);
        initFrame();
        messageTxt.setText("Fuck you. Enter a valid integer.");
        messageTxt.setEditable(false);
        initTable();
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

        addBtn.addActionListener(e -> showTransparentFrame());
        startBtn.addActionListener(e -> startClick());
        deleteBtn.addActionListener(e -> deleteSelectedRows());
        resetBtn.addActionListener(e -> resetData());
        stopBtn.addActionListener(e -> stopClick());

        globalKeyListener();

        addComponentsToPanel(panelBtn, addBtn, startBtn, stopBtn, resetBtn, deleteBtn);
        addComponentsToPanel(panelLoop, repeatLabel, repeatTimesTxt, infiniteRBtn, hideFrameRBtn);

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(panelBtn, BorderLayout.NORTH);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

        bottomPanel.add(panelLoop, BorderLayout.NORTH);
        bottomPanel.add(messageTxt, BorderLayout.SOUTH);

        frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private static void initFrame() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = (int) screenSize.getWidth();
        int screenHeight = (int) screenSize.getHeight();
        frame.setLocation((screenWidth - WIDTH) / 2, (screenHeight - HEIGHT) / 2);
    }

    private static void initTable() {
        model.addColumn("xAxis");
        model.addColumn("yAxis");
        model.addColumn("Time");
    }

    private static void showTransparentFrame() {
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

    private static void addData(int x, int y) {
        listItems.add(new Model(x, y));
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.addRow(new Object[]{x, y, listItems.get(listItems.size() - 1).getTimeToNext()});
    }

    private static void startClick() {
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
        }
    }

    private static void startInfiniteAutoClick() {
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

    private static void startLimitedAutoClick() {
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

    private static void performClick(Model m) throws AWTException {
        Robot robot = new Robot();
        robot.mouseMove(m.getXAxis(), m.getYAxis());
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    private static void globalKeyListener() {
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

    private static void deleteSelectedRows() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }

        int[] selectedRows = table.getSelectedRows();
        for (int i = selectedRows.length - 1; i >= 0; i--) {
            model.removeRow(selectedRows[i]);
            listItems.remove(selectedRows[i]);
        }
    }

    private static void resetData() {
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
}

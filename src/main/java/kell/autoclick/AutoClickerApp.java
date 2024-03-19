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
import java.util.logging.Level;
import java.util.logging.Logger;

public class AutoClickerApp {

    private static final int WIDTH = 400;
    private static final int HEIGHT = 300;
    private static final ArrayList<Model> listItems = new ArrayList<>();
    private static boolean isStopClick = false;

    public static void main(String[] args) {
        JFrame frame = new JFrame("AutoClicker App");
        frame.setSize(WIDTH, HEIGHT);
        centerFrame(frame);

        JButton addBtn = new JButton("Add");
        JButton startBtn = new JButton("Start");
        JButton stopBtn = new JButton("Stop");
        JButton resetBtn = new JButton("Reset");
        JButton deleteBtn = new JButton("Delete");

        DefaultTableModel model = new DefaultTableModel();
        JTable tableItems = new JTable(model);
        setupTable(tableItems, model);

        addBtn.addActionListener(e -> showTransparentFrame(tableItems));

        startBtn.addActionListener(e -> startAutoClick());

        deleteBtn.addActionListener(e -> deleteSelectedRows(tableItems, model));

        resetBtn.addActionListener(e -> resetTable(tableItems, model));

        stopBtn.addActionListener(e -> stopAutoClick());

        setupGlobalKeyListener();

        JPanel panel = new JPanel(new FlowLayout());
        addComponentsToPanel(panel, addBtn, startBtn, stopBtn, resetBtn, deleteBtn);

        JScrollPane scrollPane = new JScrollPane(tableItems);

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(panel, BorderLayout.NORTH);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private static void centerFrame(JFrame frame) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = (int) screenSize.getWidth();
        int screenHeight = (int) screenSize.getHeight();
        frame.setLocation((screenWidth - WIDTH) / 2, (screenHeight - HEIGHT) / 2);
    }

    private static void setupTable(JTable table, DefaultTableModel model) {
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
                addRowToListAndTable(table, e.getX(), e.getY());
                transparentFrame.setVisible(false);
            }
        });

        transparentFrame.setVisible(true);
    }

    private static void addRowToListAndTable(JTable table, int x, int y) {
        listItems.add(new Model(x, y));
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.addRow(new Object[]{x, y, listItems.get(listItems.size() - 1).getTimeToNext()});
    }

    private static void startAutoClick() {
        if (!listItems.isEmpty()) {
            isStopClick = false;
            new Thread(() -> {
                try {
                    while (!isStopClick) {
                        for (Model m : listItems) {
                            if (isStopClick) {
                                break;
                            }
                            performClick(m);
                            Thread.sleep(m.getTimeToNext());
                        }
                    }
                } catch (AWTException | InterruptedException ex) {
                    ex.printStackTrace();
                }
            }).start();
        }
    }

    private static void performClick(Model m) throws AWTException {
        Robot robot = new Robot();
        robot.mouseMove(m.getXAxis(), m.getYAxis());
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    private static void setupGlobalKeyListener() {
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

    private static void stopAutoClick() {
        isStopClick = true;
    }

    private static void addComponentsToPanel(JPanel panel, JComponent... components) {
        for (JComponent component : components) {
            panel.add(component);
        }
    }
}

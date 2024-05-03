package kell.autoclick.click;

import kell.autoclick.frame.AddPointFrame;
import kell.autoclick.model.ClickItem;
import kell.autoclick.frame.MainFrame;
import kell.autoclick.frame.SubFrame;
import lombok.Getter;
import lombok.Setter;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;


@Getter
@Setter
public class AutoClicker {

    private MainFrame MAIN_FRAME = new MainFrame();

    private SubFrame SUB_FRAME = new SubFrame();

    private AddPointFrame ADD_POINT_FRAME = new AddPointFrame();

    private boolean isStopClick = false;

    private boolean infinite = false;

    private int repeatTimes = 0;

    public final Object lock = new Object();

    public AutoClicker() {
        MAIN_FRAME.initMainFrame();
        SUB_FRAME.initSubFrame();
        MAIN_FRAME.getFrame().setVisible(true);
    }

    public void start() {
        MAIN_FRAME.getTableClickItems().getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                int row = e.getFirstRow();
                int column = e.getColumn();
                if (row >= 0 && column >= 0) {
                    Object data = MAIN_FRAME.getTableClickItems().getModel().getValueAt(row, column);
                    switch (column) {
                        case 0:
                            MAIN_FRAME.getTableClickItems().getListItems().get(row).setName((String) data);
                            break;
                        case 1:
                            MAIN_FRAME.getTableClickItems().getListItems().get(row).setXAxis(Integer.parseInt(((String) data).trim()));
                            break;
                        case 2:
                            MAIN_FRAME.getTableClickItems().getListItems().get(row).setYAxis(Integer.parseInt(((String) data).trim()));
                            break;
                        case 3:
                            MAIN_FRAME.getTableClickItems().getListItems().get(row).setTimeToNext(Integer.parseInt(((String) data).trim()));
                            break;
                    }
                }
            }
        });

        btnListener();

        globalKeyListener();
    }

    public void btnListener() {
        MAIN_FRAME.getInfiniteRBtn().addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                infinite = e.getStateChange() == ItemEvent.SELECTED;
                MAIN_FRAME.getRepeatTimesTxt().setEnabled(!(e.getStateChange() == ItemEvent.SELECTED));
            }
        });

        MAIN_FRAME.getHideMainFrameRBtn().addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                MAIN_FRAME.setHideFrame(e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        MAIN_FRAME.getRepeatTimesTxt().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String input = MAIN_FRAME.getRepeatTimesTxt().getText();
                try {
                    repeatTimes = Integer.parseInt(input);
                } catch (NumberFormatException ex) {
                    System.out.println("Invalid input: Please enter a valid integer.");
                }
            }
        });

        MAIN_FRAME.getAddClickBtn().addActionListener(e -> showAddPointFrame());

        MAIN_FRAME.getStartBtn().addActionListener(e -> startClick());

        MAIN_FRAME.getDeleteBtn().addActionListener(e -> deleteSelected());

        MAIN_FRAME.getResetBtn().addActionListener(e -> resetData());

        MAIN_FRAME.getStopBtn().addActionListener(e -> stopClick());

        MAIN_FRAME.getSaveBtn().addActionListener(e -> saveData());

        MAIN_FRAME.getLoadBtn().addActionListener(e -> loadData());

        SUB_FRAME.getConfirmBtn().addActionListener(e -> {
            changeFrame(true);
        });
    }

    public void stopClick() {
        isStopClick = true;
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    public void resetData() {
        if (MAIN_FRAME.getTableClickItems().getTable().isEditing()) {
            MAIN_FRAME.getTableClickItems().getTable().getCellEditor().stopCellEditing();
        }
        infinite = false;
        repeatTimes = 0;
        MAIN_FRAME.getTableClickItems().clearTable();
        MAIN_FRAME.getInfiniteRBtn().setSelected(false);
        MAIN_FRAME.getRepeatTimesTxt().setText("");
        MAIN_FRAME.getTableClickItems().getModel().setRowCount(0);
    }

    public void deleteSelected() {
        if (MAIN_FRAME.getTableClickItems().getTable().getSelectedRows().length < 1) {
            String msg = "";
            if (MAIN_FRAME.getTableClickItems().getListItems().isEmpty()) {
                msg = "Fuck you, list is empty";
            } else {
                msg = "Fuck you, select row to delete";
            }
            changeFrame(false, msg);

        }
        MAIN_FRAME.getTableClickItems().deleteSelectedRows();
    }

    public void startClick() {
        if (!MAIN_FRAME.getTableClickItems().getListItems().isEmpty()) {
            if (!infinite && ("").equals(MAIN_FRAME.getRepeatTimesTxt().getText())) {
                changeFrame(false, "Fuck you. Enter repeat times");
            } else if (infinite) {
                MAIN_FRAME.getFrame().setVisible(!MAIN_FRAME.isHideFrame());
                enableInput(false);
                isStopClick = false;
                infiniteAutoClick();
            } else {
                String input = MAIN_FRAME.getRepeatTimesTxt().getText();
                try {
                    repeatTimes = Integer.parseInt(input);
                } catch (NumberFormatException ex) {
                    changeFrame(false, "Fuck you. Enter a valid integer");
                }
                if (repeatTimes > 0) {
                    MAIN_FRAME.getFrame().setVisible(!MAIN_FRAME.isHideFrame());
                    enableInput(false);
                    isStopClick = false;
                    limitedAutoClick();
                }
            }
        } else {
            changeFrame(false, "Fuck, add position");
        }
    }

    public void infiniteAutoClick() {
        new Thread(() -> {
            try {
                synchronized (lock) {
                    while (!isStopClick) {
                        for (ClickItem item : MAIN_FRAME.getTableClickItems().getListItems()) {
                            if (isStopClick) {
                                break;
                            }
                            performClick(item);
//                            CountDownLatch latch = new CountDownLatch(1);
//                            performPress("AD", 300, latch);
//                            latch.await();
                            lock.wait(item.getTimeToNext());
                        }
                    }
                    enableInput(true);
                    MAIN_FRAME.getFrame().setVisible(true);
                }
            } catch (AWTException | InterruptedException ex) {
                Logger.getLogger(AutoClicker.class.getName()).log(Level.SEVERE, "Error in auto click", ex);
            }
        }).start();
    }

    private void limitedAutoClick() {
        new Thread(() -> {
            try {
                synchronized (lock) {
                    while (!isStopClick && repeatTimes > 0) {
                        for (ClickItem item : MAIN_FRAME.getTableClickItems().getListItems()) {
                            if (isStopClick) {
                                break;
                            }
                            performClick(item);
                            lock.wait(item.getTimeToNext());
//                            CountDownLatch latch = new CountDownLatch(1);
//                            performPress("Hello  World 123", 300, latch);
//                            latch.await();
                        }
                        repeatTimes--;
                    }
                    enableInput(true);
                    MAIN_FRAME.getFrame().setVisible(true);
                }
            } catch (AWTException | InterruptedException ex) {
                Logger.getLogger(AutoClicker.class.getName()).log(Level.SEVERE, "Error in auto click", ex);
            }
        }).start();
    }

    private void performClick(ClickItem clickItem) throws AWTException {
        try {
            Robot robot = new Robot();
            robot.mouseMove(clickItem.getXAxis(), clickItem.getYAxis());
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        } catch (AWTException e) {
            stopClick();
            Logger logger = Logger.getLogger(AutoClicker.class.getName());
            logger.log(Level.SEVERE, "An AWTException", e);
        }
    }

    private void performPress(String input, int delay, CountDownLatch latch) {
        Timer timer = new Timer(delay, new ActionListener() {
            int index = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                int keyCode = 0;
                if (index < input.length()) {
                    char ch = input.charAt(index);
                    Robot robot = null;
                    try {
                        robot = new Robot();
                        keyCode = KeyEvent.getExtendedKeyCodeForChar(ch);
                        System.out.println(keyCode);
                        if (Character.isLetterOrDigit(ch) || Character.isSpaceChar(ch)) {
                            robot.keyPress(keyCode);
                            robot.keyRelease(keyCode);
                        }
                    } catch (AWTException awtException) {
                        stopClick();
                        Logger logger = Logger.getLogger(AutoClicker.class.getName());
                        logger.log(Level.SEVERE, "An InterruptedException ", e);
                    }
                    index++;
                } else {
                    ((Timer) e.getSource()).stop();
                    latch.countDown();
                }
                if (isStopClick) {
                    ((Timer) e.getSource()).stop();
                    latch.countDown();
                }
            }
        });
        timer.start();
    }

    private void showAddPointFrame() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                MAIN_FRAME.getTableClickItems().addItem(new ClickItem(e.getX(), e.getY()));
                ADD_POINT_FRAME.getAddPointFrame().setVisible(false);
                ADD_POINT_FRAME.getAddPointFrame().removeMouseListener(this);
            }
        };
        ADD_POINT_FRAME.getAddPointFrame().addMouseListener(mouseAdapter);
        ADD_POINT_FRAME.getAddPointFrame().setVisible(true);
    }

    private void globalKeyListener() {
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
                if (e.getKeyCode() == NativeKeyEvent.VC_F7 && isStopClick) {
                    startClick();
                } else if (e.getKeyCode() == NativeKeyEvent.VC_F8 && !isStopClick) {
                    stopClick();
                    SwingUtilities.invokeLater(() -> MAIN_FRAME.getFrame().setVisible(true));
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

    private void changeFrame(boolean isMainFrame) {
        this.changeFrame(isMainFrame, "");
    }

    private void changeFrame(boolean isMainFrame, String message) {
        MAIN_FRAME.getFrame().setEnabled(isMainFrame);
        SUB_FRAME.getFrame().setVisible(!isMainFrame);
        SUB_FRAME.getMessageTxt().setText(message);
    }

    private void enableInput(boolean isShow) {
        MAIN_FRAME.getInfiniteRBtn().setEnabled(isShow);
        MAIN_FRAME.getRepeatTimesTxt().setEnabled(isShow);
    }

    private void saveData() {
        String filename = "data.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            System.out.println(MAIN_FRAME.getTableClickItems().getListItems().size());
            for (ClickItem item : MAIN_FRAME.getTableClickItems().getListItems()) {
                System.out.println(item);
                String line = item.getName() + " / " + item.getXAxis() + " / " + item.getYAxis() + " / " + item.getHoldTime() + " / " + item.getTimeToNext();
                writer.write(line);
                writer.newLine();
            }
            System.out.println("Data has been successfully saved to file: " + filename);
        } catch (IOException e) {
            System.err.println("Error while saving data to file: " + e.getMessage());
        }
    }

    private void loadData(){
        Vector<ClickItem> listItems = new Vector<>();
        String filename = "data.txt";
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" / ");
                if (parts.length == 5) {
                    String name = parts[0];
                    int xAxis = Integer.parseInt(parts[1]);
                    int yAxis = Integer.parseInt(parts[2]);
                    int holdTime = Integer.parseInt(parts[3]);
                    int timeToNext = Integer.parseInt(parts[4]);

                    ClickItem item = new ClickItem(name, xAxis, yAxis, holdTime, timeToNext);
                    System.out.println(item);
                    listItems.add(item);
                }
            }
            MAIN_FRAME.getTableClickItems().setListItems(listItems);
            System.out.println("Data has been successfully read from file: " + filename);
        } catch (IOException e) {
            System.err.println("Error while reading data from file: " + e.getMessage());
        }
    }
}

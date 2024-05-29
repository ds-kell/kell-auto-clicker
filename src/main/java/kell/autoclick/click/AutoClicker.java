package kell.autoclick.click;

import kell.autoclick.frame.*;
import kell.autoclick.model.GlobalConst;
import kell.autoclick.model.ItemClick;
import kell.autoclick.model.Item;
import kell.autoclick.model.ItemPress;
import lombok.Getter;
import lombok.Setter;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;


@Getter
@Setter
public class AutoClicker {

    private MainFrame MAIN_FRAME = new MainFrame();

    private MessageFrame MSG_FRAME = new MessageFrame();

    private AddPointFrame ADD_POINT_FRAME = new AddPointFrame();

    private AddPressKeyFrame ADD_PRESS_FRAME = new AddPressKeyFrame();

    private AddScrollFrame ADD_SCROLL_FRAME = new AddScrollFrame();

    private boolean isStopClick = true;

    private boolean infinite = false;

    private int repeatTimes = 0;

    public final Object lock = new Object();

    private Vector<Thread> runningThreads = new Vector<>();

    Random random = new Random();

    public AutoClicker() {
        MAIN_FRAME.init();
        MSG_FRAME.init();
        ADD_PRESS_FRAME.init();
        ADD_SCROLL_FRAME.init();
        MAIN_FRAME.getFrame().setVisible(true);
    }

    public void start() {

        eventListener();

        tableListener();

        globalKeyListener();

        windowFocus();
    }

    private void windowFocus(){
        MAIN_FRAME.getFrame().addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                if (ADD_PRESS_FRAME.getFrame().isVisible()) {
                    ADD_PRESS_FRAME.getFrame().toFront();
                }
                if (ADD_SCROLL_FRAME.getFrame().isVisible()) {
                    ADD_SCROLL_FRAME.getFrame().toFront();
                }
                if (MSG_FRAME.getFrame().isVisible()) {
                    MSG_FRAME.getFrame().toFront();
                }
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
            }
        });

        ADD_PRESS_FRAME.getFrame().addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                if (MSG_FRAME.getFrame().isVisible()) {
                    MSG_FRAME.getFrame().toFront();
                }
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                if (MAIN_FRAME.getFrame().isEnabled()) {
                    MAIN_FRAME.getFrame().toFront();
                }
            }
        });

        ADD_SCROLL_FRAME.getFrame().addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                if (MSG_FRAME.getFrame().isVisible()) {
                    MSG_FRAME.getFrame().toFront();
                }
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                if (MAIN_FRAME.getFrame().isEnabled()) {
                    MAIN_FRAME.getFrame().toFront();
                }
            }
        });

        MSG_FRAME.getFrame().addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                if (ADD_PRESS_FRAME.getFrame().isEnabled() && ADD_PRESS_FRAME.getFrame().isVisible()) {
                    ADD_PRESS_FRAME.getFrame().toFront();
                    ADD_PRESS_FRAME.getKeyTxt().requestFocus();
                } else if (ADD_SCROLL_FRAME.getFrame().isEnabled() && ADD_SCROLL_FRAME.getFrame().isVisible()) {
                    ADD_SCROLL_FRAME.getFrame().toFront();
                    ADD_SCROLL_FRAME.getWheelAmtTxt().requestFocus();
                } else if (MAIN_FRAME.getFrame().isEnabled() && MAIN_FRAME.getFrame().isVisible()) {
                    MAIN_FRAME.getFrame().toFront();
                }
            }
        });
    }

    private void eventListener() {
        MAIN_FRAME.getInfiniteRBtn().addItemListener(e -> {
            infinite = e.getStateChange() == ItemEvent.SELECTED;
            MAIN_FRAME.getRepeatTimesTxt().setEnabled(!(e.getStateChange() == ItemEvent.SELECTED));
        });

        MAIN_FRAME.getHideMainFrameRBtn().addItemListener(e -> MAIN_FRAME.setHideFrame(e.getStateChange() == ItemEvent.SELECTED));

        MAIN_FRAME.getRepeatTimesTxt().addActionListener(e -> {
            String input = MAIN_FRAME.getRepeatTimesTxt().getText();
            try {
                repeatTimes = Integer.parseInt(input);
            } catch (NumberFormatException ex) {
                Logger logger = Logger.getLogger(AutoClicker.class.getName());
                logger.log(Level.SEVERE, "\"Invalid input: Please enter a valid integer: ", e);
            }
        });

        MAIN_FRAME.getAddClickBtn().addActionListener(e -> showAddPointFrame());

        MAIN_FRAME.getAddKeyBtn().addActionListener(e -> {
            showAddPressFrame(true);
            ADD_PRESS_FRAME.getKeyTxt().requestFocus();
        });

        MAIN_FRAME.getAddScrollBtn().addActionListener(e -> {
            showAddScrollFrame(true);
            ADD_SCROLL_FRAME.getWheelAmtTxt().requestFocus();
        });

        MAIN_FRAME.getStartBtn().addActionListener(e -> startClick());

        MAIN_FRAME.getDeleteBtn().addActionListener(e -> deleteSelected());

        MAIN_FRAME.getResetBtn().addActionListener(e -> resetData());

        MAIN_FRAME.getStopBtn().addActionListener(e -> stopClick());

        MAIN_FRAME.getSaveBtn().addActionListener(e -> saveData());

        MAIN_FRAME.getLoadBtn().addActionListener(e -> loadData());

        MSG_FRAME.getConfirmBtn().addActionListener(e -> changeFrame(true));

        ADD_PRESS_FRAME.getAddBtn().addActionListener(e -> addPressKey());

        ADD_PRESS_FRAME.getCancelBtn().addActionListener(e -> showAddPressFrame(false));

        ADD_SCROLL_FRAME.getAddBtn().addActionListener(e -> addScrollEvent());

        ADD_SCROLL_FRAME.getCancelBtn().addActionListener(e -> showAddScrollFrame(false));

        ADD_PRESS_FRAME.getIsSinglePressRBtn().addItemListener(e -> {
            ADD_PRESS_FRAME.setIsSinglePress(e.getStateChange() == ItemEvent.SELECTED);
            ADD_PRESS_FRAME.getIsMultiPressRBtn().setSelected(!ADD_PRESS_FRAME.getIsSinglePress());
        });
        ADD_PRESS_FRAME.getIsMultiPressRBtn().addItemListener(e -> {
            ADD_PRESS_FRAME.setIsSinglePress(!(e.getStateChange() == ItemEvent.SELECTED));
            ADD_PRESS_FRAME.getIsSinglePressRBtn().setSelected(ADD_PRESS_FRAME.getIsSinglePress());
        });
        ADD_PRESS_FRAME.getFrame().addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                showAddPressFrame(false);
            }
        });
    }

    private void tableListener() {
        MAIN_FRAME.getTableItems().getModel().addTableModelListener(e -> {
            int row = e.getFirstRow();
            int column = e.getColumn();
            if (row >= 0 && column >= 0) {
                Object data = MAIN_FRAME.getTableItems().getModel().getValueAt(row, column);
                Item item = MAIN_FRAME.getTableItems().getListItems().get(row);
                switch (column) {
                    case 0:
                        item.setName((String) data);
                        MAIN_FRAME.getTableItems().getListItems().remove(row);
                        MAIN_FRAME.getTableItems().getListItems().add(row, item);
                        break;
                    case 1:
                        item.setPressTime(Integer.parseInt(((String) data).trim()));
                        MAIN_FRAME.getTableItems().getListItems().remove(row);
                        MAIN_FRAME.getTableItems().getListItems().add(row, item);
                        break;
                    case 2:
                        item.setNextTime(Integer.parseInt(((String) data).trim()));
                        MAIN_FRAME.getTableItems().getListItems().remove(row);
                        MAIN_FRAME.getTableItems().getListItems().add(row, item);
                        break;
                }

            }
        });
    }

    public void stopClick() {
        synchronized (lock) {
            lock.notifyAll();
        }
        isStopClick = true;
        for (Thread thread : runningThreads) {
            thread.interrupt();
        }
        runningThreads.clear();
        enableInput(true);
        SwingUtilities.invokeLater(() -> {
            MAIN_FRAME.getFrame().setVisible(true);
            MAIN_FRAME.getFrame().setEnabled(true);
        });
    }

    public void resetData() {
        if (MAIN_FRAME.getTableItems().getTable().isEditing()) {
            MAIN_FRAME.getTableItems().getTable().getCellEditor().stopCellEditing();
        }
        infinite = false;
        repeatTimes = 0;
        MAIN_FRAME.getTableItems().clearTable();
        MAIN_FRAME.getInfiniteRBtn().setSelected(false);
        MAIN_FRAME.setHideFrame(true);
        MAIN_FRAME.getHideMainFrameRBtn().setSelected(true);
        MAIN_FRAME.getRepeatTimesTxt().setText("");
        MAIN_FRAME.getTableItems().getModel().setRowCount(0);
    }

    public void deleteSelected() {
        if (MAIN_FRAME.getTableItems().getTable().getSelectedRows().length < 1) {
            String msg;
            if (MAIN_FRAME.getTableItems().getListItems().isEmpty()) {
                msg = "Fuck you, list is empty";
            } else {
                msg = "Fuck you, select row to delete";
            }
            changeFrame(false, msg);

        }
        MAIN_FRAME.getTableItems().deleteSelectedRows();
    }

    public void startClick() {
        if (!MAIN_FRAME.getTableItems().getListItems().isEmpty()) {
            if (!infinite && ("").equals(MAIN_FRAME.getRepeatTimesTxt().getText())) {
                changeFrame(false, "Fuck you. Enter repeat times");
            } else if (infinite) {
                MAIN_FRAME.getFrame().setVisible(!MAIN_FRAME.isHideFrame());
                MAIN_FRAME.getFrame().setEnabled(false);
                enableInput(false);
                isStopClick = false;
                repeatTimes = Integer.MAX_VALUE;
                performClick();
            } else {
                String input = MAIN_FRAME.getRepeatTimesTxt().getText();
                try {
                    repeatTimes = Integer.parseInt(input);
                } catch (NumberFormatException ex) {
                    changeFrame(false, "Fuck you. Enter a valid integer");
                }
                if (repeatTimes > 0) {
                    MAIN_FRAME.getFrame().setVisible(!MAIN_FRAME.isHideFrame());
                    MAIN_FRAME.getFrame().setEnabled(false);
                    enableInput(false);
                    isStopClick = false;
                    performClick();
                }
            }
        } else {
            changeFrame(false, "Fuck, add position");
        }
    }

    private void performClick() {
        Thread clickThread = new Thread(() -> {
            try {
                synchronized (lock) {
                    while (!isStopClick && repeatTimes > 0) {
                        for (Item item : MAIN_FRAME.getTableItems().getListItems()) {
                            if (isStopClick) {
                                break;
                            }
                            if(item.getTypeEvent() == GlobalConst.EVENT_CLICK){
                                ItemClick itemClick = (ItemClick) item;
                                clickEvent(itemClick);
                            } else if(item.getTypeEvent() == GlobalConst.EVENT_PRESS) {
                                CountDownLatch latch = new CountDownLatch(1);
                                ItemPress itemPress = (ItemPress) item;
                                singlePressEvent(itemPress,latch);
                                latch.await();
                            } else {
                                performScroll(item);
                            }
                            lock.wait(item.getNextTime() + random.nextInt(10) * 10);
                        }
                        repeatTimes--;
                    }
                    stopClick();
                }
            } catch (AWTException | InterruptedException ex) {
                stopClick();
                Logger.getLogger(AutoClicker.class.getName()).log(Level.SEVERE, "Error in auto click", ex);
            }
        });
        clickThread.start();
        runningThreads.add(clickThread);
    }
    private void performVerticalClick() {
        Thread clickThread = new Thread(() -> {
            try {
                synchronized (lock) {
                    Robot robot = new Robot();
                    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    int midWidth;
                    int height = (int) screenSize.getWidth();
                    while (!isStopClick && repeatTimes > 0) {
                        if(repeatTimes % 2 == 0) {
                            midWidth = (int) (screenSize.getWidth() / 2) - 15;
                        } else {
                            midWidth = (int) (screenSize.getWidth() / 2) + 15;
                        }
                        for(int i = 10; i < height; i += 20){
                            robot.mouseMove(midWidth, i);
                            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                        }
                        lock.wait(10);
                        repeatTimes--;
                    }
                    stopClick();
                }
            } catch (InterruptedException ex) {
                stopClick();
                Logger.getLogger(AutoClicker.class.getName()).log(Level.SEVERE, "Error in auto click", ex);
            } catch (AWTException e) {
                throw new RuntimeException(e);
            }
        });
        clickThread.start();
        runningThreads.add(clickThread);
    }

    private void performHorizontalClick() {
        Thread clickThread = new Thread(() -> {
            try {
                synchronized (lock) {
                    Robot robot = new Robot();
                    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    int width = (int) screenSize.getWidth() - 20;
                    int midHeight;
                    while (!isStopClick && repeatTimes > 0) {
                        if(repeatTimes % 2 == 0) {
                            midHeight = (int) (screenSize.getHeight() / 2) - 15;
                        } else {
                            midHeight = (int) (screenSize.getHeight() / 2) + 15;
                        }
                        for(int i = 10; i < width; i += 20){
                            robot.mouseMove(i, midHeight);
                            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                        }
                        lock.wait(10);
                        repeatTimes--;
                    }
                    stopClick();
                }
            } catch (InterruptedException ex) {
                stopClick();
                Logger.getLogger(AutoClicker.class.getName()).log(Level.SEVERE, "Error in auto click", ex);
            } catch (AWTException e) {
                throw new RuntimeException(e);
            }
        });
        clickThread.start();
        runningThreads.add(clickThread);
    }

    private void clickEvent(ItemClick itemClick) throws AWTException {
        try {
            Robot robot = new Robot();
            robot.mouseMove(itemClick.getXAxis() + (random.nextInt(10) - 5), itemClick.getYAxis() + (random.nextInt(10) - 5));
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        } catch (AWTException e) {
            stopClick();
            Logger logger = Logger.getLogger(AutoClicker.class.getName());
            logger.log(Level.SEVERE, "An AWTException", e);
        }
    }

    private void singlePressEvent(ItemPress item, CountDownLatch latch) {
        Timer timer = new Timer(item.getPressTime(), e -> {
            if (isStopClick) {
                ((Timer) e.getSource()).stop();
                latch.countDown();
                return;
            }
            try {
                Robot robot = new Robot();
                if (Character.isLetterOrDigit(item.getKeyCode()) || Character.isSpaceChar(item.getKeyCode())) {
                    robot.keyPress(item.getKeyCode());
                    robot.keyRelease(item.getKeyCode());
                }
            } catch (AWTException awtException) {
                stopClick();
                Logger logger = Logger.getLogger(AutoClicker.class.getName());
                logger.log(Level.SEVERE, "An InterruptedException ", e);
            }
            ((Timer) e.getSource()).stop();
            latch.countDown();
        });
        timer.start();
    }
    private void multiPressEvent(ItemPress item, CountDownLatch latch) {
        Timer timer = new Timer(item.getPressTime(), new ActionListener() {
            int index = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isStopClick) {
                    ((Timer) e.getSource()).stop();
                    latch.countDown();
                    return;
                }
                if(index < item.getName().length()){
                    try {
                        Robot robot = new Robot();
                        if (Character.isLetterOrDigit(item.getKeyCode()) || Character.isSpaceChar(item.getKeyCode())) {
                            robot.keyPress(item.getKeyCode());
                            robot.keyRelease(item.getKeyCode());
                        }
                    } catch (AWTException awtException) {
                        stopClick();
                        Logger logger = Logger.getLogger(AutoClicker.class.getName());
                        logger.log(Level.SEVERE, "An InterruptedException ", e);
                    }
                    index++;
                } else{
                    ((Timer) e.getSource()).stop();
                    latch.countDown();
                }
                ((Timer) e.getSource()).stop();
                latch.countDown();
            }
        });
        timer.start();
    }

    private void performScroll(Item item) throws AWTException {
        try {
            Robot robot = new Robot();
            robot.mouseWheel(item.getPressTime());
        } catch (AWTException awtException) {
            stopClick();
            Logger logger = Logger.getLogger(AutoClicker.class.getName());
            logger.log(Level.SEVERE, "Exception ", awtException);
        }
    }

    private void showAddPointFrame() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                MAIN_FRAME.getTableItems().addItem(new ItemClick(e.getX(), e.getY()));
                ADD_POINT_FRAME.getFrame().setVisible(false);
                ADD_POINT_FRAME.getFrame().removeMouseListener(this);
            }
        };
        ADD_POINT_FRAME.getFrame().addMouseListener(mouseAdapter);
        ADD_POINT_FRAME.getFrame().setVisible(true);
    }

    private void showAddPressFrame(boolean showAddPressFrame) {
        MAIN_FRAME.getFrame().setEnabled(!showAddPressFrame);
        ADD_PRESS_FRAME.getFrame().setVisible(showAddPressFrame);
    }
    private void showAddScrollFrame(boolean showAddScrollFrame) {
        MAIN_FRAME.getFrame().setEnabled(!showAddScrollFrame);
        ADD_SCROLL_FRAME.getFrame().setVisible(showAddScrollFrame);
    }

    private void addPressKey() {
        String input = ADD_PRESS_FRAME.getKeyTxt().getText();
        if(input.isEmpty()) {
            changeFrame(false, "Fuck, add key");
            return;
        }
        int keyCode = KeyEvent.getExtendedKeyCodeForChar(input.charAt(0));
        int pressTime = Integer.parseInt(ADD_PRESS_FRAME.getPressTimeTxt().getText());
        int nextTime = Integer.parseInt(ADD_PRESS_FRAME.getNextTimeTxt().getText());
        ItemPress item = new ItemPress(input, keyCode, pressTime, nextTime, ADD_PRESS_FRAME.getIsSinglePress());
        MAIN_FRAME.getTableItems().addItem(item);
        ADD_PRESS_FRAME.getKeyTxt().setText("");
        MAIN_FRAME.getFrame().setEnabled(true);
        ADD_PRESS_FRAME.getFrame().setVisible(false);
    }

    private void addScrollEvent() {
        String input = ADD_SCROLL_FRAME.getWheelAmtTxt().getText();
        if(input.isEmpty()) {
            changeFrame(false, "Fuck, add wheel amt");
            return;
        }
        int nextTime = Integer.parseInt(ADD_SCROLL_FRAME.getNextTimeTxt().getText());
        Item item = new Item(input, Integer.parseInt(input), nextTime, GlobalConst.EVENT_SCROLL);
        MAIN_FRAME.getTableItems().addItem(item);
        ADD_SCROLL_FRAME.getWheelAmtTxt().setText("");
        MAIN_FRAME.getFrame().setEnabled(true);
        ADD_SCROLL_FRAME.getFrame().setVisible(false);
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
        if(ADD_PRESS_FRAME.getFrame().isVisible()) {
            ADD_PRESS_FRAME.getFrame().setEnabled(isMainFrame);
        }
        MSG_FRAME.getFrame().setVisible(!isMainFrame);
        MSG_FRAME.getMessageLabel().setText(message);
    }

    private void enableInput(boolean isShow) {
        MAIN_FRAME.getInfiniteRBtn().setEnabled(isShow);
        MAIN_FRAME.getRepeatTimesTxt().setEnabled(isShow);
    }

    private void saveData() {
        if (MAIN_FRAME.getTableItems().getListItems().isEmpty()) {
            changeFrame(false, "Fuck you, list is empty");
        } else {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                String selectedFolder = fileChooser.getSelectedFile().getAbsolutePath();
                String filename = selectedFolder + "/data" + System.currentTimeMillis()  + ".txt";
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
                    for (Item item : MAIN_FRAME.getTableItems().getListItems()) {
                        String line = getString(item);
                        writer.write(line);
                        writer.newLine();
                    }
                    changeFrame(false, "Data saved");
                } catch (IOException e) {
                    Logger logger = Logger.getLogger(AutoClicker.class.getName());
                    logger.log(Level.SEVERE, "Error while saving data to file: ", e);
                }
            }
        }
    }

    private static String getString(Item item) {
        String line = "";
        if (item.getTypeEvent() == GlobalConst.EVENT_CLICK){
            ItemClick itemClick = (ItemClick) item;
            line = GlobalConst.EVENT_CLICK + "|" + itemClick.getName() + "|" + itemClick.getPressTime() + "|" + itemClick.getNextTime()
                    + "|" + itemClick.getXAxis() + "|" + itemClick.getYAxis();
        } else if (item.getTypeEvent() == GlobalConst.EVENT_PRESS) {
            ItemPress itemPress = (ItemPress) item;
            line = GlobalConst.EVENT_PRESS + "|" + itemPress.getName() + "|" + itemPress.getPressTime() + "|" + itemPress.getNextTime()
                    + "|" + itemPress.getKeyCode() + "|" + itemPress.isMultiPress();
        } else {
            line = GlobalConst.EVENT_SCROLL + "|" + item.getName() + "|" + item.getPressTime() + "|" + item.getNextTime();
        }
        return line;
    }

    private void loadData() {
        Vector<Item> listItems = new Vector<>();

        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            String filename = fileChooser.getSelectedFile().getAbsolutePath();
            try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (parts.length > 0) {
                        int event = Integer.parseInt(parts[0]);
                        String name = parts[1];
                        int pressTime = Integer.parseInt(parts[2]);
                        int nextTime = Integer.parseInt(parts[3]);
                        if (event == GlobalConst.EVENT_CLICK) {
                            int xAxis = Integer.parseInt(parts[4]);
                            int yAxis = Integer.parseInt(parts[5]);
                            ItemClick itemClick = new ItemClick(name, xAxis, yAxis, pressTime, nextTime);
                            listItems.add(itemClick);
                        } else if (event == GlobalConst.EVENT_PRESS){
                            int keyCode = Integer.parseInt(parts[4]);
                            boolean isMultiPress = Boolean.parseBoolean(parts[5]);
                            ItemPress itemPress = new ItemPress(name, keyCode, pressTime, nextTime, isMultiPress);
                            listItems.add(itemPress);
                        } else {
                            Item item = new Item(name, pressTime, nextTime, GlobalConst.EVENT_SCROLL);
                            listItems.add(item);
                        }
                    }
                }
                MAIN_FRAME.getTableItems().setListItems(listItems);
                changeFrame(false, "Data loaded");
            } catch (IOException e) {
                Logger logger = Logger.getLogger(AutoClicker.class.getName());
                logger.log(Level.SEVERE, "Error while loading data to file: ", e);
            }
        }
    }
}

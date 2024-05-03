package kell.autoclick.click;

import kell.autoclick.model.ClickItem;
import kell.autoclick.panel.Panel;
import kell.autoclick.button.Button;
import kell.autoclick.frame.MainFrame;
import kell.autoclick.frame.SubFrame;
import kell.autoclick.label.LabelF;
import kell.autoclick.table.TableClickItems;
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
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;


@Getter
@Setter
public class AutoClicker implements ClickEvent {

    private MainFrame MAIN_FRAME = new MainFrame();

    private SubFrame SUB_FRAME = new SubFrame();

    private TableClickItems table = new TableClickItems();

    private Button btn = new Button();

    private Panel panel = new Panel(table);

    private LabelF label = new LabelF();

    private boolean isStopClick = false;

    private boolean infinite = false;

    private int repeatTimes = 0;

    public final Object lock = new Object();

    public AutoClicker(){
        addToPanel();
        btn.getHideMainFrameRBtn().setSelected(MAIN_FRAME.isHideFrame());
        MAIN_FRAME.getMainFrame().setVisible(true);
        SUB_FRAME.getSubFrame().setVisible(false);
    }

    public void start(){
        table.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                int row = e.getFirstRow();
                int column = e.getColumn();
                if (row >= 0 && column >= 0) {
                    Object data = table.getModel().getValueAt(row, column);
                    switch (column) {
                        case 0:
                            table.getListItems().get(row).setName((String) data);
                            break;
                        case 1:
                            table.getListItems().get(row).setXAxis(Integer.parseInt((String) data));
                            break;
                        case 2:
                            table.getListItems().get(row).setYAxis(Integer.parseInt((String) data));
                            break;
                        case 3:
                            table.getListItems().get(row).setTimeToNext(Integer.parseInt((String) data));
                            break;
                    }
                }
            }
        });

        btnListener();

        globalKeyListener();
    }
    public void btnListener(){
        btn.getInfiniteRBtn().addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                infinite = e.getStateChange() == ItemEvent.SELECTED;
                label.getRepeatTimesTxt().setEnabled(!(e.getStateChange() == ItemEvent.SELECTED));
            }
        });

        btn.getHideMainFrameRBtn().addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                MAIN_FRAME.setHideFrame(e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        label.getRepeatTimesTxt().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String input = label.getRepeatTimesTxt().getText();
                try {
                    repeatTimes = Integer.parseInt(input);
                } catch (NumberFormatException ex) {
                    System.out.println("Invalid input: Please enter a valid integer.");
                }
            }
        });

        btn.getAddClickBtn().addActionListener(e -> showAddPointFrame());

        btn.getStartBtn().addActionListener(e -> startClick());

        btn.getDeleteBtn().addActionListener(e -> deleteSelected());

        btn.getResetBtn().addActionListener(e -> resetData());

        btn.getStopBtn().addActionListener(e -> stopClick());

        btn.getConfirmBtn().addActionListener(e -> {
            changeFrame(true, "");
        });
    }

    public void addToPanel(){
        panel.addComponentsToPanel(panel.getBtnAddPanel(), btn.getAddClickBtn(), btn.getAddKeyBtn(), btn.getDeleteBtn(), btn.getResetBtn());
        panel.addComponentsToPanel(panel.getBtnControlPanel(), btn.getStartBtn(), btn.getStopBtn(), btn.getSaveBtn(), btn.getLoadBtn());
        panel.addComponentsToPanel(panel.getLoopPanel(), label.getRepeatTimesLabel(), label.getRepeatTimesTxt(), btn.getInfiniteRBtn(), btn.getHideMainFrameRBtn());

        panel.getTopPanel().setLayout(new BorderLayout());
        panel.getTopPanel().add(panel.getBtnAddPanel(), BorderLayout.NORTH);
        panel.getTopPanel().add(panel.getBtnControlPanel(), BorderLayout.SOUTH);

        MAIN_FRAME.getMainFrame().getContentPane().setLayout(new BorderLayout());
        MAIN_FRAME.getMainFrame().getContentPane().add(panel.getTopPanel(), BorderLayout.NORTH);
        MAIN_FRAME.getMainFrame().getContentPane().add(panel.getScrollPane(), BorderLayout.CENTER);

        panel.getBottomPanel().setLayout(new BorderLayout());
        panel.getBottomPanel().add(panel.getLoopPanel(), BorderLayout.NORTH);

        MAIN_FRAME.getMainFrame().getContentPane().add(panel.getBottomPanel(), BorderLayout.SOUTH);
        MAIN_FRAME.getMainFrame().setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        panel.getMsgPanel().add(label.getMessageTxt(), BorderLayout.CENTER);
        panel.getMsgPanel().add(btn.getConfirmBtn(), BorderLayout.SOUTH);
        SUB_FRAME.getSubFrame().getContentPane().setLayout(new BorderLayout());
        SUB_FRAME.getSubFrame().getContentPane().add(panel.getMsgPanel(), BorderLayout.CENTER);
    }

    @Override
    public void stopClick() {
        isStopClick = true;
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    @Override
    public void resetData() {
        if (table.getTable().isEditing()) {
            table.getTable().getCellEditor().stopCellEditing();
        }
        table.clearTable();;
        infinite = false;
        repeatTimes = 0;
        btn.getInfiniteRBtn().setSelected(false);
        label.getRepeatTimesTxt().setText("");
        table.getModel().setRowCount(0);
    }

    @Override
    public void deleteSelected() {
        if(table.getTable().getSelectedRows().length < 1){
            String msg = "";
            if(table.getListItems().isEmpty()) {
                msg = "Fuck you, list is empty";
            } else {
                msg = "Fuck you, select row to delete";
            }
            changeFrame(false, msg);

        }
        table.deleteSelectedRows();
    }

    @Override
    public void startClick() {
        if(!table.getListItems().isEmpty()){
            if(!infinite && ("").equals(label.getRepeatTimesTxt().getText())){
                changeFrame(false, "Fuck you. Enter repeat times");
            } else if (infinite) {
                label.getRepeatTimesTxt().setEnabled(false);
                isStopClick = false;
                MAIN_FRAME.getMainFrame().setVisible(!MAIN_FRAME.isHideFrame());
                btn.getInfiniteRBtn().setEnabled(false);
                infiniteAutoClick();
            } else {
                String input = label.getRepeatTimesTxt().getText();
                try {
                    repeatTimes = Integer.parseInt(input);
                } catch (NumberFormatException ex) {
                    changeFrame(false, "Fuck you. Enter a valid integer");
                }
                if(repeatTimes > 0) {
                    isStopClick = false;
                    label.getRepeatTimesTxt().setEnabled(false);
                    MAIN_FRAME.getMainFrame().setVisible(!MAIN_FRAME.isHideFrame());
                    btn.getInfiniteRBtn().setEnabled(false);
                    limitedAutoClick();
                }
            }
        } else{
            changeFrame(false, "Fuck, add position");
        }
    }

    @Override
    public void infiniteAutoClick() {
        new Thread(() -> {
            try {
                synchronized (lock) {
                    while (!isStopClick) {
                        for (ClickItem item: table.getListItems()) {
                            if (isStopClick) {
                                break;
                            }
                            performClick(item);
                            performPress();
                            lock.wait(item.getTimeToNext());
                        }
                    }
                    btn.getInfiniteRBtn().setEnabled(true);
                    label.getRepeatTimesLabel().setEnabled(true);
                }
            } catch (AWTException | InterruptedException ex) {
                Logger.getLogger(AutoClicker.class.getName()).log(Level.SEVERE, "Error in auto click", ex);
            }
        }).start();
    }

    @Override
    public void limitedAutoClick() {
        new Thread(() -> {
            try {
                synchronized (lock) {
                    while (!isStopClick && repeatTimes > 0) {
                        for (ClickItem item: table.getListItems()) {
                            if (isStopClick) {
                                break;
                            }
                            performClick(item);
//                            performPress();
                            lock.wait(item.getTimeToNext());
                            CountDownLatch latch = new CountDownLatch(1);
                            performInput("Hello  World 123", latch);
                            latch.await();
                        }
                        repeatTimes--;
                    }
                    btn.getInfiniteRBtn().setEnabled(true);
                    label.getRepeatTimesLabel().setEnabled(true);
                    MAIN_FRAME.getMainFrame().setVisible(true);
                    label.getRepeatTimesTxt().setEnabled(true);
                }
            } catch (AWTException | InterruptedException ex) {
                Logger.getLogger(AutoClicker.class.getName()).log(Level.SEVERE, "Error in auto click", ex);
            }
        }).start();
    }

    @Override
    public void performClick(ClickItem clickItem) throws AWTException {
        try {
            Robot robot = new Robot();
            robot.mouseMove(clickItem.getXAxis(), clickItem.getYAxis());
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        } catch (AWTException e){
            Logger logger = Logger.getLogger(AutoClicker.class.getName());
            logger.log(Level.SEVERE, "An AWTException", e);
        }
    }

    @Override
    public void performPress() throws AWTException, InterruptedException {
        try {
            Robot robot = new Robot();
            robot.keyPress(KeyEvent.VK_A);
            Thread.sleep(500);
            robot.keyRelease(KeyEvent.VK_A);
            robot.keyPress(KeyEvent.VK_D);
            Thread.sleep(500);
            robot.keyRelease(KeyEvent.VK_D);
        } catch (AWTException e){
            stopClick();
            Logger logger = Logger.getLogger(AutoClicker.class.getName());
            logger.log(Level.SEVERE, "An AWTException", e);
        } catch (InterruptedException e){
            stopClick();
            Logger logger = Logger.getLogger(AutoClicker.class.getName());
            logger.log(Level.SEVERE, "An InterruptedException ", e);
        }
    }

    @Override
    public void performInput(String input, CountDownLatch latch) {
        Timer timer = new Timer(100, new ActionListener() {
            int index = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                int keyCode = 0 ;
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


    @Override
    public void showAddPointFrame() {
        JFrame transparentFrame = new JFrame();
        transparentFrame.setUndecorated(true);
        transparentFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        transparentFrame.setBackground(new Color(100, 100, 100, 100));

        transparentFrame.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                table.addItem(new ClickItem(e.getX(), e.getY()));
                transparentFrame.setVisible(false);
            }
        });
        transparentFrame.setVisible(true);
    }

    @Override
    public void globalKeyListener() {
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
                    SwingUtilities.invokeLater(() -> MAIN_FRAME.getMainFrame().setVisible(true));
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

    private void changeFrame(boolean isMainFrmae, String message) {
        MAIN_FRAME.getMainFrame().setEnabled(isMainFrmae);
        SUB_FRAME.getSubFrame().setVisible(!isMainFrmae);
        label.getMessageTxt().setText(message);
    }

    @Override
    public void showAddPressKeyFrame() {

    }
}

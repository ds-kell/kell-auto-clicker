package kell.autoclick;

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
import java.util.logging.Level;
import java.util.logging.Logger;


@Getter
@Setter
public class AutoClicker implements ClickEvent{

    private MainFrame MAIN_FRAME = new MainFrame();

    private SubFrame SUB_FRAME = new SubFrame();

    private Table table = new Table();

    private Button btn = new Button();

    private Panel panel = new Panel(table);

    private LabelF label = new LabelF();

    private boolean isStopClick = false;

    private boolean infinite = false;

    private int repeatTimes = 0;

    public final Object lock = new Object();

    AutoClicker(){
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

        btn.getAddBtn().addActionListener(e -> showTransparentFrame());

        btn.getStartBtn().addActionListener(e -> startClick());

        btn.getDeleteBtn().addActionListener(e -> deleteSelected());

        btn.getResetBtn().addActionListener(e -> resetData());

        btn.getStopBtn().addActionListener(e -> stopClick());

        btn.getConfirmBtn().addActionListener(e -> {
            changeFrame(true, "");
        });
    }

    public void addToPanel(){
        panel.addComponentsToPanel(panel.getBtnPanel(), btn.getAddBtn(), btn.getStartBtn(), btn.getStopBtn(), btn.getDeleteBtn(), btn.getResetBtn());
        panel.addComponentsToPanel(panel.getPanelLoop(), label.getRepeatTimesLabel(), label.getRepeatTimesTxt(), btn.getInfiniteRBtn(), btn.getHideMainFrameRBtn());

        MAIN_FRAME.getMainFrame().getContentPane().setLayout(new BorderLayout());
        MAIN_FRAME.getMainFrame().getContentPane().add(panel.getBtnPanel(), BorderLayout.NORTH);
        MAIN_FRAME.getMainFrame().getContentPane().add(panel.getScrollPane(), BorderLayout.CENTER);

        panel.getBottomPanel().add(panel.getPanelLoop(), BorderLayout.NORTH);

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
            SUB_FRAME.getSubFrame().setVisible(true);
            if(table.getListItems().isEmpty()) {
                label.getMessageTxt().setText("Fuck you, list is empty");
            } else {
                label.getMessageTxt().setText("Fuck you, select row to delete");
            }

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
                        for (ClickPoint m : table.getListItems()) {
                            if (isStopClick) {
                                break;
                            }
                            performClick(m);
                            lock.wait(m.getTimeToNext());
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
                        for (ClickPoint m : table.getListItems()) {
                            if (isStopClick) {
                                break;
                            }
                            performClick(m);
                            lock.wait(m.getTimeToNext());
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
    public void performClick(ClickPoint m) throws AWTException {
        Robot robot = new Robot();
        robot.mouseMove(m.getXAxis(), m.getYAxis());
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }


    @Override
    public void showTransparentFrame() {
        JFrame transparentFrame = new JFrame();
        transparentFrame.setUndecorated(true);
        transparentFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        transparentFrame.setBackground(new Color(100, 100, 100, 100));

        transparentFrame.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                table.addItem(new ClickPoint(e.getX(), e.getY()));
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
}

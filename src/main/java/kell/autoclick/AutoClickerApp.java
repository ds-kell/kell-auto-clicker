package kell.autoclick;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.event.*;

public class AutoClickerApp extends DeclareClass{

    public static void main(String[] args) {

        // Init
        initFrame();
        addPanel();

        // Listener events
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
        confirmBtn.addActionListener(e -> {
            frame.setEnabled(true);
            subFrame.setVisible(false);
        });
        globalKeyListener();

    }
}

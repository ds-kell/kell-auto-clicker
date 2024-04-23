package kell.autoclick;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.Vector;

@Getter
@Setter
public class Table {

    public final Vector<ClickPoint> listItems = new Vector<>();

    private final DefaultTableModel model = new DefaultTableModel();

    private final JTable table = new JTable(model);

    Table(){
        model.addColumn("Point");
        model.addColumn("xAxis");
        model.addColumn("yAxis");
        model.addColumn("Time");
    }

    public void deleteSelectedRows(){
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }

        int[] selectedRows = table.getSelectedRows();
        for (int i = selectedRows.length - 1; i >= 0; i--) {
            model.removeRow(selectedRows[i]);
            listItems.remove(selectedRows[i]);
        }
    }

    public void clearTable(){
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        listItems.clear();
        model.setRowCount(0);
    }

    public void addItem(ClickPoint point){
        listItems.add(point);
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.addRow(new Object[]{point.getName(), point.getXAxis(), point.getYAxis(), listItems.get(listItems.size() - 1).getTimeToNext()});
    }
}

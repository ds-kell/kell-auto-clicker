package kell.autoclick.table;


import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.Vector;

@Getter
@Setter
public abstract class TableItems<T> {
    public final Vector<T> listItems = new Vector<>();

    public final DefaultTableModel model = new DefaultTableModel();

    public final JTable table = new JTable(model);

    public TableItems(){
        model.addColumn("Name");
        model.addColumn("Hold time");
        model.addColumn("Time to next");
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

    public abstract void addItem(T item);
}

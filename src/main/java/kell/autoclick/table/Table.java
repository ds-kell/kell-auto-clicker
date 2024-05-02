package kell.autoclick.table;

import kell.autoclick.model.ClickItem;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.Vector;

@Getter
@Setter
public class Table {

    public final Vector<ClickItem> listClickItems = new Vector<>();

    private final DefaultTableModel model = new DefaultTableModel();

    private final JTable table = new JTable(model);

    public Table(){
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
            listClickItems.remove(selectedRows[i]);
        }
    }

    public void clearTable(){
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        listClickItems.clear();
        model.setRowCount(0);
    }

    public void addItem(ClickItem clickItem){
        listClickItems.add(clickItem);
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.addRow(new Object[]{clickItem.getName(), clickItem.getXAxis(), clickItem.getYAxis(), listClickItems.get(listClickItems.size() - 1).getTimeToNext()});
    }
}

package kell.autoclick.table;


import kell.autoclick.model.ClickItem;
import kell.autoclick.model.Item;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.Vector;

@Getter
@Setter
public class TableItems {
    public final Vector<ClickItem> listItems = new Vector<>();

    public final DefaultTableModel model = new DefaultTableModel();

    public final JTable table = new JTable(model);

    public TableItems(){
        model.addColumn("Name");
        model.addColumn("xAxis");
        model.addColumn("yAxis");
        model.addColumn("Delay");
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

    public void addItem(ClickItem item){
        listItems.add(item);
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.addRow(new Object[]{ item.getName(), item.getXAxis(), item.getYAxis(), item.getTimeToNext()});
    };

    public void setListItems(Vector<ClickItem> listItems) {
        this.listItems.clear();
        this.listItems.addAll(listItems);
        model.setRowCount(0);
        for(ClickItem item : listItems){
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.addRow(new Object[]{ item.getName(), item.getXAxis(), item.getYAxis(), item.getTimeToNext()});
        }
    }
}

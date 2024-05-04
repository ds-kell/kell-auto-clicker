package kell.autoclick.table;


import kell.autoclick.model.ItemClick;
import kell.autoclick.model.Item;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.Vector;

@Getter
@Setter
public class TableItems {
    public final Vector<Item> listItems = new Vector<>();

    public final DefaultTableModel model = new DefaultTableModel();

    public final JTable table = new JTable(model);

    public TableItems(){
        model.addColumn("Name");
        model.addColumn("press Time");
        model.addColumn("Next Time");
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

    public void addItem(Item item){
        listItems.add(item);
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.addRow(new Object[]{ item.getName(), item.getPressTime(), item.getNextTime()});
    };

    public void setListItems(Vector<Item> listItems) {
        this.listItems.clear();
        this.listItems.addAll(listItems);
        model.setRowCount(0);
        for(Item item : listItems){
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.addRow(new Object[]{ item.getName(), item.getPressTime(), item.getNextTime()});
        }
    }
}

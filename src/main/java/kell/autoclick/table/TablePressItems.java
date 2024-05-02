package kell.autoclick.table;

import kell.autoclick.model.PressItem;
import lombok.Getter;
import lombok.Setter;

import javax.swing.table.DefaultTableModel;

@Getter
@Setter
public class TablePressItems extends TableItems<PressItem> {

    public TablePressItems(){
        model.addColumn("Key");
        model.addColumn("Press time");
        model.addColumn("Time");
    }

    @Override
    public void addItem(PressItem item) {
        listItems.add(item);
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.addRow(new Object[]{item.getName(), item.getHoldTime(), item.getTimeToNext()});

    }
}

package kell.autoclick.table;

import kell.autoclick.model.ClickItem;
import lombok.Getter;
import lombok.Setter;

import javax.swing.table.DefaultTableModel;


@Getter
@Setter
public class TableClickItems extends TableItems<ClickItem>{

    @Override
    public void addItem(ClickItem item) {
        listItems.add(item);
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.addRow(new Object[]{item.getName(), item.getHoldTime(), item.getTimeToNext()});

    }
}

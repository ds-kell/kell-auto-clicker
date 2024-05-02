package kell.autoclick.panel;

import kell.autoclick.table.Table;
import kell.autoclick.table.TableClickItems;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

@Getter
@Setter
public class Panel {

    private JScrollPane scrollPane;

    private final  JPanel topPanel = new JPanel(new FlowLayout());

    private final  JPanel btnControlPanel = new JPanel(new FlowLayout());

    private final  JPanel btnAddPanel = new JPanel(new FlowLayout());

    private final  JPanel loopPanel = new JPanel(new FlowLayout());

    private final  JPanel bottomPanel = new JPanel(new BorderLayout());

    private final  JPanel msgPanel = new JPanel(new BorderLayout());

    public Panel(TableClickItems table) {
        scrollPane = new JScrollPane(table.getTable());
    }

    public void addComponentsToPanel(JPanel panel, JComponent... components) {
        for (JComponent component : components) {
            panel.add(component);
        }
    }
}

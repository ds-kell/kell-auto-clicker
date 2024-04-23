package kell.autoclick;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

@Getter
@Setter
public class Panel {

    private JScrollPane scrollPane;

    private final  JPanel btnPanel = new JPanel(new FlowLayout());

    private final  JPanel panelLoop = new JPanel(new FlowLayout());

    private final  JPanel bottomPanel = new JPanel(new BorderLayout());

    private final  JPanel msgPanel = new JPanel(new BorderLayout());

    public Panel(Table table) {
        scrollPane = new JScrollPane(table.getTable());
    }

    public void addComponentsToPanel(JPanel panel, JComponent... components) {
        for (JComponent component : components) {
            panel.add(component);
        }
    }
}

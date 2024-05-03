package kell.autoclick.model;

import kell.autoclick.model.Item;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClickItem extends Item {

    private int xAxis;

    private int yAxis;

    public ClickItem(int xAxis, int yAxis) {
        super("Point(" + xAxis + ", " + yAxis + ")");
        this.xAxis = xAxis;
        this.yAxis = yAxis;
    }
}

package kell.autoclick.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemClick extends Item {

    private int xAxis;

    private int yAxis;

    public ItemClick(int xAxis, int yAxis) {
        super("Click("+ xAxis + ", " + yAxis +")");
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        this.isClick = true;
    }
    public ItemClick(String name, int xAxis, int yAxis, int pressTime, int nextTime) {
        super(name, pressTime, nextTime);
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        this.isClick = true;
    }

    @Override
    public String toString() {
        return name + " " + xAxis + " " + yAxis + " " + pressTime + " " + nextTime;
    }
}

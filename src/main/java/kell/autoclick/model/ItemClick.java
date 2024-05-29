package kell.autoclick.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemClick extends Item {

    private int xAxis;

    private int yAxis;

    public ItemClick(int xAxis, int yAxis) {
        super(xAxis + ", " + yAxis, GlobalConst.EVENT_CLICK);
        this.xAxis = xAxis;
        this.yAxis = yAxis;
    }
    public ItemClick(String name, int xAxis, int yAxis, int pressTime, int nextTime) {
        super(name, pressTime, nextTime, GlobalConst.EVENT_CLICK);
        this.xAxis = xAxis;
        this.yAxis = yAxis;
    }

    public ItemClick(String name, int xAxis, int yAxis, int pressTime, int nextTime, int typeEvent) {
        super(name, pressTime, nextTime, typeEvent);
        this.xAxis = xAxis;
        this.yAxis = yAxis;
    }


    @Override
    public String toString() {
        return name + " " + xAxis + " " + yAxis + " " + pressTime + " " + nextTime;
    }
}

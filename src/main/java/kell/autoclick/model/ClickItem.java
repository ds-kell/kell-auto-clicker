package kell.autoclick.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClickItem extends Item {

    private int xAxis;

    private int yAxis;

    public ClickItem(int xAxis, int yAxis) {
        super("Coor");
        this.xAxis = xAxis;
        this.yAxis = yAxis;
    }
    public ClickItem(String name, int xAxis, int yAxis, int holdTime, int timeToNext) {
        super(name, holdTime, timeToNext);
        this.xAxis = xAxis;
        this.yAxis = yAxis;
    }

    @Override
    public String toString() {
        return name + " " + xAxis + " " + yAxis + " " + holdTime + " " + timeToNext;
    }
}

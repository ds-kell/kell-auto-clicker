package kell.autoclick;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClickPoint {

    private String name = "Point";

    private int xAxis;

    private int yAxis;

    private int timeToNext = 1000;

    public ClickPoint(int xAxis, int yAxis) {
        this.xAxis = xAxis;
        this.yAxis = yAxis;
    }
}

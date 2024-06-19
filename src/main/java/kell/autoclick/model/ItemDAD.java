package kell.autoclick.model;

public class ItemDAD extends Item{
    private int xAxis1;

    private int yAxis1;

    private int xAxis2;

    private int yAxis2;

    public ItemDAD(int xAxis1, int yAxis1, int xAxis2, int yAxis2, int pressTime, int nextTime) {
        super(xAxis1 + ", " + yAxis1 + "-" + xAxis2 + ", " + yAxis2, pressTime, nextTime, GlobalConst.EVENT_DAD);
        this.xAxis1 = xAxis1;
        this.yAxis1 = yAxis1;

        this.xAxis2 = xAxis2;
        this.yAxis2 = yAxis2;
    }
}

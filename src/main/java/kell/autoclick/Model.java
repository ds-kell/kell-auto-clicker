package kell.autoclick;

public class Model {
    private int xAxis;

    private int yAxis;

    private int timeToNext;

    public Model(int xAxis, int yAxis) {
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        this.timeToNext = 1000;
    }
    public Model(int xAxis, int yAxis, int timeToNext) {
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        this.timeToNext = timeToNext;
    }

    public void setXAxis( int value ) {
        this.xAxis = value;
    }
    public int getXAxis() { return xAxis; }

    public void setYAxis( int value ) {
        this.yAxis = value;
    }
    public int getYAxis() { return yAxis; }

    public void setTimeToNext(int timeToNext) {
        this.timeToNext = timeToNext;
    }

    public int getTimeToNext(){
        return timeToNext;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(xAxis);
        sb.append(", ");
        sb.append(yAxis);
        sb.append(", ");
        sb.append(timeToNext);
        return sb.toString();
    }
}

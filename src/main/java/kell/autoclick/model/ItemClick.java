package kell.autoclick.model;

import java.awt.event.InputEvent;

public class ItemClick extends Item {

    private int    xAxis;
    private int    yAxis;
    /** "Left" or "Right" — human-readable, stored in JSON as-is. */
    private String button;

    public ItemClick(int xAxis, int yAxis) {
        super(xAxis + ", " + yAxis, GlobalConst.EVENT_CLICK);
        this.xAxis  = xAxis;
        this.yAxis  = yAxis;
        this.button = "Left";
    }

    public ItemClick(int xAxis, int yAxis, String button) {
        super(xAxis + ", " + yAxis, GlobalConst.EVENT_CLICK);
        this.xAxis  = xAxis;
        this.yAxis  = yAxis;
        this.button = button != null ? button : "Left";
    }

    public ItemClick(String name, int xAxis, int yAxis, int pressTime, int nextTime) {
        super(name, pressTime, nextTime, GlobalConst.EVENT_CLICK);
        this.xAxis  = xAxis;
        this.yAxis  = yAxis;
        this.button = "Left";
    }

    public ItemClick(String name, int xAxis, int yAxis, int pressTime, int nextTime, String button) {
        super(name, pressTime, nextTime, GlobalConst.EVENT_CLICK);
        this.xAxis  = xAxis;
        this.yAxis  = yAxis;
        this.button = button != null ? button : "Left";
    }

    public int    getXAxis()           { return xAxis; }
    public void   setXAxis(int x)      { this.xAxis = x; }
    public int    getYAxis()           { return yAxis; }
    public void   setYAxis(int y)      { this.yAxis = y; }
    public String getButton()          { return button; }
    public void   setButton(String b)  { this.button = b != null ? b : "Left"; }

    public boolean isRightClick() { return "Right".equalsIgnoreCase(button); }

    /** AWT mask for use with Robot. */
    public int getAwtButtonMask() {
        return isRightClick() ? InputEvent.BUTTON3_DOWN_MASK : InputEvent.BUTTON1_DOWN_MASK;
    }
}

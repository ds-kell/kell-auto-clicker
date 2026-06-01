package kell.autoclick.model;

import kell.autoclick.contants.GlobalConst;

public class ItemPress extends Item {

    private int     keyCode;
    private boolean multiPress;

    public ItemPress(String name, int keyCode, int pressTime, int nextTime, boolean multiPress) {
        super(name, pressTime, nextTime, GlobalConst.EVENT_PRESS);
        this.keyCode    = keyCode;
        this.multiPress = multiPress;
    }

    public int     getKeyCode()           { return keyCode; }
    public void    setKeyCode(int kc)     { this.keyCode = kc; }

    public boolean isMultiPress()         { return multiPress; }
    public void    setMultiPress(boolean m) { this.multiPress = m; }
}

package kell.autoclick.model;

import kell.autoclick.contants.GlobalConst;

public class ItemPress extends Item {

    private int     keyCode;
    private boolean multiPress;
    /** null for normal key press, "down"/"up" for event-level recordings. */
    private String  keyAction;

    public ItemPress(String name, int keyCode, int pressTime, int nextTime, boolean multiPress) {
        super(name, pressTime, nextTime, GlobalConst.EVENT_PRESS);
        this.keyCode    = keyCode;
        this.multiPress = multiPress;
    }

    public int     getKeyCode()           { return keyCode; }
    public void    setKeyCode(int kc)     { this.keyCode = kc; }

    public boolean isMultiPress()         { return multiPress; }
    public void    setMultiPress(boolean m) { this.multiPress = m; }

    public String getKeyAction()            { return keyAction; }
    public void   setKeyAction(String action) { this.keyAction = action; }
    public boolean isKeyDownEvent()         { return "down".equalsIgnoreCase(keyAction); }
    public boolean isKeyUpEvent()           { return "up".equalsIgnoreCase(keyAction); }
}

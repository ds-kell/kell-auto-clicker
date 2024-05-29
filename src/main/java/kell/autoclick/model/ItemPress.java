package kell.autoclick.model;

import lombok.Getter;
import lombok.Setter;

import java.awt.event.KeyEvent;
import java.util.List;

@Getter
@Setter
public class ItemPress extends Item {

    private int keyCode;

    private List<Integer> keyCodes;

    private boolean isMultiPress;

    public ItemPress(String name, int keyCode, int pressTime, int nextTime, boolean isMultiPress) {
        super(name, pressTime, nextTime, GlobalConst.EVENT_PRESS);
        this.keyCode = keyCode;
        this.isMultiPress = isMultiPress;
    }

    public ItemPress(String name, int keyCode, int pressTime, int nextTime, boolean isMultiPress, int typeEvent) {
        super(name, pressTime, nextTime, typeEvent);
        this.keyCode = keyCode;
        this.isMultiPress = isMultiPress;
    }
    public ItemPress(String name, int pressTime, int nextTime, boolean isMultiPress) {
        super(name, pressTime, nextTime);
        for(Character c: name.toCharArray()) {
            int keyCode = KeyEvent.getExtendedKeyCodeForChar(c);
            if (Character.isLetterOrDigit(c) || Character.isSpaceChar(c)) {
                keyCodes.add(keyCode);
            }
        }
        this.isMultiPress = isMultiPress;
    }
}

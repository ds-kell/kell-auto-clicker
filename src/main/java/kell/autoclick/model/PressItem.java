package kell.autoclick.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PressItem extends Item {

    private int keyCode;

    private long pressTime = 100;

    public PressItem(int keyCode) {
        super("Key");
        this.keyCode = keyCode;
    }
}

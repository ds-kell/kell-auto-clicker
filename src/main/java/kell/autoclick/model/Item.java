package kell.autoclick.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Item {

    protected String name;

    protected int pressTime = 0;

    protected int nextTime = 1000;

    protected Boolean isClick;

    public Item(String name) {
        this.name = name;
    }

    public Item(String name, int pressTime, int nextTime) {
        this.name = "Press(" + name.toUpperCase() + ")";
        this.pressTime = pressTime;
        this.nextTime = nextTime;
    }
}

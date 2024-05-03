package kell.autoclick.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Item {

    protected String name;

    protected int holdTime = 0;

    protected int timeToNext = 1000;

    public Item(String name) {
        this.name = name;
    }

    public Item(String name, int holdTime, int timeToNext) {
        this.name = name;
        this.holdTime = holdTime;
        this.timeToNext = timeToNext;
    }
}

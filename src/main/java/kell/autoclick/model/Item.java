package kell.autoclick.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Item {

    protected String name;

    protected int pressTime = 0;

    protected int nextTime = 100;

    protected int typeEvent = 0;

    public Item(String name, int typeEvent) {
        this.typeEvent = typeEvent;
        this.name = name;
    }

    public Item(String name, int pressTime, int nextTime) {
        this.name = name;
        this.pressTime = pressTime;
        this.nextTime = nextTime;
    }

    public Item(String name, int pressTime, int nextTime, int typeEvent) {
        this.name = name;
        this.pressTime = pressTime;
        this.nextTime = nextTime;
        this.typeEvent = typeEvent;
    }
}

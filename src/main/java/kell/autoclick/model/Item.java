package kell.autoclick.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Item {

    private String name;

    private int holdTime = 0;

    private int timeToNext = 1000;

    public Item(String name) {
        this.name = name;
    }
}

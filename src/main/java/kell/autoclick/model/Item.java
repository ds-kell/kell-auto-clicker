package kell.autoclick.model;

public class Item {

    protected String name;
    protected int pressTime = 0;
    protected int nextTime  = 100;
    protected String typeEvent = "";
    protected String sourceFile = null;
    protected boolean recorded = false;

    public Item(String name, String typeEvent) {
        this.typeEvent = typeEvent;
        this.name = name;
    }

    public Item(String name, int pressTime, int nextTime) {
        this.name = name;
        this.pressTime = pressTime;
        this.nextTime = nextTime;
    }

    public Item(String name, int pressTime, int nextTime, String typeEvent) {
        this.name = name;
        this.pressTime = pressTime;
        this.nextTime = nextTime;
        this.typeEvent = typeEvent;
    }

    public String getName()          { return name; }
    public void   setName(String n)  { this.name = n; }

    public int  getPressTime()       { return pressTime; }
    public void setPressTime(int t)  { this.pressTime = t; }

    public int  getNextTime()        { return nextTime; }
    public void setNextTime(int t)   { this.nextTime = t; }

    public String getTypeEvent()         { return typeEvent; }
    public void   setTypeEvent(String t) { this.typeEvent = t; }

    public String getSourceFile()          { return sourceFile; }
    public void   setSourceFile(String sf) { this.sourceFile = sf; }

    public boolean isRecorded()             { return recorded; }
    public void    setRecorded(boolean val) { this.recorded = val; }
}

package kell.autoclick.button;

import lombok.Getter;

import javax.swing.*;
@Getter
public class Button {
    private final JButton addClickBtn = new JButton("Add Click");

    private final JButton addKeyBtn = new JButton("Add Key");

    private final JButton startBtn = new JButton("Start-(F7)");
    
    private final JButton stopBtn = new JButton("Stop-(F8)");
    
    private final JButton resetBtn = new JButton("Reset");
    
    private final JButton deleteBtn = new JButton("Delete");
    
    private final JButton saveBtn = new JButton("Save");
    
    private final JButton loadBtn = new JButton("Load");
    
    private final JButton confirmBtn = new JButton("OK");
    
    private final JRadioButton hideMainFrameRBtn = new JRadioButton("Hide frame");

    private final JRadioButton infiniteRBtn = new JRadioButton("Until stop");

    public Button(){

    }
}

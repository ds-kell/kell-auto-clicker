package kell.autoclick.frame;

import kell.autoclick.custom.CButton;
import kell.autoclick.custom.CFrame;
import kell.autoclick.table.TableItems;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MainFrame extends CFrame {

    private static final Font UI_FONT      = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font UI_FONT_BOLD = new Font("Segoe UI", Font.BOLD,  13);

    // ── Add-item buttons ──────────────────────────────────────────────────────
    private final JButton addClickBtn   = new CButton("Add Click");
    private final JButton addKeyBtn     = new CButton("Add Key  (soon)");
    private final JButton addScrollBtn  = new CButton("Add Scroll");
    private final JButton addDADBtn     = new CButton("Add Drag & Drop");
    private final JButton addCommentBtn = new CButton("Add Note");

    // ── Row-order / edit buttons ──────────────────────────────────────────────
    private final JButton moveUpBtn   = new CButton("Move Up");
    private final JButton moveDownBtn = new CButton("Move Down");
    private final JButton deleteBtn   = new CButton("Delete Row");
    private final JButton resetBtn    = new CButton("Clear All");

    // ── Run controls ──────────────────────────────────────────────────────────
    private final JButton startBtn = new CButton("Start (F7)");
    private final JButton stopBtn  = new CButton("Stop (F8)");

    // ── File controls ─────────────────────────────────────────────────────────
    private final JButton saveBtn   = new CButton("Save");
    private final JButton loadBtn   = new CButton("Load");
    private final JButton appendBtn = new CButton("Append File");

    // ── Repeat mode ───────────────────────────────────────────────────────────
    private final JRadioButton repeatByCountRBtn = new JRadioButton("Repeat:", true);
    private final JRadioButton repeatByTimeRBtn  = new JRadioButton("Run for:");
    private final JRadioButton infiniteRBtn      = new JRadioButton("Run until stopped");
    private final JRadioButton hideMainFrameRBtn = new JRadioButton("Hide window while running");

    public final JTextField repeatTimesTxt = new JTextField(7);
    public final JTextField hoursField     = new JTextField(3);
    public final JTextField minutesField   = new JTextField(3);
    public final JTextField secondsField   = new JTextField(3);

    // ── Status bar ────────────────────────────────────────────────────────────
    private final JLabel progressLabel = new JLabel("Ready");

    // ── Table ─────────────────────────────────────────────────────────────────
    private final TableItems tableItems = new TableItems();

    private boolean hideFrame = true;

    public MainFrame() {
        super(860, 660);
        frame.setTitle("Kell Auto Clicker");
    }

    public void init() {
        hideMainFrameRBtn.setSelected(hideFrame);
        applyTheme();

        ButtonGroup repeatGroup = new ButtonGroup();
        repeatGroup.add(repeatByCountRBtn);
        repeatGroup.add(repeatByTimeRBtn);
        repeatGroup.add(infiniteRBtn);

        hoursField.setText("0");
        minutesField.setText("0");
        secondsField.setText("0");
        hoursField.setEnabled(false);
        minutesField.setEnabled(false);
        secondsField.setEnabled(false);

        repeatByCountRBtn.addActionListener(e -> {
            repeatTimesTxt.setEnabled(true);
            hoursField.setEnabled(false);
            minutesField.setEnabled(false);
            secondsField.setEnabled(false);
        });
        repeatByTimeRBtn.addActionListener(e -> {
            repeatTimesTxt.setEnabled(false);
            hoursField.setEnabled(true);
            minutesField.setEnabled(true);
            secondsField.setEnabled(true);
        });
        infiniteRBtn.addActionListener(e -> {
            repeatTimesTxt.setEnabled(false);
            hoursField.setEnabled(false);
            minutesField.setEnabled(false);
            secondsField.setEnabled(false);
        });

        // Row 1: add-item buttons
        JPanel row1 = centeredRow();
        row1.add(addClickBtn);
        row1.add(addKeyBtn);
        row1.add(addScrollBtn);
        row1.add(addDADBtn);
        row1.add(addCommentBtn);

        // Row 2: row-management buttons
        JPanel row2 = centeredRow();
        row2.add(moveUpBtn);
        row2.add(moveDownBtn);
        row2.add(deleteBtn);
        row2.add(resetBtn);

        // Row 3: run + file controls
        JPanel row3 = centeredRow();
        row3.add(startBtn);
        row3.add(stopBtn);
        row3.add(saveBtn);
        row3.add(loadBtn);
        row3.add(appendBtn);

        // Row 4: repeat by count
        JPanel row4 = centeredRow();
        row4.add(repeatByCountRBtn);
        row4.add(repeatTimesTxt);
        row4.add(label("times"));
        row4.add(hgap(16));
        row4.add(repeatByTimeRBtn);
        row4.add(hoursField);
        row4.add(label("h"));
        row4.add(minutesField);
        row4.add(label("m"));
        row4.add(secondsField);
        row4.add(label("s"));
        row4.add(hgap(16));
        row4.add(infiniteRBtn);

        // Row 5: hide window option (separate context from repeat settings)
        JPanel row5 = centeredRow();
        row5.add(hideMainFrameRBtn);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBorder(new EmptyBorder(4, 6, 4, 6));
        topPanel.add(row1);
        topPanel.add(row2);
        topPanel.add(row3);
        topPanel.add(row4);
        topPanel.add(row5);

        JScrollPane scrollPane = new JScrollPane(tableItems.getTable());

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(new EmptyBorder(3, 8, 3, 8));
        progressLabel.setFont(UI_FONT_BOLD);
        progressLabel.setFocusable(false);
        statusBar.add(progressLabel, BorderLayout.WEST);

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(topPanel, BorderLayout.NORTH);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        frame.getContentPane().add(statusBar, BorderLayout.SOUTH);

        frame.getContentPane().requestFocus();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    // ── Progress updates ──────────────────────────────────────────────────────
    public void updateProgress(long current, long total) {
        SwingUtilities.invokeLater(() ->
            progressLabel.setText("Executed: " + current + " / " + (total < 0 ? "unlimited" : total) + " cycles"));
    }

    public void updateProgressTime(long current, long remainSec) {
        SwingUtilities.invokeLater(() -> {
            long h = remainSec / 3600, m = (remainSec % 3600) / 60, s = remainSec % 60;
            progressLabel.setText(String.format("Executed: %d cycles  |  Time remaining: %02d:%02d:%02d", current, h, m, s));
        });
    }

    public void setProgressText(String text) {
        SwingUtilities.invokeLater(() -> progressLabel.setText(text));
    }

    // ── Theme ─────────────────────────────────────────────────────────────────
    private void applyTheme() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        // Uniform font BOLD for radio buttons
        for (JRadioButton rb : new JRadioButton[]{
                repeatByCountRBtn, repeatByTimeRBtn, infiniteRBtn, hideMainFrameRBtn}) {
            rb.setFont(UI_FONT_BOLD);
            rb.setFocusable(false);
        }

        // All buttons: bold font
        for (JButton btn : new JButton[]{addClickBtn, addKeyBtn, addScrollBtn, addDADBtn,
                addCommentBtn, moveUpBtn, moveDownBtn, deleteBtn, resetBtn,
                startBtn, stopBtn, saveBtn, loadBtn, appendBtn}) {
            btn.setFont(UI_FONT_BOLD);
        }

        // Input fields: right-aligned, consistent size
        for (JTextField f : new JTextField[]{repeatTimesTxt, hoursField, minutesField, secondsField}) {
            f.setFont(UI_FONT);
            f.setHorizontalAlignment(JTextField.RIGHT);
            f.setPreferredSize(new Dimension(f.getPreferredSize().width, 28));
        }

        startBtn.setBackground(new Color(0x2E, 0x7D, 0x32));
        startBtn.setForeground(Color.WHITE);
        startBtn.setOpaque(true);

        stopBtn.setBackground(new Color(0xC6, 0x28, 0x28));
        stopBtn.setForeground(Color.WHITE);
        stopBtn.setOpaque(true);

        Color blue = new Color(0x15, 0x65, 0xC0);
        for (JButton btn : new JButton[]{saveBtn, loadBtn, appendBtn}) {
            btn.setBackground(blue);
            btn.setForeground(Color.WHITE);
            btn.setOpaque(true);
        }

        // Add Key: show as grayed-out (NOT disabled — disabled hides text on Windows L&F)
        // Use gray color to signal WIP, MouseListener handles toast
        addKeyBtn.setForeground(new Color(0x99, 0x99, 0x99));
        addKeyBtn.setBackground(new Color(0xE0, 0xE0, 0xE0));
        addKeyBtn.setOpaque(true);
        addKeyBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                showToast("Key Press feature is coming soon!");
            }
        });
    }

    /** Show a non-blocking toast at the bottom of the window. */
    public void showToast(String message) {
        JWindow toast = new JWindow(frame);
        JLabel lbl = new JLabel("  " + message + "  ", SwingConstants.CENTER);
        lbl.setFont(UI_FONT);
        lbl.setForeground(Color.WHITE);
        lbl.setOpaque(true);
        lbl.setBackground(new Color(40, 40, 40));
        lbl.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 80, 80)),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        toast.add(lbl);
        toast.pack();
        Point loc = frame.getLocationOnScreen();
        Dimension sz = frame.getSize();
        toast.setLocation(loc.x + (sz.width - toast.getWidth()) / 2, loc.y + sz.height - 70);
        toast.setVisible(true);
        new javax.swing.Timer(2000, e -> { toast.setVisible(false); toast.dispose(); }) {{
            setRepeats(false); start();
        }};
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private JPanel centeredRow() {
        return new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UI_FONT_BOLD);
        l.setFocusable(false);
        return l;
    }

    private Component hgap(int w) {
        return Box.createHorizontalStrut(w);
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public JTextField   getRepeatTimesTxt()      { return repeatTimesTxt; }
    public JTextField   getHoursField()          { return hoursField; }
    public JTextField   getMinutesField()        { return minutesField; }
    public JTextField   getSecondsField()        { return secondsField; }
    public JRadioButton getRepeatByCountRBtn()   { return repeatByCountRBtn; }
    public JRadioButton getRepeatByTimeRBtn()    { return repeatByTimeRBtn; }
    public JRadioButton getInfiniteRBtn()        { return infiniteRBtn; }
    public JRadioButton getHideMainFrameRBtn()   { return hideMainFrameRBtn; }
    public JButton      getAddClickBtn()         { return addClickBtn; }
    public JButton      getAddKeyBtn()           { return addKeyBtn; }
    public JButton      getAddScrollBtn()        { return addScrollBtn; }
    public JButton      getAddDADBtn()           { return addDADBtn; }
    public JButton      getAddCommentBtn()       { return addCommentBtn; }
    public JButton      getMoveUpBtn()           { return moveUpBtn; }
    public JButton      getMoveDownBtn()         { return moveDownBtn; }
    public JButton      getDeleteBtn()           { return deleteBtn; }
    public JButton      getResetBtn()            { return resetBtn; }
    public JButton      getStartBtn()            { return startBtn; }
    public JButton      getStopBtn()             { return stopBtn; }
    public JButton      getSaveBtn()             { return saveBtn; }
    public JButton      getLoadBtn()             { return loadBtn; }
    public JButton      getAppendBtn()           { return appendBtn; }
    public TableItems   getTableItems()          { return tableItems; }
    public boolean      isHideFrame()            { return hideFrame; }
    public void         setHideFrame(boolean b)  { this.hideFrame = b; }
}

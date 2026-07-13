package kell.autoclick.frame;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;

import kell.autoclick.custom.CButton;
import kell.autoclick.custom.CDigitField;
import kell.autoclick.custom.CFrame;
import kell.autoclick.table.TableItems;

import static kell.autoclick.contants.GlobalConst.SYSTEM_FONT;

public class MainFrame extends CFrame {

    private static final Logger LOG = Logger.getLogger(MainFrame.class.getName());

    private static final Font UI_FONT      = new Font(SYSTEM_FONT, Font.PLAIN, 13);
    private static final Font UI_FONT_BOLD = new Font(SYSTEM_FONT, Font.BOLD,  13);

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
    private final JButton recordBtn = new CButton("Record (F9)");

    // ── File controls ─────────────────────────────────────────────────────────
    private final JButton saveBtn   = new CButton("Save");
    private final JButton loadBtn   = new CButton("Load");
    private final JButton appendBtn = new CButton("Append File");

    // ── Repeat mode ───────────────────────────────────────────────────────────
    private final JRadioButton repeatByCountRBtn = new JRadioButton("Repeat", true);
    private final JRadioButton repeatByTimeRBtn  = new JRadioButton("For");
    private final JRadioButton infiniteRBtn      = new JRadioButton("Forever");
    private final JRadioButton hideMainFrameRBtn = new JRadioButton("Hide window while running");

    public final CDigitField repeatTimesTxt = new CDigitField(7);
    public final CDigitField hoursField     = new CDigitField(3);
    public final CDigitField minutesField   = new CDigitField(3);
    public final CDigitField secondsField   = new CDigitField(3);

    // ── Status bar ────────────────────────────────────────────────────────────
    private final JLabel progressLabel = new JLabel("Ready");

    // ── Table ─────────────────────────────────────────────────────────────────
    private final TableItems tableItems = new TableItems();

    private JWindow recordControlWindow;

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
        row3.add(recordBtn);
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

        // Swing key bindings as a fallback for F7/F8 when the global native
        // hook isn't available (e.g. Windows AV blocking jnativehook, or
        // macOS Accessibility permission not granted). These work whenever
        // the main window has focus.
        installHotkeys();
    }

    /**
     * Reusable hotkey wiring on the root pane. The runnables are wired in
     * AutoClicker via {@link #setHotkeyHandlers}.
     */
    private Runnable onF7 = () -> {};
    private Runnable onF8 = () -> {};
    private Runnable onF9 = () -> {};

    public void setHotkeyHandlers(Runnable f7, Runnable f8, Runnable f9) {
        if (f7 != null) this.onF7 = f7;
        if (f8 != null) this.onF8 = f8;
        if (f9 != null) this.onF9 = f9;
    }

    private void installHotkeys() {
        JComponent root = frame.getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();
        im.put(KeyStroke.getKeyStroke("F7"), "kell-start");
        im.put(KeyStroke.getKeyStroke("F8"), "kell-stop");
        im.put(KeyStroke.getKeyStroke("F9"), "kell-record");
        am.put("kell-start", new AbstractAction() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override public void actionPerformed(java.awt.event.ActionEvent e) { onF7.run(); }
        });
        am.put("kell-stop", new AbstractAction() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override public void actionPerformed(java.awt.event.ActionEvent e) { onF8.run(); }
        });
        am.put("kell-record", new AbstractAction() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override public void actionPerformed(java.awt.event.ActionEvent e) { onF9.run(); }
        });
    }

    // ── Progress updates ──────────────────────────────────────────────────────
    public void updateProgress(long current, long total) {
        SwingUtilities.invokeLater(() ->
            progressLabel.setText("Executed: " + current + " / " + (total < 0 ? "unlimited" : total) + " cycles"));
    }

    public void updateProgressTime(long current, long remainSec) {
        SwingUtilities.invokeLater(() -> {
            long h = remainSec / 3600;
            long m = (remainSec % 3600) / 60;
            long s = remainSec % 60;
            progressLabel.setText(String.format(
                    Locale.ROOT,
                    "Executed: %d cycles  |  Time remaining: %02d:%02d:%02d",
                    current,
                    h,
                    m,
                    s
            ));
        });
    }

    public void setProgressText(String text) {
        SwingUtilities.invokeLater(() -> progressLabel.setText(text));
    }

    public void showRecordControl(Runnable onStop) {
        SwingUtilities.invokeLater(() -> {
            hideRecordControl();
            recordControlWindow = new JWindow(frame);
            recordControlWindow.setAlwaysOnTop(true);

            JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xEF, 0x6C, 0x00), 2, true),
                    BorderFactory.createEmptyBorder(2, 6, 2, 6)
            ));
            panel.setBackground(new Color(0x24, 0x24, 0x24));

            JLabel label = new JLabel("● Recording");
            label.setFont(UI_FONT_BOLD);
            label.setForeground(new Color(0xFF, 0xB7, 0x4D));
            label.setFocusable(false);

            JButton stop = new CButton("Stop");
            stop.setFont(UI_FONT_BOLD);
            paintAsFlat(stop, new Color(0xC6, 0x28, 0x28), Color.WHITE);
            stop.addActionListener(_ -> onStop.run());

            panel.add(label);
            panel.add(stop);
            recordControlWindow.getContentPane().add(panel);
            recordControlWindow.pack();
            placeRecordControl();
            recordControlWindow.setVisible(true);
        });
    }

    public void hideRecordControl() {
        if (recordControlWindow != null) {
            recordControlWindow.setVisible(false);
            recordControlWindow.dispose();
            recordControlWindow = null;
        }
    }

    public boolean isInsideRecordControl(int x, int y) {
        return recordControlWindow != null
                && recordControlWindow.isShowing()
                && recordControlWindow.getBounds().contains(x, y);
    }

    // ── Theme ─────────────────────────────────────────────────────────────────
    private void applyTheme() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException
                 | UnsupportedLookAndFeelException
                 | RuntimeException e) {
            LOG.log(Level.FINE, "Falling back to the default look and feel", e);
        }

        // Uniform font BOLD for radio buttons
        for (JRadioButton rb : new JRadioButton[]{
                repeatByCountRBtn, repeatByTimeRBtn, infiniteRBtn, hideMainFrameRBtn}) {
            rb.setFont(UI_FONT_BOLD);
            rb.setFocusable(false);
        }

        // All buttons: bold font
        for (JButton btn : new JButton[]{addClickBtn, addKeyBtn, addScrollBtn, addDADBtn,
                addCommentBtn, moveUpBtn, moveDownBtn, deleteBtn, resetBtn,
                startBtn, stopBtn, recordBtn, saveBtn, loadBtn, appendBtn}) {
            btn.setFont(UI_FONT_BOLD);
        }

        // Input fields: right-aligned, consistent size
        for (JTextField f : new JTextField[]{repeatTimesTxt, hoursField, minutesField, secondsField}) {
            f.setFont(UI_FONT);
            f.setHorizontalAlignment(SwingConstants.RIGHT);
            f.setPreferredSize(new Dimension(f.getPreferredSize().width, 28));
        }

        // Colored action buttons — paint flat so they look identical on Aqua/Windows/GTK.
        paintAsFlat(startBtn, new Color(0x2E, 0x7D, 0x32), Color.WHITE); // green
        paintAsFlat(stopBtn,  new Color(0xC6, 0x28, 0x28), Color.WHITE); // red
        paintAsFlat(recordBtn, new Color(0xEF, 0x6C, 0x00), Color.WHITE); // orange

        Color blue = new Color(0x15, 0x65, 0xC0);
        for (JButton btn : new JButton[]{saveBtn, loadBtn, appendBtn}) {
            paintAsFlat(btn, blue, Color.WHITE);
        }

        // Add Key: WIP placeholder (disabled-look but still clickable to show toast).
        paintAsFlat(addKeyBtn, new Color(0xE0, 0xE0, 0xE0), new Color(0x99, 0x99, 0x99));
        addKeyBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                showToast("Key Press feature is coming soon!");
            }
        });
    }

    /**
     * Force a flat painted button with the given base/text colors plus
     * hover & pressed states. Bypasses the native L&F button delegate so
     * the colors show identically on macOS Aqua, Windows, and GTK.
     */
    private static void paintAsFlat(JButton btn, Color base, Color textColor) {
        btn.setUI(new BasicButtonUI());
        btn.setOpaque(true);
        btn.setBorderPainted(true);
        btn.setContentAreaFilled(true);
        btn.setFocusPainted(false);
        btn.setForeground(textColor);
        btn.setBackground(base);
        // Slightly darker border so it stands out on light backgrounds.
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(darken(base, 0.75f), 1, true),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        Color hover   = darken(base, 0.92f);
        Color pressed = darken(base, 0.80f);
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(hover); }
            @Override public void mouseExited (MouseEvent e) { btn.setBackground(base);  }
            @Override public void mousePressed(MouseEvent e) { btn.setBackground(pressed); }
            @Override public void mouseReleased(MouseEvent e) {
                btn.setBackground(btn.getMousePosition() != null ? hover : base);
            }
        });
    }

    private static Color darken(Color c, float factor) {
        return new Color(
                Math.clamp(Math.round(c.getRed()   * factor), 0, 255),
                Math.clamp(Math.round(c.getGreen() * factor), 0, 255),
                Math.clamp(Math.round(c.getBlue()  * factor), 0, 255));
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
        var timer = new javax.swing.Timer(2000, e -> {
            toast.setVisible(false);
            toast.dispose();
        });
        timer.setRepeats(false);
        timer.start();
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

    private void placeRecordControl() {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int margin = 18;
        recordControlWindow.setLocation(
                screen.width - recordControlWindow.getWidth() - margin,
                margin
        );
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
    public JButton      getRecordBtn()           { return recordBtn; }
    public JButton      getSaveBtn()             { return saveBtn; }
    public JButton      getLoadBtn()             { return loadBtn; }
    public JButton      getAppendBtn()           { return appendBtn; }
    public TableItems   getTableItems()          { return tableItems; }
    public boolean      isHideFrame()            { return hideFrame; }
    public void         setHideFrame(boolean b)  { this.hideFrame = b; }
}

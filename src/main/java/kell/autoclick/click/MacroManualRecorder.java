package kell.autoclick.click;

import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import kell.autoclick.contants.GlobalConst;
import kell.autoclick.frame.AddCommentFrame;
import kell.autoclick.frame.AddDADFrame;
import kell.autoclick.frame.AddPointFrame;
import kell.autoclick.frame.AddPressKeyFrame;
import kell.autoclick.frame.AddScrollFrame;
import kell.autoclick.frame.MainFrame;
import kell.autoclick.model.Item;
import kell.autoclick.model.ItemClick;
import kell.autoclick.model.ItemComment;
import kell.autoclick.model.ItemDAD;
import kell.autoclick.model.ItemPress;

final class MacroManualRecorder {

    private final MainFrame mainFrame;
    private final AddPointFrame addPointFrame;
    private final AddPressKeyFrame addPressFrame;
    private final AddScrollFrame addScrollFrame;
    private final AddDADFrame addDadFrame;
    private final AddCommentFrame addCommentFrame;
    private final Consumer<String> showError;

    MacroManualRecorder(
            MainFrame mainFrame,
            AddPointFrame addPointFrame,
            AddPressKeyFrame addPressFrame,
            AddScrollFrame addScrollFrame,
            AddDADFrame addDadFrame,
            AddCommentFrame addCommentFrame,
            Consumer<String> showError
    ) {
        this.mainFrame = mainFrame;
        this.addPointFrame = addPointFrame;
        this.addPressFrame = addPressFrame;
        this.addScrollFrame = addScrollFrame;
        this.addDadFrame = addDadFrame;
        this.addCommentFrame = addCommentFrame;
        this.showError = showError;
    }

    void bindActions() {
        mainFrame.getAddClickBtn().addActionListener(_ -> showAddPointFrame());
        mainFrame.getAddKeyBtn().addActionListener(_ -> showAddPressFrame(true));
        mainFrame.getAddScrollBtn().addActionListener(_ -> {
            showAddScrollFrame(true);
            addScrollFrame.getWheelAmtTxt().requestFocus();
        });
        mainFrame.getAddDADBtn().addActionListener(_ -> showAddDADFrame(true));
        mainFrame.getAddCommentBtn().addActionListener(_ -> showAddCommentFrame(true));

        addDadFrame.getFromBtn().addActionListener(_ -> pickDADPoint(true));
        addDadFrame.getToBtn().addActionListener(_ -> pickDADPoint(false));
        addDadFrame.getAddBtn().addActionListener(_ -> addDADEvent());
        addDadFrame.getCancelBtn().addActionListener(_ -> showAddDADFrame(false));
        addDadFrame.getDialog().addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent ignoredEvent) { showAddDADFrame(false); }
        });

        addPressFrame.getAddBtn().addActionListener(_ -> addPressKey());
        addPressFrame.getCancelBtn().addActionListener(_ -> showAddPressFrame(false));
        addPressFrame.getIsSinglePressRBtn().addItemListener(e -> {
            addPressFrame.setIsSinglePress(e.getStateChange() == ItemEvent.SELECTED);
            addPressFrame.getIsMultiPressRBtn().setSelected(!addPressFrame.getIsSinglePress());
        });
        addPressFrame.getIsMultiPressRBtn().addItemListener(e -> {
            addPressFrame.setIsSinglePress(e.getStateChange() != ItemEvent.SELECTED);
            addPressFrame.getIsSinglePressRBtn().setSelected(addPressFrame.getIsSinglePress());
        });
        addPressFrame.getDialog().addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent ignoredEvent) { showAddPressFrame(false); }
        });

        addScrollFrame.getAddBtn().addActionListener(_ -> addScrollEvent());
        addScrollFrame.getCancelBtn().addActionListener(_ -> showAddScrollFrame(false));
        addScrollFrame.getDialog().addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent ignoredEvent) { showAddScrollFrame(false); }
        });

        addCommentFrame.getAddBtn().addActionListener(_ -> addComment());
        addCommentFrame.getCancelBtn().addActionListener(_ -> showAddCommentFrame(false));
        addCommentFrame.getDialog().addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent ignoredEvent) { showAddCommentFrame(false); }
        });
    }

    private void showAddPointFrame() {
        addPointFrame.reset();
        mainFrame.getFrame().setVisible(false);
        var mouseAdapter = new MouseAdapter() {
            @Override public void mouseReleased(MouseEvent event) {
                if (addPointFrame.hintBounds().contains(event.getPoint())) {
                    return;
                }

                String button;
                if (SwingUtilities.isRightMouseButton(event)) {
                    button = "Right";
                } else if (SwingUtilities.isLeftMouseButton(event)) {
                    button = "Left";
                } else {
                    return;
                }
                mainFrame.getTableItems().addItem(new ItemClick(event.getX(), event.getY(), button));
                addPointFrame.getFrame().setVisible(false);
                addPointFrame.getFrame().removeMouseListener(this);
                restoreMainFrameAfterPointPick();
            }
        };
        addPointFrame.setOnCancel(() -> {
            addPointFrame.getFrame().removeMouseListener(mouseAdapter);
            restoreMainFrameAfterPointPick();
        });
        addPointFrame.getFrame().addMouseListener(mouseAdapter);
        addPointFrame.getFrame().setVisible(true);
    }

    private void restoreMainFrameAfterPointPick() {
        mainFrame.getFrame().setVisible(true);
        mainFrame.getFrame().toFront();
    }

    private void showAddPressFrame(boolean show) {
        mainFrame.getFrame().setEnabled(!show);
        addPressFrame.setVisible(show);
        if (!show) { mainFrame.getFrame().setEnabled(true); mainFrame.getFrame().toFront(); }
        if (show) addPressFrame.getKeyTxt().requestFocus();
    }

    private void showAddScrollFrame(boolean show) {
        mainFrame.getFrame().setEnabled(!show);
        addScrollFrame.setVisible(show);
        if (!show) { mainFrame.getFrame().setEnabled(true); mainFrame.getFrame().toFront(); }
        if (show) addScrollFrame.getWheelAmtTxt().requestFocus();
    }

    private void showAddDADFrame(boolean show) {
        if (show) addDadFrame.resetCoords();
        mainFrame.getFrame().setEnabled(!show);
        addDadFrame.getDialog().setVisible(show);
        if (!show) { mainFrame.getFrame().setEnabled(true); mainFrame.getFrame().toFront(); }
    }

    private void pickDADPoint(boolean isFrom) {
        addDadFrame.getDialog().setVisible(false);
        addPointFrame.setOnCancel(() -> {});
        var picker = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                addPointFrame.getFrame().setVisible(false);
                addPointFrame.getFrame().removeMouseListener(this);
                if (isFrom) addDadFrame.setFrom(event.getX(), event.getY());
                else addDadFrame.setTo(event.getX(), event.getY());
                addDadFrame.getDialog().setVisible(true);
                addDadFrame.getDialog().toFront();
            }
        };
        addPointFrame.getFrame().addMouseListener(picker);
        addPointFrame.getFrame().setVisible(true);
    }

    private void addDADEvent() {
        if (!addDadFrame.isFromSet() || !addDadFrame.isToSet()) {
            showError.accept("Please pick both the start and end points first.");
            return;
        }
        int nextTime = AutoClickerUtils.parseInt(addDadFrame.getNextTimeTxt().getText(), 500);
        mainFrame.getTableItems().addItem(new ItemDAD(
                addDadFrame.getFromX(), addDadFrame.getFromY(),
                addDadFrame.getToX(), addDadFrame.getToY(), 0, nextTime));
        showAddDADFrame(false);
    }

    private void showAddCommentFrame(boolean show) {
        mainFrame.getFrame().setEnabled(!show);
        if (show) addCommentFrame.getCommentTxt().setText("");
        addCommentFrame.getDialog().setVisible(show);
        if (!show) { mainFrame.getFrame().setEnabled(true); mainFrame.getFrame().toFront(); }
        if (show) addCommentFrame.getCommentTxt().requestFocus();
    }

    private void addPressKey() {
        String input = addPressFrame.getKeyTxt().getText().trim();
        if (input.isEmpty()) {
            showError.accept("Please enter a key to press.");
            return;
        }
        int pressTime = AutoClickerUtils.parseInt(addPressFrame.getPressTimeTxt().getText(), 100);
        int nextTime = AutoClickerUtils.parseInt(addPressFrame.getNextTimeTxt().getText(), 500);

        for (int i = 0; i < input.length(); i++) {
            char keyChar = input.charAt(i);
            int keyCode = KeyEvent.getExtendedKeyCodeForChar(keyChar);
            if (keyCode == KeyEvent.VK_UNDEFINED) {
                showError.accept("Unsupported key: %s".formatted(keyChar));
                return;
            }
            mainFrame.getTableItems().addItem(new ItemPress(
                    KeyEvent.getKeyText(keyCode),
                    keyCode,
                    pressTime,
                    nextTime,
                    addPressFrame.getIsSinglePress()
            ));
        }
        addPressFrame.getKeyTxt().setText("");
        showAddPressFrame(false);
    }

    private void addScrollEvent() {
        String input = addScrollFrame.getWheelAmtTxt().getText();
        if (input.isEmpty()) {
            showError.accept("Enter wheel amount");
            return;
        }
        int nextTime = AutoClickerUtils.parseInt(addScrollFrame.getNextTimeTxt().getText(), 500);
        mainFrame.getTableItems().addItem(
                new Item(input, AutoClickerUtils.parseInt(input, 1), nextTime, GlobalConst.EVENT_SCROLL));
        addScrollFrame.getWheelAmtTxt().setText("");
        showAddScrollFrame(false);
    }

    private void addComment() {
        String text = addCommentFrame.getCommentTxt().getText().trim();
        if (text.isEmpty()) {
            showError.accept("Enter comment text");
            return;
        }
        if (!text.startsWith(GlobalConst.COMMENT_PREFIX)) {
            text = GlobalConst.COMMENT_PREFIX + " " + text;
        }
        mainFrame.getTableItems().addItem(new ItemComment(text));
        showAddCommentFrame(false);
    }
}

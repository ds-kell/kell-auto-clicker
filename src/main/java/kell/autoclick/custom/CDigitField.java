package kell.autoclick.custom;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.Serial;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import static kell.autoclick.contants.GlobalConst.SYSTEM_FONT;

/**
 * Text field that only accepts digits 0-9, optionally with a leading minus
 * sign so callers can enter negative numbers (e.g. mouse-wheel scroll up).
 * On invalid input the field gives three independent feedback signals so
 * the user always notices, regardless of where the field is placed:
 *   1. The border flashes red for ~600ms.
 *   2. A small "Numbers only" label pops up below the field for ~1.5s.
 *   3. The system beep fires.
 *
 * Callers may also register an {@link #setOnInvalid} callback for extra
 * behavior (e.g. showing a global toast as well).
 */
public class CDigitField extends JTextField {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Font  UI_FONT       = new Font(SYSTEM_FONT, Font.PLAIN, 13);
    private static final int   HEIGHT        = 28;
    private static final Color ERROR_COLOR   = new Color(0xC6, 0x28, 0x28);
    private static final int   FLASH_MS      = 600;
    private static final int   POPUP_MS      = 1500;
    private static final String DEFAULT_MSG  = "Numbers only";

    private transient Consumer<String> onInvalid;
    private transient Border           originalBorder;
    private transient Popup            activePopup;
    private boolean          allowNegative = false;

    public CDigitField(int columns) {
        super(columns);
        setFont(UI_FONT);
        setHorizontalAlignment(SwingConstants.RIGHT);
        enableInputMethods(false);
        var pref = getPreferredSize();
        setPreferredSize(new Dimension(pref.width, HEIGHT));
        installFilter();
    }

    /**
     * Allow a leading minus sign (e.g. for mouse-wheel scroll where negative
     * = scroll up). Disabled by default so most fields stay strictly digit-only.
     */
    public void setAllowNegative(boolean allow) {
        this.allowNegative = allow;
    }

    /** Optional extra callback when invalid input is rejected. */
    public void setOnInvalid(Consumer<String> onInvalid) {
        this.onInvalid = onInvalid;
    }

    private void installFilter() {
        ((AbstractDocument) getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offs, String str, AttributeSet a)
                    throws BadLocationException {
                if (str == null) return;
                String existing = fb.getDocument().getText(0, fb.getDocument().getLength());
                if (isValidInsert(existing, offs, str)) {
                    super.insertString(fb, offs, str, a);
                } else {
                    rejectInput(str);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offs, int len, String str, AttributeSet a)
                    throws BadLocationException {
                if (str == null || str.isEmpty()) {
                    super.replace(fb, offs, len, str, a);
                    return;
                }
                String existing = fb.getDocument().getText(0, fb.getDocument().getLength());
                String afterReplace = existing.substring(0, offs)
                        + str
                        + existing.substring(offs + len);
                if (isValidValue(afterReplace)) {
                    super.replace(fb, offs, len, str, a);
                } else {
                    rejectInput(str);
                }
            }
        });
    }

    /**
     * Validates the resulting string. Allows an optional leading "-" when
     * {@link #allowNegative} is true, plus zero or more digits.
     */
    private boolean isValidValue(String s) {
        if (s.isEmpty()) return true; // user emptied the field
        int start = 0;
        if (allowNegative && s.charAt(0) == '-') {
            if (s.length() == 1) return true; // just a minus, user is mid-edit
            start = 1;
        }
        for (int i = start; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return true;
    }

    /** Used for insertString — checks whether the inserted fragment fits. */
    private boolean isValidInsert(String existing, int offs, String fragment) {
        String afterInsert = existing.substring(0, offs) + fragment + existing.substring(offs);
        return isValidValue(afterInsert);
    }

    private void rejectInput(String fragment) {
        flashErrorBorder();
        showErrorPopup(DEFAULT_MSG);
        Toolkit.getDefaultToolkit().beep();
        if (onInvalid != null) onInvalid.accept(fragment);
    }

    private void flashErrorBorder() {
        if (originalBorder == null) originalBorder = getBorder();
        setBorder(BorderFactory.createLineBorder(ERROR_COLOR, 2));
        var t = new Timer(FLASH_MS, e -> setBorder(originalBorder));
        t.setRepeats(false);
        t.start();
    }

    private void showErrorPopup(String message) {
        if (!isShowing()) return; // not on screen yet — nothing to anchor to
        // Hide any previous popup so rapid bad keystrokes don't stack hints.
        if (activePopup != null) {
            activePopup.hide();
            activePopup = null;
        }
        var lbl = new JLabel(message);
        lbl.setOpaque(true);
        lbl.setBackground(ERROR_COLOR);
        lbl.setForeground(Color.WHITE);
        lbl.setFont(getFont().deriveFont(Font.BOLD));
        lbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ERROR_COLOR.darker(), 1),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)));

        Point pt;
        try {
            pt = getLocationOnScreen();
        } catch (IllegalComponentStateException _) {
            return;
        }
        Popup popup = PopupFactory.getSharedInstance()
                .getPopup(this, lbl, pt.x, pt.y + getHeight() + 2);
        popup.show();
        activePopup = popup;

        var hide = new Timer(POPUP_MS, e -> {
            popup.hide();
            if (activePopup == popup) activePopup = null;
        });
        hide.setRepeats(false);
        hide.start();
    }

    /** Convenience: parse the current value, returning fallback on empty/blank. */
    public int getIntValue(int fallback) {
        var t = getText().trim();
        if (t.isEmpty() || "-".equals(t)) return fallback;
        try { return Integer.parseInt(t); }
        catch (NumberFormatException _) { return fallback; }
    }
}

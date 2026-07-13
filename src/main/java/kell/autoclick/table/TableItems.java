package kell.autoclick.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.Serial;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import kell.autoclick.contants.GlobalConst;
import kell.autoclick.model.Item;
import kell.autoclick.model.ItemClick;
import kell.autoclick.model.ItemComment;
import kell.autoclick.model.ItemDAD;
import kell.autoclick.model.ItemPress;

import static kell.autoclick.contants.GlobalConst.SYSTEM_FONT;

/**
 * Columns: # | Action | Name | X | Y | Hold (ms) | Pause (ms)
 * X and Y are only populated for Click and DAD items.
 */
public class TableItems {

    private static final Logger LOG = Logger.getLogger(TableItems.class.getName());

    // col indices
    private static final int COL_IDX    = 0;
    private static final int COL_ACTION = 1;
    private static final int COL_NAME   = 2;
    private static final int COL_X      = 3;
    private static final int COL_Y      = 4;
    private static final int COL_HOLD   = 5;
    private static final int COL_DELAY  = 6;
    private static final int COL_COUNT  = 7;

    // colours
    private static final Color COLOR_HIGHLIGHT  = new Color(0xFF, 0xD7, 0x00, 180);
    private static final Color COLOR_COMMENT_BG = new Color(0xF0, 0xF0, 0xF0);
    private static final Color COLOR_COMMENT_FG = new Color(0x88, 0x88, 0x88);
    private static final Color COLOR_HEADER_BG  = new Color(0x4A, 0x90, 0xD9);
    private static final Color COLOR_HEADER_FG  = Color.WHITE;
    private static final Color COLOR_ROW_ALT    = new Color(0xF5, 0xF8, 0xFF);
    private static final Color COLOR_ROW_NORM   = Color.WHITE;
    private static final String FILE_HEADER_PREFIX = "§FILE§";

    public final Vector<Item> listItems = new Vector<>();

    public final DefaultTableModel model = new DefaultTableModel() {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int col) {
            if (col == COL_IDX || col == COL_ACTION) return false;
            Item item = safeGet(row);
            if (item == null || isFileHeader(item)) return false;
            if (GlobalConst.EVENT_COMMENT.equals(item.getTypeEvent())) return col == COL_NAME;
            // X/Y only editable for click/DAD
            if (col == COL_X || col == COL_Y) {
                String t = item.getTypeEvent();
                return GlobalConst.EVENT_CLICK.equals(t) || GlobalConst.EVENT_DAD.equals(t);
            }
            return true;
        }
        @Override public Class<?> getColumnClass(int col) { return String.class; }
    };

    public final JTable table = new JTable(model);
    private int highlightRow = -1;

    public TableItems() {
        model.addColumn("#");
        model.addColumn("Action");
        model.addColumn("Name");
        model.addColumn("X");
        model.addColumn("Y");
        model.addColumn("Hold (ms)");
        model.addColumn("Pause (ms)");

        // column widths
        table.getColumnModel().getColumn(COL_IDX).setPreferredWidth(36);
        table.getColumnModel().getColumn(COL_IDX).setMaxWidth(44);
        table.getColumnModel().getColumn(COL_ACTION).setPreferredWidth(110);
        table.getColumnModel().getColumn(COL_NAME).setPreferredWidth(140);
        table.getColumnModel().getColumn(COL_X).setPreferredWidth(70);
        table.getColumnModel().getColumn(COL_Y).setPreferredWidth(70);
        table.getColumnModel().getColumn(COL_HOLD).setPreferredWidth(80);
        table.getColumnModel().getColumn(COL_DELAY).setPreferredWidth(80);

        table.setRowHeight(26);
        table.setFont(new Font(SYSTEM_FONT, Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font(SYSTEM_FONT, Font.BOLD, 13));
        table.setSelectionBackground(new Color(0xBB, 0xD7, 0xFF));
        table.setGridColor(new Color(0xDD, 0xDD, 0xDD));
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));

        RowRenderer renderer = new RowRenderer();
        for (int i = 0; i < COL_COUNT; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        table.setDragEnabled(true);
        table.setDropMode(DropMode.INSERT_ROWS);
        table.setTransferHandler(new RowTransferHandler());
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public Vector<Item>      getListItems() { return listItems; }
    public DefaultTableModel getModel()     { return model; }
    public JTable            getTable()     { return table; }

    public void addItem(Item item) {
        listItems.add(item);
        model.addRow(buildRow(item, listItems.size()));
    }

    public void addFileHeader(String filename) {
        ItemComment header = new ItemComment(FILE_HEADER_PREFIX + filename);
        listItems.add(header);
        Object[] row = new Object[COL_COUNT];
        row[COL_ACTION] = "── " + filename + " ──";
        model.addRow(row);
    }

    public void deleteSelectedRows() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        int[] sel = table.getSelectedRows();
        for (int i = sel.length - 1; i >= 0; i--) {
            model.removeRow(sel[i]);
            listItems.remove(sel[i]);
        }
        refreshIndexColumn();
    }

    public void clearTable() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        listItems.clear();
        model.setRowCount(0);
        highlightRow = -1;
    }

    public void setListItems(Vector<Item> items) {
        clearTable();
        for (Item item : items) addItem(item);
    }

    public void appendItems(Vector<Item> items, String filename) {
        addFileHeader(filename);
        for (Item item : items) {
            item.setSourceFile(filename);
            addItem(item);
        }
    }

    public void updateLastDelay(int delayMs) {
        if (listItems.isEmpty()) {
            return;
        }
        int row = listItems.size() - 1;
        Item item = listItems.get(row);
        item.setNextTime(delayMs);
        model.setValueAt(String.valueOf(delayMs), row, COL_DELAY);
    }

    public void updateHoldTime(int row, int holdMs) {
        if (row < 0 || row >= listItems.size()) {
            return;
        }
        Item item = listItems.get(row);
        item.setPressTime(holdMs);
        model.setValueAt(String.valueOf(holdMs), row, COL_HOLD);
    }

    public void moveSelectedUp() {
        int row = table.getSelectedRow();
        if (row <= 0) return;
        swapRows(row, row - 1);
        table.setRowSelectionInterval(row - 1, row - 1);
    }

    public void moveSelectedDown() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= model.getRowCount() - 1) return;
        swapRows(row, row + 1);
        table.setRowSelectionInterval(row + 1, row + 1);
    }

    public void setHighlightRow(int row) {
        this.highlightRow = row;
        table.repaint();
        if (row >= 0 && row < table.getRowCount())
            table.scrollRectToVisible(table.getCellRect(row, 0, true));
    }

    public void refreshIndexColumn() {
        int idx = 1;
        for (int i = 0; i < model.getRowCount(); i++) {
            Item item = safeGet(i);
            model.setValueAt(isFileHeader(item) ? "" : String.valueOf(idx++), i, COL_IDX);
        }
    }

    // ── Row building ──────────────────────────────────────────────────────────

    private Object[] buildRow(Item item, int displayIdx) {
        Object[] row = new Object[COL_COUNT];
        if (isFileHeader(item)) {
            row[COL_ACTION] = "── " + item.getName().substring(FILE_HEADER_PREFIX.length()) + " ──";
            return row;
        }
        if (GlobalConst.EVENT_COMMENT.equals(item.getTypeEvent())) {
            row[COL_IDX]    = String.valueOf(displayIdx);
            row[COL_ACTION] = "Comment";
            row[COL_NAME]   = item.getName();
            return row;
        }
        row[COL_IDX]   = String.valueOf(displayIdx);
        row[COL_NAME]  = item.getName();
        row[COL_HOLD]  = String.valueOf(item.getPressTime());
        row[COL_DELAY] = String.valueOf(item.getNextTime());

        switch (item) {
            case ItemClick ic -> {
                row[COL_ACTION] = clickActionName(ic);
                row[COL_X]      = String.valueOf(ic.getXAxis());
                row[COL_Y]      = String.valueOf(ic.getYAxis());
            }
            case ItemDAD id -> {
                row[COL_ACTION] = "Drag & Drop";
                row[COL_X]      = id.getXAxis1() + " → " + id.getXAxis2();
                row[COL_Y]      = id.getYAxis1() + " → " + id.getYAxis2();
            }
            case ItemPress ip -> row[COL_ACTION] = keyActionName(ip);
            default -> row[COL_ACTION] = switch (item.getTypeEvent()) {
                case GlobalConst.EVENT_SCROLL -> "Scroll";
                default                       -> item.getTypeEvent();
            };
        }
        return row;
    }

    private String clickActionName(ItemClick item) {
        String button = item.isRightClick() ? "Right" : "Left";
        if (item.isMouseDownEvent()) {
            return button + " Down";
        }
        if (item.isMouseUpEvent()) {
            return button + " Up";
        }
        return button + " Click";
    }

    private String keyActionName(ItemPress item) {
        if (item.isKeyDownEvent()) {
            return "Key Down";
        }
        if (item.isKeyUpEvent()) {
            return "Key Up";
        }
        return "Key Press";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Pattern-matching helper: an Item that's a file header marker. */
    private boolean isFileHeader(Item item) {
        return item instanceof ItemComment ic
                && ic.getName() != null
                && ic.getName().startsWith(FILE_HEADER_PREFIX);
    }

    private Item safeGet(int row) {
        if (row < 0 || row >= listItems.size()) return null;
        return listItems.get(row);
    }

    private void swapRows(int a, int b) {
        for (int col = 0; col < COL_COUNT; col++) {
            Object tmp = model.getValueAt(a, col);
            model.setValueAt(model.getValueAt(b, col), a, col);
            model.setValueAt(tmp, b, col);
        }
        Item tmp = listItems.get(a);
        listItems.set(a, listItems.get(b));
        listItems.set(b, tmp);
        refreshIndexColumn();
    }

    // ── Cell Renderer ─────────────────────────────────────────────────────────

    private class RowRenderer extends DefaultTableCellRenderer {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {
            super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
            Item item = safeGet(row);
            setFont(tbl.getFont());
            setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
            if (isSelected) return this;

            if (row == highlightRow) {
                setBackground(COLOR_HIGHLIGHT);
                setForeground(Color.BLACK);
                setFont(tbl.getFont().deriveFont(Font.BOLD));
                return this;
            }
            if (isFileHeader(item)) {
                setBackground(COLOR_HEADER_BG);
                setForeground(COLOR_HEADER_FG);
                setFont(tbl.getFont().deriveFont(Font.BOLD));
                setHorizontalAlignment(col == COL_ACTION ? LEFT : CENTER);
                return this;
            }
            if (item != null && GlobalConst.EVENT_COMMENT.equals(item.getTypeEvent())) {
                setBackground(COLOR_COMMENT_BG);
                setForeground(COLOR_COMMENT_FG);
                setFont(tbl.getFont().deriveFont(Font.ITALIC));
                return this;
            }
            setBackground(row % 2 == 0 ? COLOR_ROW_NORM : COLOR_ROW_ALT);
            setForeground(Color.BLACK);
            return this;
        }
    }

    // ── Drag-and-drop row reorder ─────────────────────────────────────────────

    private class RowTransferHandler extends TransferHandler {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override public int getSourceActions(JComponent c) { return MOVE; }
        @Override protected Transferable createTransferable(JComponent c) {
            return new StringSelection(String.valueOf(table.getSelectedRow()));
        }
        @Override public boolean canImport(TransferSupport s) {
            return s.isDrop() && s.isDataFlavorSupported(DataFlavor.stringFlavor);
        }
        @Override public boolean importData(TransferSupport support) {
            if (!canImport(support)) return false;
            try {
                int fromRow = Integer.parseInt(
                    (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor));
                int toRow = ((JTable.DropLocation) support.getDropLocation()).getRow();
                if (toRow > fromRow) toRow--;
                if (fromRow == toRow) return false;
                if (fromRow < toRow) for (int i = fromRow; i < toRow; i++) swapRows(i, i + 1);
                else for (int i = fromRow; i > toRow; i--) swapRows(i, i - 1);
                table.setRowSelectionInterval(toRow, toRow);
                return true;
            } catch (UnsupportedFlavorException
                     | IOException
                     | NumberFormatException
                     | ClassCastException e) {
                LOG.log(Level.FINE, "Could not import dragged table row", e);
                return false;
            }
        }
    }
}

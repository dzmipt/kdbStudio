package studio.ui.action;

import studio.kdb.K;
import studio.ui.StudioWindow;
import studio.ui.UserAction;
import studio.ui.Util;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public abstract class TableUserAction extends UserAction {

    protected final StudioWindow studioWindow;
    protected final JTable table;

    private int col = -1;
    private int row = -1;

    public TableUserAction(StudioWindow studioWindow, JTable table,
                           String text, Icon icon, String desc, Integer mnemonic, KeyStroke key) {
        super(text, icon, desc, mnemonic, key);
        this.studioWindow = studioWindow;
        this.table = table;
    }

    public void setLocation(int row, int col) {
        this.row = row;
        this.col = col;
    }

    protected int getRow() {
        return row == -1 ? table.getSelectedRow() : row;
    }

    protected int getColumn() {
        return col == -1 ? table.getSelectedColumn() : col;
    }


    public static class InspectCellAction extends TableUserAction {
        public InspectCellAction(StudioWindow studioWindow, JTable table) {
            super(studioWindow, table, "Inspect cell", Util.BLANK_ICON, "Show in a separate tab",
                    KeyEvent.VK_S, null);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            int aRow = getRow();
            int aCol = getColumn();
            if (aRow == -1 || aCol == -1) return;
            studioWindow.addResultTab(new QueryResult((K.KBase)table.getValueAt(aRow, aCol)), "a cell from previous result");
        }
    }

    public static class InspectLineAction extends TableUserAction {
        public InspectLineAction(StudioWindow studioWindow, JTable table) {
            super(studioWindow, table, "Inspect line", Util.BLANK_ICON, "Show line as dictionary in a separate tab",
                    KeyEvent.VK_S, null);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int aRow = getRow();
            if (aRow == -1) return;

            int count = table.getColumnCount();
            String[] names = new String[count];
            K.KBase[] values = new K.KBase[count];
            for (int i = 0; i < count; i++) {
                names[i] = table.getColumnName(i);
                values[i] = (K.KBase) table.getValueAt(aRow, i);
            }

            studioWindow.addResultTab(new QueryResult(
                    new K.Dict(new K.KSymbolVector(names), new K.KList(values))), "a line from previous result");
        }
    }
}

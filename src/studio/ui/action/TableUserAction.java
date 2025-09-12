package studio.ui.action;

import studio.kdb.K;
import studio.kdb.query.QueryResult;
import studio.ui.ResultTab;
import studio.ui.UserAction;
import studio.ui.Util;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public abstract class TableUserAction extends UserAction {

    protected ResultTab resultTab;
    protected final JTable table;

    private int col = -1;
    private int row = -1;

    public TableUserAction(ResultTab resultTab, JTable table,
                           String text, Icon icon, String desc, Integer mnemonic, KeyStroke key) {
        super(text, icon, desc, mnemonic, key);
        this.resultTab = resultTab;
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
        public InspectCellAction(ResultTab resultTab, JTable table) {
            super(resultTab, table, "Inspect cell", Util.BLANK_ICON, "Show in a separate tab",
                    KeyEvent.VK_S, null);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            int aRow = getRow();
            int aCol = getColumn();
            if (aRow == -1 || aCol == -1) return;
            resultTab.addResult(new QueryResult((K.KBase)table.getValueAt(aRow, aCol)), "cell from previous result");
        }
    }

    public static class InspectLineAction extends TableUserAction {
        public InspectLineAction(ResultTab resultTab, JTable table) {
            super(resultTab, table, "Inspect line", Util.BLANK_ICON, "Show line as dictionary in a separate tab",
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

            resultTab.addResult(new QueryResult(
                    new K.Dict(new K.KSymbolVector(names), new K.KList(values))), "line from previous result");
        }
    }
}

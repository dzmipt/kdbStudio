package studio.ui.action;

import studio.kdb.K;
import studio.ui.StudioWindow;
import studio.ui.UserAction;
import studio.ui.Util;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public abstract class TableUserAction extends UserAction {

    protected int col = -1;
    protected int row = -1;

    public TableUserAction(String text, Icon icon, String desc, Integer mnemonic, KeyStroke key) {
        super(text, icon, desc, mnemonic, key);
    }

    public void setLocation(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public static class ShowSeparateAction extends TableUserAction {

        private final StudioWindow studioWindow;
        private final JTable table;

        public ShowSeparateAction(StudioWindow studioWindow, JTable table) {
            super("To separate tab", Util.BLANK_ICON, "Show in a separate tab",
                    KeyEvent.VK_S, null);
            this.studioWindow = studioWindow;
            this.table = table;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            int aRow = row;
            int aCol = col;
            if (aRow == -1 || aCol == -1) {
                aRow = table.getSelectedRow();
                aCol = table.getSelectedColumn();
            }
            if (aRow == -1 || aCol == -1) return;
            studioWindow.addResultTab(new QueryResult((K.KBase)table.getValueAt(aRow, aCol)), "an element from previous result");
        }
    }
}

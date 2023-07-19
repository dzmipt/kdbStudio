package studio.ui.statusbar;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import studio.ui.MinSizeLabel;

import javax.swing.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class MainStatusBar extends StatusBar {

    private final MinSizeLabel lblRowCol;
    private final MinSizeLabel lblInsStatus;

    private JComponent compInFocus = null;

    public MainStatusBar() {
        lblInsStatus = new MinSizeLabel("INS");
        lblInsStatus.setHorizontalAlignment(JLabel.CENTER);
        lblInsStatus.setMinimumWidth("INS", "OVR");
        addComponent(lblInsStatus);

        lblRowCol = new MinSizeLabel("");
        lblRowCol.setHorizontalAlignment(JLabel.CENTER);
        lblRowCol.setMinimumWidth("9999:9999");
        addComponent(lblRowCol);
    }

    public void bindTextArea(RSyntaxTextArea bindTextArea) {
        bindTextArea.addCaretListener(e -> updateRowColStatus(bindTextArea));
        bindTextArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                updateTextModeStatus(bindTextArea);
            }
        });
        bindTextArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                compInFocus = bindTextArea;

                updateTextModeStatus(bindTextArea);
                updateRowColStatus(bindTextArea);
            }
        });
    }

    public void bindTable(JTable table) {
        table.getSelectionModel().addListSelectionListener(e -> updateRowColStatus(table));
        table.getColumnModel().getSelectionModel().addListSelectionListener(e -> updateRowColStatus(table));

        table.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                compInFocus = table;

                updateRowColStatus(table);
                lblInsStatus.setText(" ");
            }
        });
    }

    private void updateRowColStatus(RSyntaxTextArea textArea) {
        if (compInFocus != textArea) return;

        int row = textArea.getCaretLineNumber() + 1;
        int col = textArea.getCaretPosition() - textArea.getLineStartOffsetOfCurrentLine() + 1;
        lblRowCol.setText("" + row + ":" + col);
    }

    private void updateRowColStatus(JTable table) {
        if (compInFocus != table) return;

        int row = table.getSelectedRow();
        int col = table.getSelectedColumn();

        if (row == -1 || col == -1) lblRowCol.setText(" ");
        else lblRowCol.setText("" + row +":" + col);
    }

    private void updateTextModeStatus(RSyntaxTextArea textArea) {
        if (compInFocus != textArea) return;

        String text = textArea.getTextMode() == RSyntaxTextArea.INSERT_MODE ? "INS" : "OVR";
        lblInsStatus.setText(text);
    }

}

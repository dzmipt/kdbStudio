package studio.ui.statusbar;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import studio.ui.MinSizeLabel;
import studio.ui.search.Position;

import javax.swing.*;
import java.awt.event.*;

public class MainStatusBar extends StatusBar {

    private final MinSizeLabel lblRowCol;
    private final MinSizeLabel lblInsStatus;

    public MainStatusBar() {
        lblInsStatus = new MinSizeLabel("INS");
        lblInsStatus.setHorizontalAlignment(JLabel.CENTER);
        lblInsStatus.setMinimumWidth("INS", "OVR");
        addComponent(lblInsStatus);

        lblRowCol = new MinSizeLabel(" ");
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
                updateStatuses(bindTextArea);
            }
        });
    }

    public void bindTable(JTable table) {
        table.getSelectionModel().addListSelectionListener(e -> updateRowColStatus(table));
        table.getColumnModel().getSelectionModel().addListSelectionListener(e -> updateRowColStatus(table));

        table.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                updateStatuses(table);
            }
        });
    }

    public void updateStatuses(RSyntaxTextArea textArea) {
        updateTextModeStatus(textArea);
        updateRowColStatus(textArea);
    }

    public void updateStatuses(JTable table) {
        updateRowColStatus(table);
        lblInsStatus.setText(" ");
    }

    public void resetStatuses() {
        lblRowCol.setText(" ");
        lblInsStatus.setText(" ");
    }

    public void setRowColStatus(Position position) {
        lblRowCol.setText("" + position.getRow() + ":" + position.getColumn());
    }

    private void updateRowColStatus(RSyntaxTextArea textArea) {
        int row = textArea.getCaretLineNumber() + 1;
        int col = textArea.getCaretPosition() - textArea.getLineStartOffsetOfCurrentLine() + 1;
        lblRowCol.setText("" + row + ":" + col);
    }

    private void updateRowColStatus(JTable table) {
        int row = table.getSelectedRow();
        int col = table.getSelectedColumn();

        if (row == -1 || col == -1) lblRowCol.setText(" ");
        else lblRowCol.setText("" + row +":" + col);
    }

    private void updateTextModeStatus(RSyntaxTextArea textArea) {
        String text = textArea.getTextMode() == RSyntaxTextArea.INSERT_MODE ? "INS" : "OVR";
        lblInsStatus.setText(text);
    }

}

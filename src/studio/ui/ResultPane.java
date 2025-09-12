package studio.ui;

import kx.K4Exception;
import studio.kdb.K;
import studio.kdb.KTableModel;
import studio.kdb.ListModel;
import studio.kdb.query.QueryResult;

import javax.swing.*;
import java.awt.*;

public class ResultPane extends JPanel {

    private final QueryResult queryResult;
    private EditorPane editor = null;
    private QGrid grid = null;
    private final ResultType type;

    public ResultPane(StudioWindow studioWindow, ResultTab resultTab, QueryResult queryResult) {
        super(new BorderLayout());
        this.queryResult = queryResult;

        K.KBase result = queryResult.getResult();
        JComponent component;
        if (result != null) {
            KTableModel model = KTableModel.getModel(result);
            if (model != null) {
                grid = new QGrid(studioWindow, resultTab, model);
                component = grid;
                if (model instanceof ListModel) {
                    type = ResultType.LIST;
                } else {
                    type = ResultType.TABLE;
                }
            } else {
                editor = new EditorPane(false, studioWindow.getResultSearchPanel(), studioWindow.getMainStatusBar());
                component = editor;
                type = ResultType.TEXT;
            }

        } else {
            JTextPane textArea = new JTextPane();
            Throwable error = queryResult.getError();
            String msg;
            if (error instanceof K4Exception) {
                msg = "An error occurred during execution of the query.\nThe server sent the response:\n" + error.getMessage();
            } else {
                msg = "An unexpected error occurred whilst communicating with server.\nError: " + error.toString();
                if (error.getMessage() != null) {
                    msg += "\nMessage: " + error.getMessage();
                }
            }
            textArea.setText(msg);
            textArea.setForeground(Color.RED);
            textArea.setEditable(false);
            component = new JScrollPane(textArea);
            type = ResultType.ERROR;

        }
        add(component, BorderLayout.CENTER);
    }

    public QueryResult getQueryResult() {
        return queryResult;
    }

    public EditorPane getEditor() {
        return editor;
    }

    public QGrid getGrid() {
        return grid;
    }

    public ResultType getType() {
        return type;
    }
}

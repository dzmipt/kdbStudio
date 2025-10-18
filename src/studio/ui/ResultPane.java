package studio.ui;

import kx.K4Exception;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import studio.kdb.K;
import studio.kdb.KTableModel;
import studio.kdb.KType;
import studio.kdb.ListModel;
import studio.kdb.config.ColorToken;
import studio.qeditor.QTokenMakerFactory;
import studio.ui.action.QueryResult;
import studio.ui.rstextarea.StudioRSyntaxTextArea;

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
                KType kType = result.getType();
                boolean enlist = kType.isVector() && result.count() == 1;
                ColorToken colorToken = kType.getColorToken();
                if (colorToken != ColorToken.DEFAULT && ! enlist) {
                    StudioRSyntaxTextArea textArea = editor.getTextArea();
                    ((RSyntaxDocument)textArea.getDocument()).setTokenMakerFactory(QTokenMakerFactory.INSTANCE);
                    String contentType = QTokenMakerFactory.getContentType(colorToken);
                    textArea.setSyntaxEditingStyle(contentType);
                }
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

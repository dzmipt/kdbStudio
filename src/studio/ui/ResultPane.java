package studio.ui;

import kx.K4Exception;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import studio.kdb.*;
import studio.kdb.ListModel;
import studio.kdb.config.ColorToken;
import studio.kdb.config.EditorColorToken;
import studio.kdb.config.TokenStyle;
import studio.qeditor.QTokenMakerFactory;
import studio.ui.action.QueryResult;
import studio.ui.rstextarea.StudioRSyntaxTextArea;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;

public class ResultPane extends JPanel {

    private final QueryResult queryResult;
    private EditorPane editor = null;
    private JTextComponent errorPane = null;
    private ResultGrid grid = null;
    private final ResultType type;

    public ResultPane(StudioWindow studioWindow, ResultTab resultTab, QueryResult queryResult) {
        super(new BorderLayout());
        this.queryResult = queryResult;

        K.KBase result = queryResult.getResult();
        JComponent component;
        if (result != null) {
            KTableModel model = KTableModel.getModel(result);
            if (model != null) {
                grid = new ResultGrid(studioWindow, resultTab, model);
                component = grid;
                if (model instanceof ListModel) {
                    type = ResultType.LIST;
                } else {
                    type = ResultType.TABLE;
                }
            } else {
                editor = new EditorPane(false, studioWindow.getResultSearchPanel(), studioWindow.getMainStatusBar());
                StudioRSyntaxTextArea textArea = editor.getTextArea();

                KType kType = result.getType();
                boolean enlist = kType.isVector() && result.count() == 1;
                ColorToken colorToken = kType.getColorToken();
                if (colorToken != ColorToken.DEFAULT && ! enlist) {
                    ((RSyntaxDocument)textArea.getDocument()).setTokenMakerFactory(QTokenMakerFactory.INSTANCE);
                    String contentType = QTokenMakerFactory.getContentType(colorToken);
                    textArea.setSyntaxEditingStyle(contentType);
                }
                component = editor;
                type = ResultType.TEXT;
            }

        } else {
            Config config = Config.getInstance();
            Font font = config.getFont(Config.FONT_EDITOR);

            Color bgColor = config.getEditorColors().get(EditorColorToken.BACKGROUND);
            TokenStyle style = config.getTokenStyleConfig().get(ColorToken.ERROR);
            Color fgColor = style.getColor();

            errorPane = new JTextPane();
            errorPane.setBackground(bgColor);
            errorPane.setForeground(fgColor);
            errorPane.setFont(font);

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
            errorPane.setText(msg);
            errorPane.setEditable(false);
            component = new JScrollPane(errorPane);
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

    public JTextComponent getErrorPane() {
        return errorPane;
    }

    public ResultGrid getGrid() {
        return grid;
    }

    public ResultType getType() {
        return type;
    }
}

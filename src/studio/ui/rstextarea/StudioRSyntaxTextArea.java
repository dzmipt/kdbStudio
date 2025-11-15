package studio.ui.rstextarea;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RUndoManager;
import studio.kdb.config.ColorMap;
import studio.kdb.config.EditorColorToken;
import studio.kdb.config.TokenStyle;
import studio.kdb.config.TokenStyleMap;
import studio.qeditor.RSToken;

import java.awt.*;

public class StudioRSyntaxTextArea extends RSyntaxTextArea {

    private Gutter gutter = null;
    private Runnable actionsUpdateListener = null;

    public StudioRSyntaxTextArea(String text) {
        super(text);
    }

    @Override
    protected void handleReplaceSelection(String content) {
        //Probably this is a dirty hack, but here is we convert character 160 to space
        //which is pasted by Skype for Business and result in 'char error from kdb
        content = content.replace((char)160,' ');
        super.handleReplaceSelection(content);
    }

    public void setGutter(Gutter gutter) {
        this.gutter = gutter;
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        if (gutter != null) gutter.setLineNumberFont(font);
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        if (gutter != null) gutter.setBackground(bg);
    }

    public void setSyntaxScheme(Font font, TokenStyleMap tokenStyleMap) {
        setFont(font);
        SyntaxScheme scheme = new SyntaxScheme(false);
        Style[] defaultStyles = scheme.getStyles();
        Style[] styles = new Style[RSToken.NUM_TOKEN_TYPES];
        System.arraycopy(defaultStyles, 0, styles, 0, defaultStyles.length);
        for (RSToken token: RSToken.values()) {
            TokenStyle tokenStyle = tokenStyleMap.get(token.getColorToken());
            Style style = new Style(tokenStyle.getColor(), null, tokenStyle.getStyle().applyStyle(font));

            styles[token.getTokenType()] = style;
        }
        scheme.setStyles(styles);

        setSyntaxScheme(scheme);
    }

    public void setEditorColors(ColorMap editorColors) {
        setBackground(editorColors.get(EditorColorToken.BACKGROUND));
        setSelectionColor(editorColors.get(EditorColorToken.SELECTED));
        setCurrentLineHighlightColor(editorColors.get(EditorColorToken.CURRENT_LINE_HIGHLIGHT));
    }

    public void setActionsUpdateListener(Runnable actionUpdateListener) {
        this.actionsUpdateListener = actionUpdateListener;
    }

    @Override
    protected RUndoManager createUndoManager() {
        return new StudioRUndoManager(this);
    }

    private class StudioRUndoManager extends RUndoManager {

        public StudioRUndoManager(RTextArea textArea) {
            super(textArea);
        }

        //It turns out that in the updates from the RSTA.getDocument() DocumentListener, canUndo() and canRedo() returns previous state
        @Override
        public void updateActions() {
            super.updateActions();
            if (actionsUpdateListener != null) actionsUpdateListener.run();
        }
    }
}

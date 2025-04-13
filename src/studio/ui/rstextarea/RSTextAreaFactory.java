package studio.ui.rstextarea;

import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rsyntaxtextarea.folding.CurlyFoldParser;
import org.fife.ui.rsyntaxtextarea.folding.FoldParserManager;
import org.fife.ui.rtextarea.ClipboardHistory;
import org.fife.ui.rtextarea.ConfigurableCaret;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RecordableTextAction;
import studio.kdb.Config;
import studio.kdb.config.ColorTokenConfig;
import studio.qeditor.RSToken;
import studio.qeditor.RSTokenMaker;
import studio.ui.Util;

import javax.swing.*;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import static org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKit.*;

public class RSTextAreaFactory {

    public static final String rstaCutAsStyledTextAction = "kdbStudio.rstaCutAsStyledTextAction";
    public static final String rstaCopyAsStyledTextAction = "kdbStudio.rstaCopyAsStyledTextAction";

    private static final ActionMap actionMap;


    static {
        java.util.List<Action> actions = new ArrayList<>();
        actions.addAll(Arrays.asList(new RSyntaxTextAreaEditorKit().getActions()));

        List<String> wrongInsPairCharActionName = new ArrayList<>();
        for (ListIterator<Action> iterator = actions.listIterator(); iterator.hasNext(); ) {
            Action action = iterator.next();
            if (action instanceof InsertPairedCharacterAction) {
                wrongInsPairCharActionName.add(((InsertPairedCharacterAction) action).getName());
                iterator.remove();
            }
        }

        List<InsertPairedCharacterAction> insertPairedActions = Arrays.asList(
                new InsertPairedCharacterAction(rstaOpenParenAction, '(', ')'),
                new InsertPairedCharacterAction(rstaOpenSquareBracketAction, '[', ']'),
                new InsertPairedCharacterAction(rstaOpenCurlyAction, '{', '}'),
                new InsertQuoteAction(rstaDoubleQuoteAction, InsertQuoteAction.QuoteType.DOUBLE_QUOTE)
// We should not wrap ` and '
//                new InsertQuoteAction(rstaSingleQuoteAction, RSyntaxTextAreaEditorKit.InsertQuoteAction.QuoteType.SINGLE_QUOTE),
//                new InsertQuoteAction(rstaBacktickAction, RSyntaxTextAreaEditorKit.InsertQuoteAction.QuoteType.BACKTICK)
        );

        wrongInsPairCharActionName.removeAll(
                insertPairedActions.stream().map(RecordableTextAction::getName).collect(Collectors.toList())
        );

        actions.addAll(insertPairedActions);

        actions.add(new CopyCutAsStyledTextAction(false));
        actions.add(new CopyCutAsStyledTextAction(true));

        actions.add(new FindReplaceAction(false));
        actions.add(new FindReplaceAction(true));
        actions.add(new HideSearchPanelAction());
        actions.add(new ConvertTabsToSpacesAction());
        actions.add(new CommentAction());

        actionMap = new ActionMapUIResource();
        for (Action a : actions) {
            actionMap.put(a.getValue(Action.NAME), a);
        }
        UIManager.put("RSyntaxTextAreaUI.actionMap", actionMap);


        int shift = InputEvent.SHIFT_DOWN_MASK;
        int defaultModifier = RTextArea.getDefaultModifier();
        InputMap inputMap = new RSyntaxTextAreaDefaultInputMap();
        if (Util.MAC_OS_X) {
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), DefaultEditorKit.endLineAction);
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), DefaultEditorKit.beginLineAction);

            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, shift), DefaultEditorKit.selectionEndLineAction);
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, shift), DefaultEditorKit.selectionBeginLineAction);
        }

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C,      defaultModifier), rstaCopyAsStyledTextAction);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_COPY,   0),      rstaCopyAsStyledTextAction);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, defaultModifier), rstaCopyAsStyledTextAction);

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X,      defaultModifier), rstaCutAsStyledTextAction);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_CUT,    0),      rstaCutAsStyledTextAction);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, shift),           rstaCutAsStyledTextAction);

        inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, defaultModifier)); // used for execute current line
        inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_J, defaultModifier)); // used for adding thousand commas

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F,      defaultModifier), FindReplaceAction.findAction);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R,      defaultModifier), FindReplaceAction.replaceAction);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,      0), HideSearchPanelAction.action);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, defaultModifier), CommentAction.action);

        for (KeyStroke ks: inputMap.allKeys() ) {
            Object action = inputMap.get(ks);
            if (wrongInsPairCharActionName.contains(action)) {
                inputMap.remove(ks);
            }
        }

        UIManager.put("RSyntaxTextAreaUI.inputMap", inputMap);

        FoldParserManager.get().addFoldParserMapping(RSTokenMaker.CONTENT_TYPE, new CurlyFoldParser());

        AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        atmf.putMapping(RSTokenMaker.CONTENT_TYPE, RSTokenMaker.class.getName());
    }

    public static Action getAction(String name) {
        return actionMap.get(name);
    }

    public static StudioRSyntaxTextArea newTextArea(boolean editable) {
        StudioRSyntaxTextArea textArea = new StudioRSyntaxTextArea("");
        textArea.setEditable(editable);
        textArea.setLineWrap(Config.getInstance().getBoolean(Config.RSTA_WORD_WRAP));
        textArea.setAnimateBracketMatching(Config.getInstance().getBoolean(Config.RSTA_ANIMATE_BRACKET_MATCHING));
        //Never highlight current line if not editable
        textArea.setHighlightCurrentLine(editable && Config.getInstance().getBoolean(Config.RSTA_HIGHLIGHT_CURRENT_LINE));
        if (! editable) {
            textArea.setCaret(new HideOnFocusLostCaret(textArea));
        }

        textArea.setCodeFoldingEnabled(true);
        textArea.setCloseCurlyBraces(true);

        textArea.setSyntaxEditingStyle(RSTokenMaker.CONTENT_TYPE);
        textArea.setSyntaxScheme(getDefaulSyntaxScheme());
        textArea.setHyperlinksEnabled(false);

        textArea.setTabsEmulated(Config.getInstance().getBoolean(Config.EDITOR_TAB_EMULATED));
        textArea.setTabSize(Config.getInstance().getInt(Config.EDITOR_TAB_SIZE));

        textArea.setInsertPairedCharacters(Config.getInstance().getBoolean(Config.RSTA_INSERT_PAIRED_CHAR));
        return textArea;
    }

    private static SyntaxScheme getDefaulSyntaxScheme() {
        SyntaxScheme scheme = new SyntaxScheme(false);
        Style[] defaultStyles = scheme.getStyles();
        Style[] styles = new Style[RSToken.NUM_TOKEN_TYPES];
        System.arraycopy(defaultStyles, 0, styles, 0, defaultStyles.length);
        ColorTokenConfig tokenConfig = Config.getInstance().getColorTokenConfig();
        for (RSToken token: RSToken.values()) {
            Font font = RSyntaxTextArea.getDefaultFont();
            if (token.getFontStyle() != Font.PLAIN) font = font.deriveFont(token.getFontStyle());
            Color color = tokenConfig.getColor(token.getColorToken());
            Style style = new Style(color, null, font);

            styles[token.getTokenType()] = style;
        }
        scheme.setStyles(styles);
        return scheme;
    }


    private static class HideOnFocusLostCaret extends ConfigurableCaret {

        private final RSyntaxTextArea textArea;

        HideOnFocusLostCaret(RSyntaxTextArea textArea) {
            this.textArea = textArea;
        }

        @Override
        public boolean isVisible() {
            return textArea.isFocusOwner();
        }

    }

    // Taken from PR: https://github.com/bobbylight/RSyntaxTextArea/pull/406.
    // Once/if the library gets the change, probably the below we will need to remove
    public static class CopyCutAsStyledTextAction extends RecordableTextAction {

        private Theme theme;
        private boolean cutAction = false;

        private static final long serialVersionUID = 2L;

        private static String getActionName(boolean cutAction) {
            return cutAction ? rstaCutAsStyledTextAction : rstaCopyAsStyledTextAction;
        }

        public CopyCutAsStyledTextAction(boolean cutAction) {
            super(getActionName(cutAction));
            this.cutAction = cutAction;
        }

        @Override
        public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
            if (cutAction && (!textArea.isEditable() || !textArea.isEnabled())) {
                UIManager.getLookAndFeel().provideErrorFeedback(textArea);
                return;
            }

            ((RSyntaxTextArea)textArea).copyAsStyledText(theme);
            ClipboardHistory.get().add(textArea.getSelectedText());
            if (cutAction) {
                int selStart = textArea.getSelectionStart();
                int selEnd = textArea.getSelectionEnd();

                try {
                    textArea.getDocument().remove(selStart, selEnd - selStart);
                } catch (BadLocationException ex) {
                    System.err.println("Oops... That's not expected: " + ex);
                    ex.printStackTrace();
                }
            }
            textArea.requestFocusInWindow();
        }

        @Override
        public final String getMacroID() {
            return getName();
        }

    }

}

package studio.qeditor;

import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMakerBase;

import javax.swing.text.Segment;

public class OneTokenMaker extends TokenMakerBase {

    private final RSToken token;

    public OneTokenMaker(RSToken token) {
        this.token = token;
    }

    @Override
    public Token getTokenList(Segment text, int initialTokenType, int startOffset) {
        resetTokenList();
        addToken(text, text.offset, text.getEndIndex() - 1, token.getTokenType(), startOffset);
        return firstToken;
    }

}

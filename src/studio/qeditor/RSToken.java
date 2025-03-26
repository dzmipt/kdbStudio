package studio.qeditor;

import org.fife.ui.rsyntaxtextarea.TokenTypes;
import studio.kdb.config.ColorToken;

import java.awt.*;
import java.util.Arrays;

public enum RSToken {

    NULL(TokenTypes.NULL, ColorToken.DEFAULT),
    SYMBOL(TokenTypes.DEFAULT_NUM_TOKEN_TYPES, ColorToken.SYMBOL),
    STRING(TokenTypes.LITERAL_CHAR, ColorToken.CHARVECTOR),
    ML_STRING(TokenTypes.DEFAULT_NUM_TOKEN_TYPES + 1, ColorToken.CHARVECTOR),
    ERROR_STRING(TokenTypes.DEFAULT_NUM_TOKEN_TYPES + 2, Font.BOLD, ColorToken.ERROR),
    IDENTIFIER(TokenTypes.IDENTIFIER, ColorToken.IDENTIFIER),
    OPERATOR(TokenTypes.OPERATOR, ColorToken.OPERATOR),
    BRACKET(TokenTypes.SEPARATOR, ColorToken.BRACKET),
    EOL_COMMENT(TokenTypes.COMMENT_EOL, Font.ITALIC, ColorToken.EOLCOMMENT),
    ML_COMMENT(TokenTypes.COMMENT_MULTILINE, Font.ITALIC, ColorToken.EOLCOMMENT),
    KEYWORD(TokenTypes.RESERVED_WORD, Font.BOLD, ColorToken.KEYWORD),
    WHITESPACE(TokenTypes.WHITESPACE, ColorToken.WHITESPACE),
    UNKNOWN(TokenTypes.ERROR_NUMBER_FORMAT, Font.BOLD, ColorToken.ERROR),
    INTEGER(TokenTypes.DEFAULT_NUM_TOKEN_TYPES + 3, ColorToken.INTEGER),
    MINUTE(TokenTypes.DEFAULT_NUM_TOKEN_TYPES + 4, ColorToken.MINUTE),
    SECOND(TokenTypes.DEFAULT_NUM_TOKEN_TYPES + 5, ColorToken.SECOND),
    TIME(TokenTypes.DEFAULT_NUM_TOKEN_TYPES + 6, ColorToken.TIME),
    DATE(TokenTypes.DEFAULT_NUM_TOKEN_TYPES + 7, ColorToken.DATE),
    MONTH(TokenTypes.DEFAULT_NUM_TOKEN_TYPES + 8, ColorToken.MONTH),
    FLOAT(TokenTypes.LITERAL_NUMBER_FLOAT, ColorToken.FLOAT),
    LONG(TokenTypes.LITERAL_NUMBER_DECIMAL_INT, ColorToken.LONG),
    SHORT(TokenTypes.DEFAULT_NUM_TOKEN_TYPES + 9, ColorToken.SHORT),
    REAL(TokenTypes.DEFAULT_NUM_TOKEN_TYPES + 10, ColorToken.REAL),
    BYTE(TokenTypes.DEFAULT_NUM_TOKEN_TYPES + 11, ColorToken.BYTE),
    BOOLEAN(TokenTypes.LITERAL_BOOLEAN, ColorToken.BOOLEAN),
    DATETIME(TokenTypes.DEFAULT_NUM_TOKEN_TYPES + 12, ColorToken.DATETIME),
    TIMESTAMP(TokenTypes.DEFAULT_NUM_TOKEN_TYPES + 13, ColorToken.TIMESTAMP),
    TIMESPAN(TokenTypes.DEFAULT_NUM_TOKEN_TYPES + 14, ColorToken.TIMESPAN),
    SYSTEM(TokenTypes.PREPROCESSOR, ColorToken.SYSTEM),
    COMMAND(TokenTypes.VARIABLE, ColorToken.COMMAND);

    public final static int NUM_TOKEN_TYPES = TokenTypes.DEFAULT_NUM_TOKEN_TYPES + 15;

    private static RSToken[] tokenTypesToQToken = new RSToken[NUM_TOKEN_TYPES];
    static {
        Arrays.fill(tokenTypesToQToken, null);
        for (RSToken token: values()) {
            tokenTypesToQToken[token.getTokenType()] = token;
        }
    }

    public static RSToken fromTokenType(int tokenType) {
        RSToken result = tokenTypesToQToken[tokenType];
        if (result == null) throw new IllegalArgumentException(String.format("Token type %d is not defined", tokenType));
        return result;
    }

    public int getTokenType() {
        return tokenType;
    }

    private final int tokenType;

    private final int fontStyle;
    private final ColorToken colorToken;

    public int getFontStyle() {
        return fontStyle;
    }

    public ColorToken getColorToken() {
        return colorToken;
    }
    private
    RSToken(int tokenType, int fontStyle, ColorToken colorToken) {
        this.tokenType = tokenType;
        this.fontStyle = fontStyle;
        this.colorToken = colorToken;
    }

    RSToken(int tokenType, ColorToken colorToken) {
        this(tokenType, Font.PLAIN, colorToken);
    }

}

package studio.kdb.config;

import java.awt.*;

public enum ColorToken {
    DEFAULT(Color.BLACK),
    ERROR(Color.RED),
    BACKGROUND(Color.WHITE),
    WHITESPACE(Color.BLACK),
    BRACKET(Color.BLACK),
    OPERATOR(Color.BLACK),
    COMMAND(new Color(240,180,0)),
    SYSTEM(new Color(240,180,0)),
    EOLCOMMENT( Color.GRAY),
    KEYWORD(new Color(0,0,255)),
    IDENTIFIER(new Color(180,160,0)),
    SYMBOL(new Color(179,0,134)),
    CHARVECTOR(new Color(0,200,20)),
    BOOLEAN(new Color(51,204,255)),
    BYTE(new Color(51,104,255)),
    SHORT(new Color(51,104,255)),
    LONG(new Color(51,104,255)),
    REAL(new Color(51,104,255)),
    INTEGER(new Color(51,104,255)),
    FLOAT(new Color(51,104,255)),
    TIMESTAMP(new Color(184,138,0)),
    TIMESPAN(new Color(184,138,0)),
    DATETIME(new Color(184,138,0)),
    DATE(new Color(184,138,0)),
    MONTH(new Color(184,138,0)),
    MINUTE(new Color(184,138,0)),
    SECOND(new Color(184,138,0)),
    TIME(new Color(184,138,0));

    private Color color;
    public Color getColor() {
        return color;
    }
    ColorToken(Color color) {
        this.color = color;
    }
}

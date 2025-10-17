package studio.kdb.config;

import java.awt.*;

public enum ColorToken {
    DEFAULT("Default", Color.BLACK),
    ERROR("Error", Color.RED),
    WHITESPACE("Whitespace", Color.BLACK),
    BRACKET("Brackets", Color.BLACK),
    OPERATOR("Operators", Color.BLACK),
    COMMAND("Commands", new Color(240,180,0)),
    SYSTEM("System", new Color(240,180,0)),
    EOLCOMMENT("Comments", Color.GRAY),
    KEYWORD("Keywords", new Color(0,0,255)),
    IDENTIFIER("Variables", new Color(180,160,0)),
    SYMBOL("Symbols", new Color(179,0,134)),
    CHARVECTOR("Strings ", new Color(0,200,20)),
    BOOLEAN("Booleans", new Color(51,204,255)),
    BYTE("Bytes", new Color(51,104,255)),
    SHORT("Shorts", new Color(51,104,255)),
    LONG("Longs", new Color(51,104,255)),
    REAL("Reals", new Color(51,104,255)),
    INTEGER("Integers", new Color(51,104,255)),
    FLOAT("Floats", new Color(51,104,255)),
    TIMESTAMP("Timestamps", new Color(184,138,0)),
    TIMESPAN("Timespans", new Color(184,138,0)),
    DATETIME("Datetimes", new Color(184,138,0)),
    DATE("Dates", new Color(184,138,0)),
    MONTH("Months", new Color(184,138,0)),
    MINUTE("Minutes", new Color(184,138,0)),
    SECOND("Seconds", new Color(184,138,0)),
    TIME("Times", new Color(184,138,0));

    private final Color color;
    private final String description;

    public Color getColor() {
        return color;
    }

    public String getDescription() {
        return description;
    }

    ColorToken(String description, Color color) {
        this.description = description;
        this.color = color;
    }

}

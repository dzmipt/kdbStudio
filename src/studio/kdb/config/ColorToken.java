package studio.kdb.config;

import java.awt.*;

public enum ColorToken {
    DEFAULT("Default", Color.BLACK),
    ERROR("Error", Color.RED),
//    WHITESPACE("Whitespace", Color.BLACK),
    BRACKET("Bracket", Color.BLACK),
//    OPERATOR("Operators", Color.BLACK),
    COMMAND("Command", new Color(240,180,0)),
//    SYSTEM("System", new Color(240,180,0)),
    EOLCOMMENT("Comment", Color.GRAY),
    KEYWORD("Keyword", new Color(0,0,255)),
    IDENTIFIER("Variable", new Color(180,160,0)),
    SYMBOL("Symbol", new Color(179,0,134)),
    CHARVECTOR("String", new Color(0,200,20)),
    BOOLEAN("boolean", new Color(51,204,255)),
    BYTE("byte", new Color(51,104,255)),
    SHORT("short", new Color(51,104,255)),
    LONG("long", new Color(51,104,255)),
    REAL("real", new Color(51,104,255)),
    INTEGER("int", new Color(51,104,255)),
    FLOAT("float", new Color(51,104,255)),
    TIMESTAMP("timestamp", new Color(184,138,0)),
    TIMESPAN("timespan", new Color(184,138,0)),
    DATETIME("datetimes", new Color(184,138,0)),
    DATE("date", new Color(184,138,0)),
    MONTH("month", new Color(184,138,0)),
    MINUTE("minute", new Color(184,138,0)),
    SECOND("second", new Color(184,138,0)),
    TIME("time", new Color(184,138,0));

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

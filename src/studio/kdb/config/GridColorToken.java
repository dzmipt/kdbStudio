package studio.kdb.config;

public enum GridColorToken {

    NULL("null"),
    KEY("key"),
    EVEN("even"),
    ODD("odd"),
    MARK("marked"),
    SELECTED("selected"),
    MARK_SELECTED("marked and selected");

    public final static GridColorToken[] FG = GridColorToken.values();
    public final static GridColorToken[] BG = new GridColorToken[] {KEY, EVEN, ODD, MARK, SELECTED, MARK_SELECTED};

    private final String description;

    public String getDescription() {
        return description;
    }
    GridColorToken(String description) {
        this.description = description;
    }
}

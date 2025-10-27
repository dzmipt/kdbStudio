package studio.kdb.config;

public enum GridColorToken {

    KEY("key"),
    EVEN("even"),
    ODD("odd"),
    MARK("marked"),
    SELECTED("selected"),
    MARK_SELECTED("marked and selected");

    private final String description;

    public String getDescription() {
        return description;
    }
    GridColorToken(String description) {
        this.description = description;
    }
}

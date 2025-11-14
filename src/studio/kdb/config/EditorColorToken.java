package studio.kdb.config;

public enum EditorColorToken {

    BACKGROUND("background"),
    SELECTED("selected"),
    CURRENT_LINE_HIGHLIGHT("current line");

    private final String description;

    public String getDescription() {
        return description;
    }
    EditorColorToken(String description) {
        this.description = description;
    }

}

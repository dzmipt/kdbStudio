package studio.kdb.config;

public enum KdbMessageLimitAction {
    ASK("show dialog to download"),
    BLOCK("close session (do not download)");

    private final String description;

    KdbMessageLimitAction(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }

}

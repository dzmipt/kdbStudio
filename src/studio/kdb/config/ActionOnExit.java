package studio.kdb.config;

public enum ActionOnExit {
    NOTHING("Don't ask to save and keep workspace as is"),
    SAVE("Ask to save all modified tabs"),
    CLOSE_ANONYMOUS_NOT_SAVED("Ask to save and close all anonymous not-saved tabs");

    private final String description;

    ActionOnExit(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}



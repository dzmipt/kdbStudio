package studio.kdb.config;

public enum TLSResolutionMode {
    TCP("Plain tcp"),
    TLS("TLS"),
    TLS_TCP("TLS with fallback to tcp"),
    TCP_TLS("tcp with fallback to TLS");

    public static TLSResolutionMode get(boolean useTLS, boolean flipTLS) {
        if (useTLS) {
            return flipTLS ? TLS_TCP : TLS;
        } else {
            return flipTLS ? TCP_TLS : TCP;
        }
    }

    public boolean isFlipTLS() {
        return this == TLS_TCP || this == TCP_TLS;
    }

    public boolean isUseTLS() {
        return this == TLS_TCP || this == TLS;
    }

    private final String description;

    TLSResolutionMode(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}

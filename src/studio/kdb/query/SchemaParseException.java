package studio.kdb.query;

public class SchemaParseException extends Exception {
    public SchemaParseException(String message) {
        super(message);
    }

    public SchemaParseException(String message, Throwable cause) {
        super(message, cause);
    }
}

package kx;

public class K4Exception extends Exception {
    public K4Exception(String s) {
        super("'" + s);
    }
}

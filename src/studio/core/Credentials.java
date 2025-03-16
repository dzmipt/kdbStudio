package studio.core;

import java.util.Objects;

public class Credentials {
    private final String username;
    private final String password;

    public final static Credentials DEFAULT = new Credentials();

    public Credentials() {
        this("", "");
    }

    public Credentials(String username,String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Credentials)) return false;
        Credentials that = (Credentials) o;
        return Objects.equals(username, that.username) && Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password);
    }
}

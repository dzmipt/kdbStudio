package studio.kdb.config.server;

import studio.kdb.Server;
import studio.kdb.config.TLSResolutionMode;

import java.awt.*;
import java.util.List;

public class FieldGetter<E> {

    public enum Names {
        name("Server Name"),
        fullName("Full Name"),
        folderPath("Folder Path"),
        folderName("Folder Name"),
        host("Host"),
        port("Port"),
        tls("Use TLS"),
        user("Username"),
        password("Password"),
        auth("Auth. Method"),
        color("Background Color");

        private final String text;
        Names(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    private final Names name;
    private final Getter<E> getter;

    public final static FieldGetter<String> NAME = new FieldGetter<>(Names.name, Server::getName);
    public final static FieldGetter<String> FULL_NAME = new FieldGetter<>(Names.fullName, Server::getFullName);
    public final static FieldGetter<List<String>> FOLDER_PATH = new FieldGetter<>(Names.folderPath, Server::getFolderPath);
    public final static FieldGetter<String> FOLDER_NAME = new FieldGetter<>(Names.folderName, Server::getFolderName);
    public final static FieldGetter<String> HOST = new FieldGetter<>(Names.host, Server::getHost);
    public final static FieldGetter<Integer> PORT = new FieldGetter<>(Names.port, Server::getPort);
    public final static FieldGetter<TLSResolutionMode> TLS = new FieldGetter<>(Names.tls, Server::getTLSResolutionMode);
    public final static FieldGetter<String> USER = new FieldGetter<>(Names.user, Server::getUsername);
    public final static FieldGetter<String> PASSWORD = new FieldGetter<>(Names.password, Server::getPassword);
    public final static FieldGetter<String> AUTH = new FieldGetter<>(Names.auth, Server::getAuthenticationMechanism);
    public final static FieldGetter<Color> COLOR = new FieldGetter<>(Names.color, Server::getBackgroundColor);


    private interface Getter<E> {
        E get(Server server);
    }

    private FieldGetter(Names name, Getter<E> getter) {
        this.name = name;
        this.getter = getter;
    }

    public E getValue(Server server) {
        return getter.get(server);
    }

    public Names getName() {
        return name;
    }

    @Override
    public String toString() {
        return name.name();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FieldGetter)) return false;
        FieldGetter<?> that = (FieldGetter<?>) o;
        return name == that.name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}

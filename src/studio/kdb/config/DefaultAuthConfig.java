package studio.kdb.config;

import studio.core.Credentials;
import studio.core.DefaultAuthenticationMechanism;

import java.util.*;

public class DefaultAuthConfig {
    private final String defaultAuth;
    private final Map<String, Credentials> credentials;

    public static final DefaultAuthConfig DEFAULT = new DefaultAuthConfig(DefaultAuthenticationMechanism.NAME, new HashMap<>());

    public DefaultAuthConfig(String defaultAuth, Map<String, Credentials> credentials) {
        this.defaultAuth = defaultAuth;
        this.credentials = new TreeMap<>();
        init(credentials);
    }

    public DefaultAuthConfig(DefaultAuthConfig config, String defaultAuth) {
        this(defaultAuth, config.credentials);
    }

    public DefaultAuthConfig(DefaultAuthConfig config, String auth, Credentials credential) {
        this(config.getDefaultAuth(), config.credentials);
        set(auth, credential);
    }

    private void init(Map<String, Credentials> credentials) {
        for (String authMethod: credentials.keySet()) {
            Credentials credential = credentials.get(authMethod);
            set(authMethod, credential);
        }
    }

    private void set(String authMethod, Credentials credential) {
        if (! credential.equals(Credentials.DEFAULT)) {
            this.credentials.put(authMethod, credential);
        }
    }

    public String getDefaultAuth() {
        return defaultAuth;
    }

    public Set<String> getAuthMethods() {
        return credentials.keySet();
    }

    public Credentials getCredential(String authMethod) {
        Credentials credential = credentials.get(authMethod);

        if (credential == null) return Credentials.DEFAULT;
        return credential;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DefaultAuthConfig)) return false;
        DefaultAuthConfig that = (DefaultAuthConfig) o;
        if (! defaultAuth.equals(that.defaultAuth)) return false;

        if (credentials.size() != that.credentials.size()) return false;

        for (String key: credentials.keySet()) {
            if (! credentials.get(key).equals(that.credentials.get(key))) return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(defaultAuth, credentials);
    }
}

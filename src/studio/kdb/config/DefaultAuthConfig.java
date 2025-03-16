package studio.kdb.config;

import studio.core.Credentials;
import studio.core.DefaultAuthenticationMechanism;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

public class DefaultAuthConfig {
    private String defaultAuth = DefaultAuthenticationMechanism.NAME;
    private final Map<String, Credentials> credentials = new TreeMap<>();

    public DefaultAuthConfig() {
    }

    public String getDefaultAuth() {
        return defaultAuth;
    }

    public Set<String> getAuthMethods() {
        return credentials.keySet();
    }

    public void setDefaultAuth(String defaultAuth) {
        this.defaultAuth = defaultAuth;
    }

    public Credentials getCredential(String authMethod) {
        Credentials credential = credentials.get(authMethod);

        if (credential == null) return Credentials.DEFAULT;
        return credential;
    }

    public void setCredentials(String authMethod, Credentials credential) {
        if (credential.equals(Credentials.DEFAULT)) {
            credentials.remove(authMethod);
        } else {
            credentials.put(authMethod, credential);
        }
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

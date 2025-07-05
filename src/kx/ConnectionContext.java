package kx;

import studio.core.DefaultAuthenticationMechanism;

import java.security.cert.X509Certificate;

public class ConnectionContext {
    private boolean trusted = false;
    private boolean connected = false;
    private boolean secure = false;
    private X509Certificate[] certChain;
    private String authMethod = DefaultAuthenticationMechanism.NAME;

    public boolean isTrusted() {
        return trusted;
    }

    public void setTrusted(boolean trusted) {
        this.trusted = trusted;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public String getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
    }

    public X509Certificate[] getCertChain() {
        return certChain;
    }

    public void setCertChain(X509Certificate[] certChain) {
        this.certChain = certChain;
    }
}

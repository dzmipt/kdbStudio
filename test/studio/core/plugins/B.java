package studio.core.plugins;

import studio.core.Credentials;
import studio.core.IAuthenticationMechanism;

import java.util.Properties;

public class B implements IAuthenticationMechanism {
    @Override
    public String getMechanismName() {
        return "B";
    }

    @Override
    public String[] getMechanismPropertyNames() {
        return new String[0];
    }

    @Override
    public void setProperties(Properties props) {

    }

    @Override
    public Credentials getCredentials() {
        return null;
    }
}

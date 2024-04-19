package studio.kdb.config.rules;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.kdb.Server;

import java.awt.*;
import java.util.Optional;
import java.util.Properties;

public class Rule {
    private Optional<Matcher.Regex> matcherFullPath;
    private Optional<Matcher.Regex> matcherHost;
    private Optional<Matcher.Int> matcherPort;

    private Optional<String> username;
    private Optional<String> password;
    private Optional<String> authMethod;
    private Optional<Color> bgColor;
    private Optional<Boolean> useTLS;

    private static final Logger log = LogManager.getLogger();

    public Rule(Optional<Matcher.Regex> matcherFullPath,
                Optional<Matcher.Regex> matcherHost,
                Optional<Matcher.Int> matcherPort,
                Optional<String> username,
                Optional<String> password,
                Optional<String> authMethod,
                Optional<Color> bgColor,
                Optional<Boolean> useTLS) {
        this.matcherFullPath = matcherFullPath;
        this.matcherHost = matcherHost;
        this.matcherPort = matcherPort;
        this.username = username;
        this.password = password;
        this.authMethod = authMethod;
        this.bgColor = bgColor;
        this.useTLS = useTLS;
    }

    public Rule() {
        this(Optional.empty(),Optional.empty(),Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public Optional<Matcher.Regex> getMatcherFullPath() {
        return matcherFullPath;
    }

    public void setMatcherFullPath(Optional<Matcher.Regex> matcherFullPath) {
        this.matcherFullPath = matcherFullPath;
    }

    public Optional<Matcher.Regex> getMatcherHost() {
        return matcherHost;
    }

    public void setMatcherHost(Optional<Matcher.Regex> matcherHost) {
        this.matcherHost = matcherHost;
    }

    public Optional<Matcher.Int> getMatcherPort() {
        return matcherPort;
    }

    public void setMatcherPort(Optional<Matcher.Int> matcherPort) {
        this.matcherPort = matcherPort;
    }

    public Optional<String> getUsername() {
        return username;
    }

    public void setUsername(Optional<String> username) {
        this.username = username;
    }

    public Optional<String> getPassword() {
        return password;
    }

    public void setPassword(Optional<String> password) {
        this.password = password;
    }

    public Optional<String> getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(Optional<String> authMethod) {
        this.authMethod = authMethod;
    }

    public Optional<Color> getBgColor() {
        return bgColor;
    }

    public void setBgColor(Optional<Color> bgColor) {
        this.bgColor = bgColor;
    }

    public Optional<Boolean> getUseTLS() {
        return useTLS;
    }

    public void setUseTLS(Optional<Boolean> useTLS) {
        this.useTLS = useTLS;
    }

    public Server apply(Server server) {
        boolean matched = true;
        try {
            if (matcherFullPath.isPresent()) {
                matched = matcherFullPath.get().match(server.getFullName());
            }
            if (matched && matcherHost.isPresent()) {
                matched = matcherHost.get().match(server.getHost());
            }
            if (matched && matcherPort.isPresent()) {
                matched = matcherPort.get().match(Integer.toString(server.getPort()));
            }
        } catch (IllegalArgumentException e) {
            matched = false;
        }

        if (! matched) return server;

        boolean override = false;
        Optional<String> cfgUserName = server.getCfgUsername();
        if (! cfgUserName.isPresent() && username.isPresent()) {
            cfgUserName = username;
            override = true;
        }

        Optional<String> cfgPassword= server.getCfgPassword();
        if (! cfgPassword.isPresent() && password.isPresent()) {
            cfgPassword = password;
            override = true;
        }

        Optional<String> cfgAuthMethod= server.getCfgAuthMethod();
        if (! cfgAuthMethod.isPresent() && authMethod.isPresent()) {
            cfgAuthMethod = authMethod;
            override = true;
        }

        Optional<Color> cfgBgColor= server.getCfgBgColor();
        if (! cfgBgColor.isPresent() && bgColor.isPresent()) {
            cfgBgColor = bgColor;
            override = true;
        }

        Optional<Boolean> cfgUseTLS = server.getCfgUseTLS();
        if (! cfgUseTLS.isPresent() && useTLS.isPresent()) {
            cfgUseTLS = useTLS;
            override = true;
        }

        if (! override) return server;

        return new Server(server.getName(), server.getHost(), server.getPort(),
                cfgUserName, cfgPassword, cfgBgColor, cfgAuthMethod, cfgUseTLS,
                server.getFolderPath());
    }


    public void save(Properties p, String prefix) {
        matcherFullPath.ifPresent(m -> p.setProperty(prefix + "fullPath", m.getPattern()));
        matcherHost.ifPresent(m -> p.setProperty(prefix + "host", m.getPattern()));
        matcherPort.ifPresent(m -> p.setProperty(prefix + "port", m.getPattern()));

        username.ifPresent(value -> p.setProperty(prefix + "username", value));
        password.ifPresent(value -> p.setProperty(prefix + "password", value));
        bgColor.ifPresent(value -> p.setProperty(prefix + "bgColor", Integer.toHexString(value.getRGB()).substring(2)));
        authMethod.ifPresent(value -> p.setProperty(prefix + "authMethod", value));
        useTLS.ifPresent(value -> p.setProperty(prefix + "useTLS", Boolean.toString(value)));
    }

    public boolean load(Properties p, String prefix) {
        matcherFullPath = Optional.empty();
        matcherHost = Optional.empty();
        matcherPort = Optional.empty();

        username = Optional.empty();
        password = Optional.empty();
        bgColor = Optional.empty();
        authMethod = Optional.empty();
        useTLS = Optional.empty();

        boolean loaded = false;
        try {
            String fullPath = p.getProperty(prefix + "fullPath", null);
            if (fullPath != null) {
                matcherFullPath = Optional.of(new Matcher.Regex(fullPath));
                loaded = true;
            }

            String host = p.getProperty(prefix + "host", null);
            if (host != null) {
                matcherHost = Optional.of(new Matcher.Regex(host));
                loaded = true;
            }

            String port = p.getProperty(prefix + "port", null);
            if (host != null) {
                matcherPort = Optional.of(new Matcher.Int(port));
                loaded = true;
            }

            String pUsername = p.getProperty(prefix + "username", null);
            if (pUsername != null) {
                username = Optional.of(pUsername);
                loaded = true;
            }

            String pPassword = p.getProperty(prefix + "password", null);
            if (pPassword != null) {
                password = Optional.of(pPassword);
                loaded = true;
            }

            String pBgColor = p.getProperty(prefix + "bgColor", null);
            if (pBgColor != null) {
                bgColor = Optional.of(new Color(Integer.parseInt(pBgColor, 16)));
                loaded = true;
            }

            String pAuthMethod = p.getProperty(prefix + "authMethod", null);
            if (pAuthMethod != null) {
                authMethod = Optional.of(pAuthMethod);
                loaded = true;
            }

            String pUseTLS = p.getProperty(prefix + "useTLS", null);
            if (pUseTLS != null) {
                useTLS = Optional.of(Boolean.parseBoolean(pUseTLS));
                loaded = true;
            }

        } catch (IllegalArgumentException e) {
            log.error("Error in loading rule config", e);
        }

        return loaded;
    }
}

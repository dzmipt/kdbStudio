package studio.kdb.config.rules;

import studio.core.DefaultAuthenticationMechanism;
import studio.kdb.Server;

import java.awt.*;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Properties;

public class RuleSet extends ArrayList<Rule> {

    private Rule defaultRule;

    public RuleSet() {
        defaultRule = new Rule();
        checkDefaultRule();
    }

    private void checkDefaultRule() {
        defaultRule.setMatcherFullPath(Optional.empty());
        defaultRule.setMatcherHost(Optional.empty());
        defaultRule.setMatcherPort(Optional.empty());

        if (! defaultRule.getUsername().isPresent()) defaultRule.setUsername(Optional.of(""));
        if (! defaultRule.getPassword().isPresent()) defaultRule.setPassword(Optional.of(""));
        if (! defaultRule.getAuthMethod().isPresent()) defaultRule.setAuthMethod(Optional.of(DefaultAuthenticationMechanism.NAME));
        if (! defaultRule.getBgColor().isPresent()) defaultRule.setBgColor(Optional.of(Color.WHITE));
        if (! defaultRule.getUseTLS().isPresent()) defaultRule.setUseTLS(Optional.of(false));
    }

    public Rule getDefaultRule() {
        return defaultRule;
    }

    public Server apply(Server server) {
        for (Rule rule: this) {
            server = rule.apply(server);
        }
        return server;
    }

    public void save(Properties p) {
        defaultRule.save(p,"defaultServerRule.");

        int index = 0;
        for (Rule rule: this) {
            rule.save(p, "serverRule" + index + ".");
            index++;
        }
    }

    public void load(Properties p) {
        defaultRule = new Rule();
        defaultRule.load(p, "defaultServerRule.");
        checkDefaultRule();

        for (int index = 0; ;index++) {
            Rule rule = new Rule();
            boolean loaded = rule.load(p, "serverRule" + index + ".");
            if (!loaded) break;
            add(rule);
        }
    }

}

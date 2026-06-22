package studio.kdb.config;

import org.junit.jupiter.api.Test;
import studio.kdb.config.server.FieldGetter;
import studio.kdb.config.server.Operation;
import studio.kdb.config.server.ServerFilterRule;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

public class ServerFilterRuleTest {

    @Test
    public void fromTest() {
        ServerFilterRule<?> rule = ServerFilterRule.newDefault();
        assertEquals(FieldGetter.Names.name, rule.getFieldName());
        assertEquals(Operation.Names.contains, rule.getOperationName());

        assertEquals("", rule.newEditable().getArg());
        assertEquals(rule, rule.newEditable().getRule());


        rule = ServerFilterRule.from(FieldGetter.Names.folderName, Operation.Names.likes);
        assertEquals(FieldGetter.Names.folderName, rule.getFieldName());
        assertEquals(Operation.Names.likes, rule.getOperationName());

        assertEquals("", rule.newEditable().getArg());
        assertEquals(rule, rule.newEditable().getRule());

        rule = ServerFilterRule.from(FieldGetter.Names.port, Operation.Names.bigger);
        assertEquals(FieldGetter.Names.port, rule.getFieldName());
        assertEquals(Operation.Names.bigger, rule.getOperationName());

        assertEquals(0, rule.newEditable().getArg());
        assertEquals(rule, rule.newEditable().getRule());


        rule = ServerFilterRule.from(FieldGetter.Names.tls, Operation.Names.equals);
        assertEquals(FieldGetter.Names.tls, rule.getFieldName());
        assertEquals(Operation.Names.equals, rule.getOperationName());

        assertEquals(false, rule.newEditable().getArg());
        assertEquals(rule, rule.newEditable().getRule());
    }

    @Test
    public void editTest() {
        ServerFilterRule<String> rule = (ServerFilterRule<String>) ServerFilterRule.newDefault();

        ServerFilterRule.EditableServerFilterRule<String> editable = rule.newEditable();

        editable.setArg("");
        assertEquals(rule, editable.getRule());

        editable.setArg("testValue");
        assertEquals("testValue", editable.getArg());
        assertNotEquals(rule, editable.getRule());

        editable = rule.newEditable();
        editable.setColor(new Color(11, 22, 33));
        assertEquals(new Color(11, 22, 33), editable.getColor());
        assertNotEquals(rule, editable.getRule());

        editable = editable.getRule().newEditable();
        assertEquals(new Color(11, 22, 33), editable.getColor());
    }

    @Test
    public void editDoesntBreakInitTest() {
        ServerFilterRule<String> rule = (ServerFilterRule<String>) ServerFilterRule.from(FieldGetter.Names.user, Operation.Names.likes);
        ServerFilterRule.EditableServerFilterRule<String> editable = rule.newEditable();
        Color initColor = editable.getColor();

        assertEquals("", editable.getArg());
        editable.setColor(new Color(44, 55, 66));
        editable.setArg("aValue");

        rule = editable.getRule();
        editable = rule.newEditable();
        assertEquals("aValue", editable.getArg());
        assertEquals(new Color(44, 55, 66), editable.getColor());


        rule = (ServerFilterRule<String>) ServerFilterRule.from(FieldGetter.Names.user, Operation.Names.likes);
        editable = rule.newEditable();
        assertEquals("", editable.getArg());
        assertEquals(initColor, editable.getColor());

    }

    @Test
    public void editIntTest() {
        ServerFilterRule<Integer> rule = (ServerFilterRule<Integer>) ServerFilterRule.from(FieldGetter.PORT.getName(), Operation.Names.bigger);

        ServerFilterRule.EditableServerFilterRule<Integer> editable = rule.newEditable();
        editable.setArg(0);
        assertEquals(rule, editable.getRule());

        editable.setArg(10000);
        assertEquals(10000, editable.getArg());
        assertNotEquals(rule, editable.getRule());
    }

    @Test
    public void errorTest() {
        ServerFilterRule<String> rule = (ServerFilterRule<String>) ServerFilterRule.from(FieldGetter.Names.fullName, Operation.Names.likes);

        ServerFilterRule.EditableServerFilterRule<String> editable = rule.newEditable();

        assertEquals("", editable.getArg());
        editable.setArg("newArg");
        assertEquals("newArg", editable.getArg());

        ServerFilterRule<String> newRule = editable.getRule();

        assertThrows(RuntimeException.class, () -> editable.setArg("(invalidRegexp"));
        assertEquals("newArg", editable.getArg());
        assertEquals(newRule, editable.getRule());
    }

}
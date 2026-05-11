package studio.ui.settings;

import studio.core.Studio;
import studio.kdb.config.server.FieldGetter;
import studio.kdb.config.server.Operation;
import studio.kdb.config.server.ServerFilterRule;
import studio.ui.ColorLabel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ServerFilterRulePanel extends JPanel {

    private final JLabel lblWhen = new JLabel("When ");
    private final JLabel lblSet = new JLabel(", background color is replaced to ");


    private JComboBox<FieldGetter.Names> comboField;
    private JComboBox<Operation.Names> comboOperation = null;
    private final ColorLabel colorLabel = new ColorLabel();

    private final Cursor HAND_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

    private ServerFilterRule.EditableServerFilterRule<?> rule;

    public ServerFilterRulePanel(ServerFilterRule<?> rule) {
        super(new BorderLayout());
        setRule(rule);
    }

    private void setRule(ServerFilterRule<?> rule) {
        Color color = this.rule == null ? null : this.rule.getColor();
        this.rule = rule.newEditable();
        if (color != null) this.rule.setColor(color);
        refresh();
    }

    public ServerFilterRule<?> getRule() {
        return rule.getRule();
    }

    private void refresh() {
        comboField = new JComboBox<>(ServerFilterRule.getFieldNames());
        comboField.setSelectedItem(rule.getFieldName());
        comboField.addActionListener(this::fieldSelected);

        comboOperation = new JComboBox<>(rule.getOperationNames());
        comboOperation.setSelectedItem(rule.getOperationName());
        comboOperation.addActionListener(this::operationSelected);

        colorLabel.setColor(rule.getColor());
        colorLabel.addChangeListener(this::colorSelected);
        colorLabel.setCursor(HAND_CURSOR);

        removeAll();
        JPanel left = new JPanel(new FlowLayout());
        left.add(lblWhen);
        left.add(comboField);
        left.add(comboOperation);

        JPanel right = new JPanel(new FlowLayout());
        right.add(lblSet);
        right.add(colorLabel);

        add(left, BorderLayout.WEST);
        add(rule.getComponent(), BorderLayout.CENTER);
        add(right, BorderLayout.EAST);

        revalidate();
        repaint();
    }

    private void fieldSelected(ActionEvent evt) {
        FieldGetter.Names fieldName = (FieldGetter.Names) comboField.getSelectedItem();
        if (rule.getFieldName() == fieldName) return;
        setRule(ServerFilterRule.fromField(fieldName));
    }

    private void operationSelected(ActionEvent evt) {
        Operation.Names opName = (Operation.Names) comboOperation.getSelectedItem();
        if (rule.getOperationName() == opName) return;

        setRule(ServerFilterRule.from((FieldGetter.Names)comboField.getSelectedItem(), opName) );
    }

    private void colorSelected(ChangeEvent evt) {
        rule.setColor(colorLabel.getColor());
    }

    public static void main(String[] args) {
        Studio.initLF();

        JPanel content = new JPanel(new BorderLayout());

        ServerFilterRule<Integer> rule = ServerFilterRule.newRule(FieldGetter.Names.port, Operation.Names.smaller, Color.RED, 1024 );

        content.add(new ServerFilterRulePanel(rule), BorderLayout.NORTH);

        JFrame frame = new JFrame("Test");
        frame.setContentPane(content);
        frame.setSize(600, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

    }
}

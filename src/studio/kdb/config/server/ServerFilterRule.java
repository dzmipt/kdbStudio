package studio.kdb.config.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import studio.kdb.Config;
import studio.kdb.Server;
import studio.kdb.config.ConfigType;
import studio.kdb.config.TLSResolutionMode;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class ServerFilterRule<E> {

    private final static List<FieldGetter<?>> fields = List.of(
            FieldGetter.NAME,
            FieldGetter.FULL_NAME,
            FieldGetter.FOLDER_NAME,
            FieldGetter.HOST,
            FieldGetter.PORT,
            FieldGetter.AUTH,
            FieldGetter.TLS,
            FieldGetter.USER
    );

    private final static Map<FieldGetter.Names, Map<Operation.Names, ServerFilterRule<?>>> rules = new LinkedHashMap<>();


    private static <E> Map<Operation.Names, ServerFilterRule<?>> getOpMap(FieldGetter<E> field,
                                                                 Supplier<Editor<E>> editorCreator,
                                                                 Supplier<Operation<E>>... opCreators) {
        Map<Operation.Names, ServerFilterRule<?>> map = new LinkedHashMap<>();
        for (Supplier<Operation<E>> opCreator: opCreators) {
            map.put(opCreator.get().getName(), new ServerFilterRule<E>(field, opCreator, editorCreator) );
        }

        return map;
    }

    static {
        for (FieldGetter<?> f: fields) {
            Map<Operation.Names, ServerFilterRule<?>> map;
            Object value = f.getValue(Server.NO_SERVER);
            if (value instanceof String) {
                if (f.getName() == FieldGetter.Names.auth) {
                    map = getOpMap((FieldGetter<String>) f,
                            Editor.AuthMethodEditor::new,
                            () -> Operation.newEquals("")
                    );
                } else {
                    map = getOpMap((FieldGetter<String>) f,
                            Editor.TextEditor::new,
                            () -> Operation.newContains(""),
                            () -> Operation.newLikes("")
                    );
                }
            } else if (value instanceof Integer) {

                map = getOpMap((FieldGetter<Integer>) f,
                        Editor.IntEditor::new,
                        () -> Operation.newEquals(0),
                        () -> Operation.newBigger(0),
                        () -> Operation.newLess(0)
                );

            } else if (value instanceof TLSResolutionMode) {

                map = getOpMap((FieldGetter<TLSResolutionMode>) f,
                        Editor.TLSEditor::new,
                        () -> Operation.newEquals(TLSResolutionMode.TLS)
                );

            } else {
                throw new IllegalStateException("Unexpected Server field type: " + f);
            }

            rules.put(f.getName(), map);
        }
    }


    public static ServerFilterRule<?> from(FieldGetter.Names fieldName, Operation.Names opName) {
        return rules.get(fieldName).get(opName);
    }

    public static ServerFilterRule<?> fromField(FieldGetter.Names fieldName) {
        return rules.get(fieldName).values().iterator().next();
    }

    public static ServerFilterRule<?> newDefault() {
        return from(FieldGetter.Names.name, Operation.Names.contains);
    }

    public static ServerFilterRule<String> newRule(FieldGetter.Names fieldName, Operation.Names opName,
                                                   Color color, String arg) {
        ServerFilterRule<String> rule = (ServerFilterRule<String>)from (fieldName, opName);
        return rule.newEditable().setColor(color).setArg(arg).getRule();
    }

    public static ServerFilterRule<Integer> newRule(FieldGetter.Names fieldName, Operation.Names opName,
                                                   Color color, int arg) {
        ServerFilterRule<Integer> rule = (ServerFilterRule<Integer>)from (fieldName, opName);
        return rule.newEditable().setColor(color).setArg(arg).getRule();
    }

    public static ServerFilterRule<TLSResolutionMode> newRule(FieldGetter.Names fieldName, Operation.Names opName,
                                                   Color color, TLSResolutionMode arg) {
        ServerFilterRule<TLSResolutionMode> rule = (ServerFilterRule<TLSResolutionMode>)from (fieldName, opName);
        return rule.newEditable().setColor(color).setArg(arg).getRule();
    }

    public static FieldGetter.Names[] getFieldNames() {
        return fields.stream().map(FieldGetter::getName).toArray(FieldGetter.Names[]::new);
    }

    public static Operation.Names[] getOperationNames(FieldGetter.Names fieldName) {
        return rules.get(fieldName).keySet().toArray(Operation.Names[]::new);
    }

    public Operation.Names[] getOperationNames() {
        return getOperationNames(getFieldName());
    }

    protected final FieldGetter<E> field;
    protected final Operation<E> operation;

    protected final Supplier<Operation<E>> operationCreator;
    protected final Supplier<Editor<E>> editorCreator;

    protected Color color;

    protected ServerFilterRule(FieldGetter<E> field,
                            Supplier<Operation<E>> operationCreator,
                            Supplier<Editor<E>> editorCreator) {
        this.operationCreator = operationCreator;
        this.editorCreator = editorCreator;

        this.field = field;
        this.operation = operationCreator.get();
        this.color = null;
    }

    public boolean test(Server server) {
        E value = field.getValue(server);
        return operation.test(value);
    }

    public Color getServerColor(Server server) {
        if (test(server)) {
            return this.getColor();
        } else {
            return null;
        }
    }

    public FieldGetter.Names getFieldName() {
        return field.getName();
    }

    public Operation.Names getOperationName() {
        return operation.getName();
    }

    public Color getColor() {
        return color == null ? Config.getInstance().getBackgroundColor() : color;
    }

    public EditableServerFilterRule<E> newEditable() {
        EditableServerFilterRule<E> rule = new EditableServerFilterRule<>(field, operationCreator, editorCreator);
        rule.setArg(this.operation.getArg());
        rule.color = this.color;
        return rule;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (color != null) {
            json.add("color", ConfigType.COLOR.toJson(color));
        }
        json.add("field", ConfigType.ENUM.toJson(field.getName()) );
        json.add("operation", ConfigType.ENUM.toJson(operation.getName()));

        Object value = operation.getArg();
        JsonElement jsonArg;
        if (value instanceof String) {
            jsonArg = new JsonPrimitive((String) value);
        } else if (value instanceof Integer) {
            jsonArg = new JsonPrimitive((Integer)value);
        } else if (value instanceof TLSResolutionMode) {
            jsonArg = ConfigType.ENUM.toJson(value);
        } else {
            throw new IllegalStateException("Unknown argument type for field: " + field);
        }
        json.add("argument", jsonArg);
        return json;
    }

    public static ServerFilterRule<?> fromJson(JsonObject json) {
        FieldGetter.Names fieldName = (FieldGetter.Names) ConfigType.ENUM.fromJson(json.get("field"), FieldGetter.Names.name);
        Operation.Names opName = (Operation.Names) ConfigType.ENUM.fromJson(json.get("operation"), Operation.Names.equals);

        Color color = null;
        JsonElement jsonColor = json.get("color");
        if (jsonColor != null) {
            color = (Color) ConfigType.COLOR.fromJson(jsonColor, null);
        }
        JsonElement jsonArg = json.get("argument");

        ServerFilterRule<?> rule = from(fieldName, opName);
        ServerFilterRule.EditableServerFilterRule<?> editableRule = rule.newEditable();

        if (color != null) editableRule.setColor(color);

        Object defaultValue = rule.field.getValue(Server.NO_SERVER);
        if (defaultValue instanceof String) {
            ((ServerFilterRule.EditableServerFilterRule<String>)editableRule).setArg(jsonArg.getAsString());
        } else if (defaultValue instanceof Integer) {
            ((ServerFilterRule.EditableServerFilterRule<Integer>)editableRule).setArg(jsonArg.getAsInt());
        } else if (defaultValue instanceof TLSResolutionMode) {
            TLSResolutionMode tls = (TLSResolutionMode) ConfigType.ENUM.fromJson(jsonArg, TLSResolutionMode.TLS);
            ((ServerFilterRule.EditableServerFilterRule<TLSResolutionMode>)editableRule).setArg(tls);
        } else {
            throw new IllegalStateException("Unknown argument type for field: " + rule.field);
        }

        return editableRule.getRule();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ServerFilterRule)) return false;
        ServerFilterRule<?> that = (ServerFilterRule<?>) o;

        return Objects.equals(field, that.field)
                && Objects.equals(operation, that.operation)
                && Objects.equals(color, that.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, operation, color);
    }

    public static class EditableServerFilterRule<E> extends ServerFilterRule<E> {

        private final Editor<E> editor;

        private EditableServerFilterRule(FieldGetter<E> field,
                                 Supplier<Operation<E>> operationCreator,
                                 Supplier<Editor<E>> editorCreator) {
            super(field, operationCreator, editorCreator);

            editor = editorCreator.get();
            editor.setChangeListener(evt -> operation.setArg(editor.getValue()));
        }

        public E getArg() {
            return operation.getArg();
        }

        public EditableServerFilterRule<E> setArg(E value) {
            operation.setArg(value);
            editor.setValue(value);
            return this;
        }

        public EditableServerFilterRule<E> setColor(Color color) {
            this.color = color;
            return this;
        }

        public JComponent getComponent() {
            return editor.getComponent();
        }

        public ServerFilterRule<E> getRule() {
            ServerFilterRule<E> rule = new ServerFilterRule<>(field, operationCreator, editorCreator);
            rule.operation.setArg(this.operation.getArg());
            rule.color = this.color;
            return rule;
        }
    }

}

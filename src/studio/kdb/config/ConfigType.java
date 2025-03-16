package studio.kdb.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import studio.core.Credentials;
import studio.kdb.FileChooserConfig;

import java.awt.*;
import java.lang.reflect.Array;

public enum ConfigType {
    STRING {
        @Override
        public Object fromJson(JsonElement jsonElement, Object defaultValue) {
            return jsonElement.getAsString();
        }

        @Override
        public JsonElement toJson(Object value) {
            return new JsonPrimitive((String) value);
        }
    },
    INT {
        @Override
        public Object fromJson(JsonElement jsonElement, Object defaultValue) {
            return jsonElement.getAsInt();
        }

        @Override
        public JsonElement toJson(Object value) {
            return new JsonPrimitive((Integer) value);
        }
    },
    DOUBLE {
        @Override
        public Object fromJson(JsonElement jsonElement, Object defaultValue) {
            return jsonElement.getAsDouble();
        }

        @Override
        public JsonElement toJson(Object value) {
            return new JsonPrimitive((Double) value);
        }
    },
    BOOLEAN {
        @Override
        public Object fromJson(JsonElement jsonElement, Object defaultValue) {
            return jsonElement.getAsBoolean();
        }

        @Override
        public JsonElement toJson(Object value) {
            return new JsonPrimitive((Boolean) value);
        }
    },
    FONT {
        @Override
        public Object fromJson(JsonElement jsonElement, Object defaultValue) {
            JsonObject json = jsonElement.getAsJsonObject();
            String name = json.get("name").getAsString();
            int size = json.get("size").getAsInt();
            int style = FontStyle.valueOf(json.get("style").getAsString()).getStyle();
            return new Font(name, style, size);
        }

        @Override
        public JsonElement toJson(Object value) {
            Font font = (Font) value;
            JsonObject json = new JsonObject();
            json.add("name", new JsonPrimitive(font.getName()));
            json.add("size", new JsonPrimitive(font.getSize()));
            FontStyle fontStyle = FontStyle.values()[font.getStyle()];
            json.add("style", new JsonPrimitive(fontStyle.name()));
            return json;
        }
    },
    BOUNDS {
        @Override
        public Object fromJson(JsonElement jsonElement, Object defaultValue) {
            JsonObject json = jsonElement.getAsJsonObject();
            int x = json.get("x").getAsInt();
            int y = json.get("y").getAsInt();
            int width = json.get("width").getAsInt();
            int height = json.get("height").getAsInt();
            return new Rectangle(x, y, width, height);
        }

        @Override
        public JsonElement toJson(Object value) {
            Rectangle bounds = (Rectangle) value;
            JsonObject json = new JsonObject();
            json.add("x", new JsonPrimitive(bounds.x));
            json.add("y", new JsonPrimitive(bounds.y));
            json.add("width", new JsonPrimitive(bounds.width));
            json.add("height", new JsonPrimitive(bounds.height));
            return json;
        }
    },
    COLOR {
        @Override
        public Object fromJson(JsonElement jsonElement, Object defaultValue) {
            String value = jsonElement.getAsString();
            return new Color(Integer.parseInt(value, 16));
        }

        @Override
        public JsonElement toJson(Object value) {
            Color color = (Color) value;
            return new JsonPrimitive(Integer.toHexString(color.getRGB()).substring(2));
        }
    },
    ENUM {
        @Override
        public Object fromJson(JsonElement jsonElement, Object defaultValue) {
            String value = jsonElement.getAsString();
            return Enum.valueOf((Class<? extends Enum>) defaultValue.getClass(), value);
        }

        @Override
        public JsonElement toJson(Object value) {
            return new JsonPrimitive(((Enum)value).name());
        }
    },
    SIZE {
        @Override
        public Object fromJson(JsonElement jsonElement, Object defaultValue) {
            JsonObject json = jsonElement.getAsJsonObject();
            int width = json.get("width").getAsInt();
            int height = json.get("height").getAsInt();
            return new Dimension(width, height);
        }

        @Override
        public JsonElement toJson(Object value) {
            Dimension size = (Dimension) value;
            JsonObject json = new JsonObject();
            json.add("width", new JsonPrimitive(size.width));
            json.add("height", new JsonPrimitive(size.height));
            return json;
        }
    },
    FILE_CHOOSER {
        @Override
        public Object fromJson(JsonElement jsonElement, Object defaultValue) {
            JsonObject json = jsonElement.getAsJsonObject();
            String filename = json.get("filename").getAsString();
            Dimension size = (Dimension) SIZE.fromJson(json.get("size"), null);
            return new FileChooserConfig(filename, size);
        }

        @Override
        public JsonElement toJson(Object value) {
            FileChooserConfig config = (FileChooserConfig) value;
            JsonObject json = new JsonObject();
            json.add("filename", new JsonPrimitive(config.getFilename()));
            json.add("size", SIZE.toJson(config.getPreferredSize()));
            return json;
        }

    },
    CREDENTIALS {
        @Override
        public Object fromJson(JsonElement jsonElement, Object defaultValue) {
            JsonObject json = jsonElement.getAsJsonObject();
            String user = json.get("user").getAsString();
            String password = json.get("password").getAsString();
            return new Credentials(user, password);
        }

        @Override
        public JsonElement toJson(Object value) {
            Credentials credentials = (Credentials) value;
            JsonObject json = new JsonObject();
            json.add("user", new JsonPrimitive(((Credentials) value).getUsername()));
            json.add("password", new JsonPrimitive(((Credentials) value).getPassword()));
            return json;
        }
    },
    DEFAULT_AUTH_CONFIG {
        @Override
        public Object fromJson(JsonElement jsonElement, Object defaultValue) {
            DefaultAuthConfig config = new DefaultAuthConfig();
            JsonObject json = jsonElement.getAsJsonObject();
            String auth = json.get("default").getAsString();
            config.setDefaultAuth(auth);
            for (String method: json.keySet()) {
                if (method.equals("default")) continue;
                config.setCredentials(method, (Credentials) CREDENTIALS.fromJson(json.get(method), null));
            }
            return config;
        }

        @Override
        public JsonElement toJson(Object value) {
            DefaultAuthConfig config = (DefaultAuthConfig) value;
            JsonObject json = new JsonObject();
            json.add("default", new JsonPrimitive(config.getDefaultAuth()));
            for (String method: config.getAuthMethods()) {
                json.add(method, CREDENTIALS.toJson(config.getCredential(method)));
            }
            return json;
        }
    },
    STRING_ARRAY(STRING),
    INT_ARRAY(INT),
    ENUM_ARRAY(ENUM);

    private final ConfigType elementType;

    ConfigType() {
        this(null);
    }

    ConfigType(ConfigType elementType) {
        this.elementType = elementType;
    }
    public Object fromJson(JsonElement jsonElement, Object defaultValue) {
        if (elementType == null) throw new IllegalStateException("ConfigType should implement fromJson: " + name());

        JsonArray jsonArray = jsonElement.getAsJsonArray();
        int count = jsonArray.size();

        Object array = Array.newInstance(defaultValue.getClass().getComponentType(), count);
        for (int i=0; i<count; i++) {
            Array.set(array, i, elementType.fromJson(jsonArray.get(i), Array.get(defaultValue, 0)));
        }
        return array;
    }

    public JsonElement toJson(Object value) {
        if (elementType == null) throw new IllegalStateException("ConfigType should implement fromJson: " + name());

        int count = Array.getLength(value);
        JsonArray jsonArray = new JsonArray(count);
        for (int i=0; i<count; i++) {
            jsonArray.add(elementType.toJson(Array.get(value, i)));
        }
        return jsonArray;
    }
}

package studio.kdb.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.collections4.list.UnmodifiableList;
import studio.core.Credentials;
import studio.kdb.FileChooserConfig;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            json.addProperty("name", font.getName());
            json.addProperty("size", font.getSize());
            FontStyle fontStyle = FontStyle.values()[font.getStyle()];
            json.addProperty("style", fontStyle.name());
            return json;
        }
    },
    BOUNDS {
        @Override
        public Object clone(Object value) {
            return ConfigType.clone( (Rectangle) value);
        }

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
            json.addProperty("x", bounds.x);
            json.addProperty("y", bounds.y);
            json.addProperty("width", bounds.width);
            json.addProperty("height", bounds.height);
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
        public Object clone(Object value) {
            return ConfigType.clone( (Rectangle)value);
        }

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
            json.addProperty("width", size.width);
            json.addProperty("height", size.height);
            return json;
        }
    },
    FILE_CHOOSER {
        @Override
        public Object clone(Object value) {
            return ConfigType.clone( (FileChooserConfig)value);
        }

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
            json.addProperty("filename", config.getFilename());
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
            JsonObject json = jsonElement.getAsJsonObject();
            String defaultAuth = json.get("default").getAsString();
            Map<String, Credentials> map = new HashMap<>();
            for (String method: json.keySet()) {
                if (method.equals("default")) continue;
                map.put(method, (Credentials) CREDENTIALS.fromJson(json.get(method), null));
            }
            return new DefaultAuthConfig(defaultAuth, map);
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
    TABLE_CONN_EXTRACTOR {
        @Override
        public Object fromJson(JsonElement jsonElement, Object defaultValue) {
            JsonObject json = jsonElement.getAsJsonObject();
            int maxConnections = json.get("maxConnections").getAsInt();
            List<String> connWords = (List<String>) STRING_ARRAY.fromJson(json.get("connectionWords"), List.of());
            List<String> hostWords = (List<String>) STRING_ARRAY.fromJson(json.get("hostWords"), List.of());
            List<String> portWords = (List<String>) STRING_ARRAY.fromJson(json.get("portWords"), List.of());
            TableConnExtractor extractor = new TableConnExtractor(maxConnections, connWords, hostWords, portWords);
            return extractor;
        }

        @Override
        public JsonElement toJson(Object value) {
            TableConnExtractor extractor = (TableConnExtractor)value;
            JsonObject json = new JsonObject();
            json.addProperty("maxConnections", extractor.getMaxConn());
            json.add("connectionWords", STRING_ARRAY.toJson(extractor.getConnWords()));
            json.add("hostWords", STRING_ARRAY.toJson(extractor.getHostWords()));
            json.add("portWords", STRING_ARRAY.toJson(extractor.getPortWords()));
            return json;
        }
    },
    COLOR_TOKEN_CONFIG {
        @Override
        public Object fromJson(JsonElement jsonElement, Object defaultValue) {
            Map<ColorToken, Color> map = new HashMap<>();
            JsonObject json = jsonElement.getAsJsonObject();
            for (String key: json.keySet()) {
                ColorToken token = ColorToken.valueOf(key.toUpperCase());
                Color color = (Color) COLOR.fromJson(json.get(key), null);
                map.put(token, color);
            }
            return new ColorTokenConfig(map);
        }

        @Override
        public JsonElement toJson(Object value) {
            ColorTokenConfig config = (ColorTokenConfig) value;
            JsonObject json = new JsonObject();
            for (ColorToken token: ColorToken.values()) {
                Color color = config.getColor(token);
                if (color.equals(token.getColor())) continue;
                json.add(token.name().toLowerCase(), COLOR.toJson(color));
            }
            return json;
        }
    },
    SERVER_HISTORY {
        @Override
        public Object fromJson(JsonElement jsonElement, Object defaultValue) {
            JsonObject json = jsonElement.getAsJsonObject();
            List<String> list = (List<String>) STRING_ARRAY.fromJson(json.get("names"), List.of());
            ServerHistoryConfig config = new ServerHistoryConfig(json.get("depth").getAsInt(), list);
            return config;
        }

        @Override
        public JsonElement toJson(Object value) {
            ServerHistoryConfig config = (ServerHistoryConfig) value;
            JsonObject json = new JsonObject();
            json.addProperty("depth", config.getDepth());
            json.add("names", STRING_ARRAY.toJson(config.get()));
            return json;
        }
    },
    STRING_ARRAY(STRING),
    INT_ARRAY(INT),
    ENUM_ARRAY(ENUM);


    private static Rectangle clone(Rectangle r) {
        return new Rectangle(r.x, r.y, r.width, r.height);
    }

    private static Dimension clone(Dimension d) {
        return new Dimension(d.width, d.height);
    }

    private static FileChooserConfig clone (FileChooserConfig config) {
        return new FileChooserConfig(config.getFilename(), clone(config.getPreferredSize()));
    }

    private final ConfigType elementType;

    ConfigType() {
        this(null);
    }

    ConfigType(ConfigType elementType) {
        this.elementType = elementType;
    }

    public Object clone(Object value) {
        return value;
    }

    public Object fromJson(JsonElement jsonElement, Object defaultValue) {
        if (elementType == null) throw new IllegalStateException("ConfigType should implement fromJson: " + name());

        JsonArray jsonArray = jsonElement.getAsJsonArray();
        int count = jsonArray.size();
        List<Object> defList = (List<Object>) defaultValue;
        Object defValue = defList.size() == 0 ? null: defList.get(0);

        List<Object> list = new ArrayList<>();
        for (int i=0; i<count; i++) {
            list.add(elementType.fromJson(jsonArray.get(i), defValue));
        }
        return UnmodifiableList.unmodifiableList(list);
    }

    public JsonElement toJson(Object value) {
        if (elementType == null) throw new IllegalStateException("ConfigType should implement fromJson: " + name());
        List<Object> list = (List<Object>) value;
        JsonArray jsonArray = new JsonArray(list.size());
        for (Object element: list) {
            jsonArray.add(elementType.toJson(element));
        }
        return jsonArray;
    }
}

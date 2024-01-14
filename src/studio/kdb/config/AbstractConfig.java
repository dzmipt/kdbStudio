package studio.kdb.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.kdb.FileChooserConfig;
import studio.ui.Util;
import studio.utils.PropertiesConfig;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class AbstractConfig {

    private static final Logger log = LogManager.getLogger();

    protected enum ConfigType { STRING, INT, DOUBLE, BOOLEAN, FONT, BOUNDS, COLOR, ENUM, SIZE, FILE_CHOOSER}

    protected enum FontStyle {
        Plain(Font.PLAIN), Bold(Font.BOLD), Italic(Font.ITALIC), ItalicAndBold(Font.BOLD|Font.ITALIC);
        private int style;

        FontStyle(int style) {
            this.style = style;
        }
        public int getStyle() {
            return style;
        }
    }

    private static final Map<String,? super Object> defaultValues = new HashMap();
    private static final Map<String, ConfigType> configTypes = new HashMap();

    protected PropertiesConfig config;
    private final String filename;

    protected AbstractConfig(String filename) {
        this.filename = filename;
        config = new PropertiesConfig(filename);
    }

    public String getFilename() {
        return filename;
    }

    protected void save() {
        config.save();
    }

    private Object checkAndGetDefaultValue(String key, ConfigType passed) {
        ConfigType type = configTypes.get(key);
        if (type == null) {
            throw new IllegalStateException("Oops... Wrong access to config " + key + ". The key wasn't defined");
        }
        if (type != passed) {
            throw new IllegalStateException("Oops... Wrong access to config " + key + ". Expected type: " + type + "; passed: " + passed);
        }
        return defaultValues.get(key);
    }

    protected static String configDefault(String key, ConfigType type, Object defaultValue) {
        defaultValues.put(key, defaultValue);
        configTypes.put(key, type);

        //Looks like a hack? How to make it more elegant?
        if (type == ConfigType.FILE_CHOOSER) {
            FileChooserConfig config = (FileChooserConfig) defaultValue;
            configDefault(key + ".filename", ConfigType.STRING, config.getFilename());
            configDefault(key + ".prefSize", ConfigType.SIZE, config.getPreferredSize());
        }

        return key;
    }

    protected String get(String key, String defaultValue) {
        String value = config.getProperty(key);
        return value == null ? defaultValue : value;
    }

    public String getString(String key) {
        return get(key, (String) checkAndGetDefaultValue(key, ConfigType.STRING));
    }

    // Returns whether the value was changed
    public boolean setString(String key, String value) {
        String currentValue = getString(key);
        if (currentValue.equals(value)) return false;

        config.setProperty(key, value);
        save();
        return true;
    }

    protected boolean get(String key, boolean defaultValue) {
        String value = config.getProperty(key);
        if (value == null) return defaultValue;

        return Boolean.parseBoolean(value);
    }

    public boolean getBoolean(String key) {
        return get(key, (Boolean) checkAndGetDefaultValue(key, ConfigType.BOOLEAN));
    }

    // Returns whether the value was changed
    public boolean setBoolean(String key, boolean value) {
        boolean currentValue = getBoolean(key);
        if (currentValue == value) {
            return false;
        }

        config.setProperty(key, "" + value);
        save();
        return true;
    }

    protected double get(String key, double defaultValue) {
        String value = config.getProperty(key);
        if (value == null) return defaultValue;

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            log.error("Failed to parse config key " + key + " from config", e);
        }
        return defaultValue;
    }

    public double getDouble(String key) {
        return get(key, (Double) checkAndGetDefaultValue(key, ConfigType.DOUBLE));
    }

    // Returns whether the value was changed
    public boolean setDouble(String key, double value) {
        double currentValue = getDouble(key);
        if (currentValue == value) {
            return false;
        }

        config.setProperty(key, "" + value);
        save();
        return true;
    }

    protected int get(String key, int defaultValue) {
        String value = config.getProperty(key);
        if (value == null) return defaultValue;

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.error("Failed to parse config key " + key + " from config", e);
        }
        return defaultValue;
    }

    public int getInt(String key) {
        return get(key, (Integer) checkAndGetDefaultValue(key, ConfigType.INT));
    }

    // Returns whether the value was changed
    public boolean setInt(String key, int value) {
        int currentValue = getInt(key);
        if (currentValue == value) {
            return false;
        }

        config.setProperty(key, "" + value);
        save();
        return true;
    }

    private Rectangle getBounds(String key, Object defaultBoundOrScale) {
        try {
            String strX = config.getProperty(key + ".x");
            String strY = config.getProperty(key + ".y");
            String strWidth = config.getProperty(key + ".width");
            String strHeight = config.getProperty(key + ".height");

            if (strX != null && strY != null && strWidth != null && strHeight != null) {
                Rectangle bounds = new Rectangle(Integer.parseInt(strX), Integer.parseInt(strY),
                        Integer.parseInt(strWidth), Integer.parseInt(strHeight));

                if (Util.fitToScreen(bounds)) return bounds;

                log.info("Bounds of {} doesn't fit to any of current monitors - falling back to a default value", key);
            }

        } catch (NumberFormatException e) {
            log.error("Failed to parse bounds from config key " + key, e);
        }

        DisplayMode displayMode = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDisplayMode();

        int width = displayMode.getWidth();
        int height = displayMode.getHeight();

        int w,h;

        if (defaultBoundOrScale instanceof Dimension) {
            Dimension defaultSize = (Dimension)defaultBoundOrScale;
            w = Math.min(width / 2, defaultSize.width);
            h = Math.min(height / 2, defaultSize.height);
        } else {
            double scale = 0.5;
            if (defaultBoundOrScale instanceof Double) {
                scale = (Double) defaultBoundOrScale;
            } else {
                log.error("Internal error. Wrong default value passed to getBounds - key = {}; value = {}", key, defaultBoundOrScale);
            }
            w = (int) (width * scale);
            h = (int) (height * scale);
        }

        int x = (width - w) / 2;
        int y = (height - h) / 2;
        return new Rectangle(x,y,w,h);

    }

    public Rectangle getBounds(String key) {
        return getBounds(key, checkAndGetDefaultValue(key, ConfigType.BOUNDS));
    }

    public void setBounds(String key, Rectangle bound) {
        config.setProperty(key + ".x", "" + bound.x);
        config.setProperty(key + ".y", "" + bound.y);
        config.setProperty(key + ".width", "" + bound.width);
        config.setProperty(key + ".height", "" + bound.height);
        save();
    }

    protected Dimension get(String key, Dimension defaultValue) {
        try {
            String strWidth = config.getProperty(key + ".width");
            String strHeight = config.getProperty(key + ".height");

            if (strWidth != null && strHeight != null) {
                return new Dimension(Integer.parseInt(strWidth), Integer.parseInt(strHeight));
            }

        } catch (NumberFormatException e) {
            log.error("Failed to parse dimension from config key " + key, e);
        }
        return defaultValue;
    }

    public Dimension getSize(String key) {
        return get(key, (Dimension) checkAndGetDefaultValue(key, ConfigType.SIZE));
    }
    public boolean setSize(String key, Dimension value) {
        Dimension currentValue = getSize(key);
        if (currentValue.equals(value)) return false;

        config.setProperty(key + ".width", "" + value.width);
        config.setProperty(key + ".height", "" + value.height);
        save();
        return true;
    }

    protected FileChooserConfig get(String key, FileChooserConfig defaultValue) {
        String filename = get(key + ".filename", (String) null);
        Dimension size = get(key + ".prefSize", (Dimension) null);

        if (size != null && filename != null) {
            return new FileChooserConfig(filename, size);
        }

        return defaultValue;
    }

    public FileChooserConfig getFileChooserConfig(String key) {
        return get(key, (FileChooserConfig) checkAndGetDefaultValue(key, ConfigType.FILE_CHOOSER));
    }

    public boolean setFileChooserConfig(String key, FileChooserConfig value) {
        FileChooserConfig currentValue = getFileChooserConfig(key);
        if (currentValue.equals(value)) return false;

        setString(key + ".filename", value.getFilename());
        setSize(key + ".prefSize", value.getPreferredSize());
        save();
        return true;
    }

    protected Color get(String key, Color defaultValue) {
        String value = config.getProperty(key);
        if (value == null) return defaultValue;

        try {
            return new Color(Integer.parseInt(value, 16));
        } catch (NumberFormatException e) {
            log.error("Failed to parse {} for config key {}", value, key, e);
        }
        return defaultValue;
    }

    public Color getColor(String key) {
        return get(key, (Color) checkAndGetDefaultValue(key, ConfigType.COLOR));
    }

    // Returns whether the value was changed
    public boolean setColor(String key, Color value) {
        Color currentValue = getColor(key);
        if (currentValue.equals(value)) {
            return false;
        }
        config.setProperty(key, Integer.toHexString(value.getRGB()).substring(2));
        save();
        return true;
    }

    protected <T extends Enum<T>> T get(String key, T defaultValue) {
        String value = config.getProperty(key);
        if (value == null) return defaultValue;

        try {
            return (T) Enum.valueOf(defaultValue.getClass(), value);
        } catch (IllegalArgumentException e) {
            log.error("Failed to parse {} for config key {}", value, key, e);
        }
        return defaultValue;
    }

    public <T extends Enum<T>> T getEnum(String key) {
        return get(key, (T) checkAndGetDefaultValue(key, ConfigType.ENUM));
    }

    public <T extends Enum<T>> boolean setEnum(String key, T value) {
        T currentValue = getEnum(key);
        if (currentValue == value) {
            return false;
        }

        config.setProperty(key, value.name());
        save();
        return true;
    }

    protected Font get(String key, Font defaultValue) {
        String name = config.getProperty(key + ".name");
        if (name == null) return defaultValue;

        int size = get(key + ".size", 14);
        int style = get(key +".style", FontStyle.Plain).getStyle();

        return new Font(name, style, size);
    }

    public Font getFont(String key) {
        return get(key, (Font) checkAndGetDefaultValue(key, ConfigType.FONT));
    }

    // Returns whether the value was changed
    public boolean setFont(String key, Font value) {
        Font currentValue = getFont(key);
        if (currentValue.equals(value))
            if (currentValue == value) {
                return false;
            }
        config.setProperty(key + ".name", value.getName());
        config.setProperty(key + ".size", "" + value.getSize());

        int style = value.getStyle();
        if (style < 0 || style > 3) style = 0; // Not sure if it is posible
        FontStyle fontStyle = FontStyle.values()[style];
        config.setProperty(key + ".style", fontStyle.name());

        save();
        return true;
    }

}

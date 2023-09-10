package studio.utils;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

public class PropertiesConfig extends Properties {

    public PropertiesConfig() {}

    @Override
    public synchronized Enumeration<Object> keys() {
        List keys = Collections.list(super.keys());
        Collections.sort(keys);
        return Collections.enumeration(keys);
    }
}

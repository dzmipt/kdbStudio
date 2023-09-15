package studio.utils;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

// The purposes at the moment is to have sorted keys in storing properties to disk
// It turns out that Properties.store is implemented via keys() in Java 8 and via entrySet in Java 17.
// Didn't check if there are other implementation
public class PropertiesConfig extends Properties {

    public PropertiesConfig() {}

    @Override
    public synchronized Enumeration<Object> keys() {
        List keys = Collections.list(super.keys());
        Collections.sort(keys);
        return Collections.enumeration(keys);
    }

    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {
        ConcurrentSkipListMap map = new ConcurrentSkipListMap(this);
        return map.entrySet();
    }

}

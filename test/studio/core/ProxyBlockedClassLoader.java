package studio.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProxyBlockedClassLoader extends ClassLoader{

    private List<String> blocked = new ArrayList<>();

    public ProxyBlockedClassLoader(ClassLoader parent) {
        super(parent);
    }

    public void block(String ... names) {
        blocked.addAll(Arrays.asList(names));
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (blocked.contains(name)) {
            System.out.println("Blocked loading: " + name);
            return null;
        }
        return super.loadClass(name, resolve);
    }
}

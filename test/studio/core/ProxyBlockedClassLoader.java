package studio.core;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
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

    public static Class<?> newClass(Class<?> clazz) throws IOException,ClassNotFoundException {
        ProxyBlockedClassLoader proxyClassLoader = new ProxyBlockedClassLoader(clazz.getClassLoader());
        proxyClassLoader.block(clazz.getName());

        URL url = clazz.getProtectionDomain().getCodeSource().getLocation();

        try (URLClassLoader urlClassLoader = new URLClassLoader(new URL[] {url}, proxyClassLoader)) {
            return urlClassLoader.loadClass(clazz.getName());
        }
    }
}

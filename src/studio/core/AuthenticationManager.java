package studio.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.utils.log4j.EnvConfig;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AuthenticationManager {

    private static final Logger log = LogManager.getLogger();

    private static final AuthenticationManager instance = new AuthenticationManager();

    private Map classMap = new HashMap();
    private String[] authMechanisms;

    public Class lookup(String authenticationMethod) {
        return (Class) classMap.get(authenticationMethod);
    }

    public String[] getAuthenticationMechanisms() {
        return authMechanisms;
    }

    public synchronized static AuthenticationManager getInstance() {
        return instance;
    }

    private void init() {
        loadPlugins();

        List<String> auths = new ArrayList<>(classMap.keySet());
        auths.remove(DefaultAuthenticationMechanism.NAME);
        Collections.sort(auths);
        auths.add(0, DefaultAuthenticationMechanism.NAME);
        authMechanisms = auths.toArray(new String[0]);
    }

    private void tryLoadPlugins(ClassLoader loader, URL jarFile) throws IOException {
        log.debug("scanning url {}", jarFile);
        JarURLConnection conn = (JarURLConnection) jarFile.openConnection();
        JarFile jar = conn.getJarFile();

        Enumeration e = jar.entries();
        while (e.hasMoreElements()) {
            JarEntry entry = (JarEntry) e.nextElement();
            String name = entry.getName();
            if (!entry.isDirectory() && name.endsWith(".class")) {
                String externalName = name.substring(0, name.indexOf('.')).replace('/', '.');
                try {
                    log.debug("Trying to load class {}", externalName);
                    Class c = loader.loadClass(externalName);
                    if (IAuthenticationMechanism.class.isAssignableFrom(c)) {
                        log.debug("Trying to invoke constructor for the class {}", externalName);
                        IAuthenticationMechanism am = (IAuthenticationMechanism) c.newInstance();
                        classMap.put(am.getMechanismName(), c);
                        log.debug("Loaded auth. method {}", am.getMechanismName());
                    }
                } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | Error e1) {
                    log.debug("Error in loading class {}", name, e1);
                }
            }
        }

    }


    private void loadPluginForPath(Path child) {
        try {
            log.debug("loading Plugin for {}", child);
            if (Files.isHidden(child)) {
                log.debug("Skip hidden file {}", child);
                return;
            }

            List<Path> pluginPaths = new ArrayList<>();
            if (Files.isDirectory(child)) {
                log.debug("Loading directory {}", child);
                try (Stream<Path> stream = Files.list(child)) {
                    pluginPaths = stream.filter(
                                    p -> {
                                        try {
                                            log.debug("Checking path {}", p);
                                            if (Files.isHidden(p) || !Files.isRegularFile(p)) return false;
                                            if (! p.toString().endsWith(".jar")) return false;
                                            log.debug("Adding library {} to the ClassLoader", p);
                                            return true;
                                        } catch (IOException e) {
                                            log.error("Error with Filesystem on file {}", p, e);
                                        }
                                        return false;
                                    })
                            .collect(Collectors.toList());
                }
            } else {
                if (! child.toString().endsWith(".jar")) {
                    log.debug("Skip non jar file {}", child);
                    return;
                }
                pluginPaths.add(child);
            }

            URL[] urls = new URL[pluginPaths.size()];
            for (int index = 0; index < urls.length; index++) {
                urls[index] = new URL(String.format("jar:file:%s/!/", pluginPaths.get(index)));
            }


            log.debug("URLClssLoader with {} url's", urls.length);
            URLClassLoader classLoader = new URLClassLoader(urls, AuthenticationManager.class.getClassLoader());
            for(URL url: urls) {
                tryLoadPlugins(classLoader, url);
            }
        } catch (IOException e) {
            log.error("Error loading plugin {}", child, e);
        }

    }

    private void loadPlugins() {
        DefaultAuthenticationMechanism dam = new DefaultAuthenticationMechanism();
        classMap.put(dam.getMechanismName(),dam.getClass());

        Path pluginFolder = EnvConfig.getPluginFolder();

        log.info("Looking for plugins in the folder {}", pluginFolder);

        if (! Files.exists(pluginFolder)) {
            log.debug("Plugin folder is not exist");
            return;
        }

        try (Stream<Path> stream = Files.list(pluginFolder)) {
            Iterator<Path> iterator = stream.iterator();
            while (iterator.hasNext()) {
                loadPluginForPath(iterator.next());
            }
        } catch (IOException e) {
            log.error("Error listing plugin folder", e);
        }

    }

    private AuthenticationManager() {
        init();
    }
}

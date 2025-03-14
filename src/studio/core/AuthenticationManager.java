package studio.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
        JarURLConnection conn = (JarURLConnection) jarFile.openConnection();
        JarFile jar = conn.getJarFile();

        Enumeration e = jar.entries();
        while (e.hasMoreElements()) {
            JarEntry entry = (JarEntry) e.nextElement();
            String name = entry.getName();
            if (!entry.isDirectory() && name.endsWith(".class")) {
                String externalName = name.substring(0, name.indexOf('.')).replace('/', '.');
                try {
                    Class c = loader.loadClass(externalName);
                    if (IAuthenticationMechanism.class.isAssignableFrom(c)) {
                        IAuthenticationMechanism am = (IAuthenticationMechanism) c.newInstance();
                        classMap.put(am.getMechanismName(), c);
                        log.info("Loaded auth. method {}", am.getMechanismName());
                    }
                } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | Error e1) {
                    log.debug("Error in loading class {}", name, e1);
                }
            }
        }

    }


    private void loadPlugins() {
        DefaultAuthenticationMechanism dam = new DefaultAuthenticationMechanism();
        classMap.put(dam.getMechanismName(),dam.getClass());

        String curDir = System.getProperty("user.dir");
        curDir = curDir + "/plugins";

        log.info("Looking for plugins in the folder {}", curDir);

        File dir = new File(curDir);
        if (!dir.exists()) {
            log.debug("Plugin folder is not exist");
            return;
        }

        ClassLoader parentClassLoader = AuthenticationManager.class.getClassLoader();

        String[] children = dir.list();
        if (children == null) return;
        for (String child: children) {
            String filename = dir.getAbsolutePath() + "/" + child;
            try {
                File file = new File(filename);
                if (file.isHidden()) {
                    log.debug("Skip hidden file {}", filename);
                    continue;
                }

                List<String> pluginFilenames = new ArrayList<>();
                if (file.isDirectory()) {
                    String[] subChildren = file.list();
                    if (subChildren == null) {
                        log.debug("Folder {} returns null list. Skipping...", filename);
                        continue;
                    }
                    for (String subChild: subChildren) {
                        String subFilename = filename + "/" + subChild;
                        File subFile = new File(subFilename);
                        if (subFile.isHidden() || !subFile.isFile()) {
                            log.debug("File {} is either hidden or a folder. Skipping...", subFilename);
                            continue;
                        }
                        pluginFilenames.add(subFilename);
                    }
                } else {
                    if (! filename.endsWith(".jar")) continue;
                    pluginFilenames.add(filename);
                }

                URL[] urls = new URL[pluginFilenames.size()];
                for (int index = 0; index < urls.length; index++) {
                    urls[index] = new URL(String.format("jar:file:%s/!/", pluginFilenames.get(index)));
                }

                URLClassLoader classLoader = new URLClassLoader(urls, parentClassLoader);
                for(URL url: urls) {
                    tryLoadPlugins(classLoader, url);
                }

            } catch (IOException e) {
                log.error("Error loading plugin {}", filename, e);
            }

        }

    }

    private AuthenticationManager() {
        init();
    }
}

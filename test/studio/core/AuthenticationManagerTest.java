package studio.core;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import studio.core.plugins.A;
import studio.core.plugins.B;
import studio.core.plugins.C;
import studio.utils.MockConfig;
import studio.utils.log4j.EnvConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class AuthenticationManagerTest {

    private static String toFileName(Class clazz) {
        return clazz.getName().replace('.', '/') + ".class";
    }

    private static URL getURL(Class clazz) {
        return clazz.getClassLoader().getResource(toFileName(clazz));
    }

    private static URL getParent(URL url) throws URISyntaxException, MalformedURLException {
        return new URL("file:" + Paths.get(url.toURI()).getParent().toString() + "/");
    }

    private static Path tmpDir;

    @BeforeAll
    public static void prepare() throws IOException {
        tmpDir = MockConfig.createTempDir();

        Path pluginDir = Files.createDirectory(tmpDir.resolve("plugins"));
        Path folderDir = pluginDir.resolve("folder");
        Files.createDirectory(folderDir);

        buildJar(tmpDir.resolve("auth.jar"), AuthenticationManager.class);
        buildJar(pluginDir.resolve("c.jar"), C.class);
        buildJar(folderDir.resolve("a.jar"), A.class);
        buildJar(folderDir.resolve("b.jar"), B.class);

        EnvConfig.setPluginFolder(pluginDir);
    }

    @Test
    public void testPluginLoading() throws ClassNotFoundException, IllegalAccessException, MalformedURLException, URISyntaxException, NoSuchMethodException, InvocationTargetException {
        ProxyBlockedClassLoader proxyCL = new ProxyBlockedClassLoader(AuthenticationManagerTest.class.getClassLoader());
        proxyCL.block(AuthenticationManager.class.getName(),
                A.class.getName(),
                B.class.getName(),
                C.class.getName());

        URLClassLoader urlClassLoader = new URLClassLoader(new URL[] {
                new URL(String.format("jar:file:%s/!/", tmpDir.resolve("auth.jar")))
        }, proxyCL);


        Class clazz = urlClassLoader.loadClass(AuthenticationManager.class.getName());
        Method getInstanceMethod = clazz.getDeclaredMethod("getInstance");
        Object authManager = getInstanceMethod.invoke(null);
        String[] mechanisms = (String[]) clazz.getDeclaredMethod("getAuthenticationMechanisms").invoke(authManager);
        Method  lookupMethod = clazz.getDeclaredMethod("lookup", String.class);

        Class clazzA = (Class)lookupMethod.invoke(authManager,"A");
        Class clazzB = (Class)lookupMethod.invoke(authManager,"B");
        Class clazzC = (Class)lookupMethod.invoke(authManager,"C");

        assertEquals(4, mechanisms.length);

        assertEquals(A.class.getName(), clazzA.getName());
        assertEquals(B.class.getName(), clazzB.getName());
        assertEquals(C.class.getName(), clazzC.getName());

        assertNotEquals(A.class, clazzA);
        assertNotEquals(B.class, clazzB);
        assertNotEquals(C.class, clazzC);

        assertNotEquals(clazzA.getClassLoader(), clazzC.getClassLoader());
        assertEquals(clazzA.getClassLoader(), clazzB.getClassLoader());

    }


    private static void buildJar(Path jarFile, Class... classes) throws IOException {
        FileOutputStream fos = new FileOutputStream(jarFile.toFile());
        JarOutputStream jos = new JarOutputStream(fos, createManifest());

        for(Class clazz: classes) {
            addFileToJar(jos, toFileName(clazz), new File(getURL(clazz).getFile()));
        }
        jos.close();
        fos.close();
    }

    private static Manifest createManifest() {
        Manifest manifest = new Manifest();
        Attributes attrs = manifest.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        return manifest;
    }

    private static void addFileToJar(JarOutputStream jos, String entryName, File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        ZipEntry entry = new ZipEntry(entryName);
        jos.putNextEntry(entry);

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = fis.read(buffer)) != -1) {
            jos.write(buffer, 0, bytesRead);
        }

        jos.closeEntry();
        fis.close();
    }

}

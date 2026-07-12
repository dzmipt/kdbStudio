package studio.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.assertj.swing.junit.runner.FailureScreenshotTaker;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.awt.*;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.swing.annotation.GUITestFinder.isGUITest;
import static org.assertj.swing.junit.runner.Formatter.testNameFrom;

public class TestRunner extends BlockJUnit4ClassRunner {

    private final static String SCREENSHOT_FOLDER = "build/screenshots";

    private final static Logger log = LogManager.getLogger();

    public TestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected Statement methodInvoker(FrameworkMethod method, Object test) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    method.invokeExplosively(test);
                } catch (Throwable t) {
                    takeScreenshot(method);
                    throw t;
                }
            }
        };
    }


    private void takeScreenshot(FrameworkMethod method) {
        Method realMethod = method.getMethod();
        final Class<?> testClass = realMethod.getDeclaringClass();
        if (!(isGUITest(testClass, realMethod))) return;

        try {
            Path path = Path.of(SCREENSHOT_FOLDER);
            Files.createDirectories(path);

            String failedTest = testNameFrom(testClass, realMethod);
            log.info("Taking screenshot for failed test: {} into {}; is headless GraphicsEnvironment: {}",
                    failedTest, path.toAbsolutePath(), GraphicsEnvironment.isHeadless());
            new FailureScreenshotTaker(path.toFile()).saveScreenshot(failedTest);
        } catch (Exception e) {
            log.error("Error occurred while taking screenshot", e);
        }
    }

}

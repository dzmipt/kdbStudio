package studio.ui;

import com.formdev.flatlaf.FlatLaf;
import org.junit.jupiter.api.Test;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.*;

public class StudioLookAndFeelsTest {

    @Test
    public void testDarkMatchesFlatLaf() throws Exception {
        StudioLookAndFeels.install();

        int flatLafCount = 0;
        for (UIManager.LookAndFeelInfo info: UIManager.getInstalledLookAndFeels()) {
            String className = info.getClassName();
            // Look and Feels from the JDK are light
            if (! className.startsWith("com.formdev.flatlaf.")) {
                assertFalse(StudioLookAndFeels.isDark(className), className);
                continue;
            }
            FlatLaf lf = (FlatLaf) Class.forName(className).getDeclaredConstructor().newInstance();
            assertEquals(lf.isDark(), StudioLookAndFeels.isDark(className), className);
            flatLafCount++;
        }
        assertEquals(6, flatLafCount);
    }

    @Test
    public void testUnknownIsLight() {
        assertFalse(StudioLookAndFeels.isDark("not.existing.Laf"));
        assertFalse(StudioLookAndFeels.isDark(null));
    }
}

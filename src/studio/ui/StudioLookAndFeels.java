package studio.ui;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;

import java.util.Set;

/**
 * FlatLaf Look and Feels which Studio offers in addition to the ones from the JDK.
 */
public class StudioLookAndFeels {

    private final static Set<String> DARK = Set.of(
            FlatDarkLaf.class.getName(),
            FlatDarculaLaf.class.getName(),
            FlatMacDarkLaf.class.getName()
    );

    public static void install() {
        FlatLightLaf.installLafInfo();
        FlatMacLightLaf.installLafInfo();
        FlatIntelliJLaf.installLafInfo();
        FlatDarkLaf.installLafInfo();
        FlatDarculaLaf.installLafInfo();
        FlatMacDarkLaf.installLafInfo();
    }

    /**
     * Whether the Look and Feel is dark. Unlike FlatLaf.isLafDark(), it works for a Look and Feel which is not
     * installed yet (e.g. when the config is loaded, or one selected in the Settings dialog).
     * The Look and Feels from the JDK are light.
     */
    public static boolean isDark(String lookAndFeelClassName) {
        return lookAndFeelClassName != null && DARK.contains(lookAndFeelClassName);
    }
}

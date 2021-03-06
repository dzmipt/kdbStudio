package studio.core;

import java.awt.*;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Locale;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import studio.kdb.Config;
import studio.ui.ExceptionGroup;
import studio.ui.StudioPanel;

import java.util.TimeZone;
import javax.swing.UIManager;

public class Studio {

    private static final Logger log = LogManager.getLogger();

    private static void initStdLoggers() {
        PrintStream stdoutStream = IoBuilder.forLogger("stdout").setLevel(Level.INFO).buildPrintStream();
        PrintStream stderrStream = IoBuilder.forLogger("stderr").setLevel(Level.ERROR).buildPrintStream();
        System.setOut(stdoutStream);
        System.setErr(stderrStream);
    }

    public static void main(final String[] args) {
        initStdLoggers();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

        if(System.getProperty("os.name","").contains("OS X")){ 
            System.setProperty("apple.laf.useScreenMenuBar","true");
            //     System.setProperty("apple.awt.brushMetalLook", "true");
            System.setProperty("apple.awt.showGrowBox","true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name","Studio for kdb+");
            System.setProperty("com.apple.mrj.application.live-resize","true");
            System.setProperty("com.apple.macos.smallTabs","true");
            System.setProperty("com.apple.mrj.application.growbox.intrudes","false");
        }

        if(Config.getInstance().getLookAndFeel()!=null){
            try {
                UIManager.setLookAndFeel(Config.getInstance().getLookAndFeel());
            } catch (Exception e) {
                // go on with default one
                log.warn("Can't set LookAndFeel from Config {}", Config.getInstance().getLookAndFeel(), e);
            }
        }

        studio.ui.I18n.setLocale(Locale.getDefault());

     //   studio.ui.I18n.setLocale(new Locale("zh", "cn"));

        /*if (!Config.getInstance().getAcceptedLicense()) {
            LicensePanel panel = new LicensePanel();
            Object[] options = new String[]{
                "Accept","Do Not Accept"
            };
            int answer = JOptionPane.showOptionDialog(null,
                                                      panel,"Studio for kdb+",
                                                      JOptionPane.YES_NO_OPTION,
                                                      JOptionPane.QUESTION_MESSAGE,
                                                      Studio.getImage(Config.imageBase + "32x32/question.png"), //do not use a custom Icon
                                                      options, //the titles of buttons
                                                      options[1]); //default button title

            if (answer == JOptionPane.NO_OPTION)
                System.exit(0);

            Config.getInstance().setAcceptedLicense(Lm.buildDate);
        }
        */
        UIManager.put("Table.font",new javax.swing.plaf.FontUIResource("Monospaced",Font.PLAIN,UIManager.getFont("Table.font").getSize()));
        System.setProperty("awt.useSystemAAFontSettings","on");
        System.setProperty("swing.aatext", "true");
     
        ThreadGroup exceptionThreadGroup = new ExceptionGroup();

        new Thread(exceptionThreadGroup,"Init thread") {
            public void run() {
                log.info("Start Studio with args {}", Arrays.asList(args));
                StudioPanel.init(args);
            }
        }.start();

    }
}

package studio.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class StudioFrame extends JFrame {

    private Helper helper = null;
    private boolean removeContentOnDispose = true;

    public StudioFrame() {
        helper = new Helper(this);
        setContentPane(getContentPane());
    }

    public StudioFrame(String title) {
        this();
        setTitle(title);
    }

    @Override
    public final String getTitle() {
        if (!Util.MAC_OS_X) return super.getTitle();
        return "";
    }

    public String getRealTitle() {
        return helper.getTitle();
    }

    @Override
    public void setTitle(String title) {
        if (helper == null) return;
        if (! helper.setTitle(title) ) super.setTitle(title);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (helper == null) return;
        helper.invalidate();
    }

    @Override
    public Container getContentPane() {
        if (helper == null) return super.getContentPane();
        Container contentPane = helper.getContentPane();
        if (contentPane != null) return contentPane;
        return super.getContentPane();
    }

    @Override
    public void setContentPane(Container contentPane) {
        super.setContentPane(helper.decorateContentPane(contentPane));
    }

    public void setRemoveContentOnDispose(boolean removeContentOnDispose) {
        this.removeContentOnDispose = removeContentOnDispose;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (removeContentOnDispose) {
            helper.dispose();
            getContentPane().removeAll();
        }
    }

    public static class Helper {

        private Container origContentPane = null;
        private JPanel macOSTitlePanel = null;
        private JLabel macOSTitle = null;
        private JDialog dialog = null;
        private JFrame frame = null;

        public Helper(JDialog dialog) {
            this.dialog = dialog;
        }
        public Helper(JFrame frame) {
            this.frame = frame;
        }

        public void invalidate() {
            if (macOSTitlePanel == null) return;
            macOSTitlePanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(0,0,2,0),
                    BorderFactory.createMatteBorder(0,0,1,0,UIManager.getColor("windowBorder"))
            ));
        }

        public String getTitle() {
            if (! Util.MAC_OS_X) {
                if (dialog != null) {
                    return dialog.getTitle();
                } else {
                    return frame.getTitle();
                }
            }

            if (macOSTitle == null) return "";

            return macOSTitle.getText();
        }

        public boolean setTitle(String title) {
            if (! Util.MAC_OS_X) {
                if (dialog != null) {
                    return Objects.equals(title, dialog.getTitle());
                } else {
                    return Objects.equals(title, frame.getTitle());
                }
            }
            if (macOSTitle == null) {
                macOSTitle = new JLabel();
            }
            macOSTitle.setText(title);
            return true;
        }

        public Container getContentPane() {
            return origContentPane;
        }

        public Container decorateContentPane(Container contentPane) {
            if (!Util.MAC_OS_X) return contentPane;
            origContentPane = contentPane;

            JRootPane rootPane = dialog == null ? frame.getRootPane() : dialog.getRootPane();
            rootPane.putClientProperty( "apple.awt.fullWindowContent", true );
            rootPane.putClientProperty( "apple.awt.transparentTitleBar", true );

            if (macOSTitle == null) macOSTitle = new JLabel();
            macOSTitle.setHorizontalAlignment(SwingConstants.CENTER);
            macOSTitle.setVerticalAlignment(SwingConstants.CENTER);
            Map<TextAttribute, Object> map = new HashMap<>();
            map.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
            macOSTitle.setFont(macOSTitle.getFont().deriveFont(map));

            macOSTitlePanel = new JPanel(new BorderLayout());
            macOSTitlePanel.add(macOSTitle, BorderLayout.CENTER);
            macOSTitlePanel.add(Box.createVerticalStrut(25), BorderLayout.WEST);

            JPanel decoratedContentPane = new JPanel(new BorderLayout());
            decoratedContentPane.add(macOSTitlePanel, BorderLayout.NORTH);
            decoratedContentPane.add(contentPane, BorderLayout.CENTER);

            return decoratedContentPane;
        }

        public void dispose() {
            macOSTitle = null;
            macOSTitlePanel = null;
            origContentPane = null;
        }
    }
}

package studio.ui;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;

public class TabPanel extends JPanel {
    private Icon icon;
    private String title;
    private JComponent component;

    public TabPanel(String title,Icon icon,JComponent component) {
        this.title = title;
        this.icon = icon;
        this.component = component;
        initComponents();
    }

    private void initComponents() {
        JToggleButton tglBtnComma = new JToggleButton(Util.COMMA_CROSSED_ICON);
        tglBtnComma.setSelectedIcon(Util.COMMA_ICON);

        tglBtnComma.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        tglBtnComma.setToolTipText("Add comma as thousands separators for numbers");
        tglBtnComma.setFocusable(false);
        tglBtnComma.addActionListener(e-> {
            System.out.println("Click");
        });
        JToolBar toolbar = new JToolBar();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.Y_AXIS));
        toolbar.setFloatable(false);
        toolbar.add(tglBtnComma);

        setLayout(new BorderLayout());
        add(toolbar, BorderLayout.WEST);
        add(component, BorderLayout.CENTER);
    }

    public Icon getIcon() {
        return icon;
    }

    public String getTitle() {
        return title;
    }

}


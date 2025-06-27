package studio.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.KeyEvent;

public class Toolbar extends JToolBar {

    private static final Logger log = LogManager.getLogger();

    private int gap = -1;
    private Border buttonBorder = null;

    public void setGap(int gap) {
        this.gap = gap;
    }

    public void setButtonBorder(Border border) {
        this.buttonBorder = border;
    }

    public void addAll(Object... elements) {
        for (Object element: elements) {
            if (element == null) {
                addSeparator();
            } else if (element instanceof Action) {
                addCustom((Action)element);
            } else if (element instanceof Component) {
                add((Component)element);
            } else {
                throw new IllegalStateException("Internal error");
            }
        }
    }

    @Override
    public JButton add(Action a) {
        AbstractButton btn = addCustom(a);
        if (! (btn instanceof JButton))
            throw new IllegalStateException("Can't cast to JButton. addCustom() should be used instead: " + btn.getClass());
        return (JButton)btn;
    }

    public AbstractButton addCustom(Action a) {
        if (getComponentCount() > 0 && gap > 0 ) {
            add(Box.createRigidArea(new Dimension(gap, gap)));
        }

        AbstractButton btn;
        Icon toggleIcon = (Icon)a.getValue(UserAction.TOGGLE_ICON);
        if (toggleIcon != null) {
            if (a.getValue(Action.SELECTED_KEY) == null) {
                a.putValue(Action.SELECTED_KEY, Boolean.FALSE);
            }
            btn = new JToggleButton();
            btn.setSelectedIcon(toggleIcon);
        } else {
            btn = new JButton();
        }
        btn.setAction(a);

        btn.setFocusable(false);
        btn.setMnemonic(KeyEvent.VK_UNDEFINED);

        btn.setName("toolbar" + a.getValue(Action.NAME));
        btn.setHideActionText(true);

        String tooltip = (String)a.getValue(Action.SHORT_DESCRIPTION);
        KeyStroke accelerator = (KeyStroke) a.getValue(Action.ACCELERATOR_KEY);
        if (accelerator != null) {
            tooltip = Util.getTooltipWithAccelerator(tooltip, accelerator);
        }
        btn.setToolTipText(tooltip);

        if (buttonBorder != null) btn.setBorder(buttonBorder);
        add(btn);
        return btn;
    }

}

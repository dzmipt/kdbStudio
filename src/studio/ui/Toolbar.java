package studio.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;

public class Toolbar extends JToolBar {

    private static final Logger log = LogManager.getLogger();

    @Override
    public JButton add(Action a) {
        JButton btn = super.add(a);
        updateTooltipText(btn);

        a.addPropertyChangeListener( e-> {
            if (e.getPropertyName().equals(Action.ACCELERATOR_KEY)) {
                updateTooltipText(btn);
            }
        });

        return btn;
    }

    private void updateTooltipText(JButton btn) {
        Action a = btn.getAction();
        String tooltip = (String)a.getValue(Action.SHORT_DESCRIPTION);

        KeyStroke accelerator = (KeyStroke) a.getValue(Action.ACCELERATOR_KEY);
        if (accelerator != null) {
            tooltip = Util.getTooltipWithAccelerator(tooltip, accelerator);
        }
        btn.setToolTipText(tooltip);
    }

}

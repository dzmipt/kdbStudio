package studio.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class Toolbar extends JToolBar {

    private static final Logger log = LogManager.getLogger();

    @Override
    public JButton add(Action a) {
        JButton btn = super.add(a);
        updateTooltipTest(btn);

        return btn;
    }

    private void updateTooltipTest(JButton btn) {
        Action a = btn.getAction();
        String tooltip = (String)a.getValue(Action.SHORT_DESCRIPTION);

        KeyStroke accelerator = (KeyStroke) a.getValue(Action.ACCELERATOR_KEY);
        if (accelerator != null) {
            tooltip = Util.getTooltipWithAccelerator(tooltip, accelerator);
        }
        btn.setToolTipText(tooltip);
    }

    @Override
    protected PropertyChangeListener createActionChangeListener(JButton b) {
        return new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName() == Action.ACCELERATOR_KEY) {
                    updateTooltipTest(b);
                }
            }
        };
    }
}

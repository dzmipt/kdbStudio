package studio.ui;

import javax.swing.*;

public class Toolbar extends JToolBar {

    @Override
    public JButton add(Action a) {
        JButton btn = super.add(a);

        KeyStroke accelerator = (KeyStroke) a.getValue(Action.ACCELERATOR_KEY);
        if (accelerator == null) {
            return btn;
        }

        String tooltip = Util.getTooltipWithAccelerator((String)a.getValue(Action.SHORT_DESCRIPTION), accelerator);
        btn.setToolTipText(tooltip);
        return btn;
    }
}

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

        String text = "<html>" + a.getValue(Action.SHORT_DESCRIPTION) +
                " <small>" + Util.getAcceleratorString(accelerator) + "</small></html>";

        btn.setToolTipText(text);
        return btn;
    }
}

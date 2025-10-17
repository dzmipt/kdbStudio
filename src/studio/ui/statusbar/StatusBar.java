package studio.ui.statusbar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class StatusBar extends Box {

    private final JLabel lblStatus;

    private final Timer tempStatusTimer;

    private String oldStatus = "";

    private int yGap;
    private int xGap;

    public StatusBar() {
        super(BoxLayout.X_AXIS);

        tempStatusTimer =  new Timer(3000, this::tempStatusTimerAction);
        tempStatusTimer.setRepeats(false);



        lblStatus = new JLabel(" ");
        Box boxStatus = Box.createHorizontalBox();
        boxStatus.add(lblStatus);
        boxStatus.add(Box.createHorizontalGlue());
        setBorder(boxStatus);

        boxStatus.setMinimumSize(new Dimension(0,0));
        add(boxStatus);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        FontMetrics fm = getFontMetrics(UIManager.getFont("Label.font"));
        yGap = Math.round(0.1f * fm.getHeight());
        xGap = Math.round(0.25f * SwingUtilities.computeStringWidth(fm, "x"));
    }

    protected void addComponent(JComponent component) {
        setBorder(component);
        add(component);
    }

    public void setStatus(String status) {
        if (tempStatusTimer.isRunning()) {
            oldStatus = status;
        }
        lblStatus.setText(status);
    }

    public void setTemporaryStatus(String status) {
        if (!tempStatusTimer.isRunning()) {
            oldStatus = lblStatus.getText();
        }
        lblStatus.setText(status);
        tempStatusTimer.restart();
    }

    private void tempStatusTimerAction(ActionEvent event) {
        setStatus(oldStatus);
    }

    private void setBorder(JComponent component) {
        component.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createCompoundBorder(
                                BorderFactory.createEmptyBorder(yGap,xGap,yGap,xGap),
                                BorderFactory.createLineBorder(Color.LIGHT_GRAY)
                        ),
                        BorderFactory.createEmptyBorder(2*yGap, 2*xGap, yGap, 2*xGap)
                )
        );
    }

}

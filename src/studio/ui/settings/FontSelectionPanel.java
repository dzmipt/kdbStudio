package studio.ui.settings;

import org.drjekyll.fontchooser.FontDialog;
import studio.kdb.Config;
import studio.ui.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FontSelectionPanel extends JPanel implements ActionListener {

    private Font font;
    private JLabel fontLabel;
    private JDialog ownerDialog;
    private String configKey;

    public FontSelectionPanel(JDialog ownerDialog, String label, String configKey) {
        super();
        this.ownerDialog = ownerDialog;
        this.configKey = configKey;

        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        fontLabel = new JLabel();
        fontLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLoweredBevelBorder(),
                    BorderFactory.createEmptyBorder(5,5,5,5)
                ));

        fontLabel.setAlignmentY(JComponent.CENTER_ALIGNMENT);

        JButton dialogBtn = new JButton("...");
        dialogBtn.addActionListener(this);
        add(new JLabel(label));
        add(Box.createRigidArea(new Dimension(5,0)));
        add(fontLabel);
        add(Box.createRigidArea(new Dimension(5,0)));
        add(dialogBtn);

        updateFont(Config.getInstance().getFont(configKey));
    }

    private void updateFont(Font font) {
        this.font = font;
        fontLabel.setFont(font);
        fontLabel.setText(font.getFontName());
        revalidate();
    }

    public boolean saveSettings() {
        return Config.getInstance().setFont(configKey, font);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        FontDialog dialog = new FontDialog(ownerDialog, "Select Font", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setSelectedFont(font);

        Util.centerChildOnParent(dialog, ownerDialog);
        dialog.setVisible(true);

        if (dialog.isCancelSelected()) return;

        updateFont(dialog.getSelectedFont());
    }

    public static void main(String[] args) {
        JDialog d = new JDialog((Frame)null, "Test", true);

        d.setContentPane(new FontSelectionPanel(d, "Select font:", Config.FONT_EDITOR));
        d.setSize(600, 400);
        d.setVisible(true);
    }
}

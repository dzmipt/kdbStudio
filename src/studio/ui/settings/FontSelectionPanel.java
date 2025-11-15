package studio.ui.settings;

import org.drjekyll.fontchooser.FontChooser;
import studio.kdb.Config;
import studio.ui.EscapeDialog;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class FontSelectionPanel extends JPanel implements ActionListener {

    private Font font;
    private JLabel fontLabel;
    private JDialog ownerDialog;

    private final List<ChangeListener> listeners = new ArrayList<>();

    public FontSelectionPanel(JDialog ownerDialog, String label, Font font) {
        super();
        this.ownerDialog = ownerDialog;

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

        updateFont(font);
    }

    private boolean updateFont(Font font) {
        if (font.equals(this.font)) return false;

        this.font = font;
        fontLabel.setFont(font);
        fontLabel.setText(font.getFontName());
        revalidate();

        return true;
    }

    public Font getSelectedFont() {
        return font;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        FontChooser fontChooser = new FontChooser(font);
        EscapeDialog dialog = new EscapeDialog(ownerDialog, "Select Font");

        JButton btnOK = new JButton("OK");
        JButton btnCancel = new JButton("Cancel");
        btnOK.addActionListener(e-> dialog.accept());
        btnCancel.addActionListener(e-> dialog.cancel());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        btnPanel.add(btnOK);
        btnPanel.add(btnCancel);

        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.add(fontChooser, BorderLayout.CENTER);
        contentPane.add(btnPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPane);

        boolean accepted = dialog.alignAndShow();
        if (! accepted) return;

        if ( updateFont(fontChooser.getSelectedFont()) ) {
            fireEvent();
        }
    }

    public void addChangeListener(ChangeListener listener) {
        listeners.add(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        listeners.remove(listener);
    }

    private void fireEvent() {
        ChangeEvent event = new ChangeEvent(this);
        listeners.forEach(l -> l.stateChanged(event));
    }

    public static void main(String[] args) {
        JDialog d = new JDialog((Frame)null, "Test", true);

        d.setContentPane(new FontSelectionPanel(d, "Select font:", Config.getInstance().getFont(Config.FONT_EDITOR)));
        d.setSize(600, 400);
        d.setVisible(true);
    }
}

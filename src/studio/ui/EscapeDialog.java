package studio.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class EscapeDialog extends JDialog {

    public enum DialogResult {ACCEPTED, CANCELLED};

    private DialogResult result = DialogResult.CANCELLED;

    private boolean removeContentOnDispose = true;
    private StudioFrame.Helper helper = null;

    private static Window getWindow(Component component) {
        if (component == null) return null;
        if (component instanceof Window) return (Window) component;
        return SwingUtilities.getWindowAncestor(component);
    }

    public EscapeDialog(Component windowOwner, String title) {
        super(getWindow(windowOwner), "", ModalityType.APPLICATION_MODAL);
        helper = new StudioFrame.Helper(this);
        setContentPane(getContentPane());
        setTitle(title);
        initComponents();
    }

    @Override
    public String getTitle() {
        if (!Util.MAC_OS_X) return super.getTitle();
        return "";
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
    public void setContentPane(Container contentPane) {
        super.setContentPane(helper.decorateContentPane(contentPane));
    }

    @Override
    public Container getContentPane() {
        if (helper == null) return super.getContentPane();
        Container contentPane = helper.getContentPane();
        if (contentPane != null) return contentPane;
        return super.getContentPane();
    }

    public void align() {
        pack();
        Util.centerChildOnParent(this, getParent());
    }

    public boolean alignAndShow() {
        align();
        setVisible(true);
        return result == DialogResult.ACCEPTED;
    }

    private void initComponents() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0);
        this.getRootPane().registerKeyboardAction(this::escapeAction, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void escapeAction(ActionEvent e) {
        MenuElement[] path = MenuSelectionManager.defaultManager().getSelectedPath();

        if (path.length > 0) {
            MenuSelectionManager.defaultManager().clearSelectedPath();
        } else {
            cancel();
        }
    }

    public void cancel() {
        result = DialogResult.CANCELLED;
        dispose();
    }

    public void accept() {
        result = DialogResult.ACCEPTED;
        dispose();
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

    public DialogResult getResult() {
        return result;
    }
}


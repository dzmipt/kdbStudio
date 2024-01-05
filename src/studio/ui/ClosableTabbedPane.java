package studio.ui;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;

public class ClosableTabbedPane {

    public interface CloseTabAction {
        boolean close(int index, boolean force);
    }

    public interface PinTabAction extends CloseTabAction {
        boolean isPinned(int index);
        void setPinned(int index, boolean pinned);
    }

    public static void makeCloseable(JTabbedPane tabbedPane, CloseTabAction closeTabAction) {
        tabbedPane.addMouseListener(new MouseAdapter() {
            //We need to override both mouseReleased and mousePressed...
            // Popup trigger is set in mousePressed on Mac and mouseReleased is for Windows
            @Override
            public void mouseReleased(MouseEvent e) {
                int tabIndex = tabbedPane.indexAtLocation(e.getX(), e.getY());
                if (tabIndex == -1) return;

                if (checkPopup(tabIndex, e)) return;

                if (SwingUtilities.isMiddleMouseButton(e)) {
                    // Other listeners can be broken if we close tab now. Therefore we will close after all are processed
                    // Found NPE in com.apple.laf.AquaTabbedPaneUI$MouseHandler.repaint(AquaTabbedPaneUI.java:949)
                    SwingUtilities.invokeLater(() -> closeTabAction.close(tabIndex, true));
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int tabIndex = tabbedPane.indexAtLocation(e.getX(), e.getY());
                if (tabIndex == -1) return;

                checkPopup(tabIndex, e);
            }

            private boolean checkPopup(int tabIndex, MouseEvent e) {
                if (e.isPopupTrigger()) {
                    JPopupMenu popup = createTabbedPopupMenu(tabbedPane, closeTabAction, tabIndex);
                    popup.show(e.getComponent(), e.getX(), e.getY());
                    return true;
                }
                return false;
            }
        });

        tabbedPane.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_W, StudioWindow.menuShortcutKeyMask | KeyEvent.SHIFT_MASK), "closeTab");
        tabbedPane.getActionMap().put("closeTab",
                UserAction.create("Close tab", e -> {
                    int index = tabbedPane.getSelectedIndex();
                    if (index != -1) closeTabAction.close(index, true);
                })  );

    }

    private static JPopupMenu createTabbedPopupMenu(JTabbedPane tabbedPane, CloseTabAction closeTabAction, int index) {        
        UserAction closeAction = UserAction.create("Close", "Close current tab",
                0, e-> closeTabAction.close(index, true) );

        UserAction closeOthersAction = UserAction.create("Close other tabs", "Close other tabs",
                0, e -> {
                    int rightTab = tabbedPane.getTabCount();
                    while(--rightTab > index) {
                        if (!closeTabAction.close(rightTab, false)) return;
                    }
                    for (int leftTab = index-1; leftTab >= 0; leftTab--) {
                        if (!closeTabAction.close(leftTab, false)) return;
                    }                    
                });

        UserAction closeRightsAction = UserAction.create("Close tabs to the right", "Close tabs to the right",
                0, e -> {
                    int rightTab = tabbedPane.getTabCount();
                    while(--rightTab > index) {
                        if (!closeTabAction.close(rightTab, false)) return;
                    }
                });

        closeOthersAction.setEnabled(tabbedPane.getTabCount() > 1);
        closeRightsAction.setEnabled(tabbedPane.getTabCount() > index+1);

        JPopupMenu popup = new JPopupMenu();

        if (closeTabAction instanceof PinTabAction) {
            PinTabAction pinTabAction = (PinTabAction)closeTabAction;

            UserAction pinAction = UserAction.create("Pin", "Pin current tab",
                    0, e -> {
                        pinTabAction.setPinned(index, true);
                    });

            UserAction unpinAction = UserAction.create("Unpin", "Unpin current tab",
                    0, e -> {
                        pinTabAction.setPinned(index, false);
                    });

            if (pinTabAction.isPinned(index)) {
                popup.add(unpinAction);
            } else {
                popup.add(pinAction);
            }
            popup.addSeparator();
        }

        popup.add(closeAction);
        popup.add(closeOthersAction);
        popup.add(closeRightsAction);
        return popup;
    }

}

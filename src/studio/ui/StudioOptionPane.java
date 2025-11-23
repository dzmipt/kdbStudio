package studio.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;

public class StudioOptionPane {

    public static class Option {
        private String text;
        private int mnemonic;

        public Option(String text, int mnemonic) {
            this.text = text;
            this.mnemonic = mnemonic;
        }

        public String getText() {
            return text;
        }

        public int getMnemonic() {
            return mnemonic;
        }

        public char getKey() {
            return (char)mnemonic;
        }

        @Override
        public int hashCode() {
            return text.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (! (obj instanceof Option)) return false;
            return text.equals(((Option)obj).getText());
        }

        @Override
        public String toString() {
            return text;
        }
    }

    public static final Option OK_OPTION = new Option("OK", KeyEvent.VK_O);
    public static final Option YES_OPTION = new Option("Yes", KeyEvent.VK_Y);
    public static final Option NO_OPTION = new Option("No", KeyEvent.VK_N);
    public static final Option CANCEL_OPTION = new Option("Cancel", KeyEvent.VK_C);

    public static final Option RELOAD_OPTION = new Option("Reload", KeyEvent.VK_R);
    public static final Option IGNOREALL_OPTION = new Option("Ignore all", KeyEvent.VK_I);

    public static final Option[] OK_OPTIONS = new Option[] {OK_OPTION};
    public static final Option[] OK_CANCEL_OPTIONS = new Option[] {OK_OPTION, CANCEL_OPTION};
    public static final Option[] YES_NO_OPTIONS = new Option[] {YES_OPTION, NO_OPTION};
    public static final Option[] YES_NO_CANCEL_OPTIONS = new Option[] {YES_OPTION, NO_OPTION, CANCEL_OPTION};

    public static final Option[] RELOADFILE_OPTIONS = new Option[] {RELOAD_OPTION, CANCEL_OPTION, IGNOREALL_OPTION};

    public static final int RELOAD_RESULT = 0;
    public static final int CANCEL_RESULT = 1;
    public static final int IGNOREALL_RESULT = 2;

    private static boolean mocked = false;
    private static int mockedResult = JOptionPane.OK_OPTION;

    private final static Logger log = LogManager.getLogger();

    public static void setMocked(boolean isMocked) {
        if (mocked == isMocked) return;
        mocked = isMocked;
        log.info("setMocked: {}", isMocked);
    }

    public static void setMockedResult(int result) {
        setMocked(true);
        mockedResult = result;
    }

    public static void showError(String message, String title) {
        showError(null, message, title);
    }

    public static void showError(Component parentComponent, String message, String title) {
        showOptionDialog(parentComponent, message, title, JOptionPane.ERROR_MESSAGE, Util.ERROR_ICON, OK_OPTIONS, OK_OPTION);
    }

    public static void showMessage(Component parentComponent, String message, String title) {
        showOptionDialog(parentComponent, message, title, JOptionPane.INFORMATION_MESSAGE, Util.INFORMATION_ICON, OK_OPTIONS, OK_OPTION);
    }

    public static void showWarning(Component parentComponent, String message, String title) {
        showOptionDialog(parentComponent, message, title, JOptionPane.WARNING_MESSAGE, Util.WARNING_ICON, OK_OPTIONS, OK_OPTION);
    }


    public static int showYesNoDialog(Component parentComponent, String message, String title) {
        return showOptionDialog(parentComponent, message, title, JOptionPane.QUESTION_MESSAGE, Util.QUESTION_ICON, YES_NO_OPTIONS, NO_OPTION);
    }

    public static int showYesNoCancelDialog(Component parentComponent, String message, String title) {
        return showOptionDialog(parentComponent, message, title, JOptionPane.QUESTION_MESSAGE, Util.QUESTION_ICON, YES_NO_CANCEL_OPTIONS, NO_OPTION);
    }

    public static int reloadFileDialog(Component parentComponent, String message, String title) {
        return showOptionDialog(parentComponent, message, title, JOptionPane.QUESTION_MESSAGE, Util.QUESTION_ICON, RELOADFILE_OPTIONS, CANCEL_OPTION);
    }

    public static String showInputDialog(Component parentComponent, String message, String title) {
        return JOptionPane.showInputDialog(parentComponent, message, title, JOptionPane.QUESTION_MESSAGE);
    }

    private static void findButtons(List<JButton> buttons, Container container) {
        if (container instanceof JButton) {
            buttons.add((JButton)container);
        }
        for (Component c: container.getComponents()) {
            if (c instanceof Container) {
                findButtons(buttons, (Container) c);
            }
        }
    }

    public static int showComplexDialog(Component parentComponent, JComponent content, String title) {
        return showOptionDialog(parentComponent, content, title, JOptionPane.QUESTION_MESSAGE, Util.QUESTION_ICON, OK_CANCEL_OPTIONS, OK_OPTION);
    }

    private static int showOptionDialog(Component parentComponent, Object message, String title, int messageType, Icon icon, Option[] options, Option initialValue) {
        if (mocked) return mockedResult;

        JOptionPane pane = new JOptionPane(message, messageType, JOptionPane.DEFAULT_OPTION, icon, options, initialValue);
        ArrayList<JButton> buttons = new ArrayList<>();
        findButtons(buttons, pane);
        for (JButton button : buttons) {
            Option option = null;
            for (int index=0; index<options.length; index++) {
                if (button.getText().equals(options[index].getText())) {
                    option = options[index];
                    break;
                }
            }
            if (option == null) continue;

            button.setMnemonic(option.getKey());
            Action action = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    button.doClick();
                }
            };
            String actionName = "press " + option.getKey();
            pane.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(option.getKey()), actionName);
            pane.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(Character.toLowerCase(option.getKey())), actionName);
            pane.getActionMap().put(actionName, action);
        }


        EscapeDialog dialog = new EscapeDialog(parentComponent, title);
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.add(pane, BorderLayout.CENTER);

        dialog.setContentPane(contentPane);
        dialog.setResizable(false);

        final PropertyChangeListener listener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                // Let the defaultCloseOperation handle the closing
                // if the user closed the window without selecting a button
                // (newValue = null in that case).  Otherwise, close the dialog.
                if (dialog.isVisible() && event.getSource() == pane &&
                        (event.getPropertyName().equals(JOptionPane.VALUE_PROPERTY)) &&
                        event.getNewValue() != null &&
                        event.getNewValue() != JOptionPane.UNINITIALIZED_VALUE) {
                    dialog.accept();
                }
            }
        };

        WindowAdapter adapter = new WindowAdapter() {
            private boolean gotFocus = false;
            public void windowClosing(WindowEvent we) {
                dialog.cancel();
            }

            public void windowGainedFocus(WindowEvent we) {
                // Once window gets focus, set initial focus
                if (!gotFocus) {
                    pane.selectInitialValue();
                    gotFocus = true;
                }
            }
        };
        dialog.addWindowListener(adapter);
        dialog.addWindowFocusListener(adapter);

        pane.addPropertyChangeListener(listener);

        dialog.pack();
        pane.selectInitialValue();

        dialog.alignAndShow();
        pane.removePropertyChangeListener(listener);

        Object result = pane.getValue();

        if (result == null) return JOptionPane.CLOSED_OPTION;

        if (result instanceof Integer)
            return (Integer)result;

        for (int index=0; index<options.length; index++) {
            if (options[index].equals(result)) return index;
        }

        return JOptionPane.CLOSED_OPTION;
    }


    public static void main(String... args) {

        JTextField txtTitle = new JTextField(50);
        JTextField txtMessage = new JTextField(50);
        JTextField txtResult = new JTextField(50);

        JButton btnErr = new JButton("showError");
        btnErr.addActionListener(e->{
            showError(null, txtMessage.getText(), txtTitle.getText());
        });

        JButton btnMsg = new JButton("showMessage");
        btnMsg.addActionListener(e->{
            showMessage(null, txtMessage.getText(), txtTitle.getText());
        });

        JButton btnWarn = new JButton("showWarning");
        btnWarn.addActionListener(e->{
            showWarning(null, txtMessage.getText(), txtTitle.getText());
        });

        JButton btnYesNo = new JButton("showYesNoDialog");
        btnYesNo.addActionListener(e->{
            txtResult.setText(""+showYesNoDialog(null, txtMessage.getText(), txtTitle.getText()) );
        });

        JButton btnYesNoCancel = new JButton("showYesNoCancelDialog");
        btnYesNoCancel.addActionListener(e->{
            txtResult.setText(""+showYesNoCancelDialog(null, txtMessage.getText(), txtTitle.getText()) );
        });

        JButton btnReloadFileDialog = new JButton("reloadFileDialog");
        btnReloadFileDialog.addActionListener(e->{
            txtResult.setText(""+reloadFileDialog(null, txtMessage.getText(), txtTitle.getText()) );
        });

        JButton btnInput = new JButton("showInputDialog");
        btnInput.addActionListener(e->{
            txtResult.setText(showInputDialog(null, txtMessage.getText(), txtTitle.getText()) );
        });

        StringComboBox comboBox = new StringComboBox(List.of("OptionA", "OptionB", "OptionC"));
        JPanel testPanel = new JPanel();
        GroupLayoutSimple layout = new GroupLayoutSimple(testPanel);
        layout.setStacks(new GroupLayoutSimple.Stack()
                .addLineAndGlue(new JLabel("Combo:"), comboBox)
                .addLineAndGlue(new JCheckBox("Test checkbox"))
        );

        JButton btnComplex = new JButton("showComplexDialog");
        btnComplex.addActionListener(e->{
            txtResult.setText(""+showComplexDialog(null, testPanel, txtTitle.getText()) );
        });

        JPanel contentPane = new JPanel();
        layout = new GroupLayoutSimple(contentPane);
        layout.setStacks(new GroupLayoutSimple.Stack()
                .addLineAndGlue(new JLabel("Title: "), txtTitle)
                .addLineAndGlue(new JLabel("Message: "), txtMessage)
                .addLineAndGlue(btnErr, btnMsg, btnWarn)
                .addLineAndGlue(btnInput, btnReloadFileDialog)
                .addLineAndGlue(btnYesNo, btnYesNoCancel)
                .addLineAndGlue(btnComplex)
                .addLineAndGlue(new JLabel("Result: "), txtResult)
        );

        StudioFrame f = new StudioFrame("Test");
        f.setDefaultCloseOperation(StudioWindow.EXIT_ON_CLOSE);
        f.setContentPane(contentPane);
        f.setSize(400, 600);
        f.setVisible(true);

    }

}

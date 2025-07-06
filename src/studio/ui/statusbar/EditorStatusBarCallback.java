package studio.ui.statusbar;

public interface EditorStatusBarCallback {
    void connect(String authMethod);
    void connectTLS(boolean useTLS);
    void disconnect();
}

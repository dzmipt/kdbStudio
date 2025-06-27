package studio.ui;

import javax.swing.*;

public enum ResultType {
    ERROR("Error Details ", Util.ERROR_SMALL_ICON),
    TEXT(I18n.getString("ConsoleView"), Util.CONSOLE_ICON),
    LIST("List", Util.TABLE_ICON),
    TABLE("Table", Util.TABLE_ICON);

    private final String title;
    private final Icon icon;

    ResultType(String title, Icon icon) {
        this.title = title;
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public Icon getIcon() {
        return icon;
    }

    public boolean isTable() {
        return this == TABLE || this == LIST;
    }
}

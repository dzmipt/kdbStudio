package studio.kdb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.utils.Content;
import studio.utils.LineEnding;
import studio.utils.QConnection;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Workspace {

    private final List<TopWindow> windows = new ArrayList<>();
    private int selectedWindow = -1;

    public final static Rectangle DEFAULT_BOUNDS = new Rectangle(-1,-1,0,0);

    private final static Logger log = LogManager.getLogger();

    public int getSelectedWindow() {
        return selectedWindow;
    }

    public TopWindow[] getWindows() {
        return windows.toArray(new TopWindow[0]);
    }

    public TopWindow addWindow(boolean selected) {
        TopWindow window = new TopWindow();
        windows.add(window);
        if (selected) {
            selectedWindow = windows.size()-1;
        }
        return window;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof Workspace)) return false;

        Workspace workspace = (Workspace) o;
        return selectedWindow == workspace.selectedWindow && windows.equals(workspace.windows);
    }

    @Override
    public int hashCode() {
        return Objects.hash(windows, selectedWindow);
    }

    public static class Window {

        private final List<Tab> tabs = new ArrayList<>();
        private int selectedTab = -1;
        private Window left, right;
        private boolean verticalSplit;
        private double dividerLocation = 0.5;

        Window() {}

        public Window(Server server, String filename) {
            tabs.add(new Tab(server,filename));
            selectedTab = 0;
        }

        public int getSelectedTab() {
            return selectedTab;
        }

        public Tab[] getAllTabs() {
            List<Tab> list = new ArrayList<>();
            addAllTabs(list);
            return list.toArray(new Tab[0]);
        }

        private void addAllTabs(List<Tab> list) {
            if (left != null) left.addAllTabs(list);
            if (right != null) right.addAllTabs(list);
            list.addAll(tabs);
        }

        public Tab addTab(boolean selected) {
            Tab tab = new Tab();
            tabs.add(tab);
            if (selected) {
                selectedTab = tabs.size()-1;
            }
            return tab;
        }

        public boolean isSplit() {
            return left != null || right != null;
        }

        public Window getLeft() {
            return left;
        }

        public Window getRight() {
            return right;
        }

        public Window addLeft() {
            return left = new Window();
        }

        public Window addLeft(boolean verticalSplit, double dividerLocation) {
            setVerticalSplit(verticalSplit);
            setDividerLocation(dividerLocation);
            return left = new Window();
        }

        public Window addRight() {
            return right = new Window();
        }

        public boolean isVerticalSplit() {
            return verticalSplit;
        }

        public Window setVerticalSplit(boolean verticalSplit) {
            this.verticalSplit = verticalSplit;
            return this;
        }

        public double getDividerLocation() {
            return dividerLocation;
        }

        public Window setDividerLocation(double dividerLocation) {
            this.dividerLocation = dividerLocation;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Window)) return false;

            Window window = (Window) o;
            return selectedTab == window.selectedTab &&
                    verticalSplit == window.verticalSplit &&
                    Double.compare(dividerLocation, window.dividerLocation) == 0 &&
                    tabs.equals(window.tabs) &&
                    Objects.equals(left, window.left) &&
                    Objects.equals(right, window.right);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tabs, selectedTab, left, right, verticalSplit, dividerLocation);
        }
    }


    public static class TopWindow extends Window {
        private double resultDividerLocation = 0.5;
        private Rectangle location = new Rectangle();
        private Rectangle serverListBounds = new Rectangle();

        public TopWindow() {}

        public TopWindow(Server server, String filename) {
            super(server, filename);
            location = DEFAULT_BOUNDS;
            serverListBounds = DEFAULT_BOUNDS;
        }

        public double getResultDividerLocation() {
            return resultDividerLocation;
        }

        public TopWindow setResultDividerLocation(double resultDividerLocation) {
            this.resultDividerLocation = resultDividerLocation;
            return this;
        }

        public Rectangle getLocation() {
            return location;
        }

        public TopWindow setLocation(Rectangle location) {
            this.location = location;
            return this;
        }

        public Rectangle getServerListBounds() {
            return serverListBounds;
        }

        public TopWindow setServerListBounds(Rectangle serverListBounds) {
            this.serverListBounds = serverListBounds;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof TopWindow)) return false;
            if (!super.equals(o)) return false;

            TopWindow topWindow = (TopWindow) o;
            return Double.compare(resultDividerLocation, topWindow.resultDividerLocation) == 0 &&
                    location.equals(topWindow.location) && serverListBounds.equals(topWindow.serverListBounds);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), resultDividerLocation, location, serverListBounds);
        }
    }

    public static class Tab {
        private String filename = null;
        private Server server = Server.NO_SERVER;
        private String content = "";
        private boolean modified = false;
        private LineEnding lineEnding = LineEnding.Unix;
        private int caret = 0;

        Tab() {}

        public Tab(Server server, String filename) {
            this.server = server;
            this.filename = filename;
            lineEnding = Config.getInstance().getEnum(Config.DEFAULT_LINE_ENDING);
        }

        public String getFilename() {
            return filename;
        }

        public Server getServer() {
            return server;
        }

        public String getContent() {
            return content;
        }

        public boolean isModified() {
            return modified;
        }

        public LineEnding getLineEnding() {
            return lineEnding;
        }

        public int getCaret() {
            return caret;
        }

        public Tab addFilename(String filename) {
            this.filename = filename;
            return this;
        }

        public Tab addServer(Server server) {
            this.server = server;
            return this;
        }

        public Tab addServer(String serverFullName, String connectionString, String auth) {
            server = Config.getInstance().getServerConfig().getServer(serverFullName);
            if (server == Server.NO_SERVER) {
                QConnection.Parser parser = new QConnection.Parser(connectionString);
                if (!parser.hasError()) {
                    parser.setSpecifiedProtocol(true);
                    server = Config.getInstance().getServerConfig().lookup(parser, auth);
                }
            }
            return this;
        }

        public Tab addContent(String content) {
            modified = true;
            this.content = content;
            return this;
        }

        public Tab addContent(Content content) {
            return addContent(content.getContent())
                    .setLineEnding(content.getLineEnding())
                    .setModified(content.isModified());
        }

        public Tab setModified(boolean modified) {
            this.modified = modified;
            return this;
        }

        public Tab setLineEnding(LineEnding lineEnding) {
            this.lineEnding = lineEnding;
            return this;
        }

        public Tab setCaret(int caret) {
            this.caret = caret;
            return this;
        }

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof Tab)) return false;

            Tab tab = (Tab) o;
            return modified == tab.modified &&
                    caret == tab.caret &&
                    server.equals(tab.server) &&
                    Objects.equals(filename, tab.filename) &&
                    content.equals(tab.content) &&
                    lineEnding == tab.lineEnding;
        }

        @Override
        public int hashCode() {
            return Objects.hash(filename, server, content, modified, lineEnding, caret);
        }
    }
}

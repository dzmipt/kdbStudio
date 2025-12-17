package studio.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.kdb.KTableModel;
import studio.kdb.Server;
import studio.kdb.Workspace;
import studio.ui.chart.Chart;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class WindowFactory {

    private final static WindowFactory factory = new WindowFactory();
    private static boolean initialized = false;

    private final static List<StudioWindow> studioWindows = new ArrayList<>();
    private final static List<Chart> charts = new ArrayList<>();

    private static StudioWindow lastActiveWindow = null;
    private static StudioWindow activeWindow = null;

    private static Frame lastActiveChartFrame = null;
    private static Frame activeChartFrame = null;

    private final static Logger log = LogManager.getLogger();

    public static class StopIteration extends RuntimeException {}

    public static void init() {
        if (initialized ) throw new IllegalStateException("WindowFactory has been already initialized");
        initialized = true;

        KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        kfm.addPropertyChangeListener("activeWindow",
                e -> activeWindowChanged((Window)e.getOldValue(), (Window)e.getNewValue()));
    }

    public static Chart newChart(KTableModel table) {
        Chart chart = new Chart();
        charts.add(chart);
        chart.init(table);
        newFrame(chart.getFrame());
        return chart;
    }

    public static StudioWindow newStudioWindow(Server server, String filename) {
        return newStudioWindow(new Workspace.TopWindow(server, filename));
    }

    public static StudioWindow newStudioWindow(Workspace.TopWindow workspaceWindow) {
        StudioWindow window = new StudioWindow();
        newFrame(window);
        studioWindows.add(window);
        window.init(workspaceWindow);
        return window;
    }

    public static StudioWindow getActiveWindow() {
        return activeWindow;
    }

    public static StudioWindow getLastActiveWindow() {
        if (activeWindow != null) return activeWindow;
        if (lastActiveWindow != null) return lastActiveWindow;
        if (studioWindows.isEmpty()) return null;
        return studioWindows.get(0);
    }

    public static List<StudioWindow> allStudioWindows() {
        return Collections.unmodifiableList(studioWindows);
    }

    public static List<Chart> allCharts() {
        return Collections.unmodifiableList(charts);
    }

    public static boolean forEach(Consumer<StudioWindow> action) {
        try {
            for (StudioWindow window : studioWindows) {
                action.accept(window);
            }
            return true;
        } catch (StopIteration e) {
            return false;
        }
    }

    public static boolean forEachEditors(Consumer<EditorTab> action) {
        return forEach(window -> {
            for (EditorTab editor: window.getRootEditorsPanel().getAllEditors(false) ) {
                action.accept(editor);
            }
        });

    }

    public static boolean forEachResultTabs(Consumer<ResultTab> action) {
        return forEach(window -> {
            int count = window.countResultTabs();
            for (int index=0; index<count; index++) {
                action.accept(window.getResultTab(index));
            }
        });
    }


    public static void activate(JFrame f) {
        int state = f.getExtendedState();
        if ((state & Frame.ICONIFIED) != 0) {
            f.setExtendedState(state & ~Frame.ICONIFIED);
        }
        f.requestFocus();
        f.toFront();
        if (! GraphicsEnvironment.isHeadless() && Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.APP_REQUEST_FOREGROUND)) {
                desktop.requestForeground(true);
            }
        }
    }

    private static void activeWindowChanged(Window oldWin, Window newWindow) {
        if (newWindow == null) {
            return;
        }

        for (Window owner = newWindow.getOwner(); owner != null; owner = owner.getOwner()) {
            if (findWindow(owner) != -1 ) return;
            if (findChart(owner) != -1 ) return;
        }

        int index = findWindow(newWindow);
        if (index != -1) setActiveWindow(studioWindows.get(index));

        index = findChart(newWindow);
        if (index != -1) setActiveChartFrame(charts.get(index).getFrame());
    }

    private static void setActiveWindow(StudioWindow newActiveWindow) {
        if (newActiveWindow == null) log.info("new Studio activeWindow is null");
        else log.info("new activeWindow: " + newActiveWindow.getRealTitle());

        if (activeWindow != null) lastActiveWindow = activeWindow;
        activeWindow = newActiveWindow;
        refreshTaskBarMenu();
    }

    private static void setActiveChartFrame(Frame newActiveChart) {
        if (activeChartFrame != null) lastActiveChartFrame = activeChartFrame;
        activeChartFrame = newActiveChart;
        refreshTaskBarMenu();
    }

    private static int findWindow(Window window) {
        for (int i=0; i<studioWindows.size(); i++) {
            if (studioWindows.get(i) == window) return i;
        }
        return -1;
    }

    private static int findChart(Window window) {
        for (int i=0; i<charts.size(); i++) {
            if (charts.get(i).getFrame() == window) return i;
        }
        return -1;
    }

    private static void newFrame(Frame f) {
        f.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                boolean found = false;
                int index = findWindow(f);
                if (index != -1) {
                    studioWindows.remove(index);
                    found = true;
                }
                if (!found) {
                    index = findChart(f);
                    if (index != -1) {
                        charts.remove(index);
                        found = true;
                    }
                }

                if (!found) return;

                if (lastActiveWindow == f) lastActiveWindow = null;
                if (activeWindow == f) activeWindow = null;

                if (lastActiveChartFrame == f) lastActiveChartFrame = null;
                if (activeChartFrame == f) activeChartFrame = null;

                refreshTaskBarMenu();
            }
        });
        f.addPropertyChangeListener("title", e-> refreshTaskBarMenu());

        refreshTaskBarMenu();
    }

    private static String getTaskBarMenuPrefix(Object current, Object active, Object last) {
        if (current == active) return "✔ ";
        if (current == last) return "∙  ";
        return "    ";
    }

    private static void refreshTaskBarMenu() {
        if (!Util.MAC_OS_X) return;
        if (! Taskbar.isTaskbarSupported()) return;

        PopupMenu dockMenu = new PopupMenu();
        for (StudioWindow window: studioWindows) {
            MenuItem menuItem = new MenuItem(getTaskBarMenuPrefix(window, activeWindow, lastActiveWindow) + window.getRealTitle());
            menuItem.addActionListener(e -> {
                activate(window);
            });
            dockMenu.add(menuItem);
        }

        if (!studioWindows.isEmpty() && !charts.isEmpty()) {
            dockMenu.addSeparator();
        }
        for (Chart chart: charts) {
            MenuItem menuItem = new MenuItem(getTaskBarMenuPrefix(chart.getFrame(), activeChartFrame, lastActiveChartFrame) + chart.getChartTitle());
            menuItem.addActionListener(e -> activate(chart.getFrame()));
            dockMenu.add(menuItem);
        }
        Taskbar.getTaskbar().setMenu(dockMenu);

    }

}
package studio.mcp.ui;

import studio.mcp.McpGrants;
import studio.mcp.McpRunningQueries;
import studio.mcp.McpService;
import studio.mcp.McpSessions;
import studio.ui.MinSizeLabel;
import studio.ui.StudioOptionPane;
import studio.ui.StudioWindow;
import studio.utils.log4j.EnvConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Status bar widget: MCP server state, connected agents and active query grants
 */
public class McpStatusWidget extends MinSizeLabel implements McpService.Listener {

    private static final Color COLOR_IDLE = Color.GRAY;
    private static final Color COLOR_CONNECTED = new Color(0, 140, 0);
    private static final Color COLOR_RUNNING = new Color(0, 90, 200);
    private static final Color COLOR_ERROR = new Color(200, 0, 0);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Timer timer;

    public McpStatusWidget() {
        super("");
        setHorizontalAlignment(JLabel.CENTER);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        timer = new Timer(5000, e -> refresh());
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showPopup();
            }
        });
        refresh();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        McpService.getInstance().addListener(this);
        timer.start();
        refresh();
    }

    @Override
    public void removeNotify() {
        McpService.getInstance().removeListener(this);
        timer.stop();
        super.removeNotify();
    }

    @Override
    public void mcpStateChanged() {
        refresh();
    }

    private void refresh() {
        McpService service = McpService.getInstance();
        McpService.State state = service.getState();
        if (state == McpService.State.DISABLED) {
            setVisible(false);
            return;
        }
        setVisible(true);

        if (state == McpService.State.FAILED) {
            setText(" MCP: error ");
            setForeground(COLOR_ERROR);
            setToolTipText("MCP server: " + service.getError());
            return;
        }

        List<McpSessions.Session> agents = service.getSessions().getActive();
        int grants = service.getGrants().getActive().size();
        int running = service.getRunningQueries().getAll().size();
        String text;
        if (agents.isEmpty()) {
            text = "MCP :" + service.getPort();
            setForeground(COLOR_IDLE);
        } else {
            text = agents.size() == 1 ? "MCP: " + agents.get(0).getClientName() : "MCP: " + agents.size() + " agents";
            setForeground(COLOR_CONNECTED);
        }
        if (running > 0) {
            text += " • " + running + " running";
            setForeground(COLOR_RUNNING);
        }
        if (grants > 0) {
            text += " • " + grants + (grants == 1 ? " grant" : " grants");
        }
        setText(" " + text + " ");

        StringBuilder tooltip = new StringBuilder("<html>MCP server is listening on port ").append(service.getPort());
        if (agents.isEmpty()) tooltip.append("<br/>No agents connected");
        for (McpSessions.Session agent: agents) {
            tooltip.append("<br/>Connected: ").append(escape(agent.getDisplayName()));
        }
        if (running > 0) tooltip.append("<br/>Agent queries running: ").append(running);
        if (grants > 0) tooltip.append("<br/>Queries allowed without confirmation: ").append(grants).append(" server(s)");
        tooltip.append("<br/>Click for details</html>");
        setToolTipText(tooltip.toString());
    }

    private static String escape(String text) {
        return PlainText.escape(text);
    }

    private static String formatTime(java.time.Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(TIME_FORMAT);
    }

    private void showPopup() {
        McpService service = McpService.getInstance();
        JPopupMenu menu = new JPopupMenu();

        if (service.getState() == McpService.State.FAILED) {
            JMenuItem item = PlainText.menuItem(service.getError());
            item.setEnabled(false);
            menu.add(item);
        } else {
            List<McpSessions.Session> agents = service.getSessions().getActive();
            if (agents.isEmpty()) {
                JMenuItem item = new JMenuItem("No agents connected");
                item.setEnabled(false);
                menu.add(item);
            }
            for (McpSessions.Session agent: agents) {
                JMenuItem item = PlainText.menuItem(agent.getDisplayName() + " from " + agent.getRemoteAddress() +
                        ", last active at " + formatTime(agent.getLastActivity()));
                item.setEnabled(false);
                menu.add(item);
            }
        }
        menu.addSeparator();

        List<McpRunningQueries.RunningQuery> running = service.getRunningQueries().getAll();
        if (running.isEmpty()) {
            JMenuItem item = new JMenuItem("No agent queries running");
            item.setEnabled(false);
            menu.add(item);
        } else {
            for (McpRunningQueries.RunningQuery query: running) {
                long seconds = java.time.Duration.between(query.getStarted(), java.time.Instant.now()).getSeconds();
                String text = query.getQuery().replace('\n', ' ');
                if (text.length() > 60) text = text.substring(0, 60) + "...";
                JMenuItem item = PlainText.menuItem("Cancel: " + text + "  (" + query.getServer() + ", " + seconds + " s)");
                item.setToolTipText("<html>" + escape(query.getAgent()) + " on " + escape(query.getServer()) + ", " +
                        escape(query.getTarget()) + "<br/><pre>" + escape(query.getQuery()) + "</pre></html>");
                item.addActionListener(e -> service.getRunningQueries().cancel(query));
                menu.add(item);
            }
            if (running.size() > 1) {
                JMenuItem cancelAll = new JMenuItem("Cancel all agent queries");
                cancelAll.addActionListener(e -> service.getRunningQueries().cancelAll());
                menu.add(cancelAll);
            }
        }
        menu.addSeparator();

        List<McpGrants.Grant> grants = service.getGrants().getActive();
        JMenuItem grantsItem = new JMenuItem("Active query permissions (" + grants.size() + ")...");
        grantsItem.addActionListener(e -> GrantsDialog.showDialog(this));
        menu.add(grantsItem);

        JMenuItem revokeAll = new JMenuItem("Revoke all query permissions");
        revokeAll.setEnabled(!grants.isEmpty());
        revokeAll.addActionListener(e -> {
            studio.mcp.McpLog.info("All query grants are revoked by the user");
            service.getGrants().revokeAll();
        });
        menu.add(revokeAll);
        menu.addSeparator();

        JMenuItem windowItem = new JMenuItem("Show agent queries window");
        windowItem.setToolTipText("Queries which agents run on background connections, with their results");
        windowItem.addActionListener(e -> AgentQueriesWindow.open());
        menu.add(windowItem);

        JMenuItem connectItem = new JMenuItem("Connect an agent...");
        connectItem.addActionListener(e -> ConnectAgentDialog.showDialog(this));
        menu.add(connectItem);

        JMenuItem logItem = new JMenuItem("Open MCP log");
        logItem.addActionListener(e -> openLog());
        menu.add(logItem);

        JMenuItem settingsItem = new JMenuItem("Settings...");
        settingsItem.addActionListener(e -> StudioWindow.settings(studio.ui.SettingsDialog.MCP_PAGE));
        menu.add(settingsItem);

        Dimension size = menu.getPreferredSize();
        menu.show(this, getWidth() - size.width, -size.height);
    }

    private void openLog() {
        File file = EnvConfig.getFilepath("log").resolve("mcp.log").toFile();
        try {
            if (!file.exists()) {
                StudioOptionPane.showMessage(this, "The MCP log file doesn't exist yet:\n" + file, "MCP log");
                return;
            }
            Desktop.getDesktop().open(file);
        } catch (Exception e) {
            StudioOptionPane.showError(this, "Can't open " + file + "\n" + e.getMessage(), "MCP log");
        }
    }

}

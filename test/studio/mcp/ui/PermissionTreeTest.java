package studio.mcp.ui;

import com.formdev.flatlaf.FlatLightLaf;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import studio.kdb.Server;
import studio.kdb.ServerTreeNode;
import studio.mcp.McpPermissions;

import javax.swing.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PermissionTreeTest {

    private LookAndFeel original = UIManager.getLookAndFeel();

    @AfterEach
    public void restore() throws Exception {
        UIManager.setLookAndFeel(original);
    }

    // FlatLaf calls the cell renderer while the tree is being created
    @Test
    public void createWithFlatLaf() throws Exception {
        UIManager.setLookAndFeel(new FlatLightLaf());
        ServerTreeNode root = new ServerTreeNode();
        root.add("uat").add(new Server("hdb", "h", 5000, "", "", null, "auth", false));
        root.add("prod").add(new Server("hdb", "h2", 5000, "", "", null, "auth", false));

        SwingUtilities.invokeAndWait(() -> {
            PermissionTree tree = new PermissionTree(root, new McpPermissions(List.of("folder:"), "uat"));
            assertEquals("uat", tree.getPermissions().getFilter());
            assertEquals(1, tree.getPermissions().allowedServers(root).size());
        });
    }
}

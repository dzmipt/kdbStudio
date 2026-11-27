package studio.kdb;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import studio.ui.action.WorkspaceSaver;
import studio.utils.MockConfig;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class WorkspaceAgentWindowTest {

    @BeforeAll
    public static void initConfig() throws IOException {
        MockConfig.mock();
    }

    @Test
    public void agentWindowFlagIsSaved() {
        Workspace workspace = new Workspace();
        workspace.addWindow(true).addTab(true).addContent("1+1");
        workspace.addWindow(false).setAgentWindow(true).addTab(true).addContent("/ agent");

        JsonObject json = WorkspaceSaver.toJson(workspace);
        Workspace restored = WorkspaceSaver.fromJson(json);
        assertFalse(restored.getWindows()[0].isAgentWindow());
        assertTrue(restored.getWindows()[1].isAgentWindow());
    }
}

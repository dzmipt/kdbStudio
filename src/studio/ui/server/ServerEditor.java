package studio.ui.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.kdb.Server;

import java.awt.*;
import java.util.function.Predicate;

import static studio.ui.EscapeDialog.DialogResult.CANCELLED;

public class ServerEditor {

    private static boolean isMocked = false;
    private static Server mockedServer = null;

    private final static Logger log = LogManager.getLogger();

    public static Server selectServer(boolean add, Component owner, Server initServer) {
        return selectServer(add, owner, initServer, null);
    }

    public static Server selectServer(boolean add, Component owner, Server initServer, Predicate<Server> serverValidator) {
        if (isMocked) {
            if (serverValidator != null) {
                if (! serverValidator.test(mockedServer)) {
                    throw new IllegalStateException("Unexpected behaviour");
                }
            }
            return mockedServer;
        }

        String title = add ? "Add a new server" : "Edit Server Details";

        EditServerDialog dialog = new EditServerDialog(owner, title, initServer);
        if (serverValidator != null) {
            dialog.setAcceptValidator( () -> serverValidator.test(dialog.getServer()) );
        }

        dialog.alignAndShow();
        if (dialog.getResult() == CANCELLED) {
            return null;
        }

        return dialog.getServer();
    }

    public synchronized static void mock(Server server) {
        isMocked = true;
        mockedServer = server;
        log.info("Mocked with {}", server);
    }

    public synchronized static void resetMock() {
        isMocked = false;
        mockedServer = null;
        log.info("Reset mock");
    }
}

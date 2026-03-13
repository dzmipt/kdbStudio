package studio.ui;

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

        ServerForm form = new ServerForm(owner, title, initServer);
        if (serverValidator != null) {
            form.setAcceptValidator( () -> serverValidator.test(form.getServer()) );
        }

        form.alignAndShow();
        if (form.getResult() == CANCELLED) {
            return null;
        }

        return form.getServer();
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

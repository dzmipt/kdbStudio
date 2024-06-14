package studio.ui;

import studio.kdb.Server;

import java.awt.*;

public class AddServerForm extends ServerForm {
    public AddServerForm(Window owner, Server server) {
        super(owner, "Add a new server", server);
    }
}

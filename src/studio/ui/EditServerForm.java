package studio.ui;

import studio.kdb.Server;

import java.awt.*;

public class EditServerForm extends ServerForm {
//@TODO Can we Server.NO_SERVER modified here??
    public EditServerForm(Window owner, Server server) {
        super(owner,"Edit Server Details",server);
    }
}

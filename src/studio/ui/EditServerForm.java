package studio.ui;

import studio.kdb.Server;

import java.awt.*;

public class EditServerForm extends ServerForm {
//@TODO Can we Server.NO_SERVER modified here??
    public EditServerForm(Component windowOwner, Server server) {
        super(windowOwner,"Edit Server Details",server);
    }
}

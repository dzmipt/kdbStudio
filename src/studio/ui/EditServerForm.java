package studio.ui;

import studio.kdb.Server;

import java.awt.*;

public class EditServerForm extends ServerForm {

    public EditServerForm(Component windowOwner, Server server) {
        super(windowOwner,"Edit Server Details",server);
    }
}

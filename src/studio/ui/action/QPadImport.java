package studio.ui.action;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.kdb.Config;
import studio.kdb.Server;
import studio.kdb.ServerTreeNode;
import studio.ui.EscapeDialog;
import studio.ui.QPadImportDialog;
import studio.ui.StudioOptionPane;
import studio.ui.StudioWindow;
import studio.utils.QPadConverter;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class QPadImport {

    private static QPadImportDialog dialog;

    private static final Logger log = LogManager.getLogger();

    public static void doImport(StudioWindow window) {
        if (dialog == null) {
            dialog = new QPadImportDialog(window);
            dialog.align();
        }

        ServerTreeNode serverTree;
        ServerTreeNode rootToImport = new ServerTreeNode();
        while(true) {
            dialog.setVisible(true);
            if (dialog.getResult() == EscapeDialog.DialogResult.CANCELLED) return;

            if (dialog.getImportTo() == QPadImportDialog.Location.Overwrite) {
                Config.getInstance().getServerConfig().setRoot(new ServerTreeNode());
            }

            String rootName = dialog.getRootName().trim();
            if (rootName.length() > 0) {
                serverTree = Config.getInstance().getServerTree();
                if (serverTree.getChild(rootName) != null) {
                    StudioOptionPane.showError(window, "Folder to import already exists (" + rootName + ")",
                            "Folder Exists");
                    continue;
                }
                rootToImport = rootToImport.add(rootName);
            }
            break;
        }

        try {
            List<Server> serverList = QPadConverter.importFromFiles(new File(dialog.getServersCfgLocation()),
                    rootToImport, dialog.getDefaultAuthenticationMechanism(), dialog.getCredentials());
            Server[] servers = serverList.toArray(new Server[0]);
            String[] errors = Config.getInstance().getServerConfig().addServers(true, servers);

            StringBuilder errorBuilder = new StringBuilder();
            StringBuilder successfulBuilder = new StringBuilder();
            int successful = 0;
            for (int index = 0; index<errors.length; index++) {
                if (errors[index] == null) {
                    successful++;
                    successfulBuilder.append(servers[index].getFullName()).append("\n");
                }
                else {
                    errorBuilder.append(errors[index]).append("\n");
                }
            }

            StringBuilder result = new StringBuilder();
            if (errorBuilder.length()>0) {
                result.append(successful).append(" of ").append(servers.length).append(" servers have been successfully imported\n");
                result.append("---------------------------------------------------------------------\n");
                result.append(servers.length - successful).append(" errors:\n");
                result.append(errorBuilder);
                result.append("---------------------------------------------------------------------\n");
            }
            result.append(successful).append(" servers have been successfully imported:\n");
            result.append(successfulBuilder);

            JTextArea txtResult = new JTextArea(result.toString(), 20, 50);
            JOptionPane.showConfirmDialog(window, new JScrollPane(txtResult), "Import from QPad", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE);

        } catch (IOException e) {
            log.error("Error in reading Servers.cfg", e);
            StudioOptionPane.showError(window, "Error in reading QPad Servers.cfg", "Error");
        } catch (IllegalArgumentException e) {
            log.error("Error during importing", e);
            StudioOptionPane.showError(window, "Error during import", "Error");
        }

    }
}

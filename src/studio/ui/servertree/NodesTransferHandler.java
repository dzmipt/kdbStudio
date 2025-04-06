package studio.ui.servertree;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.kdb.ServerTreeNode;
import studio.utils.BasicDataFlavor;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class NodesTransferHandler extends TransferHandler {
    private static final BasicDataFlavor nodesFlavor = new BasicDataFlavor(ServerTreeNode.class);
    private static final Logger log = LogManager.getLogger();


    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        ServerTree tree = (ServerTree) c;
        ServerTreeNode node = tree.getSelectedNode();
        if (node == null) return null;

        return nodesFlavor.getTransferable(node);
    }


    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
        if (!support.isDrop()) {
            return false;
        }
        support.setShowDropLocation(true);
        if (!support.isDataFlavorSupported(nodesFlavor)) {
            return false;
        }
        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
        TreePath dest = dl.getPath();
        if (dest == null) return false;

        int index = dl.getChildIndex();
        if (index ==-1) {
            ServerTreeNode node = (ServerTreeNode) dest.getLastPathComponent();
            if (! node.isFolder()) return false;
        }

        try {
            ServerTreeNode importNode = (ServerTreeNode) support.getTransferable().getTransferData(nodesFlavor);
            if (importNode.isFolder()) {
                TreePath importPath = new TreePath(importNode.getPath());
                if (importPath.isDescendant(dest)) return false;
            }
        } catch (UnsupportedFlavorException | IOException e) {
            log.error("Error in getting node to transfer", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }

        log.info("Now we need to import");
        try {
            ServerTreeNode importNode = (ServerTreeNode) support.getTransferable().getTransferData(nodesFlavor);

            JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
            ServerTreeNode destinationNode = (ServerTreeNode) dl.getPath().getLastPathComponent();
            int index = dl.getChildIndex();
            return ((ServerTree)support.getComponent()).moveNode(importNode, destinationNode, index);
        } catch (UnsupportedFlavorException | IOException e) {
            log.error("Error in getting node to transfer", e);
            return false;
        }
    }

}

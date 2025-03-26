package studio.kdb;

import org.junit.jupiter.api.Test;
import studio.core.DefaultAuthenticationMechanism;

import javax.swing.tree.TreeNode;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;


public class ServerTreeNodeTest {

    @Test
    public void findPathTest() {
        ServerTreeNode serverTree = new ServerTreeNode();
        ServerTreeNode folderA = serverTree.add("a");
        ServerTreeNode folderB = folderA.add("b");
        ServerTreeNode serverNode = folderB.add(Server.NO_SERVER);

        ServerTreeNode folderB1 = serverTree.add("b");
        ServerTreeNode serverNode1 = folderB1.add(Server.NO_SERVER);

        assertTrue( folderB1.theSame(serverTree.findPath(new TreeNode[] {new ServerTreeNode(), new ServerTreeNode("b")})));

        ServerTreeNode folderC = serverTree.findPath(new TreeNode[] {new ServerTreeNode(), new ServerTreeNode("c")});
        assertNull(folderC);

        folderC = serverTree.findPath(new TreeNode[] {new ServerTreeNode(), new ServerTreeNode("c")}, true);
        assertNotNull(folderC);
        assertEquals(3, serverTree.getChildCount());
        assertTrue(folderC.theSame(serverTree.getChild(2)));

        // The first TreeNode should be ignored.
        ServerTreeNode folderD = serverTree.findPath(new TreeNode[] {new ServerTreeNode("c"), new ServerTreeNode("d")}, true);
        assertNotNull(folderD);
        assertEquals(0, folderC.getChildCount());
        assertEquals(4, serverTree.getChildCount());
        assertTrue(folderD.theSame(serverTree.getChild(3)));
        assertEquals("d", serverTree.getChild(3).getFolder());

    }

    @Test
    public void getFullName() {
        String name1 = "serverName1";
        String name2 = "serverName2";
        Server server1 = new Server(name1, "testHost", 1111, "user", "password", Color.WHITE,
                DefaultAuthenticationMechanism.NAME, false);
        Server server2 = new Server(name2, "testHost", 1111, "user", "password", Color.WHITE,
                DefaultAuthenticationMechanism.NAME, false);

        assertEquals(name1, server1.getFullName());

        ServerTreeNode root = new ServerTreeNode();
        ServerTreeNode parent = root.add("parent");
        ServerTreeNode childFolder = parent.add("childFolder");
        Server s1 = server1.newFolder(childFolder);

        assertEquals("parent/childFolder/" + name1, s1.getFullName());

        Server s2 = server2.newFolder(root);
        assertEquals(name2, s2.getFullName());
    }

}

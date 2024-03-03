package studio.utils;

import studio.core.Credentials;
import studio.kdb.Server;
import studio.kdb.ServerTreeNode;

import javax.swing.tree.TreeNode;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class QPadConverter {

    public static List<Server> importFromFiles(File file, ServerTreeNode root,
                                               String defaultAuth,
                                               Credentials defaultCredentials) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        List<Server> servers = new ArrayList<>();
        for(String line:lines) {
            Server server = convert(line, root, defaultAuth, defaultCredentials);
            if (server != Server.NO_SERVER) servers.add(server);
        }
        return servers;
    }

    static Server convert(String line, String defaultAuth, Credentials defaultCredentials) {
        return convert(line, new ServerTreeNode(), defaultAuth, defaultCredentials);
    }

    private static Server convertConnection(String conn, String defaultAuth,
                                            Credentials defaultCredentials) {
        try {
            Server server = QConnection.getByConnection(conn, defaultAuth, defaultCredentials, Collections.emptyList());
            String password = server.getPassword();
            if (password.contains("?")) {
                int index = password.indexOf('?');
                server = server.newAuthMethod(password.substring(0, index));
            }
            return server;
        } catch (IllegalArgumentException e) {
            return Server.NO_SERVER;
        }
    }

    static Server convert(String line, ServerTreeNode root, String defaultAuth,
                            Credentials defaultCredentials) {
        if (! line.startsWith("`")) return Server.NO_SERVER;
        line = line.substring(1);
        String[] items = line.split("\\`");
        if (items.length < 2) return Server.NO_SERVER;

        Server server = convertConnection(items[0], defaultAuth, defaultCredentials);
        if (server == Server.NO_SERVER) return Server.NO_SERVER;

        server = server.newName(items[items.length-1]);

        TreeNode[] folderNodes = Stream.concat(
                    Stream.of(""),
                    Stream.of(items).skip(1).limit(items.length-2))
                        .map(String::trim)
                        .map(folder -> folder.length()==0 ? "[empty]" : folder )
                        .map(ServerTreeNode::new).toArray(TreeNode[]:: new);
        ServerTreeNode folder = root.findPath(folderNodes, true);
        server = server.newFolder(folder);
        folder.add(server);
        return server;
    }
}

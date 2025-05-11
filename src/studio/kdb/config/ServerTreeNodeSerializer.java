package studio.kdb.config;

import com.google.gson.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.core.DefaultAuthenticationMechanism;
import studio.kdb.Server;
import studio.kdb.ServerTreeNode;
import studio.ui.FileChooser;
import studio.ui.StudioOptionPane;
import studio.utils.QConnection;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ServerTreeNodeSerializer implements JsonSerializer<ServerTreeNode>, JsonDeserializer<ServerTreeNode> {
    private static final Logger log = LogManager.getLogger();

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(ServerTreeNode.class, new ServerTreeNodeSerializer())
            .setPrettyPrinting()
            .create();


    public static String toJson(ServerTreeNode serverTree) {
        return gson.toJson(serverTree);
    }

    public static ServerTreeNode fromJson(String content) {
        return gson.fromJson(content, ServerTreeNode.class);
    }

    public static ServerTreeNode openImportDialog(Component parent) {
        File file = FileChooser.openFile(parent, FileChooser.JSON_FF);
        if (file == null) return null;
        try {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            return ServerTreeNodeSerializer.fromJson(content);

        } catch (Exception e) {
            log.error("Error in loading from file {}", file, e);
            StudioOptionPane.showError(parent, "Error in loading to file " + file, "File Error");
        }
        return null;
    }

    public static void openExportDialog(Component parent, ServerTreeNode root) {
        File file = FileChooser.saveFile(parent, FileChooser.JSON_FF);
        if (file == null) return;

        if (file.exists()) {
            int result = StudioOptionPane.showYesNoDialog(parent, "File " + file + " exist. Overwrite?", "Overwrite File");
            if (result != JOptionPane.YES_OPTION) return;
        }

        try {
            String content = ServerTreeNodeSerializer.toJson(root);
            Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("Error in saving to file {}", file, e);
            StudioOptionPane.showError(parent, "Error in saving to file " + file, "File Error");
        }
    }

    @Override
    public ServerTreeNode deserialize(JsonElement node, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject json =(JsonObject) node;
        String name = json.get("name").getAsString();
        JsonArray children = json.getAsJsonArray("children");
        if (children != null) {
            ServerTreeNode folder = new ServerTreeNode(name);
            for (JsonElement child: children) {
                folder.add((ServerTreeNode)context.deserialize(child, type));
            }
            return folder;
        } else {
            String handle = json.get("handle").getAsString();

            String colorValue = json.get("bgColor").getAsString();
            Color bgColor = Color.WHITE;
            try {
                if (colorValue != null) {
                    bgColor = new Color(Integer.parseInt(colorValue, 16));
                }
            } catch (NumberFormatException e) {
                log.error("Can't parse color {}", colorValue);
            }

            String authMethod = json.get("authMethod").getAsString();
            if (authMethod == null) authMethod = DefaultAuthenticationMechanism.NAME;


            return new ServerTreeNode(
                    new QConnection(handle).toServer(name, authMethod, bgColor) );
        }
    }

    @Override
    public JsonElement serialize(ServerTreeNode node, Type type, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        if (node.isFolder()) {
            json.addProperty("name", node.getFolder());

            JsonArray children = new JsonArray();
            for (ServerTreeNode child: node.childNodes()) {
                children.add(context.serialize(child, type));
            }

            json.add("children", children);
        } else {
            Server server = node.getServer();
            json.addProperty("name", server.getName());
            json.addProperty("handle", server.getConnectionStringWithPwd());
            json.addProperty("bgColor", Integer.toHexString(server.getBackgroundColor().getRGB()).substring(2));
            json.addProperty("authMethod", server.getAuthenticationMechanism());
        }
        return json;
    }
}

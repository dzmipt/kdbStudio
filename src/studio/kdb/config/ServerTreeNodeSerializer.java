package studio.kdb.config;

import com.google.gson.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.core.DefaultAuthenticationMechanism;
import studio.kdb.Server;
import studio.kdb.ServerTreeNode;
import studio.utils.QConnection;

import java.awt.*;
import java.lang.reflect.Type;

public class ServerTreeNodeSerializer implements JsonSerializer<ServerTreeNode>, JsonDeserializer<ServerTreeNode> {
    private static final Logger log = LogManager.getLogger();

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

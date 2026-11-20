package studio.mcp;

import com.google.gson.*;
import studio.kdb.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders kdb objects for agents as text or JSON, limiting the output size.
 */
public class ResultRenderer {

    private static final int MAX_CELL_CHARS = 200;
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();

    private static final KFormatContext WITH_TYPE = new KFormatContext(KFormatContext.DEFAULT).setShowType(true).setShowThousandsComma(false);
    private static final KFormatContext NO_TYPE = new KFormatContext(KFormatContext.DEFAULT).setShowType(false).setShowThousandsComma(false);

    public static String kind(K.KBase obj) {
        if (obj == null) return "none";
        if (obj instanceof K.Flip) return "table";
        if (obj instanceof K.Dict) {
            K.Dict dict = (K.Dict) obj;
            return dict.x instanceof K.Flip ? "keyedTable" : "dictionary";
        }
        if (obj instanceof K.KBaseVector) return "list";
        if (obj.getType().getType() < 0) return "atom";
        return obj.getType().getName();
    }

    // The number of rows for tabular objects, count for others
    public static int rowCount(K.KBase obj) {
        if (obj == null) return 0;
        KTableModel model = tabularModel(obj);
        if (model != null) return model.getRowCount();
        return obj.count();
    }

    private static KTableModel tabularModel(K.KBase obj) {
        if (!(obj instanceof K.Flip) && !(obj instanceof K.Dict)) return null;
        try {
            return KTableModel.getModel(obj);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String cellText(K.KBase value) {
        if (value == null) return "";
        String text = value.toString(value instanceof K.KBaseVector ? WITH_TYPE : NO_TYPE);
        text = text.replace('\n', ' ').replace('\r', ' ');
        if (text.length() > MAX_CELL_CHARS) text = text.substring(0, MAX_CELL_CHARS) + "...";
        return text;
    }

    public static String render(K.KBase obj, boolean json, int maxRows, int maxChars) {
        if (json) return gson.toJson(renderJsonLimited(obj, maxRows, maxChars));
        return limit(renderText(obj, maxRows), maxChars);
    }

    /**
     * JSON representation which fits into maxChars: rows are reduced (or the value is cut) if needed
     */
    public static JsonObject renderJsonLimited(K.KBase obj, int maxRows, int maxChars) {
        int rows = maxRows;
        while (true) {
            JsonObject json = renderJsonObject(obj, rows);
            int length = gson.toJson(json).length();
            if (length <= maxChars) return json;
            if (json.has("value")) {
                String value = json.get("value").getAsString();
                int keep = Math.max(0, value.length() - (length - maxChars) - 20);
                json.addProperty("value", value.substring(0, keep) + "...");
                json.addProperty("truncated", true);
                return json;
            }
            if (rows == 0) return json;
            rows = rows / 2;
        }
    }

    public static String limit(String text, int maxChars) {
        if (text.length() <= maxChars) return text;
        return text.substring(0, maxChars) + "\n... (truncated: " + maxChars + " of " + text.length() + " characters shown)";
    }

    public static String renderText(K.KBase obj, int maxRows) {
        if (obj == null) return "(no result)";
        KTableModel model = tabularModel(obj);
        if (model == null) {
            return obj.toString(WITH_TYPE);
        }

        int rows = model.getRowCount();
        int cols = model.getColumnCount();
        int shown = Math.min(rows, maxRows);

        String[][] cells = new String[shown + 1][cols];
        int[] widths = new int[cols];
        for (int col = 0; col < cols; col++) {
            cells[0][col] = model.getColumnName(col);
            widths[col] = cells[0][col].length();
        }
        for (int row = 0; row < shown; row++) {
            for (int col = 0; col < cols; col++) {
                String text = cellText(model.get(row, col));
                cells[row + 1][col] = text;
                widths[col] = Math.max(widths[col], text.length());
            }
        }

        int lastKey = -1;
        for (int col = 0; col < cols; col++) {
            if (model.isKey(col)) lastKey = col;
        }

        StringBuilder builder = new StringBuilder();
        for (int line = 0; line <= shown; line++) {
            appendLine(builder, cells[line], widths, lastKey);
            if (line == 0) {
                String[] dashes = new String[cols];
                for (int col = 0; col < cols; col++) dashes[col] = "-".repeat(widths[col]);
                appendLine(builder, dashes, widths, lastKey);
            }
        }
        if (shown < rows) {
            builder.append(String.format("... (%,d of %,d rows shown)%n", shown, rows));
        }
        return builder.toString();
    }

    private static void appendLine(StringBuilder builder, String[] cells, int[] widths, int lastKey) {
        StringBuilder line = new StringBuilder();
        for (int col = 0; col < cells.length; col++) {
            if (col > 0) line.append(col == lastKey + 1 ? " | " : " ");
            line.append(cells[col]);
            if (col < cells.length - 1) line.append(" ".repeat(widths[col] - cells[col].length()));
        }
        builder.append(line.toString().stripTrailing()).append('\n');
    }

    private static JsonElement jsonValue(K.KBase value) {
        if (value == null) return JsonNull.INSTANCE;
        KType type = value.getType();
        if (type == KType.Boolean) {
            return new JsonPrimitive(((K.KBoolean) value).b);
        }
        if (value.isNull()) return JsonNull.INSTANCE;
        if (type == KType.Short || type == KType.Int || type == KType.Long || type == KType.Byte) {
            ToDouble d = (ToDouble) value;
            if (d.isPositiveInfinity() || d.isNegativeInfinity()) return new JsonPrimitive(cellText(value));
            // longs are read directly: values above 2^53 can't be represented exactly as double
            if (value instanceof K.KLongBase) return new JsonPrimitive(((K.KLongBase) value).toLong());
            return new JsonPrimitive((long) d.toDouble());
        }
        if (type == KType.Float || type == KType.Double) {
            double d = ((ToDouble) value).toDouble();
            if (Double.isNaN(d) || Double.isInfinite(d)) return new JsonPrimitive(cellText(value));
            return new JsonPrimitive(d);
        }
        return new JsonPrimitive(cellText(value));
    }

    public static String renderJson(K.KBase obj, int maxRows) {
        return gson.toJson(renderJsonObject(obj, maxRows));
    }

    public static JsonObject renderJsonObject(K.KBase obj, int maxRows) {
        JsonObject json = new JsonObject();
        json.addProperty("type", kind(obj));
        if (obj == null) return json;

        KTableModel model = tabularModel(obj);
        if (model == null) {
            json.addProperty("qtype", obj.getType().getName());
            json.addProperty("count", obj.count());
            json.addProperty("value", obj.toString(WITH_TYPE));
            return json;
        }

        int rows = model.getRowCount();
        int cols = model.getColumnCount();
        int shown = Math.min(rows, maxRows);

        JsonArray columns = new JsonArray();
        for (int col = 0; col < cols; col++) {
            JsonObject column = new JsonObject();
            column.addProperty("name", model.getColumnName(col));
            KColumn kColumn = model.getColumn(col);
            KType elementType = kColumn.getElementType();
            column.addProperty("type", elementType == null ? "" : elementType.getName());
            column.addProperty("key", model.isKey(col));
            columns.add(column);
        }
        json.add("columns", columns);

        JsonArray jsonRows = new JsonArray();
        for (int row = 0; row < shown; row++) {
            JsonArray jsonRow = new JsonArray();
            for (int col = 0; col < cols; col++) {
                jsonRow.add(jsonValue(model.get(row, col)));
            }
            jsonRows.add(jsonRow);
        }
        json.add("rows", jsonRows);
        json.addProperty("totalRows", rows);
        json.addProperty("truncated", shown < rows);
        return json;
    }

    public static List<String> columnNames(K.KBase obj) {
        List<String> names = new ArrayList<>();
        KTableModel model = tabularModel(obj);
        if (model == null) return names;
        for (int col = 0; col < model.getColumnCount(); col++) names.add(model.getColumnName(col));
        return names;
    }
}

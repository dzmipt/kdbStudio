package studio.mcp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import studio.kdb.K;

import static org.junit.jupiter.api.Assertions.*;

public class ResultRendererTest {

    private static K.Flip table() {
        return new K.Flip(new K.KSymbolVector("sym", "price"),
                new K.KList(new K.KSymbolVector("a", "bb", "c"), new K.KDoubleVector(1.5, 2, Double.NaN)));
    }

    @Test
    public void textTable() {
        String text = ResultRenderer.renderText(table(), 100);
        assertEquals("sym price\n--- -----\na   1.5\nbb  2\nc   0n\n", text);
    }

    @Test
    public void textTableTruncated() {
        String text = ResultRenderer.renderText(table(), 2);
        assertTrue(text.endsWith("... (2 of 3 rows shown)\n"), text);
        assertFalse(text.contains("\nc"));
    }

    @Test
    public void keyedTable() {
        K.Flip key = new K.Flip(new K.KSymbolVector("id"), new K.KList(new K.KLongVector(1, 2)));
        K.Flip value = new K.Flip(new K.KSymbolVector("v"), new K.KList(new K.KSymbolVector("x", "y")));
        K.Dict dict = new K.Dict(key, value);
        assertEquals("keyedTable", ResultRenderer.kind(dict));
        assertEquals("id | v\n-- | -\n1  | x\n2  | y\n", ResultRenderer.renderText(dict, 10));

        JsonObject json = JsonParser.parseString(ResultRenderer.renderJson(dict, 10)).getAsJsonObject();
        assertTrue(json.getAsJsonArray("columns").get(0).getAsJsonObject().get("key").getAsBoolean());
        assertEquals(1, json.getAsJsonArray("rows").get(0).getAsJsonArray().get(0).getAsLong());
    }

    @Test
    public void jsonTable() {
        JsonObject json = JsonParser.parseString(ResultRenderer.renderJson(table(), 2)).getAsJsonObject();
        assertEquals("table", json.get("type").getAsString());
        assertEquals(3, json.get("totalRows").getAsInt());
        assertTrue(json.get("truncated").getAsBoolean());
        assertEquals("symbol", json.getAsJsonArray("columns").get(0).getAsJsonObject().get("type").getAsString());
        assertEquals("bb", json.getAsJsonArray("rows").get(1).getAsJsonArray().get(0).getAsString());
        assertEquals(2.0, json.getAsJsonArray("rows").get(1).getAsJsonArray().get(1).getAsDouble());
        assertEquals(2, json.getAsJsonArray("rows").size());
    }

    @Test
    public void atomAndList() {
        assertEquals("atom", ResultRenderer.kind(new K.KLong(5)));
        assertEquals("5", ResultRenderer.renderText(new K.KLong(5), 10));
        assertEquals("list", ResultRenderer.kind(new K.KLongVector(1, 2)));
        JsonObject json = JsonParser.parseString(ResultRenderer.renderJson(new K.KLongVector(1, 2), 10)).getAsJsonObject();
        assertEquals(2, json.get("count").getAsInt());
        assertEquals("(no result)", ResultRenderer.renderText(null, 10));
    }

    @Test
    public void limitChars() {
        String text = ResultRenderer.render(new K.KLongVector(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), false, 100, 5);
        assertTrue(text.startsWith("1 2 3"));
        assertTrue(text.contains("truncated"));
    }

    @Test
    public void jsonFitsIntoLimit() {
        long[] values = new long[1000];
        for (int i = 0; i < values.length; i++) values[i] = i;
        K.Flip big = new K.Flip(new K.KSymbolVector("v"), new K.KList(new K.KLongVector(values)));
        String json = ResultRenderer.render(big, true, 1000, 500);
        assertTrue(json.length() <= 500, "length " + json.length());
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        assertTrue(obj.get("truncated").getAsBoolean());
        assertEquals(1000, obj.get("totalRows").getAsInt());
    }
}

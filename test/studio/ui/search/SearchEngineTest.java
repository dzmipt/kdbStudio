package studio.ui.search;

import org.fife.ui.rtextarea.SearchContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class SearchEngineTest {

    private SearchContext context;

    @BeforeEach
    public void init() {
        context = new SearchContext();
        context.setSearchFor("aa");

        context.setMatchCase(true);
        context.setRegularExpression(false);
        context.setWholeWord(false);
    }

    @Test
    public void testSimple() {
        SearchEngine engine = new SearchEngine(context);
        assertTrue(engine.containsIn("aaa"));
        assertTrue(engine.containsIn("aa"));
        assertTrue(engine.containsIn("baa"));
        assertFalse(engine.containsIn("baba"));
        assertFalse(engine.containsIn("aA"));
    }

    @Test
    public void testMatchCase() {
        context.setMatchCase(false);
        SearchEngine engine = new SearchEngine(context);
        assertTrue(engine.containsIn("aa"));
        assertTrue(engine.containsIn("Aa"));
        assertTrue(engine.containsIn("bAa"));
    }

    @Test
    public void testMatchCaseRegexp() {
        context.setMatchCase(false);
        context.setRegularExpression(true);
        SearchEngine engine = new SearchEngine(context);
        assertTrue(engine.containsIn("aa"));
        assertTrue(engine.containsIn("Aa"));
        assertTrue(engine.containsIn("bAa"));
    }

    @Test
    public void testWholeWord() {
        context.setWholeWord(true);
        SearchEngine engine = new SearchEngine(context);
        assertTrue(engine.containsIn("b aa"));
        assertFalse(engine.containsIn("Aa"));
        assertFalse(engine.containsIn("baa"));
        assertTrue(engine.containsIn("baa aa"));
    }

    @Test
    public void testWholeWordRegexp() {
        context.setWholeWord(true);
        context.setRegularExpression(true);
        SearchEngine engine = new SearchEngine(context);
        assertTrue(engine.containsIn("b aa"));
        assertFalse(engine.containsIn("Aa"));
        assertFalse(engine.containsIn("baa"));
        assertTrue(engine.containsIn("baa aa"));
    }

    @Test
    public void testSimpleWithSpecSymbols() {
        context.setSearchFor("(aa");
        SearchEngine engine = new SearchEngine(context);
        assertTrue(engine.containsIn("(aa"));
        assertTrue(engine.containsIn("(a a(aa"));
        assertFalse(engine.containsIn("(a a(aA"));
        assertFalse(engine.containsIn("x(a ax"));
    }

    @Test
    public void testSimpleWithSpecSymbolsCaseInsensitive() {
        context.setSearchFor("(aa");
        context.setMatchCase(false);
        SearchEngine engine = new SearchEngine(context);
        assertTrue(engine.containsIn("(aa"));
        assertTrue(engine.containsIn("(a a(aa"));
        assertTrue(engine.containsIn("(a a(aA"));
        assertFalse(engine.containsIn("x(a ax"));
    }

}

package studio.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TableMarkersTest {

    private TableMarkers markers;

    @BeforeEach
    public void init() {
        markers = new TableMarkers(10);
        markers.mark(1, 2);
        markers.mark(2, 9);
        markers.mark(4, 0);
    }

    @Test
    public void testClear() {
        markers.clear();
        assertFalse(markers.isMarked(1,2));
        assertFalse(markers.isMarked(4,0));
        assertFalse(markers.isMarked(0,0));
    }

    @Test
    public void testMark() {
        assertFalse(markers.isMarked(0,0));
        assertFalse(markers.isMarked(4,9));

        markers.mark(0,0);
        assertTrue(markers.isMarked(0,0));

        markers.mark(1,3);
        assertTrue(markers.isMarked(1,3));
        assertFalse(markers.isMarked(3,1));
    }
}

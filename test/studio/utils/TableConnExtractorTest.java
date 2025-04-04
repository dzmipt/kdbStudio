package studio.utils;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.kdb.K;
import studio.kdb.config.TableConnExtractor;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class TableConnExtractorTest {

    private TableModel table = new DefaultTableModel(
            getTable(
                new String[][] {
                        {"server.com","1234","$something","$something"},
                        {"1.2.3.4","1234","$something","$something"},
                        {"1.2.3.4","1234","`:abc:1111","xyz"},
                        {"1.2.3.4","1234","abc","xyz"},
                        {"server.com","port","$something","$something"},
                        {"server.com","aHost:1050","$something","$something"},
                        {"host","not-a-port", "2022.01.22D16:50:20.310411000", "handle"},
                        {"1.2.3.4.5","1234","$something","$something"},
                        {"1111.2.3.4","1234","$something","$something"}
                }
            ), new String[] {"aHost","aPort","Connection","Handle"});

    private static TableConnExtractor extractor1;
    private static TableConnExtractor extractor2;

    private Object[][] getTable(String[][] values) {
        Object[][] res = new Object[values.length][];
        for (int row=0; row<values.length; row++) {
            res[row] = new K.KCharacterVector[values[row].length];
            for (int col=0; col<values[row].length; col++) {
                res[row][col] = new K.KCharacterVector(values[row][col]);
            }
        }
        return res;
    }

    @BeforeEach
    public void init() {
        extractor1 = new TableConnExtractor(20,
                List.of("conn", "handle"),
                List.of("host", "conn", "handle"),
                List.of("port") );

        extractor2 = new TableConnExtractor(20,
                List.of("conn", "handle"),
                List.of("host"),
                List.of("port") );
    }

    @Test
    public void test() {
        assertArrayEquals(new String[] {"server.com:1234"}, extractor1.getConnections(table, 0,0));
        assertArrayEquals(new String[] {"1.2.3.4:1234"}, extractor1.getConnections(table, 1, 0));
        assertArrayEquals(new String[] {"`:abc:1111", "1.2.3.4:1234", "xyz:1234"}, extractor1.getConnections(table, 2,0));
        assertArrayEquals(new String[] {"1.2.3.4:1234", "abc:1234", "xyz:1234"}, extractor1.getConnections(table, 3, 0));

        assertArrayEquals(new String[] {"server.com:1234"}, extractor2.getConnections(table, 0, 0));
        assertArrayEquals(new String[] {"1.2.3.4:1234"}, extractor2.getConnections(table, 1, 0));
        assertArrayEquals(new String[] {"`:abc:1111", "1.2.3.4:1234"}, extractor2.getConnections(table, 2, 0));
        assertArrayEquals(new String[] {"1.2.3.4:1234"}, extractor2.getConnections(table, 3, 0));
    }

    @Test
    public void testMaxConn() {
        TableConnExtractor extractor = new TableConnExtractor(2,
                List.of("conn", "handle"),
                List.of("host", "conn", "handle"),
                List.of("port") );

        assertArrayEquals(new String[] {"`:abc:1111", "1.2.3.4:1234"}, extractor.getConnections(table, 2, 0));
    }

    @Test
    public void testEmpty() {
        assertArrayEquals(new String[0], extractor1.getConnections(table, 4, 0));
        assertArrayEquals(new String[0], extractor2.getConnections(table, 4, 0));
    }

    @Test
    public void testValue() {
        assertArrayEquals(new String[]{"aHost:1050"}, extractor1.getConnections(table, 5, 1));
        assertArrayEquals(new String[]{"aHost:1050"}, extractor2.getConnections(table, 5, 1));

        assertArrayEquals(new String[0], extractor1.getConnections(table, 5, 0));
        assertArrayEquals(new String[0], extractor2.getConnections(table, 5, 0));

    }

    @Test
    public void testWrongServerNames() {
        assertArrayEquals(new String[0], extractor1.getConnections(table, 6, 2));
        assertArrayEquals(new String[0], extractor1.getConnections(table, 7, 0));
        assertArrayEquals(new String[0], extractor1.getConnections(table, 8, 0));
    }

}

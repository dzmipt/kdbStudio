package studio.kdb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class KColumnTest {

    private final K.Flip flip = new K.Flip(new K.KSymbolVector("a", "b"),
                                            new K.KList(
                                                    new K.KLongVector(10,20,30),
                                                    new K.KDoubleVector(10,20,30)
                                            ) );
    private final K.Flip flipWithAtom = new K.Flip(new K.KSymbolVector("a", "b"),
                                                    new K.KList(
                                                            new K.KLong(10),
                                                            new K.KDoubleVector(10,20,30)
                                                    ) );
    private final K.Flip flipWithFlip = new K.Flip(new K.KSymbolVector("a", "b"),
                                                    new K.KList(
                                                            new K.KLong(10),
                                                            new K.Flip(new K.KSymbolVector("c", "d", "e"),
                                                                    new K.KList(
                                                                            new K.KLong(1),
                                                                            new K.KLong(2),
                                                                            new K.KIntVector(12,13,14,15),
                                                                            new K.KInteger(3)
                                                                    )
                                                            )
                                                    )  );

    @Test
    public void kTypeVectorTest() {
        assertTrue(KType.List.isVector());
        assertTrue(KType.TimeVector.isVector());
        assertTrue(KType.BooleanVector.isVector());

        assertFalse(KType.Boolean.isVector());
        assertFalse(KType.Symbol.isVector());
        assertFalse(KType.Table.isVector());
        assertFalse(KType.Dict.isVector());
    }

    @Test
    public void flipCountTest() {
        assertEquals(3, flip.count());
        assertEquals(3, flipWithAtom.count());
        assertEquals(4, flipWithFlip.count());
    }

    @Test
    public void flipAtTest() {
        K.Dict expected = new K.Dict(
                new K.KSymbolVector("a","b"),
                new K.KList(
                        new K.KLong(20), new K.KDouble(20)
                ) );
        assertEquals(expected, flip.at(1));

        expected = new K.Dict(
                new K.KSymbolVector("a","b"),
                new K.KList(
                        new K.KLong(10), new K.KDouble(30)
                ) );
        assertEquals(expected, flipWithAtom.at(2));


        expected = new K.Dict(
                        new K.KSymbolVector("a","b"),
                        new K.KList(
                                new K.KLong(10),
                                new K.Dict(new K.KSymbolVector("c", "d", "e"),
                                        new K.KList(
                                            new K.KLong(1),
                                            new K.KLong(2),
                                            new K.KInteger(15),
                                            new K.KInteger(3)
                                        )
                                )
                        ) );
        assertEquals(expected, flipWithFlip.at(3));

    }
}

package studio.kdb;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class KTest {

    private static int maxFractionDigits = 7;

    @BeforeAll
    public static void initMaxFractionDigits() {
        KFormatContext.setMaxFractionDigits(maxFractionDigits);
    }

    private void check(K.KBase base, String expectedNoType, String expectedWithType) {
        String actualNoType = base.toString(KFormatContext.NO_TYPE);
        String actualWithType = base.toString(KFormatContext.DEFAULT);
        //uncomment below for easy debugging
//        System.out.println("\"" + actualNoType + "\", \"" + actualWithType + "\"");
        assertEquals(expectedNoType, actualNoType, "Test to not show type");
        assertEquals(expectedWithType, actualWithType, "Test to show type");
    }


    @Test
    public void testIntegerToString() throws Exception {
        check(new K.KInteger(-123), "-123", "-123i");
        check(new K.KInteger(-Integer.MAX_VALUE), "-0W", "-0Wi");
        check(new K.KInteger(Integer.MAX_VALUE), "0W", "0Wi");
        check(new K.KInteger(Integer.MIN_VALUE), "0N", "0Ni");

        check(new K.KIntVector(-10, 10, 3), "-10 10 3", "-10 10 3i");
        check(new K.KIntVector(), "`int$()", "`int$()");
        check(new K.KIntVector(0), "enlist 0", "enlist 0i");
        check(new K.KIntVector(5, Integer.MIN_VALUE, Integer.MAX_VALUE, -Integer.MAX_VALUE), "5 0N 0W -0W", "5 0N 0W -0Wi");
    }

    @Test
    public void testLongToString() throws Exception {
        check(new K.KLong(-123456789), "-123456789", "-123456789");
        check(new K.KLong(-Long.MAX_VALUE), "-0W", "-0W");
        check(new K.KLong(Long.MAX_VALUE), "0W", "0W");
        check(new K.KLong(Long.MIN_VALUE), "0N", "0N");

        check(new K.KLongVector(-10, 10, 3), "-10 10 3", "-10 10 3");
        check(new K.KLongVector(), "`long$()", "`long$()");
        check(new K.KLongVector(0), "enlist 0", "enlist 0");
        check(new K.KLongVector(5, Long.MIN_VALUE, Long.MAX_VALUE, -Long.MAX_VALUE), "5 0N 0W -0W", "5 0N 0W -0W");
    }

    @Test
    public void testShortToString() throws Exception {
        check(new K.KShort((short) -123), "-123", "-123h");
        check(new K.KShort((short) -32767), "-0W", "-0Wh");
        check(new K.KShort(Short.MAX_VALUE), "0W", "0Wh");
        check(new K.KShort(Short.MIN_VALUE), "0N", "0Nh");

        check(new K.KShortVector((short) -10, (short) 10, (short) 3), "-10 10 3", "-10 10 3h");
        check(new K.KShortVector(), "`short$()", "`short$()");
        check(new K.KShortVector((short) 0), "enlist 0", "enlist 0h");
        check(new K.KShortVector((short) 5, Short.MIN_VALUE, Short.MAX_VALUE, (short) -Short.MAX_VALUE), "5 0N 0W -0W", "5 0N 0W -0Wh");
    }

    @Test
    public void testByteToString() throws Exception {
        check(new K.KByte((byte) 123), "0x7b", "0x7b");
        check(new K.KByte((byte) 0), "0x00", "0x00");
        check(new K.KByte((byte) -1), "0xff", "0xff");
        check(new K.KByte((byte) 127), "0x7f", "0x7f");
        check(new K.KByte((byte) -128), "0x80", "0x80");
        check(new K.KByte((byte) -127), "0x81", "0x81");

        check(new K.KByteVector((byte) -10, (byte) 10, (byte) 3), "0xf60a03", "0xf60a03");
        check(new K.KByteVector(), "`byte$()", "`byte$()");
        check(new K.KByteVector((byte) 0), "enlist 0x00", "enlist 0x00");
        check(new K.KByteVector((byte) 5, (byte) -127, (byte) 128, (byte) 0), "0x05818000", "0x05818000");
    }


    @Test
    public void testDoubleToString() throws Exception {
        check(new K.KDouble(-1.23), "-1.23", "-1.23f");
        check(new K.KDouble(3), "3", "3f");
        check(new K.KDouble(0), "0", "0f");
        check(new K.KDouble(Double.POSITIVE_INFINITY), "0w", "0wf");
        check(new K.KDouble(Double.NEGATIVE_INFINITY), "-0w", "-0wf");
        check(new K.KDouble(Double.NaN), "0n", "0nf");

        check(new K.KDoubleVector((double) -10, (double) 10, (double) 3), "-10 10 3", "-10 10 3f");
        check(new K.KDoubleVector((double) -10, 10.1, (double) 3), "-10 10.1 3", "-10 10.1 3f");
        check(new K.KDoubleVector(), "`float$()", "`float$()");
        check(new K.KDoubleVector((double) 0), "enlist 0", "enlist 0f");
        check(new K.KDoubleVector((double) 5, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN), "5 -0w 0w 0n", "5 -0w 0w 0nf");
    }

    @Test
    public void testFloatMaxFractionDigits() {
        check(new K.KFloat(10.1f), "10.1000004", "10.1000004e");

        KFormatContext.setMaxFractionDigits(5);
        check(new K.KFloat(10.1f), "10.1", "10.1e");
        //restore for other tests
        KFormatContext.setMaxFractionDigits(maxFractionDigits);
    }

    @Test
    public void testFloatToString() throws Exception {
        check(new K.KFloat(-1.23f), "-1.23", "-1.23e");
        check(new K.KFloat(3), "3", "3e");
        check(new K.KFloat(0), "0", "0e");
        check(new K.KFloat(Float.POSITIVE_INFINITY), "0w", "0we");
        check(new K.KFloat(Float.NEGATIVE_INFINITY), "-0w", "-0we");
        check(new K.KFloat(Float.NaN), "0N", "0Ne");

        check(new K.KFloatVector(-10f, 10f, 3f), "-10 10 3", "-10 10 3e");
        check(new K.KFloatVector(-10f, 10.1f, 3f), "-10 10.1000004 3", "-10 10.1000004 3e");
        check(new K.KFloatVector(), "`real$()", "`real$()");
        check(new K.KFloatVector(0f), "enlist 0", "enlist 0e");
        check(new K.KFloatVector(5f, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NaN), "5 -0w 0w 0N", "5 -0w 0w 0Ne");
    }

    @Test
    public void testBooleanToString() throws Exception {
        check(new K.KBoolean(false), "0", "0b");
        check(new K.KBoolean(true), "1", "1b");

        check(new K.KBooleanVector(true, false), "10b", "10b");
        check(new K.KBooleanVector(), "`boolean$()", "`boolean$()");
        check(new K.KBooleanVector(true), "enlist 1b", "enlist 1b");
    }

    @Test
    public void testCharacterToString() throws Exception {
        check(new K.KCharacter(' '), " ", "\" \"");
        check(new K.KCharacter('a'), "a", "\"a\"");

        check(new K.KCharacterVector(" a"), " a", "\" a\"");
        check(new K.KCharacterVector(""), "", "\"\"");
        check(new K.KCharacterVector("a"), "enlist a", "enlist \"a\"");
    }

    @Test
    public void testSymbolToString() throws Exception {
        check(new K.KSymbol(""), "", "`");
        check(new K.KSymbol("a"), "a", "`a");
        check(new K.KSymbol("ab"), "ab", "`ab");
        check(new K.KSymbol(" "), " ", "` ");

        check(new K.KSymbolVector("b", "aa"), "`b`aa", "`b`aa");
        check(new K.KSymbolVector(), "`symbol$()", "`symbol$()");
        check(new K.KSymbolVector("", " ", "ab"), "`` `ab", "`` `ab");
    }

    @Test
    public void testGuidToString() throws Exception {
        check(new K.KGuid(new UUID(12345, -987654)), "00000000-0000-3039-ffff-fffffff0edfa", "00000000-0000-3039-ffff-fffffff0edfa");
        check(new K.KGuid(new UUID(0, 0)), "00000000-0000-0000-0000-000000000000", "00000000-0000-0000-0000-000000000000");

        check(new K.KGuidVector(new UUID(1, -1), new UUID(0, 1), new UUID(-1, 0)), "00000000-0000-0001-ffff-ffffffffffff 00000000-0000-0000-0000-000000000001 ffffffff-ffff-ffff-0000-000000000000", "00000000-0000-0001-ffff-ffffffffffff 00000000-0000-0000-0000-000000000001 ffffffff-ffff-ffff-0000-000000000000");
        check(new K.KGuidVector(), "`guid$()", "`guid$()");
        check(new K.KGuidVector(new UUID(0, 0)), "enlist 00000000-0000-0000-0000-000000000000", "enlist 00000000-0000-0000-0000-000000000000");
    }

    @Test
    public void testTimestampToString() throws Exception {
        check(new K.KTimestamp(-123456789), "1999.12.31D23:59:59.876543211", "1999.12.31D23:59:59.876543211");
        check(new K.KTimestamp(123456), "2000.01.01D00:00:00.000123456", "2000.01.01D00:00:00.000123456");
        check(new K.KTimestamp(-Long.MAX_VALUE), "-0Wp", "-0Wp");
        check(new K.KTimestamp(Long.MAX_VALUE), "0Wp", "0Wp");
        check(new K.KTimestamp(Long.MIN_VALUE), "0Np", "0Np");

        check(new K.KTimestampVector(-10, 10, 3), "1999.12.31D23:59:59.999999990 2000.01.01D00:00:00.000000010 2000.01.01D00:00:00.000000003", "1999.12.31D23:59:59.999999990 2000.01.01D00:00:00.000000010 2000.01.01D00:00:00.000000003");
        check(new K.KTimestampVector(), "`timestamp$()", "`timestamp$()");
        check(new K.KTimestampVector(0), "enlist 2000.01.01D00:00:00.000000000", "enlist 2000.01.01D00:00:00.000000000");
        check(new K.KTimestampVector(5, Long.MIN_VALUE, Long.MAX_VALUE, -Long.MAX_VALUE), "2000.01.01D00:00:00.000000005 0Np 0Wp -0Wp", "2000.01.01D00:00:00.000000005 0Np 0Wp -0Wp");
    }

    @Test
    public void testTimespanToString() throws Exception {
        check(new K.KTimespan(-765432123456789l), "-8D20:37:12.123456789", "-8D20:37:12.123456789");
        check(new K.KTimespan(123456), "0D00:00:00.000123456", "0D00:00:00.000123456");
        check(new K.KTimespan(-Long.MAX_VALUE), "-0Wn", "-0Wn");
        check(new K.KTimespan(Long.MAX_VALUE), "0Wn", "0Wn");
        check(new K.KTimespan(Long.MIN_VALUE), "0Nn", "0Nn");

        check(new K.KTimespanVector(-10, 10, 3), "-0D00:00:00.000000010 0D00:00:00.000000010 0D00:00:00.000000003", "-0D00:00:00.000000010 0D00:00:00.000000010 0D00:00:00.000000003");
        check(new K.KTimespanVector(), "`timespan$()", "`timespan$()");
        check(new K.KTimespanVector(0), "enlist 0D00:00:00.000000000", "enlist 0D00:00:00.000000000");
        check(new K.KTimespanVector(5, Long.MIN_VALUE, Long.MAX_VALUE, -Long.MAX_VALUE), "0D00:00:00.000000005 0Nn 0Wn -0Wn", "0D00:00:00.000000005 0Nn 0Wn -0Wn");
    }

    @Test
    public void testDateToString() throws Exception {
        check(new K.KDate(-1234), "1996.08.15", "1996.08.15");
        check(new K.KDate(123456), "2338.01.05", "2338.01.05");
        check(new K.KDate(-Integer.MAX_VALUE), "-0Wd", "-0Wd");
        check(new K.KDate(Integer.MAX_VALUE), "0Wd", "0Wd");
        check(new K.KDate(Integer.MIN_VALUE), "0Nd", "0Nd");

        check(new K.KDateVector(-10, 10, 3), "1999.12.22 2000.01.11 2000.01.04", "1999.12.22 2000.01.11 2000.01.04");
        check(new K.KDateVector(), "`date$()", "`date$()");
        check(new K.KDateVector(0), "enlist 2000.01.01", "enlist 2000.01.01");
        check(new K.KDateVector(5, Integer.MIN_VALUE, Integer.MAX_VALUE, -Integer.MAX_VALUE), "2000.01.06 0Nd 0Wd -0Wd", "2000.01.06 0Nd 0Wd -0Wd");
    }

    @Test
    public void testTimeToString() throws Exception {
        //@ToDo Fix me
        check(new K.KTime(-1234567890), "-342:56:07.890", "-342:56:07.890");
        check(new K.KTime(323456789), "89:50:56.789", "89:50:56.789");

        check(new K.KTime(-Integer.MAX_VALUE), "-0Wt", "-0Wt");
        check(new K.KTime(Integer.MAX_VALUE), "0Wt", "0Wt");
        check(new K.KTime(Integer.MIN_VALUE), "0Nt", "0Nt");

        check(new K.KTimeVector(-10, 10, 3), "-00:00:00.010 00:00:00.010 00:00:00.003", "-00:00:00.010 00:00:00.010 00:00:00.003");
        check(new K.KTimeVector(), "`time$()", "`time$()");
        check(new K.KTimeVector(0), "enlist 00:00:00.000", "enlist 00:00:00.000");
        check(new K.KTimeVector(5, Integer.MIN_VALUE, Integer.MAX_VALUE, -Integer.MAX_VALUE), "00:00:00.005 0Nt 0Wt -0Wt", "00:00:00.005 0Nt 0Wt -0Wt");
    }

    @Test
    public void testMonthToString() throws Exception {
        check(new K.KMonth(-12345), "0971.04", "0971.04m");
        check(new K.KMonth(123456), "12288.01", "12288.01m");
        check(new K.KMonth(-Integer.MAX_VALUE), "-0W", "-0Wm");
        check(new K.KMonth(Integer.MAX_VALUE), "0W", "0Wm");
        check(new K.KMonth(Integer.MIN_VALUE), "0N", "0Nm");

        check(new K.KMonthVector(-10, 10, 3), "1999.03 2000.11 2000.04", "1999.03 2000.11 2000.04m");
        check(new K.KMonthVector(), "`month$()", "`month$()");
        check(new K.KMonthVector(0), "enlist 2000.01", "enlist 2000.01m");
        check(new K.KMonthVector(5, Integer.MIN_VALUE, Integer.MAX_VALUE, -Integer.MAX_VALUE), "2000.06 0N 0W -0W", "2000.06 0N 0W -0Wm");
    }

    @Test
    public void testMinuteToString() throws Exception {
        //@ToDo Fix me
        check(new K.KMinute(-12345), "-205:45", "-205:45");

        check(new K.KMinute(123456), "2057:36", "2057:36");
        check(new K.KMinute(-Integer.MAX_VALUE), "-0Wu", "-0Wu");
        check(new K.KMinute(Integer.MAX_VALUE), "0Wu", "0Wu");
        check(new K.KMinute(Integer.MIN_VALUE), "0Nu", "0Nu");

        check(new K.KMinuteVector(-10, 10, 3), "-00:10 00:10 00:03", "-00:10 00:10 00:03");
        check(new K.KMinuteVector(), "`minute$()", "`minute$()");
        check(new K.KMinuteVector(0), "enlist 00:00", "enlist 00:00");
        check(new K.KMinuteVector(5, Integer.MIN_VALUE, Integer.MAX_VALUE, -Integer.MAX_VALUE), "00:05 0Nu 0Wu -0Wu", "00:05 0Nu 0Wu -0Wu");
    }

    @Test
    public void testSecondToString() throws Exception {
        //@ToDo Fix me
        check(new K.KSecond(-12345), "-03:25:45", "-03:25:45");

        check(new K.KSecond(123456), "34:17:36", "34:17:36");
        check(new K.KSecond(-Integer.MAX_VALUE), "-0Wv", "-0Wv");
        check(new K.KSecond(Integer.MAX_VALUE), "0Wv", "0Wv");
        check(new K.KSecond(Integer.MIN_VALUE), "0Nv", "0Nv");

        check(new K.KSecondVector(-10, 10, 3), "-00:00:10 00:00:10 00:00:03", "-00:00:10 00:00:10 00:00:03");
        check(new K.KSecondVector(), "`second$()", "`second$()");
        check(new K.KSecondVector(0), "enlist 00:00:00", "enlist 00:00:00");
        check(new K.KSecondVector(5, Integer.MIN_VALUE, Integer.MAX_VALUE, -Integer.MAX_VALUE), "00:00:05 0Nv 0Wv -0Wv", "00:00:05 0Nv 0Wv -0Wv");
    }

    @Test
    public void testDatetimeToString() throws Exception {
        check(new K.KDatetime(-123456.789), "1661.12.26T05:03:50.400", "1661.12.26T05:03:50.400");
        check(new K.KDatetime(123.456), "2000.05.03T10:56:38.400", "2000.05.03T10:56:38.400");
        check(new K.KDatetime(Double.NEGATIVE_INFINITY), "-0wz", "-0wz");
        check(new K.KDatetime(Double.POSITIVE_INFINITY), "0wz", "0wz");
        check(new K.KDatetime(Double.NaN), "0Nz", "0Nz");

        check(new K.KDatetimeVector(-10.0, 10.0, 3.0), "1999.12.22T00:00:00.000 2000.01.11T00:00:00.000 2000.01.04T00:00:00.000", "1999.12.22T00:00:00.000 2000.01.11T00:00:00.000 2000.01.04T00:00:00.000");
        check(new K.KDatetimeVector(), "`datetime$()", "`datetime$()");
        check(new K.KDatetimeVector(0.0), "enlist 2000.01.01T00:00:00.000", "enlist 2000.01.01T00:00:00.000");
        check(new K.KDatetimeVector(5.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN), "2000.01.06T00:00:00.000 -0wz 0wz 0Nz", "2000.01.06T00:00:00.000 -0wz 0wz 0Nz");
    }


    @Test
    public void testListToString() throws Exception {
        check(new K.KList(), "()", "()");
        check(new K.KList(new K.KLong(10), new K.KLong(Long.MAX_VALUE)), "(10;0W)", "(10;0W)");
        check(new K.KList(new K.KLong(10), new K.KInteger(10)), "(10;10)", "(10;10i)");
        check(new K.KList(new K.KLong(10), new K.KInteger(10),
                new K.KList(new K.KDouble(1.1))), "(10;10;enlist 1.1)", "(10;10i;enlist 1.1f)");
    }


    @Test
    public void testOtherToString() throws Exception {
        K.Function funcUnary = new K.Function("{1+x}");
        K.Function funcBinary = new K.Function("{x+y}");

        check(new K.UnaryPrimitive(-1), "", "");

        check(funcUnary, "{1+x}", "{1+x}");
        check(funcBinary, "{x+y}", "{x+y}");

        check(new K.FEachLeft(funcBinary), "{x+y}\\:", "{x+y}\\:");
        check(new K.FEachRight(funcBinary), "{x+y}/:", "{x+y}/:");
        check(new K.Feach(funcBinary), "{x+y}'", "{x+y}'");
        check(new K.Fover(funcBinary), "{x+y}/", "{x+y}/");
        check(new K.Fscan(funcBinary), "{x+y}\\", "{x+y}\\");
        check(new K.FPrior(funcBinary), "{x+y}':", "{x+y}':");

        check(new K.FComposition(funcUnary, funcBinary), "{1+x}{x+y}", "{1+x}{x+y}");
        check(new K.Projection(funcBinary, new K.KLong(1), new K.UnaryPrimitive(-1)), "{x+y}[1;]", "{x+y}[1;]");
        check(new K.Projection(funcBinary, new K.UnaryPrimitive(-1), new K.KLong(1)), "{x+y}[;1]", "{x+y}[;1]");

        check(new K.BinaryPrimitive(15), "~", "~");
        check(new K.UnaryPrimitive(0), "::", "::");
        check(new K.UnaryPrimitive(41), "enlist", "enlist");

        //the output from +1
        check(new K.Projection(new K.BinaryPrimitive(1), new K.KLong(1)), "+[1]", "+[1]");
        //output from '[;]
        check(new K.Projection(new K.TernaryOperator(0), new K.UnaryPrimitive(-1), new K.UnaryPrimitive(-1)), "'[;]", "'[;]");
        //output from +/:[1;]
        check(new K.Projection(new K.FEachRight(new K.BinaryPrimitive(1)), new K.KLong(1), new K.UnaryPrimitive(-1)), "+/:[1;]", "+/:[1;]");
        //output from enlist[1;]
        check(new K.Projection(new K.UnaryPrimitive(41), new K.KLong(1), new K.UnaryPrimitive(-1)), "enlist[1;]", "enlist[1;]");
    }

    @Test
    public void testComposition() {
        K.KBase composition = new K.FComposition(new K.Function("{2+x}"), new K.Function("{x+y}"));
        assertEquals("{2+x}{x+y}", composition.toString());
    }


    @Test
    public void testNull() {
        assertTrue(K.KLong.NULL.isNull());
        assertTrue(K.KTimestamp.NULL.isNull());
        assertTrue(K.KTimespan.NULL.isNull());
    }

    @Test
    public void testAdd() {
        K.KLong l = new K.KLong(5);
        K.KLong l1 = l.add(10);
        assertEquals(15, l1.toLong());
        assertNotSame(l, l1);

        K.KInteger i = new K.KInteger(-10);
        K.KInteger i1 = i.add(1);
        assertEquals(-9, i1.toInt());
        assertNotSame(i, i1);

        K.KTimespan t = new K.KTimespan(1234567);
        K.KTimespan t1 = new K.KTimespan(2345678);
        K.KTimespan t2 = t.add(t1);
        assertEquals(1234567 + 2345678, t2.toLong());
        assertNotSame(t1, t2);
        assertNotSame(t, t2);
    }

    @Test
    public void testTimestampNow() {
        Instant i1 = Instant.parse("2023-12-01T14:18:10.123Z");

        ZoneId z1 = ZoneId.of("Europe/Berlin");
        Clock c1 = Clock.fixed(i1, z1);
        K.KTimestamp k1 = K.KTimestamp.now(c1);
        assertEquals(
                "2023.12.01D15:18:10.123000000",
                k1.toString());


        ZoneId z2 = ZoneId.of("Europe/Moscow");
        Clock c2 = Clock.fixed(i1, z2);
        K.KTimestamp k2 = K.KTimestamp.now(c2);
        assertEquals(
                "2023.12.01D17:18:10.123000000",
                k2.toString());
    }


    @Test
    public void testTimespanPeriod() {
        Instant i1 = Instant.parse("2023-11-30T13:10:05.123Z");
        Instant i2 = Instant.parse("2023-12-01T14:18:10.987Z");
        Clock c1 = Clock.fixed(i1, ZoneId.systemDefault());
        Clock c2 = Clock.fixed(i2, ZoneId.systemDefault());

        K.KTimestamp t1 = K.KTimestamp.now(c1);
        K.KTimestamp t2 = K.KTimestamp.now(c2);
        K.KTimespan k = t1.span(t2);
        assertEquals(
                "1D01:08:05.864000000",
                k.toString());

    }

    @Test
    public void testDateToTimestamp() {
        K.KDate date = new K.KDate(9092);
        assertEquals("2024.11.22", date.toString());

        K.KTimestamp t = new K.KTimestamp(785548800000000000L);
        assertEquals("2024.11.22D00:00:00.000000000", t.toString());

        assertEquals(date.toTimestamp(), t);
    }


    @Test
    public void testElementClass() {
        K.KList list = new K.KList(new K.KInteger(10), K.KInteger.ZERO);
        assertNull(list.getType().getElementType());
    }

    @Test
    public void testLocalDateToDate() {
        LocalDate date = LocalDate.of(2025,5,12);
        assertEquals("2025.05.12", K.KDate.of(date).toString());

        date = LocalDate.of(1978,5,12);
        assertEquals("1978.05.12", K.KDate.of(date).toString());

        date = LocalDate.of(1918,5,12);
        assertEquals("1918.05.12", K.KDate.of(date).toString());

        date = LocalDate.of(2000,1,1);
        assertEquals("2000.01.01", K.KDate.of(date).toString());

    }

    @Test
    public void testLocalDateToMonth() {
        LocalDate date = LocalDate.of(2025,5,12);
        assertEquals("2025.05m", K.KMonth.of(date).toString());

        date = LocalDate.of(1978,5,12);
        assertEquals("1978.05m", K.KMonth.of(date).toString());

        date = LocalDate.of(1918,5,12);
        assertEquals("1918.05m", K.KMonth.of(date).toString());

        date = LocalDate.of(2000,1,1);
        assertEquals("2000.01m", K.KMonth.of(date).toString());

    }

    @Test
    public void testLocalTimeToTime() {
        LocalTime localTime = LocalTime.of(10,11,12);
        assertEquals("10:11:12.000", K.KTime.of(localTime).toString());

        localTime = LocalTime.of(10,11,12, 34_000_000);
        assertEquals("10:11:12.034", K.KTime.of(localTime).toString());

        localTime = LocalTime.of(10,11,12, 34_999_999);
        assertEquals("10:11:12.034", K.KTime.of(localTime).toString());
    }


    @Test
    public void testLocalTimeToSecond() {
        LocalTime localTime = LocalTime.of(10,11,12);
        assertEquals("10:11:12", K.KSecond.of(localTime).toString());

        localTime = LocalTime.of(10,11,12, 34_999_999);
        assertEquals("10:11:12", K.KSecond.of(localTime).toString());
    }

    @Test
    public void testLocalTimeToMinute() {
        LocalTime localTime = LocalTime.of(10,11,52);
        assertEquals("10:11", K.KMinute.of(localTime).toString());

        localTime = LocalTime.of(10,11,12, 34_999_999);
        assertEquals("10:11", K.KMinute.of(localTime).toString());
    }

    @Test
    public void testLocalDateTimeToTimestamp() {
        LocalDateTime localDateTime =
                LocalDateTime.of(2024, 12, 6, 17, 10, 53, 123456789);
        K.KTimestamp k = K.KTimestamp.of(localDateTime);
        assertEquals("2024.12.06D17:10:53.123456789", k.toString());
    }

    @Test
    public void testMonthToLocalDateTime() {
        K.KMonth month = new K.KMonth(299);
        assertEquals("2024.12m", month.toString());

        LocalDateTime expected = LocalDateTime.of(2024, 12, 1, 0, 0);
        LocalDateTime actual = month.toLocalDateTime();
        assertEquals(expected, actual);


        month = new K.KMonth(-1);
        assertEquals("1999.12m", month.toString());

        expected = LocalDateTime.of(1999, 12, 1, 0, 0);
        actual = month.toLocalDateTime();
        assertEquals(expected, actual);

        month = new K.KMonth(-598);
        assertEquals("1950.03m", month.toString());

        expected = LocalDateTime.of(1950, 3, 1, 0, 0);
        actual = month.toLocalDateTime();
        assertEquals(expected, actual);


    }

    @Test
    public void testTimestampAddDuration() {
        LocalDateTime localDateTime =
                LocalDateTime.of(2024, 12, 6, 17, 10, 53, 123456789);
        K.KTimestamp k = K.KTimestamp.of(localDateTime);


        K.KTimestamp k2 = k.add(Duration.ofDays(1)
                .plusHours(1)
                .plusMinutes(3)
                .plusSeconds(2)
                .plusNanos(123456789));

        assertEquals("2024.12.07D18:13:55.246913578", k2.toString());
    }

    @Test
    public void testTimestampAddTimespan() {
        LocalDateTime localDateTime =
                LocalDateTime.of(2024, 12, 6, 17, 10, 53, 123456789);
        K.KTimestamp k1 = K.KTimestamp.of(localDateTime);

        localDateTime =
                LocalDateTime.of(2024, 12, 7, 18, 12, 56, 234567899);
        K.KTimestamp k2 = K.KTimestamp.of(localDateTime);

        K.KTimespan span = k1.span(k2);
        assertEquals("1D01:02:03.111111110", span.toString());

        assertEquals(k2, k1.add(span));
    }

    @Test
    public void testDurationToTimespan() {
        Duration duration = Duration.ofDays(1)
                .plusHours(1)
                .plusMinutes(3)
                .plusSeconds(2)
                .plusNanos(123456789);
        K.KTimespan span = K.KTimespan.of(duration);
        assertEquals("1D01:03:02.123456789", span.toString());
    }

    private void assertFormatting(Duration duration, KFormatContext fmtContext, String expected) {
        K.KTimespan span = K.KTimespan.of(duration);
        long value = span.toLong();
        K.KTimeLong k = new K.KTimeLong(value);
        assertEquals(expected, k.toString(fmtContext));
    }

    @Test
    public void testKTimeLong() {
        Duration duration = Duration.ofDays(1)
                .plusHours(1)
                .plusMinutes(3)
                .plusSeconds(2)
                .plusNanos(123456789);
        assertFormatting(duration, KFormatContext.NO_TYPE, "25:03:02.123456789");
    }


    @Test
    public void testKTimeLongFormatting() {
        Duration duration = Duration.ofDays(1);
        assertFormatting(duration, KFormatContext.MINUTE, "24:00");
        assertFormatting(duration, KFormatContext.SECOND, "24:00:00");
        assertFormatting(duration, KFormatContext.MILLIS, "24:00:00.000");

        duration = duration.plusHours(1)
                .plusMinutes(3)
                .plusSeconds(2);

        assertFormatting(duration, KFormatContext.MINUTE, "25:03:02");
        assertFormatting(duration, KFormatContext.SECOND, "25:03:02");
        assertFormatting(duration, KFormatContext.MILLIS, "25:03:02.000");

        duration = duration.plusNanos(120_000_000);
        assertFormatting(duration, KFormatContext.MINUTE, "25:03:02.120");
        assertFormatting(duration, KFormatContext.SECOND, "25:03:02.120");
        assertFormatting(duration, KFormatContext.MILLIS, "25:03:02.120");

    }
}
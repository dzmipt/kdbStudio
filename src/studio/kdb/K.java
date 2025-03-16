package studio.kdb;

import kx.IPC;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Stream;

public class K {
    private final static DecimalFormat nsFormatter = new DecimalFormat("000000000");
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd");
    private final static SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy.MM.dd'T'HH:mm:ss.SSS");
    private final static SimpleDateFormat timestampFormatter = new SimpleDateFormat("yyyy.MM.dd'D'HH:mm:ss.");
    private static final java.text.DecimalFormat i2Formatter = new java.text.DecimalFormat("00");
    private static final java.text.DecimalFormat i3Formatter = new java.text.DecimalFormat("000");
    private static final java.text.DecimalFormat i4Formatter = new java.text.DecimalFormat("0000");

    private static String i2(int i) {
        return i2Formatter.format(i);
    }
    private static String i3(int i) {
        return i3Formatter.format(i);
    }
    private static String i4(int i) {
        return i4Formatter.format(i);
    }
    private static String l2(long i) {
        return i2Formatter.format(i);
    }

    public final static int JAVA_DAY_OFFSET = 10957;
    public final static long NS_IN_SEC = 1_000_000_000;
    public final static long NS_IN_MLS = 1_000_000;
    public final static long NS_IN_MIN = 60 * NS_IN_SEC;
    public final static long NS_IN_HOUR = 60 * NS_IN_MIN;
    public final static long SEC_IN_DAY = 24 * 60 * 60;
    public final static long MS_IN_DAY = 1000 * SEC_IN_DAY;
    public final static long NS_IN_DAY = NS_IN_SEC * SEC_IN_DAY;
    public final static long NS_IN_MONTH = (long) (NS_IN_DAY*(365*4+1)/(12*4.0));

    static {
        TimeZone gmtTimeZone = java.util.TimeZone.getTimeZone("GMT");
        Stream.of(dateFormatter, dateTimeFormatter, timestampFormatter)
                .forEach(f -> f.setTimeZone(gmtTimeZone));
    }

    private static final String enlist = "enlist ";
    private static final String flip = "flip ";

    public static void write(OutputStream o, byte b) throws IOException {
        o.write(b);
    }

    public static void write(OutputStream o, short h) throws IOException {
        write(o, (byte) (h >> 8));
        write(o, (byte) h);
    }

    public static void write(OutputStream o, int i) throws IOException {
        write(o, (short) (i >> 16));
        write(o, (short) i);
    }

    public static void write(OutputStream o, long j) throws IOException {
        write(o, (int) (j >> 32));
        write(o, (int) j);
    }

    public abstract static class KBase implements Comparable<KBase> {

        private final KType type;

        protected KBase(KType type) {
            this.type = type;
        }

        public KType getType() {
            return type;
        }

        public void serialise(OutputStream o) throws IOException {
            write(o, (byte) type.getType());
            serialiseData(o);
        }

        protected void serialiseData(OutputStream o) throws IOException {
            throw new IllegalStateException("The method is not implemented");
        }

        public boolean isNull() {
            return false;
        }

        public final String toString() {
            return toString(KFormatContext.DEFAULT);
        }

        public final String toString(KFormatContext context) {
            return format(null, context).toString();
        }

        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            if (builder == null) builder = new StringBuilder();
            return builder;
        }

        public int count() {
            return 1;
        }

        @Override
        public boolean equals(Object obj) {
            return obj.getClass().equals(this.getClass());
        }

        @Override
        public int compareTo(KBase o) {
            return toString(KFormatContext.NO_TYPE).compareTo(o.toString(KFormatContext.NO_TYPE));
        }
    }

    private abstract static class KByteBase extends KBase implements ToDouble {
        protected byte value;

        KByteBase(KType type, byte value) {
            super(type);
            this.value = value;
        }
        public double toDouble() {
            return value;
        }

        @Override
        public boolean isPositiveInfinity() {
            return false;
        }

        @Override
        public boolean isNegativeInfinity() {
            return false;
        }

        @Override
        protected void serialiseData(OutputStream o) throws IOException {
            write(o, value);
        }

        @Override
        public int hashCode() {
            return Byte.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (!super.equals(obj)) return false;
            return value == ((KByteBase)obj).value;
        }

        @Override
        public int compareTo(KBase o) {
            if (o instanceof KByteBase)  {
                return Byte.compare(value, ((KByteBase)o).value);
            }
            return super.compareTo(o);
        }
    }

    public abstract static class KIntBase extends KBase implements ToDouble {
        protected final int value;

        KIntBase(KType type, int value) {
            super(type);
            this.value = value;
        }

        public boolean isNull() {
            return value == Integer.MIN_VALUE;
        }

        @Override
        public boolean isPositiveInfinity() {
            return value == Integer.MAX_VALUE;
        }

        @Override
        public boolean isNegativeInfinity() {
            return value == -Integer.MAX_VALUE;
        }

        public double toDouble() {
            return value;
        }

        public int toInt() {
            return value;
        }

        @Override
        public void serialiseData(OutputStream o) throws IOException {
            write(o, value);
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (!super.equals(obj)) return false;
            return value == ((KIntBase)obj).value;
        }

        @Override
        public int compareTo(KBase o) {
            if (o instanceof KIntBase) {
                return Integer.compare(value, ((KIntBase)o).value);
            }
            return super.compareTo(o);
        }
    }

    public abstract static class KLongBase extends KBase implements ToDouble {
        protected final static long NULL_VALUE = Long.MIN_VALUE;

        protected final long value;

        KLongBase(KType type, long value) {
            super(type);
            this.value = value;
        }

        public boolean isNull() {
            return value == NULL_VALUE;
        }

        public double toDouble() {
            return value;
        }

        @Override
        public boolean isPositiveInfinity() {
            return value == Long.MAX_VALUE;
        }

        @Override
        public boolean isNegativeInfinity() {
            return value == -Long.MAX_VALUE;
        }

        public long toLong() {
            return value;
        }
        @Override
        public void serialiseData(OutputStream o) throws IOException {
            write(o, value);
        }
        @Override
        public int hashCode() {
            return Long.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (! obj.getClass().equals(this.getClass())) return false;
            return value == ((KLongBase)obj).value;
        }

        @Override
        public int compareTo(KBase o) {
            if (o instanceof KLongBase) {
                return Long.compare(value, ((KLongBase)o).value);
            }
            return super.compareTo(o);
        }
    }

    private abstract static class KDoubleBase extends KBase implements ToDouble {
        protected double value;

        KDoubleBase(KType type, double value) {
            super(type);
            this.value = value;
        }

        public boolean isNull() {
            return Double.isNaN(value);
        }

        public double toDouble() {
            return value;
        }

        @Override
        public boolean isPositiveInfinity() {
            return value == Double.POSITIVE_INFINITY;
        }

        @Override
        public boolean isNegativeInfinity() {
            return value == Double.NEGATIVE_INFINITY;
        }

        @Override
        public void serialiseData(OutputStream o) throws IOException {
            long j = Double.doubleToLongBits(value);
            write(o, j);
        }

        @Override
        public int hashCode() {
            return Double.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (!super.equals(obj)) return false;
            return Double.doubleToLongBits(value) == Double.doubleToLongBits(((KDoubleBase)obj).value);
        }

        @Override
        public int compareTo(KBase o) {
            if (o instanceof KDoubleBase) {
                return Double.compare(value, ((KDoubleBase)o).value);
            }
            return super.compareTo(o);
        }
    }

    private abstract static class KArrayBase extends KBase {
        protected KBase[] array;

        KArrayBase(KType type, KBase[] array) {
            super(type);
            this.array = array;
        }


        @Override
        public void serialiseData(OutputStream o) throws IOException {
            write(o, array.length);
            for (KBase obj: array) {
                obj.serialise(o);
            }
        }

        @Override
        public int hashCode() {
            return array.length;
        }

        @Override
        public boolean equals(Object obj) {
            if (! super.equals(obj)) return false;
            return Objects.deepEquals(array, ((KArrayBase)obj).array);
        }
    }

    public abstract static class Adverb extends KBase {

        protected K.KBase obj;

        public Adverb(KType type, K.KBase o) {
            super(type);
            this.obj = o;
        }

        public K.KBase getObject() {
            return obj;
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            return super.format(builder, context).append(obj.toString(context));
        }

        @Override
        protected void serialiseData(OutputStream o) throws IOException {
            obj.serialise(o);
        }

        @Override
        public int hashCode() {
            return obj.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!super.equals(obj)) return false;
            return this.obj.equals(((Adverb)obj).obj);
        }
    }

    public static class BinaryPrimitive extends Primitive {
        private final static String[] ops = {":", "+", "-", "*", "%", "&", "|", "^", "=", "<", ">", "$", ",", "#", "_", "~", "!", "?", "@", ".", "0:", "1:", "2:", "in", "within", "like", "bin", "ss", "insert", "wsum", "wavg", "div", "xexp", "setenv", "binr", "cov", "cor"};

        public BinaryPrimitive(int i) {
            super(KType.BinaryPrimitive, ops, i);
        }

    }

    public static class FComposition extends KArrayBase {

        public FComposition(KBase... array) {
            super(KType.Composition, array);
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            builder = super.format(builder, context);
            for (KBase arg: array) {
                arg.format(builder, context);
            }
            return builder;
        }
    }

    public static class FEachLeft extends Adverb {
        public FEachLeft(K.KBase o) {
            super(KType.EachLeft, o);
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            return super.format(builder, context).append("\\:");
        }
    }

    public static class FEachRight extends Adverb {
        public FEachRight(K.KBase o) {
            super(KType.EachRight, o);
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            return super.format(builder, context).append("/:");
        }
    }

    public static class FPrior extends Adverb {
        public FPrior(K.KBase o) {
            super(KType.Prior, o);
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            return super.format(builder, context).append("':");
        }
    }

    public static class Feach extends Adverb {
        public Feach(K.KBase o) {
            super(KType.Each, o);
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            return super.format(builder, context).append("'");
        }
    }

    public static class Fover extends Adverb {
        public Fover(K.KBase o) {
            super(KType.Over, o);
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            return super.format(builder, context).append("/");
        }
    }

    public static class Fscan extends Adverb {
        public Fscan(KBase o) {
            super(KType.Scan, o);
            this.obj = o;
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            return super.format(builder, context).append("\\");
        }
    }

    public static class Function extends KBase {

        private final String body;

        public Function(String body) {
            super(KType.Function);
            this.body = body;
        }

        public Function(KCharacterVector body) {
            this(body.getString());
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            return super.format(builder, context).append(body);
        }

        @Override
        protected void serialiseData(OutputStream o) throws IOException {
            write(o, (byte) 0);
            new KCharacterVector(body).serialise(o);
        }

        @Override
        public int hashCode() {
            return body.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (! (obj instanceof Function)) return false;
            return body.equals(((Function)obj).body);
        }
    }

    public abstract static class Primitive extends KByteBase {

        private String s = " ";

        public Primitive(KType type, String[] ops, int value) {
            super(type, (byte) value);
            if (value >= 0 && value < ops.length)
                s = ops[value];
        }

        public String getPrimitive() {
            return s;
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            builder = super.format(builder, context);
            if (value != -1) builder.append(getPrimitive());
            return builder;
        }

    }

    public static class Projection extends KArrayBase {

        public Projection(KBase... array) {
            super(KType.Projection, array);
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            builder = super.format(builder, context);
            if (array.length == 0) return builder; // not sure if such is possible
            array[0].format(builder, context);
            builder.append("[");
            for (int i = 1; i < array.length; i++) {
                if (i > 1) builder.append(";");
                array[i].format(builder, context);
            }
            builder.append("]");
            return builder;
        }
    }

    public static class TernaryOperator extends Primitive {
        private final static String[] ops = {"'", "/", "\\", "':", "/:", "\\:"};

        public TernaryOperator(int i) {
            super(KType.TernaryOperator, ops, i);
        }
    }

    public static class UnaryPrimitive extends Primitive {
        private static final String[] ops = {"::", "+:", "-:", "*:", "%:", "&:", "|:", "^:", "=:", "<:", ">:", "$:", ",:", "#:", "_:", "~:", "!:", "?:", "@:", ".:", "0::", "1::", "2::", "avg", "last", "sum", "prd", "min", "max", "exit", "getenv", "abs", "sqrt", "log", "exp", "sin", "asin", "cos", "acos", "tan", "atan", "enlist", "var", "dev", "hopen"};

        public UnaryPrimitive(int i) {
            super(KType.UnaryPrimitive, ops, i);
        }

        public boolean isIdentity() {
            return value == 0;
        }
    }

    public static class KBoolean extends KBase implements ToDouble {

        public boolean b;

        public KBoolean(boolean b) {
            super(KType.Boolean);
            this.b = b;
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            return super.format(builder, context)
                        .append(b ? "1" : "0")
                        .append(context.showType() ? "b" : "");
        }

        public double toDouble() {
            return b ? 1.0 : 0.0;
        }

        @Override
        public boolean isPositiveInfinity() {
            return false;
        }

        @Override
        public boolean isNegativeInfinity() {
            return false;
        }

        public boolean toBoolean() {
            return b;
        }

        @Override
        protected void serialiseData(OutputStream o) throws IOException {
            write(o,(byte) (b ? 1:0));
        }

        @Override
        public int hashCode() {
            return Boolean.hashCode(b);
        }

        @Override
        public boolean equals(Object obj) {
            if (! (obj instanceof KBoolean)) return false;
            return b == ((KBoolean)obj).b;
        }

        @Override
        public int compareTo(KBase o) {
            if (o instanceof KBoolean) {
                return Boolean.compare(b, ((KBoolean)o).b);
            }
            return super.compareTo(o);
        }
    }

    public static class KByte extends KByteBase{

        public KByte(byte b) {
            super(KType.Byte, b);
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            return super.format(builder, context)
                        .append("0x")
                        .append(Integer.toHexString((value >> 4) & 0xf))
                        .append(Integer.toHexString(value & 0xf));
        }

    }

    public static class KShort extends KBase implements ToDouble {

        public short s;

        public double toDouble() {
            return s;
        }

        @Override
        public boolean isPositiveInfinity() {
            return s == Short.MAX_VALUE;
        }

        @Override
        public boolean isNegativeInfinity() {
            return s == -Short.MAX_VALUE;
        }

        public KShort(short s) {
            super(KType.Short);
            this.s = s;
        }

        public boolean isNull() {
            return s == Short.MIN_VALUE;
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            builder = super.format(builder, context);
            if (isNull()) builder.append("0N");
            else if (s == Short.MAX_VALUE) builder.append("0W");
            else if (s == -Short.MAX_VALUE) builder.append("-0W");
            else builder.append(context.getNumberFormat().format(s));
            if (context.showType()) builder.append("h");
            return builder;
        }

        @Override
        protected void serialiseData(OutputStream o) throws IOException {
            write(o, s);
        }

        @Override
        public int hashCode() {
            return Short.hashCode(s);
        }

        @Override
        public boolean equals(Object obj) {
            if (! (obj instanceof KShort)) return false;
            return s == ((KShort)obj).s;
        }

        @Override
        public int compareTo(KBase o) {
            if (o instanceof KShort) {
                return Short.compare(s, ((KShort)o).s);
            }
            return super.compareTo(o);
        }
    }

    public static class KInteger extends KIntBase {
        public final static KInteger ZERO = new KInteger(0);

        public KInteger(int i) {
            super(KType.Int, i);
        }

        public KInteger add(int increment) {
            return new KInteger(value + increment);
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            builder = super.format(builder, context);
            if (isNull()) builder.append("0N");
            else if (value == Integer.MAX_VALUE) builder.append("0W");
            else if (value == -Integer.MAX_VALUE) builder.append("-0W");
            else builder.append(context.getNumberFormat().format(value));
            if (context.showType()) builder.append("i");
            return builder;
        }
    }

    public static class KSymbol extends KBase {

        public String s;

        public KSymbol(String s) {
            super(KType.Symbol);
            this.s = s;
        }

        public boolean isNull() {
            return s.isEmpty();
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            builder = super.format(builder, context);
            if (context.showType()) builder.append("`");
            return builder.append(s);
        }

        @Override
        public void serialiseData(OutputStream o) throws IOException {
            o.write(s.getBytes(IPC.ENCODING));
            write(o, (byte) 0);
        }

        @Override
        public int hashCode() {
            return s.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (! (obj instanceof KSymbol)) return false;
            return s.equals(((KSymbol)obj).s);
        }
    }

    public static class KLong extends KLongBase {
        public final static KLong NULL = new KLong(NULL_VALUE);
        public final static KLong ZERO = new KLong(0);

        public KLong(long j) {
            super(KType.Long, j);
        }

        public KLong add(long increment) {
            return new KLong(value + increment);
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            builder = super.format(builder, context);
            if (isNull()) builder.append("0N");
            else if (value == Long.MAX_VALUE) builder.append("0W");
            else if (value == -Long.MAX_VALUE) builder.append("-0W");
            else builder.append(context.getNumberFormat().format(value));
            return builder;
        }
    }

    public static class KCharacter extends KBase {

        public char c;

        public KCharacter(char c) {
            super(KType.Char);
            this.c = c;
        }

        public boolean isNull() {
            return c == ' ';
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            builder = super.format(builder, context);
            if (context.showType()) builder.append("\"").append(c).append("\"");
            else builder.append(c);
            return builder;
        }

        @Override
        public void serialiseData(OutputStream o) throws IOException {
            write(o, (byte) c);
        }

        @Override
        public int hashCode() {
            return Character.hashCode(c);
        }

        @Override
        public boolean equals(Object obj) {
            if (! (obj instanceof KCharacter)) return false;
            return c == ((KCharacter)obj).c;
        }
    }

    public static class KFloat extends KBase implements ToDouble {

        public float f;

        public double toDouble() {
            return f;
        }

        @Override
        public boolean isPositiveInfinity() {
            return f == Float.POSITIVE_INFINITY;
        }

        @Override
        public boolean isNegativeInfinity() {
            return f == Float.NEGATIVE_INFINITY;
        }

        public KFloat(float f) {
            super(KType.Float);
            this.f = f;
        }

        public boolean isNull() {
            return Float.isNaN(f);
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            builder = super.format(builder, context);
            if (isNull()) builder.append("0N");
            else if (f == Float.POSITIVE_INFINITY) builder.append("0w");
            else if (f == Float.NEGATIVE_INFINITY) builder.append("-0w");
            else builder.append(context.getNumberFormat().format(f));
            if (context.showType()) builder.append("e");
            return builder;
        }

        @Override
        public void serialiseData(OutputStream o) throws IOException {
            int i = Float.floatToIntBits(f);
            write(o, i);
        }

        @Override
        public int hashCode() {
            return Float.hashCode(f);
        }

        @Override
        public boolean equals(Object obj) {
            if (! (obj instanceof KFloat)) return false;
            return Float.floatToIntBits(f) == Float.floatToIntBits(((KFloat)obj).f);
        }

        @Override
        public int compareTo(KBase o) {
            if (o instanceof KFloat) {
                return Float.compare(f, ((KFloat)o).f);
            }
            return super.compareTo(o);
        }
    }

    public static class KDouble extends KDoubleBase {

        public KDouble(double d) {
            super(KType.Double, d);
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            builder = super.format(builder, context);
            if (isNull()) builder.append("0n");
            else if (value == Double.POSITIVE_INFINITY) builder.append("0w");
            else if (value == Double.NEGATIVE_INFINITY) builder.append("-0w");
            else builder.append(context.getNumberFormat().format(value));
            if (context.showType()) builder.append("f");
            return builder;
        }

    }

    public static class KDate extends KIntBase {

        public KDate(int date) {
            super(KType.Date, date);
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            builder = super.format(builder, context);
            if (isNull()) builder.append("0Nd");
            else if (value == Integer.MAX_VALUE) builder.append("0Wd");
            else if (value == -Integer.MAX_VALUE) builder.append("-0Wd");
            else builder.append(dateFormatter.format(toDate()));
            return builder;
        }

        public Date toDate() {
            return new Date(MS_IN_DAY * (value + JAVA_DAY_OFFSET));
        }

        public KTimestamp toTimestamp() {
            return new KTimestamp(value * NS_IN_DAY);
        }

        public KTimestamp toTimestamp(double fraction) {
            return new KTimestamp((long) ((value + fraction) * NS_IN_DAY));
        }

    }

    public static class KGuid extends KBase {
        static UUID nuuid = new UUID(0, 0);

        UUID uuid;

        public KGuid(UUID uuid) {
            super(KType.Guid);
            this.uuid = uuid;
        }

        public boolean isNull() {
            return uuid == nuuid;
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            return super.format(builder, context).append(uuid);
        }

        @Override
        protected void serialiseData(OutputStream o) throws IOException {
            write(o, uuid.getMostSignificantBits());
            write(o, uuid.getLeastSignificantBits());

        }

        @Override
        public int hashCode() {
            return uuid.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (! (obj instanceof KGuid)) return false;
            return uuid.equals(((KGuid)obj).uuid);
        }
    }

    public static class KTime extends KIntBase {

        public KTime(int time) {
            super(KType.Time, time);
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            builder = super.format(builder, context);
            if (isNull()) builder.append("0Nt");
            else if (value == Integer.MAX_VALUE) builder.append("0Wt");
            else if (value == -Integer.MAX_VALUE) builder.append("-0Wt");
            else {
                int v = value;
                if (v<0) {
                    builder.append("-");
                    v = -v;
                }
                int ms = v % 1000;
                int s = v / 1000 % 60;
                int m = v / 60000 % 60;
                int h = v / 3600000;
                builder.append(i2(h)).append(":").append(i2(m)).append(":").append(i2(s))
                        .append(".").append(i3(ms));
            }
            return builder;
        }

        public Time toTime() {
            return new Time(value);
        }
    }

    // Artificial class need to represent time in the charts.
    public static class KTimeLong extends KLongBase {
        public KTimeLong(long value) {
            super(KType.TimeLong, value);
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            KFormatContext.Rounding rounding = context.getRounding();

            builder = super.format(builder, context);
            long ns = Math.abs(value % NS_IN_SEC);
            long v = Math.abs(value / NS_IN_SEC);
            long sec = v % 60;
            v = v / 60;
            long min = v % 60;
            long hh = v / 60;
            if (value < 0) builder.append('-');
            builder.append(l2(hh))
                    .append(':').append(l2(min));

            if (rounding.minutes() && sec == 0 && ns == 0) return builder;
            builder.append(':').append(l2(sec));

            if (rounding.seconds() && ns == 0) return builder;

            if (rounding.millis() && ns % NS_IN_MLS == 0) {
                builder.append(".").append(i3Formatter.format(ns/NS_IN_MLS));
            } else {
                builder.append(".").append(nsFormatter.format(ns));
            }
            return builder;
        }
    }

    public static class KDatetime extends KDoubleBase {

        public KDatetime(double time) {
            super(KType.Datetime, time);
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            builder = super.format(builder, context);
            if (isNull()) builder.append("0Nz");
            else if (value == Double.POSITIVE_INFINITY) builder.append("0wz");
            else if (value == Double.NEGATIVE_INFINITY) builder.append("-0wz");
            else builder.append(dateTimeFormatter.format(toTimestamp()));
            return builder;
        }

        public Timestamp toTimestamp() {
            return new Timestamp(Math.round(8.64e7 * (value + JAVA_DAY_OFFSET)));
        }
    }


    public static class KTimestamp extends KLongBase {

        public final static KTimestamp NULL = new KTimestamp(NULL_VALUE);

        private final static Clock systemClock = Clock.systemDefaultZone();
        // 946_684_800_000L is a number of millisecond between 01-Jan-1970 and 01-Jan-2000
        private final static long MILLIS_OFFSET = 946_684_800_000L;

        static KTimestamp now(Clock clock) {
            long epoch = clock.instant().toEpochMilli();
            long offset = TimeZone.getTimeZone(clock.getZone()).getOffset(epoch);
            return new K.KTimestamp( (epoch + offset - MILLIS_OFFSET) * NS_IN_MLS);
        }

        public static KTimestamp of(LocalDateTime dateTime) {
            Instant instant = dateTime.toInstant(ZoneOffset.UTC);
            long ns = instant.getEpochSecond() * NS_IN_SEC + instant.getNano() - MILLIS_OFFSET * NS_IN_MLS;
            return new KTimestamp(ns);
        }

        public static KTimestamp now() {
            return now(systemClock);
        }

        /**
         * Equivalent to t2 - this
         */
        public KTimespan span(KTimestamp t2) {
            return new KTimespan(t2.value - value);
        }

        public KTimestamp add(Duration duration) {
            return new KTimestamp( value + duration.getNano() + NS_IN_SEC * duration.getSeconds() );
        }

        public KTimestamp add(K.KTimespan duration) {
            return new KTimestamp( value + duration.value);
        }

        public KTimestamp(long time) {
            super(KType.Timestamp, time);
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            KFormatContext.Rounding rounding = context.getRounding();

            builder = super.format(builder, context);
            if (isNull()) builder.append("0Np");
            else if (value == Long.MAX_VALUE) builder.append("0Wp");
            else if (value == -Long.MAX_VALUE) builder.append("-0Wp");
            else {
                long v = value % NS_IN_DAY;
                long epochDays = JAVA_DAY_OFFSET + value / NS_IN_DAY;
                if (v < 0) {
                    v += NS_IN_DAY;
                    epochDays--;
                }
                LocalDate localDate = LocalDate.ofEpochDay(epochDays);

                builder.append(i4(localDate.getYear()))
                        .append('.').append(i2(localDate.getMonthValue()))
                        .append('.').append(i2(localDate.getDayOfMonth()));

                if (rounding.days() && v == 0) return builder;

                long hh = v / NS_IN_HOUR;
                v = v % NS_IN_HOUR;
                long mm = v / NS_IN_MIN;
                v = v % NS_IN_MIN;

                builder.append('D').append(l2(hh))
                        .append(':').append(l2(mm));

                if (rounding.minutes() && v == 0) return builder;
                long ss = v / NS_IN_SEC;
                long ns = v % NS_IN_SEC;

                builder.append(':').append(l2(ss));

                if(rounding.seconds() && ns == 0) return builder;

                if (rounding.millis() && ns % NS_IN_MLS == 0) {
                    builder.append('.').append(i3Formatter.format(ns / NS_IN_MLS));
                } else {
                    builder.append('.').append(nsFormatter.format(ns));
                }
            }
            return builder;
        }

        public Timestamp toJavaTimestamp() {
            long k = MS_IN_DAY * JAVA_DAY_OFFSET;
            long n = 1000000000L;
            long d = value < 0 ? (value + 1) / n - 1 : value / n;
            long ltime = value == Long.MIN_VALUE ? value : (k + 1000 * d);
            int nanos = (int) (value - n * d);
            Timestamp ts = new Timestamp(ltime);
            ts.setNanos(nanos);
            return ts;
        }
    }

    public static class Dict extends KBase {

        private byte attr = 0;

        public K.KBase x;
        public K.KBase y;

        public Dict(K.KBase X, K.KBase Y) {
            super(KType.Dict);
            x = X;
            y = Y;
        }

        //@TODO: change to somethign like setSortAttr
        public void setAttr(byte attr) {
            this.attr = attr;
        }

        @Override
        public int count() {
            return x.count();
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            builder = super.format(builder, context);
            boolean useBrackets = attr != 0 || x instanceof Flip || (x.count() == 1);
            if (useBrackets) builder.append("(");
            x.format(builder, context);
            if (useBrackets) builder.append(")");
            builder.append("!");
            y.format(builder, context);
            return builder;
        }

        @Override
        public void serialise(OutputStream o) throws IOException {
            write(o, (byte) (attr == 1 ? 127 : 99));
            x.serialise(o);
            y.serialise(o);
        }

        @Override
        public int hashCode() {
            return x.hashCode() +  137 * y.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (! (obj instanceof Dict)) return false;
            Dict dict = (Dict) obj;
            return x.equals(dict.x) && y.equals(dict.y) && attr == dict.attr;
        }
    }

    abstract private static class FlipBase extends KBase {

        public FlipBase() {
            super(KType.Table);
        }

        abstract public K.KBase getX();
        abstract public K.KBase getY();

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            builder = super.format(builder, context);
            builder.append(flip);
            return new Dict(getX(), getY()).format(builder, context);
        }

        @Override
        protected void serialiseData(OutputStream o) throws IOException {
            write(o, (byte)0);
            new Dict(getX(),getY()).serialise(o);
        }

        @Override
        public int hashCode() {
            return getX().hashCode() + 137 * getY().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (! (obj instanceof FlipBase)) return false;
            FlipBase flip = (FlipBase) obj;
            return getX().equals(flip.getX()) && getY().equals(flip.getY());
        }
    }

    public static class Flip extends FlipBase {
        public K.KSymbolVector x;
        public K.KBaseVector<? extends KBase> y;

        public Flip(K.KSymbolVector names, K.KBaseVector<? extends KBase> cols) {
            super();
            x = names;
            y = cols;
        }

        @Override
        public KBase getX() {
            return x;
        }

        @Override
        public KBase getY() {
            return y;
        }

        @Override
        public int count() {
            return y.at(0).count();
        }
    }

    public static class MappedTable extends FlipBase {
        private final K.Dict dict;

        public MappedTable(K.Dict dict) {
            super();
            this.dict = dict;
        }

        @Override
        public KBase getX() {
            return dict.x;
        }

        @Override
        public KBase getY() {
            return dict.y;
        }
    }

    //@TODO: rename to KMonth
    public static class Month extends KIntBase {

        public Month(int x) {
            super(KType.Month, x);
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            builder = super.format(builder, context);
            if (isNull()) builder.append("0N");
            else if (value == Integer.MAX_VALUE) builder.append("0W");
            else if (value == -Integer.MAX_VALUE) builder.append("-0W");
            else {
                int m = value + 24000, y = m / 12;

                builder.append(i2(y / 100)).append(i2(y % 100))
                        .append(".").append(i2(1 + m % 12));
            }
            if (context.showType()) builder.append("m");
            return builder;
        }

        public Date toDate() {
            int m = value + 24000, y = m / 12;
            m %= 12;
            Calendar cal = Calendar.getInstance();
            cal.set(y, m, 1);
            return cal.getTime();
        }

        public LocalDateTime toLocalDateTime() {
            int m = value + 24000, y = m / 12;
            m %= 12;
            return LocalDateTime.of(y, m+1, 1,0,0);
        }
    }

    //@TODO: rename to Minute
    public static class Minute extends KIntBase {

        public Minute(int x) {
            super(KType.Minute, x);
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            builder = super.format(builder, context);
            if (isNull()) builder.append("0Nu");
            else if (value == Integer.MAX_VALUE) builder.append("0Wu");
            else if (value == -Integer.MAX_VALUE) builder.append("-0Wu");
            else {
                int v = Math.abs(value);
                builder.append(value<0 ? "-" : "")
                        .append(i2(v / 60)).append(":").append(i2(v % 60));
            }
            return builder;
        }
        public Date toDate() {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, value / 60);
            cal.set(Calendar.MINUTE, value % 60);
            return cal.getTime();
        }
    }

    //@TODO: rename to KSecond
    public static class Second extends KIntBase {

        public Second(int x) {
            super(KType.Second, x);
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            builder = super.format(builder, context);
            if (isNull()) builder.append("0Nv");
            else if (value == Integer.MAX_VALUE) builder.append("0Wv");
            else if (value == -Integer.MAX_VALUE) builder.append("-0Wv");
            else {
                int v = Math.abs(value);
                int s = v % 60;
                int m = v / 60 % 60;
                int h = v / 3600;
                builder.append(value<0 ? "-" : "")
                        .append(i2(h)).append(":").append(i2(m)).append(":").append(i2(s));
            }
            return builder;
        }

        public Date toDate() {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, value / (60 * 60));
            cal.set(Calendar.MINUTE,  (value % (60 * 60)) / 60);
            cal.set(Calendar.SECOND, value % 60);
            return cal.getTime();
        }
    }

    public static class KTimespan extends KLongBase {

        public final static KTimespan NULL = new KTimespan(NULL_VALUE);
        public final static KTimespan ZERO = new KTimespan(0);

        final static Map<KType, Long> NS_IN_TYPES = Map.of(
                KType.Timestamp, 1L,
                KType.Timespan, 1L,
                KType.Date, NS_IN_DAY,
                KType.Month, NS_IN_MONTH,
                KType.Minute, NS_IN_MIN,
                KType.Second, NS_IN_SEC,
                KType.Time, NS_IN_MLS,
                KType.Datetime, NS_IN_DAY
        );

        private final static Map<ChronoUnit, Long> NS_IN_UNITS = Map.of(
                ChronoUnit.NANOS, NS_IN_TYPES.get(KType.Timestamp),
                ChronoUnit.MILLIS, NS_IN_TYPES.get(KType.Time),
                ChronoUnit.SECONDS, NS_IN_TYPES.get(KType.Second),
                ChronoUnit.MINUTES, NS_IN_TYPES.get(KType.Minute),
                ChronoUnit.HOURS, 60 * NS_IN_TYPES.get(KType.Minute),
                ChronoUnit.DAYS, NS_IN_TYPES.get(KType.Date),
                ChronoUnit.MONTHS, NS_IN_TYPES.get(KType.Month),
                ChronoUnit.YEARS, 12 * NS_IN_TYPES.get(KType.Month)
        );

        private final static List<ChronoUnit> SUPPORTED_UNiTS = List.of(NS_IN_UNITS.keySet().toArray(new ChronoUnit[0]));

        public static ChronoUnit[] getSupportedUnits() {
            return new ChronoUnit[] {
                    ChronoUnit.NANOS, ChronoUnit.MILLIS, ChronoUnit.SECONDS, ChronoUnit.MINUTES,
                    ChronoUnit.HOURS, ChronoUnit.DAYS, ChronoUnit.MONTHS, ChronoUnit.YEARS };
        }

        public static KTimespan duration(double duration, KType unitType) {
            if (! NS_IN_TYPES.containsKey(unitType))
                throw new IllegalArgumentException(unitType.toString() + " is not supported");

            return new KTimespan(Math.round (duration * NS_IN_TYPES.get(unitType)));
        }

        public double toUnitValue(KType unitType) {
            if (! NS_IN_TYPES.containsKey(unitType))
                throw new IllegalArgumentException(unitType.toString() + " is not supported");

            return value / (double) NS_IN_TYPES.get(unitType);
        }

        public static KTimespan duration(double duration, ChronoUnit unit) {
            if (! NS_IN_UNITS.containsKey(unit))
                throw new IllegalArgumentException("Unit " + unit.toString() + " is not supported");

            return new KTimespan(Math.round (duration * NS_IN_UNITS.get(unit)) );

        }

        public double toUnitValue(ChronoUnit unit) {
            if (! NS_IN_UNITS.containsKey(unit))
                throw new IllegalArgumentException("Unit " + unit.toString() + " is not supported");

            return value / (double) NS_IN_UNITS.get(unit);
        }

        public static KTimespan of(Duration duration) {
            return new KTimespan(duration.getSeconds()*NS_IN_SEC + duration.getNano());
        }

        public KTimespan(long x) {
            super(KType.Timespan, x);
        }

        public KTimespan add(KTimespan increment) {
            return new KTimespan(value + increment.value);
        }

        @Override
        public StringBuilder format(StringBuilder builder, KFormatContext context) {
            builder = super.format(builder, context);
            if (isNull()) builder.append("0Nn");
            else if (value == Long.MAX_VALUE) builder.append("0Wn");
            else if (value == -Long.MAX_VALUE) builder.append("-0Wn");
            else {
                long jj = value;
                if (jj < 0) {
                    jj = -jj;
                    builder.append("-");
                }
                builder.append((int) (jj / NS_IN_DAY)).append("D").
                        append(i2((int) ((jj % NS_IN_DAY) / 3600000000000L)))
                        .append(":").append(i2((int) ((jj % 3600000000000L) / 60000000000L)))
                        .append(":").append(i2((int) ((jj % 60000000000L) / 1000000000L)))
                        .append(".").append(nsFormatter.format((int) (jj % 1000000000L)));
            }
            return builder;
        }

        public Time toTime() {
            return new Time((value / 1000000));
        }

    }

    public static abstract class KBaseVector<E extends KBase> extends KBase {
        protected Object array;
        private final int length;
        private byte attr = 0;

        protected KBaseVector(Object array, KType type) {
            super(type);
            this.array = array;
            this.length = Array.getLength(array);
        }

        public abstract E at(int i);

        public byte getAttr() {
            return attr;
        }

        public void setAttr(byte attr) {
            this.attr = attr;
        }

        @Override
        public int count() {
            return getLength();
        }

        //@TODO: replace with count()
        public int getLength() {
            return length;
        }

        public Object getArray() {
            return array;
        }

        private final static String[] sAttr = new String[]{"", "`s#", "`u#", "`p#", "`g#"};

        //default implementation
        protected StringBuilder formatVector(StringBuilder builder, KFormatContext context) {
            if (getLength() == 0) builder.append("`").append(getType().getName()).append("$()");
            else {
                if (getLength() == 1) builder.append(enlist);
                KFormatContext childContext = context.showType() ? new KFormatContext(context).setShowType(false) : context;
                for (int i = 0; i < getLength(); i++) {
                    if (i > 0) builder.append(" ");
                    at(i).format(builder, childContext);
                }
                if (context.showType()) builder.append(getType().getVectorFormatEnding());
            }
            return builder;
        }

        @Override
        public final StringBuilder format(StringBuilder builder, KFormatContext context) {
            builder = super.format(builder, context);
            if (context.showType() && attr <= sAttr.length) builder.append(sAttr[attr]);
            return formatVector(builder, context);
        }

        @Override
        public void serialiseData(OutputStream o) throws IOException {
            write(o, attr);
            write(o, length);
            for (int index=0; index<length; index++) {
                at(index).serialiseData(o);
            }
        }

        @Override
        public int hashCode() {
            return length * getType().getType();
        }

        @Override
        public boolean equals(Object obj) {
            if (!super.equals(obj)) return false;
            KBaseVector<? extends KBase> vector = (KBaseVector<? extends KBase>)obj;
            return Objects.deepEquals(array, vector.array) && attr == vector.attr;
        }
    }

    public static class KShortVector extends KBaseVector<KShort> {

        public KShortVector(short... array) {
            super(array, KType.ShortVector);
        }

        public KShort at(int i) {
            return new KShort(Array.getShort(array, i));
        }
    }

    public static class KIntVector extends KBaseVector<KInteger> {

        public KIntVector(int... array) {
            super(array, KType.IntVector);
        }

        public KInteger at(int i) {
            return new KInteger(Array.getInt(array, i));
        }
    }

    public static class KList extends KBaseVector<KBase> {

        public KList(KBase... array) {
            super(array, KType.List);
        }

        public KBase at(int i) {
            return (KBase) Array.get(array, i);
        }

        @Override
        protected StringBuilder formatVector(StringBuilder builder, KFormatContext context) {
            if (getLength() == 1) builder.append(enlist);
            else builder.append("(");
            for (int i = 0; i < getLength(); i++) {
                if (i > 0) builder.append(";");
                at(i).format(builder, context);
            }
            if (getLength() != 1) builder.append(")");
            return builder;
        }

        @Override
        public void serialiseData(OutputStream o) throws IOException {
            int length = getLength();
            write(o, (byte) 0);
            write(o, length);
            for (int index=0; index<length; index++) {
                at(index).serialise(o);
            }
        }
    }

    public static class KDoubleVector extends KBaseVector<KDouble> {

        public KDoubleVector(double... array) {
            super(array, KType.DoubleVector);
        }

        public KDouble at(int i) {
            return new KDouble(Array.getDouble(array, i));
        }
    }

    public static class KFloatVector extends KBaseVector<KFloat> {

        public KFloatVector(float... array) {
            super(array, KType.FloatVector);
        }

        public KFloat at(int i) {
            return new KFloat(Array.getFloat(array, i));
    }

    }

    public static class KLongVector extends KBaseVector<KLong> {

        public KLongVector(long... array) {
            super(array, KType.LongVector);
        }

        public KLong at(int i) {
            return new KLong(Array.getLong(array, i));
        }
    }

    public static class KMonthVector extends KBaseVector<Month> {

        public KMonthVector(int... array) {
            super(array, KType.MonthVector);
        }

        public Month at(int i) {
            return new Month(Array.getInt(array, i));
        }
    }

    public static class KDateVector extends KBaseVector<KDate> {

        public KDateVector(int... array) {
            super(array, KType.DateVector);
        }

        public KDate at(int i) {
            return new KDate(Array.getInt(array, i));
        }
    }

    public static class KGuidVector extends KBaseVector<KGuid> {

        public KGuidVector(UUID... array) {
            super(array, KType.GuidVector);
        }

        public KGuid at(int i) {
            return new KGuid((UUID) Array.get(array, i));
        }
    }

    public static class KMinuteVector extends KBaseVector<Minute> {

        public KMinuteVector(int... array) {
            super(array, KType.MinuteVector);
        }

        public Minute at(int i) {
            return new Minute(Array.getInt(array, i));
        }
    }

    public static class KDatetimeVector extends KBaseVector<KDatetime> {

        public KDatetimeVector(double... array) {
            super(array, KType.DatetimeVector);
        }

        public KDatetime at(int i) {
            return new KDatetime(Array.getDouble(array, i));
        }
    }

    public static class KTimestampVector extends KBaseVector<KTimestamp> {

        public KTimestampVector(long... array) {
            super(array, KType.TimestampVector);
        }

        public KTimestamp at(int i) {
            return new KTimestamp(Array.getLong(array, i));
        }
    }

    public static class KTimespanVector extends KBaseVector<KTimespan> {

        public KTimespanVector(long... array) {
            super(array, KType.TimespanVector);
        }

        public KTimespan at(int i) {
            return new KTimespan(Array.getLong(array, i));
        }
    }

    public static class KSecondVector extends KBaseVector<Second> {

        public KSecondVector(int... array) {
            super(array, KType.SecondVector);
        }

        public Second at(int i) {
            return new Second(Array.getInt(array, i));
        }
    }

    public static class KTimeVector extends KBaseVector<KTime> {

        public KTimeVector(int... array) {
            super(array, KType.TimeVector);
        }

        public KTime at(int i) {
            return new KTime(Array.getInt(array, i));
        }
    }

    public static class KBooleanVector extends KBaseVector<KBoolean> {

        public KBooleanVector(boolean... array) {
            super(array, KType.BooleanVector);
        }

        public KBoolean at(int i) {
            return new KBoolean(Array.getBoolean(array, i));
        }

        @Override
        protected StringBuilder formatVector(StringBuilder builder, KFormatContext context) {
            if (getLength() == 0) builder.append("`boolean$()");
            else {
                if (getLength() == 1) builder.append(enlist);
                for (int i = 0; i < getLength(); i++)
                    builder.append(Array.getBoolean(array, i) ? "1" : "0");
                builder.append("b");
            }
            return builder;
        }
    }

    public static class KByteVector extends KBaseVector<KByte> {

        public KByteVector(byte... array) {
            super(array, KType.ByteVector);
        }

        public KByte at(int i) {
            return new KByte(Array.getByte(array, i));
        }

        @Override
        protected StringBuilder formatVector(StringBuilder builder, KFormatContext context) {
            if (getLength() == 0) builder.append("`byte$()");
            else {
                if (getLength() == 1) builder.append(enlist);
                builder.append("0x");
                for (int i = 0; i < getLength(); i++) {
                    byte b = Array.getByte(array, i);
                    builder.append(Integer.toHexString((b >> 4) & 0xf))
                            .append(Integer.toHexString(b & 0xf));
                }
            }
            return builder;
        }
    }

    public static class KSymbolVector extends KBaseVector<KSymbol> {

        public KSymbolVector(String... array) {
            super(array, KType.SymbolVector);
        }

        public KSymbol at(int i) {
            return new KSymbol((String) Array.get(array, i));
        }

        @Override
        protected StringBuilder formatVector(StringBuilder builder, KFormatContext context) {
            if (getLength() == 0) builder.append("`symbol$()");
            else {
                if (getLength() == 1) builder.append(enlist);
                for (int i = 0; i < getLength(); i++)
                    builder.append("`").append(Array.get(array, i));
            }
            return builder;
        }
    }

    public static class KCharacterVector extends KBaseVector<KCharacter> {

        public KCharacterVector(String value) {
            super(value.toCharArray(), KType.CharVector);
        }

        public KCharacter at(int i) {
            return new KCharacter(Array.getChar(array, i));
        }

        public String getString() {
            return new String((char[]) array);
        }

        @Override
        protected StringBuilder formatVector(StringBuilder builder, KFormatContext context) {
            if (getLength() == 1) {
                char ch = Array.getChar(array, 0);
                if (ch<=255) {
                    builder.append(enlist);
                }
            }

            if (context.showType()) builder.append("\"");
            builder.append(getString());
            if (context.showType()) builder.append("\"");
            return builder;
        }

        @Override
        public void serialiseData(OutputStream o) throws IOException {
            write(o, getAttr());
            byte[] b = getString().getBytes(IPC.ENCODING);
            write(o, b.length);
            o.write(b);
        }
    }
}

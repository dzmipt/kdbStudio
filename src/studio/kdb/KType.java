package studio.kdb;

public enum KType {
    Boolean(-1,"boolean", 'b'),
    Guid(-2, "guid", 'g'),
    Byte(-4, "byte", 'x'),
    Short(-5, "short", 'h'),
    Int(-6, "int", 'i'),
    Long(-7, "long", 'j'),
    Float(-8, "real", 'e'),
    Double(-9, "float", 'f'),
    Char(-10, "char", 'c'),
    Symbol(-11, "symbol", 's'),
    Timestamp(-12, "timestamp", 'p'),
    Month(-13, "month", 'm'),
    Date(-14, "date", 'd'),
    Datetime(-15, "datetime", 'z'),
    Timespan(-16, "timespan", 'n'),
    Minute(-17, "minute", 'u'),
    Second(-18, "second", 'v'),
    Time(-19, "time", 't'),

    TimeLong(-19000,"timeLong", 'l'),

    List(0),
    BooleanVector(Boolean, true),
    GuidVector(Guid),
    ByteVector(Byte, true),
    ShortVector(Short, true),
    IntVector(Int, true),
    LongVector(Long),
    FloatVector(Float, true),
    DoubleVector(Double, true),
    CharVector(Char),
    SymbolVector(Symbol),
    TimestampVector(Timestamp),
    MonthVector(Month, true),
    DateVector(Date),
    DatetimeVector(Datetime),
    TimespanVector(Timespan),
    MinuteVector(Minute),
    SecondVector(Second),
    TimeVector(Time),

    Table(98),
    Dict(99),
    Function(100),
    UnaryPrimitive(101),
    BinaryPrimitive(102),
    TernaryOperator(103),
    Projection(104),
    Composition(105),
    Each(106),
    Over(107),
    Scan(108),
    Prior(109),
    EachRight(110),
    EachLeft(111),

    ;

    private final int type;
    private final String name;
    private final char typeChar;
    private final KType elementType;
    private final boolean requireFormatEnding;

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public char getTypeChar() {
        return typeChar;
    }

    public String getVectorFormatEnding() {
        if (!requireFormatEnding) return "";
        return "" + typeChar;
    }

    public boolean isVector() {
        return elementType != null;
    }

    public KType getElementType() {
        return elementType;
    }

    KType(int type) {
        this(type, "", ' ');
    }

    KType(int type, String name, char typeChar) {
        this(type, name, typeChar, null, false);
    }

    KType(int type, String name, char typeChar, KType elementType, boolean requireFormatEnding) {
        this.type = type;
        this.elementType = elementType;
        this.name = name;
        this.typeChar = typeChar;
        this.requireFormatEnding = requireFormatEnding;
    }

    KType(KType elementType) {
        this(elementType, false);
    }
    KType(KType elementType, boolean requireFormatEnding) {
        this(-elementType.type, elementType.name, elementType.typeChar, elementType, requireFormatEnding);
    }

}

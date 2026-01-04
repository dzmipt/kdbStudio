package studio.kdb;

import studio.kdb.config.ColorToken;

public enum KType {
    Boolean(-1,"boolean", 'b', ColorToken.BOOLEAN),
    Guid(-2, "guid", 'g', ColorToken.IDENTIFIER),
    Byte(-4, "byte", 'x', ColorToken.BYTE),
    Short(-5, "short", 'h', ColorToken.SHORT),
    Int(-6, "int", 'i', ColorToken.INTEGER),
    Long(-7, "long", 'j', ColorToken.LONG),
    Float(-8, "real", 'e', ColorToken.REAL),
    Double(-9, "float", 'f', ColorToken.FLOAT),
    Char(-10, "char", 'c', ColorToken.CHARVECTOR),
    Symbol(-11, "symbol", 's', ColorToken.SYMBOL),
    Timestamp(-12, "timestamp", 'p', ColorToken.TIMESTAMP),
    Month(-13, "month", 'm', ColorToken.MONTH),
    Date(-14, "date", 'd', ColorToken.DATE),
    Datetime(-15, "datetime", 'z', ColorToken.DATETIME),
    Timespan(-16, "timespan", 'n', ColorToken.TIMESPAN),
    Minute(-17, "minute", 'u', ColorToken.MINUTE),
    Second(-18, "second", 'v', ColorToken.SECOND),
    Time(-19, "time", 't', ColorToken.TIME),

    TimeLong(-19000,"timeLong", 'l', ColorToken.TIMESTAMP),

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
    private final ColorToken colorToken;

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public char getTypeChar() {
        return typeChar;
    }

    public ColorToken getColorToken() {
        return colorToken;
    }

    public String getVectorFormatEnding() {
        if (!requireFormatEnding) return "";
        return "" + typeChar;
    }

    public boolean isVector() {
        return type >= List.type && type <= TimeVector.type;
    }

    public KType getElementType() {
        return elementType;
    }

    KType(int type) {
        this(type, "", ' ', ColorToken.DEFAULT);
    }

    KType(int type, String name, char typeChar, ColorToken colorToken) {
        this(type, name, typeChar, null, false, colorToken);
    }

    KType(int type, String name, char typeChar, KType elementType, boolean requireFormatEnding, ColorToken colorToken) {
        this.type = type;
        this.elementType = elementType;
        this.name = name;
        this.typeChar = typeChar;
        this.requireFormatEnding = requireFormatEnding;
        this.colorToken = colorToken;
    }

    KType(KType elementType) {
        this(elementType, false);
    }
    KType(KType elementType, boolean requireFormatEnding) {
        this(-elementType.type, elementType.name,
                elementType.typeChar, elementType, requireFormatEnding, elementType.colorToken);
    }

}

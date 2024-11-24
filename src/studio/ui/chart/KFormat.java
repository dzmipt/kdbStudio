package studio.ui.chart;

import studio.kdb.K;
import studio.kdb.KFormatContext;
import studio.kdb.KType;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

public class KFormat extends NumberFormat {

    private KType type;

    private static final DecimalFormat fractionFormat = new DecimalFormat("+0.#####;-0.#####");

    public KFormat(KType type) {
        this.type = type;
    }

    @Override
    public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
        return format((double)number, toAppendTo, pos);
    }

    @Override
    public Number parse(String source, ParsePosition parsePosition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StringBuffer format(double value, StringBuffer toAppendTo, FieldPosition pos) {
        boolean addFraction = true;
        K.KBase kValue;
        if (type == KType.Int ||
                type == KType.Double ||
                type == KType.Float ||
                type == KType.Short ||
                type == KType.Long) {
            kValue = new K.KDouble(value);
            addFraction = false;
        } else if (type == KType.Datetime) {
            kValue = new K.KDatetime(value);
            addFraction = false;
        } else if (type == KType.Date) {
            kValue = new K.KDate(getInt(value));
        } else if (type == KType.Time) {
            kValue = new K.KTime(getInt(value));
        } else if (type == KType.Timestamp) {
            kValue = new K.KTimestamp(getLong(value));
        } else if (type == KType.Timespan) {
            kValue = new K.KTimespan(getLong(value));
        } else if (type == KType.Month) {
            kValue = new K.Month(getInt(value));
        } else if (type == KType.Second) {
            kValue = new K.Second(getInt(value));
        } else if (type == KType.Minute) {
            kValue = new K.Minute(getInt(value));
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }

        toAppendTo.append(kValue.toString(KFormatContext.NO_TYPE));

        if (addFraction) {
            double fraction = value - Math.floor(value);
            if (Math.abs(fraction) >= 1e-5) {
                if (value < 0) {
                    fraction = fraction - 1;
                }
                toAppendTo.append(fractionFormat.format(fraction));
            }
        }

        return toAppendTo;
    }

    private static long getLong(double value) {
        return (long) value;
    }

    private static int getInt(double value) {
        if (value >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (value <= -Integer.MAX_VALUE) return -Integer.MAX_VALUE;
        return (int) value;
    }

}

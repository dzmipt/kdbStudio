package studio.ui.chart;

import studio.kdb.K;
import studio.kdb.KFormatContext;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

public class KFormat extends NumberFormat {

    private Class kClass;

    private static final DecimalFormat fractionFormat = new DecimalFormat("+0.#####;-0.#####");

    public KFormat(Class kClass) {
        this.kClass = kClass;
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
        if (kClass == K.KInteger.class ||
                kClass == K.KDouble.class ||
                kClass == K.KFloat.class ||
                kClass == K.KShort.class ||
                kClass == K.KLong.class) {
            kValue = new K.KDouble(value);
            addFraction = false;
        } else if (kClass == K.KDatetime.class) {
            kValue = new K.KDatetime(value);
            addFraction = false;
        } else if (kClass == K.KDate.class) {
            kValue = new K.KDate(getInt(value));
        } else if (kClass == K.KTime.class) {
            kValue = new K.KTime(getInt(value));
        } else if (kClass == K.KTimestamp.class) {
            kValue = new K.KTimestamp(getLong(value));
        } else if (kClass == K.KTimespan.class) {
            kValue = new K.KTimespan(getLong(value));
        } else if (kClass == K.Month.class) {
            kValue = new K.Month(getInt(value));
        } else if (kClass == K.Second.class) {
            kValue = new K.Second(getInt(value));
        } else if (kClass == K.Minute.class) {
            kValue = new K.Minute(getInt(value));
        } else {
            throw new IllegalArgumentException("Unsupported class: " + kClass);
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

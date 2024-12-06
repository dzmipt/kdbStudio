package studio.kdb;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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
        K.KBase kValue;
        if (type == KType.Int ||
                type == KType.Double ||
                type == KType.Float ||
                type == KType.Short ||
                type == KType.Long) {
            kValue = new K.KDouble(value);
        } else if (type == KType.Datetime) {
            kValue = new K.KDatetime(value);
        } else if (type == KType.Timestamp) {
            kValue = new K.KTimestamp(getLong(value));
        } else if (type == KType.Timespan) {
            kValue = new K.KTimespan(getLong(value));
        } else {
            int intValue = getInt(value);
            double fraction = value - intValue;
            boolean hasFraction = Math.abs(fraction) >= 1e-5;

            if (type == KType.Date) {
                kValue = new K.KDate(intValue);
                if (hasFraction) {
                    kValue = ((K.KDate)kValue).toTimestamp(fraction);
                }
            } else if (type == KType.Month) {
                kValue = new K.Month(intValue);
                if (hasFraction) {
                    LocalDateTime dateTime = ((K.Month)kValue).toLocalDateTime();

                    LocalDateTime next = dateTime.plus((int)Math.signum(fraction), ChronoUnit.MONTHS);
                    long days = Duration.between(dateTime, next).toDays();
                    long ns = (long) (Math.abs(fraction) * days * K.NS_IN_DAY);
                    kValue = K.KTimestamp.of(dateTime).add(new K.KTimespan(ns));
                }

            } else if (type == KType.Time) {
                if (hasFraction) {
                    kValue = new K.KTimeLong((long) (value * K.NS_IN_MLS));
                } else {
                    kValue = new K.KTime(intValue);
                }
                System.out.printf("KFormat KTime: value: %f, hasFraction: %b; kValue: %s\n", value, hasFraction, kValue);
            } else if (type == KType.Second) {
                if (hasFraction) {
                    kValue = new K.KTimeLong((long) (value * K.NS_IN_SEC));
                } else {
                    kValue = new K.Second(intValue);
                }
            } else if (type == KType.Minute) {
                if (hasFraction) {
                    kValue = new K.KTimeLong((long) (value * K.NS_IN_MIN));
                } else {
                    kValue = new K.Minute(intValue);
                }
            } else {
                throw new IllegalArgumentException("Unsupported type: " + type);
            }
        }

        toAppendTo.append(kValue.toString(KFormatContext.NO_TYPE));

        return toAppendTo;
    }

    private static long getLong(double value) {
        return Math.round(value);
    }

    private static int getInt(double value) {
        long longValue = getLong(value);
        if (longValue >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (longValue <= -Integer.MAX_VALUE) return -Integer.MAX_VALUE;
        return (int) longValue;
    }

    public static double getValue(KType unit, double value) {
        return value / K.KTimespan.NS_IN_TYPES.get(unit);
    }

    public static double getValue(KType unit, long value) {
        return value / (double) K.KTimespan.NS_IN_TYPES.get(unit);
    }

}

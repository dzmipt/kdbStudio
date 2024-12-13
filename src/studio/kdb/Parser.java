package studio.kdb;

import java.text.ParseException;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Parser {

    public static double parse(KType type, String text) {
        if (type == KType.Date || type == KType.Datetime ||
            type == KType.Timestamp || type == KType.Month
        ) {
            LocalDateTime dateTime = parseLocalDateTime(text);
            if (dateTime == null) return Double.NaN;

            long value = K.KTimestamp.of(dateTime).toLong();

            if (type == KType.Timestamp) return value;
            if (type == KType.Datetime || type == KType.Date) return value / (double) K.NS_IN_DAY;

            LocalDateTime firstDay = LocalDateTime.of(dateTime.getYear(), dateTime.getMonth(), 1, 0,0,0);
            LocalDateTime nextMonth = firstDay.plusMonths(1);

            long nsFirst = K.KTimestamp.of(firstDay).toLong();
            long nsNext = K.KTimestamp.of(nextMonth).toLong();

            double fraction = (value - nsFirst) / (double) (nsNext - nsFirst);
            int intValue = (dateTime.getYear()-2000)*12 + dateTime.getMonthValue() -1;

            return intValue + fraction;
        } else if (type == KType.Time || type == KType.Minute ||
            type == KType.Second || type == KType.Timespan) {
            Duration duration = parseDuration(text);
            if (duration == null) return Double.NaN;

            long value = K.KTimespan.of(duration).toLong();

            if (type == KType.Timespan) return value;
            if (type == KType.Time) return value / (double) K.NS_IN_MLS;
            if (type == KType.Second) return value / (double) K.NS_IN_SEC;

            return value / (double) K.NS_IN_MIN;
        } else throw new IllegalArgumentException("Type: " + type + " is not supported");
    }


    enum Field {YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, NANOSECOND, SIGN, NULL}

    static class Rule {
        private final List<TokenParser> tokenParsers = new ArrayList<>();
        private final List<Field> fields = new ArrayList<>();
        private final List<Boolean> trailings = new ArrayList<>();
        Rule addChars(String chars) {
            return add(token(chars), Field.NULL);
        }
        Rule addOptionalChars(String chars) {
            return add(token(chars, true), Field.NULL);
        }
        Rule add(TokenParser tokenParser, Field field) {
            return addInternal(tokenParser, field, false);
        }
        Rule addTrailing(TokenParser tokenParser, Field field) {
            return addInternal(tokenParser, field, true);
        }
        private Rule addInternal(TokenParser tokenParser, Field field, boolean trailing) {
            tokenParsers.add(tokenParser);
            fields.add(field);
            trailings.add(trailing);
            return this;
        }
        Map<Field, Integer> parse(String line) throws ParseException {
            Map<Field, Integer> res = new HashMap<>();
            StringParser stringParser = new StringParser(line);
            int count = tokenParsers.size();
            for (int index=0; index<count; index++) {
                int value = tokenParsers.get(index).parse(stringParser);
                Field field = fields.get(index);
                if (field != Field.NULL) res.put(field, value);
                if (stringParser.eol() && trailings.get(index)) break;
            }
            if (! stringParser.eol()) throw stringParser.exception("Expected end of line");
            return res;
        }
    }

    interface TokenParser {
        int parse(StringParser parser) throws ParseException;
    }

    private final static TokenParser tokenYear = tokenNumber(1000,9999);
    private final static TokenParser tokenMonth = tokenNumber(1,12);
    private final static TokenParser tokenDay = tokenNumber(1,31);
    private final static TokenParser tokenHour = tokenNumber(0,23);
    private final static TokenParser tokenMinute = tokenNumber(0,59);
    private final static TokenParser tokenSecond = tokenNumber(0,59);

    private final static TokenParser tokenNanosecond = parser -> {
        String res = parser.readDigits(1,9);
        if (res == null) throw parser.exception("Expecting number");
        return Integer.parseInt(res) * (int) Math.pow(10, 9 - res.length());
    };

    private final static TokenParser number = parser -> {
        String res = parser.readDigits();
        if (res == null) throw parser.exception("Expecting number");
        return Integer.parseInt(res);
    };

    private static TokenParser tokenNumber(long min, long max) {
        final int minCount = (""+min).length();
        final int maxCount = (""+max).length();
        return parser -> {
            String res = parser.readDigits(minCount, maxCount);
            if (res == null) throw parser.exception("Expecting number");
            int value = Integer.parseInt(res);
            if (value>max || value<min) throw parser.exception("Expecting number between " + min + " and " + max);
            return value;
        };
    }

    private final static TokenParser sign = parser -> {
        String res = parser.readOneChar("+-");
        if (res == null || res.equals("+")) return 1;
        return -1;
    };

    private static TokenParser token(String chars) {
        return token(chars, false);
    }

    private static TokenParser token(String chars, boolean optional) {
        return parser -> {
            String res = parser.readOneChar(chars);
            if (res == null && !optional) throw parser.exception("Expected one of the following symbols: '" + chars + "'");
            return 0;
        };
    }

    private final static List<Rule> localDateTimeRules = new ArrayList<>();
    static {
        localDateTimeRules.add(
                new Rule().add(tokenYear, Field.YEAR).addChars(".")
                        .add(tokenMonth, Field.MONTH).addChars(".")
                        .addTrailing(tokenDay, Field.DAY).addChars("DTdt ")
                        .addTrailing(tokenHour, Field.HOUR).addChars(":")
                        .addTrailing(tokenMinute, Field.MINUTE).addChars(":")
                        .addTrailing(tokenSecond, Field.SECOND).addChars(".")
                        .add(tokenNanosecond, Field.NANOSECOND)
                );
        localDateTimeRules.add(
                new Rule().add(tokenYear, Field.YEAR).addChars(".")
                        .add(tokenMonth, Field.MONTH).addOptionalChars("Mm")
        );
    }

    public static LocalDateTime parseLocalDateTime(String line) {
        for (Rule rule: localDateTimeRules) {
            try {
                Map<Field, Integer> fields = rule.parse(line);

                return LocalDateTime.of(fields.get(Field.YEAR), fields.get(Field.MONTH),
                                        fields.getOrDefault(Field.DAY, 1),
                        fields.getOrDefault(Field.HOUR,0),
                        fields.getOrDefault(Field.MINUTE,0),
                        fields.getOrDefault(Field.SECOND,0),
                        fields.getOrDefault(Field.NANOSECOND,0)
                        );
            } catch (ParseException| DateTimeException ignored) {}
        }
        return null;
    }


    private final static List<Rule> durationRules = new ArrayList<>();
    static {
        durationRules.add(
                new Rule().add(sign, Field.SIGN)
                        .add(number, Field.DAY).addChars("DTdt ")
                        .addTrailing(tokenHour, Field.HOUR).addChars(":")
                        .addTrailing(tokenMinute, Field.MINUTE).addChars(":")
                        .addTrailing(tokenSecond, Field.SECOND).addChars(".")
                        .add(tokenNanosecond, Field.NANOSECOND)
        );
        durationRules.add(
                new Rule().add(sign, Field.SIGN)
                        .add(number, Field.DAY).addChars("DTdt")
        );

        durationRules.add(
                new Rule().add(sign, Field.SIGN)
                        .add(number, Field.HOUR).addChars(":")
                        .addTrailing(tokenMinute, Field.MINUTE).addChars(":")
                        .addTrailing(tokenSecond, Field.SECOND).addChars(".")
                        .add(tokenNanosecond, Field.NANOSECOND)
        );
    }

    public static Duration parseDuration(String line) {
        for (Rule rule: durationRules) {
            try {
                Map<Field, Integer> fields = rule.parse(line);

                Duration durataion = Duration.ZERO
                        .plusDays(fields.getOrDefault(Field.DAY,0))
                        .plusHours(fields.getOrDefault(Field.HOUR,0))
                        .plusMinutes(fields.getOrDefault(Field.MINUTE, 0))
                        .plusSeconds(fields.getOrDefault(Field.SECOND,0))
                        .plusNanos(fields.getOrDefault(Field.NANOSECOND, 0));
                if (fields.get(Field.SIGN) == -1) durataion = durataion.negated();
                return durataion;
            } catch (ParseException ignored) {}
        }
        return null;
    }

    static class StringParser {
        private final String line;
        private int pos = 0;
        StringParser(String line) {
            this.line = line;
        }
        boolean eol() {
            return pos==line.length();
        }
        ParseException exception(String text)  {
            return new ParseException(text, pos);
        }
        String readOneChar(String possible) {
            if (eol()) return null;
            char ch = line.charAt(pos);
            if (possible.indexOf(ch)==-1) return null;
            pos++;
            return ""+ch;
        }
        String readDigits(int min, int max) {
            int p = pos;
            while (!eol() && pos-p<max && Character.isDigit(line.charAt(pos))) pos++;

            int count = pos-p;
            if (count>=min && count<=max) return line.substring(p,pos);
            pos = p;
            return null;
        }
        String readDigits() {
            return readDigits(1, Integer.MAX_VALUE);
        }
    }
}

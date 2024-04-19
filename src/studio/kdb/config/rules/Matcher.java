package studio.kdb.config.rules;

import java.util.regex.Pattern;

public interface Matcher {
    boolean match(String value);
    String getPattern();

    abstract class AbstractMatcher implements Matcher {
        private final String rawPattern;
        AbstractMatcher(String rawPattern) {
            this.rawPattern = rawPattern;
        }

        @Override
        public String getPattern() {
            return rawPattern;
        }
    }

    class Regex extends AbstractMatcher {
        private final Pattern pattern;

        public Regex(String pattern) {
            super(pattern);
            this.pattern = Pattern.compile(pattern);
        }

        @Override
        public boolean match(String value) {
            return pattern.matcher(value).matches();
        }
    }

    class Int extends AbstractMatcher {
        private final int low, high;

        private static final Pattern SINGLE = Pattern.compile("\\s*([0-9]+)\\s*");
        private static final Pattern RANGE = Pattern.compile("\\s*([0-9]+)\\s*-([0-9]+)\\s*");

        public Int(String pattern) {
            super(pattern);
            java.util.regex.Matcher matcher = SINGLE.matcher(pattern);
            if (matcher.matches() ) {
                low = high = Integer.parseInt(matcher.group(1));
                return;
            }

            matcher = RANGE.matcher(pattern);
            if (matcher.matches() ) {
                low = Integer.parseInt(matcher.group(1));
                high = Integer.parseInt(matcher.group(2));
                return;
            }

            throw new IllegalArgumentException("Can't recognise the pattern");
        }


        @Override
        public boolean match(String value) {
            try {
                int iValue = Integer.parseInt(value);
                return iValue>=low && iValue<=high;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

}

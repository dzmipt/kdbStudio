package studio.kdb.config.server;

import studio.kdb.config.TLSResolutionMode;

import java.util.Objects;
import java.util.regex.Pattern;

public abstract class Operation<E> {

    public enum Names {
        equals ("equals"),
        contains ("contains"),
        likes("like"),
        bigger(">"),
        smaller("<");

        private final String text;
        Names(String text) {
            this.text = text;
        }


        @Override
        public String toString() {
            return text;
        }
    }

    private final Names name;
    protected volatile E arg;

    public static Operation<String> newEquals(String template) {
        return new Equals<>(template);
    }

    public static Operation<String> newContains(String template) {
        return new Contains(template);
    }

    public static Operation<String> newLikes(String template) {
        return new Likes(template);
    }

    public static Operation<Integer> newEquals(int template) {
        return new Equals<>(template);
    }

    public static Operation<Integer> newBigger(int template) {
        return new Bigger(template);
    }

    public static Operation<Integer> newLess(int template) {
        return new Smaller(template);
    }

    public static Operation<TLSResolutionMode> newEquals(TLSResolutionMode template) {
        return new Equals<>(template);
    }


    private Operation(Names name, E arg) {
        this.name = name;
        setArg(arg);
    }

    public abstract boolean test(E serverValue);

    public Names getName() {
        return name;
    }

    @Override
    public String toString() {
        return name.toString();
    }

    public E getArg() {
        return arg;
    }

    public void setArg(E arg) {
        this.arg = arg;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Operation)) return false;
        Operation<?> operation = (Operation<?>) o;
        return name == operation.name && Objects.equals(arg, operation.arg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, arg);
    }

    private static class Equals<E> extends Operation<E> {

        Equals(E template) {
            super(Names.equals, template);
        }

        @Override
        public boolean test(E serverValue) {
            if (arg instanceof String) {
                return ((String)arg).trim().equals(serverValue);
            }
            return arg.equals(serverValue);
        }
    }

    private static class Contains extends Operation<String> {

        Contains(String template) {
            super(Names.contains, template);
        }

        @Override
        public boolean test(String serverValue) {
            return serverValue.contains(arg.trim());
        }
    }

    private static class Likes extends Operation<String> {

        private Pattern pattern;
        Likes(String template) {
            super(Names.likes, template);
        }

        @Override
        public boolean test(String serverValue) {
            return pattern.matcher(serverValue).matches();
        }

        @Override
        public void setArg(String arg) {
            this.pattern = Pattern.compile(arg);
            super.setArg(arg);
        }
    }

    private static class Bigger extends Operation<Integer> {

        Bigger(Integer template) {
            super(Names.bigger, template);
        }

        @Override
        public boolean test(Integer serverValue) {
            return serverValue > arg;
        }
    }

    private static class Smaller extends Operation<Integer> {

        Smaller(Integer template) {
            super(Names.smaller, template);
        }

        @Override
        public boolean test(Integer serverValue) {
            return serverValue < arg;
        }
    }

}

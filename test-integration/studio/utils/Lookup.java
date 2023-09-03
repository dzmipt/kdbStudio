package studio.utils;

import org.assertj.swing.edt.GuiActionRunner;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class Lookup {

    public static <T extends Component> T getParent(Component component, Class<T> clazz) {
        return getParent(component, clazz, identical());
    }

    public static <T extends Component> T getParent(Component component, Class<T> clazz, Matcher<T> matcher) {
        return GuiActionRunner.execute(() -> {
            Component current = component;
            while (current != null) {
                if (clazz.isInstance(current)) {
                    T t = clazz.cast(current);
                    if (matcher.match(t)) return t;
                }
                current = current.getParent();
            }
            return null;
        });
    }

    public static boolean containsChild(Container parent, Component child) {
        return 0 < getChildren(parent, Component.class, component -> component == child).size();
    }

    public static <T extends Component> T getChild(Component component, Class<T> clazz, Matcher<T> matcher) {
        List<T> children = getChildren(component, clazz, matcher);
        assertEquals("Expect to find only one child", 1, children.size());
        return children.get(0);
    }

    public static <T extends Component> T getChild(Component component, Class<T> clazz) {
        return getChild(component, clazz, identical());
    }

    public static <T extends Component> List<T> getChildren(Component component, Class<T> clazz) {
        return getChildren(component, clazz, identical());
    }

    public static <T extends Component> List<T> getChildren(Component component, Class<T> clazz, Matcher<T> matcher) {
        return GuiActionRunner.execute(() -> {
            List<T> result = new ArrayList<>();
            List<Component> iteration = new ArrayList<>();
            iteration.add(component);
            while (iteration.size() >0 ) {
                List<Component> nextIteration = new ArrayList<>();
                for (Component current : iteration) {
                    if (clazz.isInstance(current)) {
                        T t = clazz.cast(current);
                        if (matcher.match(t)) result.add(t);
                    }
                    if (current instanceof Container) {
                        nextIteration.addAll(Arrays.asList(((Container)current).getComponents()));
                    }
                }
                iteration = nextIteration;
            }
            return result;
        });
    }

    public static <T extends Component> Matcher<T> byType(Class<T> clazz) {
        return clazz::isInstance;
    }

    public static <T extends AbstractButton> Matcher<T> byButtonText(String text) {
        return button -> text.equals(button.getText());
    }

    public static <T extends Component> Matcher<T> byName(String name) {
        return component -> name.equals(component.getName());
    }

    public static <T extends Component> Matcher<T> identical() {
        return component -> true;
    }

    public interface Matcher<T extends Component> {
        boolean match(T component);

        default Matcher<T> or(Matcher<T>... matchers) {
            return component -> {
                if (Matcher.this.match(component)) return true;

                for (Matcher<T> matcher: matchers) {
                    if (matcher.match(component)) return true;
                }
                return false;
            };
        }

        default Matcher<T> and(Matcher<T>... matchers) {
            return component -> {
                if (!Matcher.this.match(component)) return false;

                for (Matcher<T> matcher: matchers) {
                    if (!matcher.match(component)) return false;
                }
                return true;
            };
        }
        default Matcher<T> not() {
            return component -> ! Matcher.this.match(component);
        }
    }


}

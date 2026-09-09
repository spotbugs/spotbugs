import java.util.ArrayList;
import java.util.List;

public class AssertionsWithSideEffects {
    public static int iinc(int x) {
        assert x++ == 0;
        return x;
    }

    public static boolean storeBoolean(boolean b) {
        boolean y = false;
        assert y = b == true;
        return y;
    }

    public static boolean storeInt(int n) {
        boolean y = false;
        assert y = (n == 1);
        return y;
    }

    private static List<String> list = new ArrayList<String>();

    public static void addAndRemove() {
        list.add("a");
        assert list.remove(null);
    }

    public static void addAndCheckEmptiness() {
        list.add("a");
        assert !list.isEmpty();
    }

    // A sample immutable class with an "add" operation
    private static class ImmutableList<Element> {
        Object[] elements;

        public ImmutableList() {
            elements = new Object[0];
        }

        private ImmutableList(Object[] array, Element e) {
            elements = new Object[array.length + 1];
            System.arraycopy(array, 0, elements, 0, array.length);
            elements[array.length] = e;
        }

        // Does not mutate the current object but creates a new one
        public ImmutableList<Element> add(Element e) {
            return new ImmutableList<Element>(elements, e);
        }

        public boolean isEmpty() {
            return elements.length == 0;
        }
    }

    private static ImmutableList<String> il = new ImmutableList<>();

    public static void add() {
        assert !il.add("a").isEmpty();
    }
}

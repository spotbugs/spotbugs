package sfBugs;

public class Bug2824160a implements Comparable<Object> {
    private final String str;

    public Bug2824160a(final String s) {
        str = s;
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }

    public static int test(Bug2824160a b) {
        return b.compareTo("Hello");
    }

}

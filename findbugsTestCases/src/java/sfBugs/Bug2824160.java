package sfBugs;

public class Bug2824160 {
    private final String str;

    public Bug2824160(final String s) {
        str = s;
    }

    public int compareTo(String i) {
        return str.compareTo(i);
    }

    public boolean isEqualToInteger(Integer i) {
        return compareTo(i.toString()) == 0;
    }

    public boolean isEqualToIntegerFalsePositive(Integer i) {
        return str.compareTo(i.toString()) == 0;
    }
}

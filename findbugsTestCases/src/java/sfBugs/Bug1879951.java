package sfBugs;

public class Bug1879951 {
    public int getCount() {
        final int count = 10;
        return count;
    }
}

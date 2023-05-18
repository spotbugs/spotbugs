package findhidingsubclasstest;

public class GoodEmptyIterator {
    private static final GoodEmptyIterator theInstance = new GoodEmptyIterator();
    public static class OfAtomic extends GoodEmptyIterator {
        public final static OfAtomic goodField = new OfAtomic();
    }
}
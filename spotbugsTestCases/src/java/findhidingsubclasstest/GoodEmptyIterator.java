package findhidingsubclasstest;

public class GoodEmptyIterator {
    private static final EmptyIterator theInstance = new EmptyIterator();
    private static class OfAtomic extends GoodEmptyIterator {
        public final static OfAtomic goodField = new OfAtomic();
    }
}